package com.magazineforge.app.network

import com.magazineforge.app.models.JobStatusResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

interface ApiService {
    @Multipart
    @POST("upload-asset")
    suspend fun uploadAsset(
        @Part file: MultipartBody.Part
    ): Response<com.magazineforge.app.models.UploadAssetResponse>
    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>

    @POST("verify-key")
    suspend fun verifyKey(
        @Body request: com.magazineforge.app.models.VerifyKeyRequest
    ): Response<com.magazineforge.app.models.VerifyKeyResponse>

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

    @POST("generate-brief")
    suspend fun generateBrief(
        @Header("X-Gemini-Key") geminiKey: String,
        @Body request: com.magazineforge.app.models.GenerateBriefRequest
    ): Response<com.magazineforge.app.models.GenerateBriefResponse>

    @POST("generate-schema")
    suspend fun generateSchema(
        @Header("X-Gemini-Key") geminiKey: String,
        @Body request: com.magazineforge.app.models.GenerateSchemaRequest
    ): Response<com.magazineforge.app.models.MagazineSchema>

    @POST("generate-latex")
    suspend fun generateLatex(
        @Header("X-Gemini-Key") geminiKey: String,
        @Body request: com.magazineforge.app.models.GenerateLatexRequest
    ): Response<com.magazineforge.app.models.GenerateLatexResponse>


    @POST("compile-raw")
    @POST(" render-page\)
 suspend fun renderPage(@Body request: com.magazineforge.app.models.RenderPageRequest): Response<ResponseBody>

 suspend fun compileRaw(
        @Body request: com.magazineforge.app.models.CompileRawRequest
    ): Response<com.magazineforge.app.models.CompileRawResponse>

    @POST("rewrite-selection")
    suspend fun rewriteSelection(
        @Header("X-Gemini-Key") geminiKey: String,
        @Body request: com.magazineforge.app.models.RewriteSelectionRequest
    ): Response<com.magazineforge.app.models.RewriteSelectionResponse>
}
