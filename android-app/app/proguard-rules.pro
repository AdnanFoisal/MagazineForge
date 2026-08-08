# ============================================================================
# MagazineForge R8 keep rules
#
# Applied on top of proguard-android-optimize.txt (see app/build.gradle.kts).
# Release only — debug does not run R8.
#
# Every rule below names the library it protects and what breaks without it.
# Nothing here is speculative: each entry maps to a dependency that is actually
# declared in app/build.gradle.kts. Libraries NOT in this project and therefore
# deliberately absent from this file: kotlinx-serialization, Moshi, Jackson,
# Room, Dagger/Hilt, WorkManager.
# ============================================================================


# ----------------------------------------------------------------------------
# Global attributes. These matter more than any single -keep rule.
#
# Signature      : preserves generic type information. Retrofit calls
#                  Method.getGenericReturnType() to discover that
#                  `suspend fun getJobStatus(): Response<JobStatusResponse>`
#                  should be parsed as JobStatusResponse. Gson calls
#                  TypeToken.getType() for the same reason on List<T> fields.
#                  Without this, generics erase to Object and BOTH libraries
#                  fail at RUNTIME, not at build time — every API response
#                  silently deserialises to null. This is the single most
#                  important line in this file.
# *Annotation*   : @SerializedName, @GET/@POST/@Path/@Query/@Body/@Header and
#                  Firebase's @PropertyName are all read reflectively at
#                  runtime. Strip them and Gson falls back to field names and
#                  Retrofit cannot build a call at all.
# Exceptions     : keeps checked-exception metadata Retrofit's
#                  ServiceMethod validation inspects.
# InnerClasses / EnclosingMethod : Gson needs these to reject/handle
#                  non-static inner classes correctly, and Kotlin sealed
#                  hierarchies (PageSchema) depend on them.
# SourceFile / LineNumberTable : readable release stack traces. Paired with
#                  -renamesourcefileattribute so the original file name is not
#                  leaked while line numbers stay useful via mapping.txt.
# ----------------------------------------------------------------------------
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# IMPORTANT: after every release build, archive
#   app/build/outputs/mapping/release/mapping.txt
# Without it, a Play Console / Crashlytics stack trace is unreadable garbage.


# ----------------------------------------------------------------------------
# Gson  (com.squareup.retrofit2:converter-gson -> com.google.code.gson)
#
# Gson instantiates model classes reflectively and matches JSON keys to FIELD
# names. R8 renames fields by default, so without these rules every model
# field becomes `a`, `b`, `c` and all JSON parsing returns nulls — with no
# exception thrown. This is the classic "works in debug, silently empty in
# release" R8 failure.
# ----------------------------------------------------------------------------

# Every model class in this project, with every member, unconditionally.
#
# This is deliberately blunt. A narrower rule (keep only @SerializedName
# fields) would be tempting, but many models here rely on Gson's default
# field-name matching with NO annotation at all — UploadAssetResponse.kt,
# JobModels.kt, CompileRequest.kt and VerifyKeyModels.kt contain zero
# @SerializedName annotations (verified by grep). For those, the Kotlin
# property name IS the JSON contract, so it must survive verbatim.
#
# Cost: these are small data classes, so the size penalty is negligible.
# Benefit: no silent null-parsing regression, which is the failure mode that
# cannot be caught by a build and is very hard to diagnose from a bug report.
-keep class com.magazineforge.app.models.** { *; }
-keepclassmembers class com.magazineforge.app.models.** {
    <fields>;
    <init>(...);
}

# Anything annotated @SerializedName, wherever it lives.
-keepclasseswithmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson's own reflective machinery.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep class com.google.gson.stream.** { *; }
-dontwarn com.google.gson.**

# Custom Gson adapters registered by hand in ApiClient.kt. R8 cannot see the
# link from registerTypeAdapter(PageSchema::class.java, PageSchemaDeserializer())
# to these classes' no-arg constructors.
-keep class com.magazineforge.app.network.PageSchemaDeserializer { *; }
-keep class com.magazineforge.app.network.PageSchemaSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * extends com.google.gson.TypeAdapter { *; }

# The PageSchema sealed hierarchy is dispatched on at runtime by the custom
# (de)serialiser via `is ArticleSchema -> ...` and
# `context.deserialize(json, AdSchema::class.java)`. R8 has no way to see
# those Class literals as a keep reason for the subtypes' members.
-keep class com.magazineforge.app.models.PageSchema { *; }
-keep class * extends com.magazineforge.app.models.PageSchema { *; }

# ----------------------------------------------------------------------------
# Retrofit 2.9.0
#
# ApiService is an INTERFACE with no implementation in the source. Retrofit
# builds a java.lang.reflect.Proxy over it at runtime and reads the annotations
# and generic return types off each method. R8 sees an interface whose methods
# are never directly called and strips them.
# ----------------------------------------------------------------------------

