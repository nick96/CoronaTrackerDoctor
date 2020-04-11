package com.example.contactgraph

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.*

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


