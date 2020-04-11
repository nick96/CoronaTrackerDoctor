package com.example.contactgraph

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.*

private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

class MainActivity : AppCompatActivity() {
    private val sha256 = MessageDigest.getInstance("SHA-256")

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            Log.d("handler", "Received message: $msg")
            when (msg.what) {
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                    val view = findViewById<View>(android.R.id.content)
                    Snackbar.make(view, "Received message $readMessage", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
    private val mBluetoothAdatapter = BluetoothAdapter.getDefaultAdapter()

    private var mBluetoothService: BluetoothService? = null

    override fun onResume() {
        super.onResume()
        Log.d("resume", "Resume!")
        if (mBluetoothService != null) {
            Log.d("bluetooth", "BT service is not null")
            if (mBluetoothService!!.threadState == BluetoothService.STATE_NONE) {
                Log.d("bluetooth", "Starting bluetooth service")
                mBluetoothService!!.start()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (mBluetoothAdatapter == null) {
            return
        }

        if (!mBluetoothAdatapter.isEnabled) {
            Log.d("bluetooth", "BT is not enabling, requesting...")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, 3 /* Enable BT */)
        } else {
            Log.d("bluetooth", "BT is already enabled! Creating bluetooth service")
            mBluetoothService = BluetoothService(mHandler)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("activityResult", "Received activity result for request $requestCode")
        if (requestCode == 3) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d("bluetooth", "Creating bluetooth service")
                mBluetoothService = BluetoothService(mHandler)
            } else {
                Log.d("bluetooth" , "BT not enabled")
                Toast.makeText(this, "Bluetooth was not enabled", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
//        super.onActivityResult(requestCode, resultCode, data)
    }

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
            call.enqueue(object : Callback<Unit> {
                override fun onFailure(call: Call<Unit>, t: Throwable) {
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
                    call: Call<Unit>,
                    response: Response<Unit>
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
            call.enqueue(object : Callback<Unit> {
                override fun onFailure(call: Call<Unit>, t: Throwable) {
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
                    call: Call<Unit>,
                    response: Response<Unit>
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


