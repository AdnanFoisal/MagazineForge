import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.magazineforge.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.magazineforge.app"
        minSdk = 33
        targetSdk = 33
        versionCode = 2
        versionName = "2.0.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val hfToken = localProperties.getProperty("HF_TOKEN") ?: ""
        buildConfigField("String", "HF_TOKEN", "\"$hfToken\"")

        // ── App key for the Cloudflare Worker proxy ────────────────────────
        // Set APP_KEY in local.properties (gitignored) to the value you passed
        // to `wrangler secret put APP_KEY`. See cloudflare-worker/README.md.
        //
        // Rather than emitting a plain `static final String` — which lands in
        // the DEX string constant pool and is recoverable with a single
        // `strings | grep` — the key is XOR'd against a fixed pad and emitted
        // as an int[]. Array initialisers compile to bytecode in <clinit>, not
        // to a pooled string constant, so the key is not greppable and does
        // not survive a naive `strings` dump. ApiClient.deobfuscateAppKey()
        // reverses it at runtime.
        //
        // This is a speed bump, NOT a secret store. Anyone running jadx can
        // read the pad and the array and recover the key in a few minutes.
        // The real protection is that this key is worthless on its own: it
        // only unlocks the Worker proxy, is rotatable server-side, and — most
        // importantly — is NOT the HuggingFace token.
        //
        // Defaults to an empty key when local.properties has no APP_KEY entry,
        // so a build with no secrets configured still succeeds (mirrors the
        // `?: ""` fallback used for HF_TOKEN above).
        val appKey = localProperties.getProperty("APP_KEY") ?: ""
        val xorPad = intArrayOf(0x5A, 0xC3, 0x1F, 0x76, 0xE2, 0x8B, 0x4D, 0x39)
        val obfuscated = appKey.toByteArray(Charsets.UTF_8)
            .mapIndexed { i, b -> (b.toInt() and 0xFF) xor xorPad[i % xorPad.size] }
        buildConfigField("int[]", "APP_KEY_X", obfuscated.joinToString(",", "{", "}"))
        buildConfigField("int[]", "APP_KEY_PAD", xorPad.joinToString(",", "{", "}"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = "android"
            keyAlias = "magboy"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            // R8 full-mode shrink + obfuscate, and strip unused resources.
            // Keep rules live in proguard-rules.pro — this project reflects
            // heavily (Gson, Retrofit, Firebase), so those rules are load
            // bearing, not decorative.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // debug is intentionally left at the AGP defaults (isMinifyEnabled =
        // false). `assembleDebug` must stay fast and un-obfuscated so stack
        // traces from the build agent remain readable.
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.2")
    
    // Retrofit for Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Jetpack Security for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
