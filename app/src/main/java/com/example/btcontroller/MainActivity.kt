package com.example.btcontroller

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


const val MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1
const val REQUEST_ENABLE_BT = 2
const val REQ_CODE_SPEECH_INPUT = 3

data class BluetoothDeviceData(val name: String, val macAddress: String)

class MainActivity : AppCompatActivity() {
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getData.setOnClickListener {
            getVoiceCommand()
        }
        connectToArduino.setOnClickListener {
            try {
//                init()
//                getData.visibility = View.VISIBLE
            } catch (e: IOException) {
                Toast.makeText(
                    this,
                    "You should first connect to your bluetooth devices",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getVoiceCommand() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa")
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                "speech not supported",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION
                )

            }
        } else {
            enableBluetooth()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == MY_PERMISSIONS_ACCESS_FINE_LOCATION) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                enableBluetooth()
            }
        }
    }

    private lateinit var outputStream: OutputStream
    private lateinit var inStream: InputStream

    @Throws(IOException::class)
    private fun init() {
        if (bluetoothAdapter?.isEnabled == true) {
            val bondedDevices = bluetoothAdapter!!.bondedDevices
            if (bondedDevices.size > 0) {
                val devices = bondedDevices.toTypedArray() as Array<*>
                val device = devices[0] as BluetoothDevice
                val uuids = device.uuids
                val socket = device.createRfcommSocketToServiceRecord(uuids[0].uuid)
                socket.connect()
                outputStream = socket.outputStream
                inStream = socket.inputStream
            }
            Log.e("error", "No appropriate paired devices.")
        } else {
            Log.e("error", "Bluetooth is disabled.")
        }
    }

    @Throws(IOException::class)
    fun  write(s: String) {
        outputStream.write(s.toByteArray())
    }

    fun run() {
        val BUFFER_SIZE = 1024
        val buffer = ByteArray(BUFFER_SIZE)
        var bytes = 0
        val b = BUFFER_SIZE
        while (true) {
            try {
                bytes = inStream.read(buffer, bytes, BUFFER_SIZE - bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_LONG).show()
                } else {
                    finishAfterTransition()
                }
            }
            REQ_CODE_SPEECH_INPUT -> {
                if (resultCode == Activity.RESULT_OK && null != data) {
                    val result =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.let {
                            command.text = """
                                your voice command is:
                                
                                $it
                            """.trimIndent()
//                            write("0")
                        }

                }
            }
        }


    }

    private fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
}