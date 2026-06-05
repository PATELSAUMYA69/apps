package com.example.network

import com.example.data.Transaction
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class SyncRequest(
    val transactions: List<Transaction>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val syncedIds: List<Int>
)

interface SyncApi {
    @POST("sync_transactions")
    suspend fun syncTransactions(@Body request: SyncRequest): Response<SyncResponse>
}
