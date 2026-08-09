package com.magazineforge.app.network

import com.magazineforge.app.models.JobStatusResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ApiService {
    @Multipart
    @POST("upload-asset")
    suspend fun uploadAsset(
        @Part file: MultipartBody.Part
    ): Response<com.magazineforge.app.models.UploadAssetResponse>

    @Multipart
    @POST("upload-asset-fast")
    suspend fun uploadAssetFast(
        @Part file: MultipartBody.Part,
        @Query("quality") quality: String = "standard"
    ): Response<com.magazineforge.app.models.UploadAssetResponse>

    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>

    @POST("verify-key")
    suspend fun verifyKey(
        @Body request: com.magazineforge.app.models.VerifyKeyRequest
    ): Response<com.magazineforge.app.models.VerifyKeyResponse>

    // Checks the user's own stock-photo keys, one small live search per
    // provider. No LiteLLM headers: the backend handler takes only a body.
    // Once saved, the keys ride every generation request as X-Pixabay-Key /
    // X-Pexels-Key, attached by ApiClient's host-gated interceptor.
    @POST("verify-image-keys")
    suspend fun verifyImageKeys(
        @Body request: com.magazineforge.app.models.VerifyImageKeysRequest
    ): Response<com.magazineforge.app.models.VerifyImageKeysResponse>

    @GET("job/{job_id}/status")
    suspend fun getJobStatus(
        @Path("job_id") jobId: String
    ): Response<JobStatusResponse>

    @GET("job/{job_id}/download")
    suspend fun downloadJob(
        @Path("job_id") jobId: String
    ): Response<ResponseBody>

    @GET("job/{job_id}/cover")
    suspend fun downloadCover(
        @Path("job_id") jobId: String
    ): Response<ResponseBody>

    @POST("extract-contract")
    suspend fun extractContract(
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String,
        @Body request: com.magazineforge.app.models.ExtractContractRequest
    ): Response<com.magazineforge.app.models.ExtractContractResponse>

    @POST("generate-brief")
    suspend fun generateBrief(
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String,
        @Body request: com.magazineforge.app.models.GenerateBriefRequest
    ): Response<com.magazineforge.app.models.GenerateBriefResponse>

    @POST("generate-schema")
    suspend fun generateSchema(
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String,
        @Body request: com.magazineforge.app.models.GenerateSchemaRequest
    ): Response<com.magazineforge.app.models.MagazineSchema>

    @POST("generation-runs")
    suspend fun createGenerationRun(
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String,
        @Body request: com.magazineforge.app.models.GenerationRunRequest,
        @Query("generate_all") generateAll: Boolean = false
    ): Response<com.magazineforge.app.models.GenerationRunCreateResponse>

    @GET("generation-runs/{run_id}")
    suspend fun getGenerationRun(
        @Path("run_id") runId: String
    ): Response<com.magazineforge.app.models.GenerationRunStatus>

    @POST("generation-runs/{run_id}/continue")
    suspend fun continueGenerationRun(
        @Path("run_id") runId: String,
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String
    ): Response<com.magazineforge.app.models.GenerationRunCreateResponse>

    @POST("generation-runs/{run_id}/retry/{section_id}")
    suspend fun retryGenerationSection(
        @Path("run_id") runId: String,
        @Path("section_id") sectionId: Int,
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String
    ): Response<com.magazineforge.app.models.GenerationRunCreateResponse>

    // POST /generation-runs/{run_id}/cancel -> 202 {"runId": ..., "status": "CANCELLED"}
    // The backend handler takes no Request and never calls get_api_keys, so —
    // unlike /continue and /retry — this endpoint sends no LiteLLM headers.
    @POST("generation-runs/{run_id}/cancel")
    suspend fun cancelGenerationRun(
        @Path("run_id") runId: String
    ): Response<com.magazineforge.app.models.GenerationRunCreateResponse>

    @POST("generate-latex")
    suspend fun generateLatex(
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String,
        @Body request: com.magazineforge.app.models.GenerateLatexRequest
    ): Response<com.magazineforge.app.models.GenerateLatexResponse>


    @POST("render-page")
    suspend fun renderPage(@Body request: com.magazineforge.app.models.RenderPageRequest): Response<ResponseBody>

    @POST("compile-raw")
    suspend fun compileRaw(
        @Body request: com.magazineforge.app.models.CompileRawRequest
    ): Response<com.magazineforge.app.models.CompileRawResponse>

    @POST("rewrite-selection")
    suspend fun rewriteSelection(
        @Header("X-LiteLLM-Url") litellmUrl: String,
        @Header("X-LiteLLM-Key") litellmKey: String,
        @Body request: com.magazineforge.app.models.RewriteSelectionRequest
    ): Response<com.magazineforge.app.models.RewriteSelectionResponse>
}