# This project's service interface, explicitly.
-keep,allowobfuscation interface com.magazineforge.app.network.ApiService { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Retrofit 2.9.0 ships no consumer rules for these; 2.11+ does. Keeping them
# unconditionally is correct for 2.9.0 and harmless on later versions.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Retrofit reflects on Kotlin `suspend` continuations.
-keep class kotlin.coroutines.Continuation

# Retrofit's platform detection probes for optional classes at runtime.
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# R8 full mode strips generic signatures from non-kept types even when
# -keepattributes Signature is set. This rule restores them for Retrofit's
# Response<T> and Call<T> return types specifically. Without it, R8 full mode
# (the default since AGP 8.x) breaks Retrofit even though Signature is kept.
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation


# ----------------------------------------------------------------------------
# OkHttp 4.x / Okio  (transitive via Retrofit 2.9.0 — not declared directly,
# but ApiClient.kt imports okhttp3.* and okio.BufferedSink explicitly)
#
# OkHttp ships its own consumer rules, so these are mostly -dontwarn for
# optional compile-time-only dependencies (Conscrypt, BouncyCastle, OpenJSSE,
# Animal Sniffer) that OkHttp references but does not require at runtime.
# Without them R8 fails the build on missing classes.
# ----------------------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.internal.publicsuffix.PublicSuffixDatabase { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn okio.**
-keep class okio.** { *; }

# ApiClient.kt subclasses okhttp3.RequestBody anonymously and overrides
# writeTo(BufferedSink) for streaming uploads. Keep the contract.
-keep class * extends okhttp3.RequestBody { *; }
-keepclassmembers class * extends okhttp3.RequestBody {
    public void writeTo(okio.BufferedSink);
    public okhttp3.MediaType contentType();
}


# ----------------------------------------------------------------------------
# Kotlin runtime, metadata and coroutines
#
# kotlin.Metadata is read by Gson's Kotlin support and by Retrofit's suspend
# handling. The coroutines internals below are touched via reflection by the
# atomic field updaters in kotlinx-coroutines' internal scheduler.
# ----------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# ServiceLoader-based coroutine dispatcher discovery.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembers class kotlin.coroutines.SafeContinuation { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler

# Coroutines' AtomicReferenceFieldUpdater usage.
-keepclassmembers class kotlinx.coroutines.internal.LockFreeLinkedListNode {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.internal.DispatchedContinuation {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# androidx.lifecycle ViewModel  (lifecycle-viewmodel-compose 2.7.0)
#
# MainActivity.kt:90 does ViewModelProvider(this)[EditorViewModel::class.java].
# ViewModelProvider.NewInstanceFactory instantiates via
# modelClass.getDeclaredConstructor().newInstance() — pure reflection. The class
# literal keeps the CLASS, but nothing references EditorViewModel's implicit
# no-arg constructor, so R8 is free to remove it. Result would be a
# RuntimeException("Cannot create an instance of class EditorViewModel") on
# first launch of the release build.
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
    <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }


# ----------------------------------------------------------------------------
# Jetpack Compose  (BOM 2024.02.00, compiler extension 1.5.4)
#
# Compose is largely R8-safe: the AndroidX artifacts ship consumer rules and
# the compiler plugin generates code R8 understands. Deliberately NOT added
# here: a blanket `-keepclassmembers class * { @Composable <methods>; }`. That
# rule is common in copy-pasted configs but keeps every composable in the app,
# which defeats most of the shrinking R8 was enabled for, and Compose does not
# need it.
# ----------------------------------------------------------------------------
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Compose's ServiceLoader lookup for Snapshot observers.
-keepnames class androidx.compose.runtime.snapshots.SnapshotKt

# ui-text-google-fonts (1.6.2): Type.kt:15 passes
# R.array.com_google_android_gms_fonts_certs to GoogleFont.Provider, which
# hands it to the Play Services font ContentProvider. The R reference keeps
# the resource itself; these keep the classes that read it.
-keep class androidx.compose.ui.text.googlefonts.** { *; }
-keep class androidx.core.provider.** { *; }

# @Preview-annotated composables and the tooling that scans for them. Harmless
# in release (the tooling artifact is debugImplementation only) but prevents a
# missing-class warning from failing the build.
-dontwarn androidx.compose.ui.tooling.**

# core-splashscreen 1.0.1 — themes.xml references Theme.SplashScreen and
# postSplashScreenTheme by name, which R8 cannot see.
-keep class androidx.core.splashscreen.** { *; }


# ----------------------------------------------------------------------------
# Firebase  (BOM 32.7.4: analytics, storage, firestore, auth)
# + play-services-auth 20.7.0
#
# CONFIRMED PRESENT — verified against app/build.gradle.kts lines 93-99, not
# assumed. Firestore is the reflection-heavy one: ShowcaseRepository.kt calls
# snapshot.toObjects(ShowcaseItem::class.java) and documentRef.set(newItem),
# both of which map Kotlin properties to Firestore fields by REFLECTED NAME.
# Rename those fields and every showcase document silently round-trips as
# empty, exactly like the Gson failure mode above.
# ----------------------------------------------------------------------------

# Firestore POJO mapping. ShowcaseItem lives under models/ so it is already
# covered by the models rule, but state the requirement explicitly: Firestore
# needs a public no-arg constructor plus real getter/setter names.
-keep class com.magazineforge.app.models.ShowcaseItem { *; }
-keepclassmembers class com.magazineforge.app.models.ShowcaseItem {
    <init>();
    <fields>;
    public *** get*();
    public void set*(***);
}
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.firestore.**

# Firebase core / analytics / storage / auth. These register components through
# a ComponentDiscoveryService declared in the merged manifest and instantiate
# them reflectively by class name — R8 sees no call site.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.components.ComponentRegistrar { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }

# Firebase/GMS Tasks bridged into coroutines by kotlinx-coroutines-play-services
# 1.7.3 (ShowcaseRepository.kt uses `.await()`).
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn kotlinx.coroutines.tasks.**

# Firebase Auth + play-services-auth (Google Sign-In) reflect over these.
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.firebase.auth.** { *; }

# javax.annotation.concurrent.GuardedBy etc. referenced by Firestore's gRPC
# stack but not present on Android.
-dontwarn javax.naming.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn io.grpc.**


# ----------------------------------------------------------------------------
# Coil 2.5.0 (coil-compose)
#
# Coil ships its own consumer rules. The only project-specific concern is that
# MainActivity.kt:86 and MagazineForgeApp.kt:11 hand Coil ApiClient.okHttpClient
# via ImageLoader.Builder().okHttpClient(...), so Coil's OkHttp integration must
# survive — it is covered by the OkHttp rules above.
# ----------------------------------------------------------------------------
-dontwarn coil.**
-keep class coil.util.** { *; }


# ----------------------------------------------------------------------------
# androidx.security:security-crypto-ktx 1.1.0-alpha06  (used by SecureStorage.kt)
#
# Tink, the crypto backend, registers key managers reflectively by class name
# and parses protobuf key templates. Renaming them throws
# GeneralSecurityException on the first EncryptedSharedPreferences access —
# which in this app would break theme + LiteLLM credential persistence.
# ----------------------------------------------------------------------------
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.protobuf.**


# ----------------------------------------------------------------------------
# Android framework surfaces this app touches
# ----------------------------------------------------------------------------

# NOTE: no rule for android.graphics.pdf.PdfRenderer (PdfViewerScreen.kt) or any
# other android.* framework type. Those live on the boot classpath, are never
# packaged into the APK, and are therefore never shrunk or renamed. A keep rule
# for them would just produce an "unresolved class" note.

# BuildConfig carries HF_TOKEN and the XOR'd APP_KEY arrays, read by
# ApiClient.kt. Keep the fields so R8's constant propagation cannot inline and
# then discard the arrays.
-keep class com.magazineforge.app.BuildConfig { *; }
# `<fields>` is the wildcard for "any field". The previous form here was
# `public static final ***;`, which is a syntax error: `***` is a TYPE wildcard,
# so that line declared a type with no field name after it and R8 rejected the
# whole configuration with "Expected field or method name". Access modifiers are
# legal in front of `<fields>`, so this keeps exactly what was intended:
# HF_TOKEN (String) plus APP_KEY_X / APP_KEY_PAD (int[]).
-keepclassmembers class com.magazineforge.app.BuildConfig {
    public static final <fields>;
}

# Enum values() / valueOf() are called reflectively by both Gson and the
# framework's Parcelable machinery.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable CREATOR fields.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Native method bindings, if any dependency adds them.
-keepclasseswithmembernames class * {
    native <methods>;
}


# ----------------------------------------------------------------------------
# Diagnostics
#
# -printusage / -printseeds are commented out because they add build time and
# clutter. Uncomment when debugging a "works in debug, broken in release" bug:
# usage.txt tells you exactly what R8 removed.
# ----------------------------------------------------------------------------
# -printusage   build/outputs/mapping/release/usage.txt
# -printseeds   build/outputs/mapping/release/seeds.txt
# -printmapping build/outputs/mapping/release/mapping.txt

# Strip Log.v/Log.d from release builds. Safe with
# proguard-android-optimize.txt (which enables optimisation passes) and stops
# debug logging from leaking request details into logcat on user devices.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}


