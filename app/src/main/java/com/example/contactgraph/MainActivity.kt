package com.example.contactgraph

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import okhttp3.ResponseBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.lang.Exception
import java.security.MessageDigest
import java.util.*
import kotlin.collections.HashMap

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

class MainActivity : AppCompatActivity() {
    private val sha256 = MessageDigest.getInstance("SHA-256")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getPreferences(Context.MODE_PRIVATE)
        var key = prefs.getString("key", null)
        if (key == null) {
            key = UUID.randomUUID().toString()
            with(prefs.edit()) {
                putString("key", key)
                commit()
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://contact-graph.herokuapp.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ContactGraphService::class.java)

        val positiveButton = findViewById<Button>(R.id.positive)
        positiveButton.setOnClickListener { v ->
            val checksum = sha256.digest(key.toByteArray()).toHexString()
            val hash = sha256.digest((key + checksum).toByteArray()).toHexString()
            val call = service.addPositive(PositiveRequest(key, checksum, hash))
            call.enqueue(object : Callback<PositiveResponse> {
                override fun onFailure(call: Call<PositiveResponse>, t: Throwable) {
                    Log.e("positive", "Failed to send createPositive request: $t")
                    val snack = Snackbar.make(
                        v,
                        "Failed to send notification. Please try again later",
                        Snackbar.LENGTH_SHORT
                    )
                    snack.setAction("DISMISS") {}
                    snack.show()
                }

                override fun onResponse(
                    call: Call<PositiveResponse>,
                    response: Response<PositiveResponse>
                ) {
                    val snack = when {
                        response.code() in 200..299 -> Snackbar.make(
                            v,
                            "An anonymous notification of your positive test has been sent out",
                            Snackbar.LENGTH_SHORT
                        )
                        response.code() in 400..499 -> {
                            val error = parseError(retrofit, response)
                            if (error.error.title == "IntegrityError") {
                                Snackbar.make(
                                    v,
                                    "You've already sent notification of a positive test. Thank you!",
                                    Snackbar.LENGTH_SHORT
                                )
                            } else {
                                Log.i("contact", "There was a client side error: $error")
                                Snackbar.make(
                                    v,
                                    "There was a problem sending the positive test notification. Please contact the developer",
                                    Snackbar.LENGTH_SHORT
                                )
                            }
                        }
                        else -> {
                            val error = parseError(retrofit, response)
                            Log.i("contact", "There was a server side error: $error")
                            Snackbar.make(
                                v,
                                "There was a problem with the server. Please try again later",
                                Snackbar.LENGTH_SHORT
                            )
                        }
                    }
                    snack.setAction("DISMISS") {}
                    snack.show()
                }

            })
        }

        val contactButton = findViewById<Button>(R.id.contact)
        contactButton.setOnClickListener { v ->
            val checksum = sha256.digest(key.toByteArray()).toHexString()
            val hash = sha256.digest((key + checksum).toByteArray()).toHexString()
            val call = service.addContact(ContactRequest(key, checksum, hash))
            call.enqueue(object : Callback<ContactResponse> {
                override fun onFailure(call: Call<ContactResponse>, t: Throwable) {
                    Log.e("contact", "Failed to send createContact request: $t")
                    val snack = Snackbar.make(
                        v,
                        "An anonymous notification of your positive test has been sent out",
                        Snackbar.LENGTH_SHORT
                    )
                    snack.setAction("DISMISS") {}
                    snack.show()
                }

                override fun onResponse(
                    call: Call<ContactResponse>,
                    response: Response<ContactResponse>
                ) {
                    val snack = when {
                        response.code() in 200..299 -> Snackbar.make(
                            v,
                            "An anonymous notification of your contact with positive test has been sent out",
                            Snackbar.LENGTH_SHORT
                        )
                        response.code() in 400..499 -> {
                            val error = parseError(retrofit, response)
                            if (error.error.title == "IntegrityError") {
                                Snackbar.make(
                                    v,
                                    "You've already sent notification of contact with a positive test. Thank you!",
                                    Snackbar.LENGTH_SHORT
                                )
                            } else {
                                Log.i("contact", "There was a client side error: $error")
                                Snackbar.make(
                                    v,
                                    "There was a problem sending contact with positive test notification. Please contact the developer",
                                    Snackbar.LENGTH_SHORT
                                )
                            }
                        }
                        else -> {
                            val error = parseError(retrofit, response)
                            Log.i("contact", "There was a server side error: $error")
                            Snackbar.make(
                                v,
                                "There was a problem with the server. Please try again later",
                                Snackbar.LENGTH_SHORT
                            )
                        }
                    }
                    snack.setAction("DISMISS") {}
                    snack.show()
                }

            })
        }
    }
}

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