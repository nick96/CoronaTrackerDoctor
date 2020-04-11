package com.example.contactgraph

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class PositiveRequest(val key: String, val checksum: String, val hash: String)
data class PositiveResponse(val keys: List<String>)
data class ContactRequest(val key: String, val checksum: String, val hash: String)
data class ContactResponse(val keys: List<String>)
data class ErrorObject(val code: Int, val title: String, val details: Any)
data class ErrorResponse(val error: ErrorObject)
interface ContactGraphService {
    @GET("/positive")
    fun getPositives(): Call<PositiveResponse>

    @POST("/positive")
    fun addPositive(@Body positiveRequest: PositiveRequest): Call<PositiveResponse>

    @GET("/contact")
    fun getContacts(): Call<ContactResponse>

    @POST("/contact")
    fun addContact(@Body contactRequest: ContactRequest): Call<ContactResponse>
}

fun parseError(retrofit: Retrofit, response: Response<*>): ErrorResponse {
    val converter: Converter<ResponseBody, ErrorResponse> = retrofit
        .responseBodyConverter(
            ErrorResponse::class.java,
            arrayOfNulls<Annotation>(0)
        )
    try {
        return converter.convert(response.errorBody()!!)!!
    } catch (e: Exception) {
        throw Exception("Failed to parse json $response")
    }

}