package com.magazineforge.app.network

import com.magazineforge.app.models.JobRequest
import com.magazineforge.app.models.JobResponse
import com.magazineforge.app.models.JobStatusResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>

    @POST("verify-key")
    suspend fun verifyKey(
        @Body request: com.magazineforge.app.models.VerifyKeyRequest
    ): Response<com.magazineforge.app.models.VerifyKeyResponse>

    @POST("job")
    suspend fun createJob(
        @Header("Authorization") geminiKey: String,
        @Body request: JobRequest
    ): Response<JobResponse>

    @GET("job/{job_id}/status")
    suspend fun getJobStatus(
        @Path("job_id") jobId: String
    ): Response<JobStatusResponse>

    @GET("job/{job_id}/download")
    suspend fun downloadJob(
        @Path("job_id") jobId: String
    ): Response<ResponseBody>

    @POST("generate-schema")
    suspend fun generateSchema(
        @Header("Authorization") geminiKey: String,
        @Body request: com.magazineforge.app.models.GenerateSchemaRequest
    ): Response<com.magazineforge.app.models.MagazineSchema>

    @POST("generate-latex")
    suspend fun generateLatex(
        @Header("Authorization") geminiKey: String,
        @Body request: com.magazineforge.app.models.GenerateLatexRequest
    ): Response<com.magazineforge.app.models.GenerateLatexResponse>

    @POST("compile-raw")
    suspend fun compileRaw(
        @Body request: com.magazineforge.app.models.CompileRawRequest
    ): Response<com.magazineforge.app.models.CompileRawResponse>
}
