package com.gello94.ble_sample_app

import android.animation.ObjectAnimator
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.le.*
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.convertTo
import androidx.core.util.isEmpty
import kotlinx.android.synthetic.main.activity_ble_data_receiver.*
import java.util.*
import kotlin.system.measureTimeMillis


class bleDataReceiver : AppCompatActivity() {

    val MEASURE_PERIOD: Long = 10000

    var mName = ""
    var mAddress = ""

    var maxMeasure = 0.0

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            MainActivity.mContext!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager =
                MainActivity.mContext!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_data_receiver)

        val measure = findViewById<Button>(R.id.measure)
        val stop = findViewById<Button>(R.id.measureStop)

        val deviceAddressText = findViewById<TextView>(R.id.DeviceAddress)
        val deviceNameText = findViewById<TextView>(R.id.DeviceName)
        val deviceConnectionStatusText = findViewById<TextView>(R.id.deviceConnectionStatus)
        val deviceDataText = findViewById<TextView>(R.id.deviceData)
        val m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Check If Bluetooth is supported by Android Device
        if (m_bluetoothAdapter == null) {
            Toast.makeText(this, "this device doesn't support bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        // Retrieve Device Name and Adddress from Extra Argument passed into the Intent
        mAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)!!
        mName = intent.getStringExtra(MainActivity.EXTRA_NAME)!!

        //Show on Activity Layout the Device Info
        deviceNameText.setText(mName)
        deviceAddressText.setText(mAddress)
        deviceConnectionStatusText.setText("Connecting ...")

        // Start the measure
        measure.setOnClickListener {
            startMeasure()
            maxMeasure = 0.0
        }
        // Stop the measure
        stop.setOnClickListener {
            stopMeasure()
            deviceDataText.setText(maxMeasure.toString().format("%.2f"))
            deviceDataText.setTextColor(Color.parseColor("#779ecb"))
        }
    }

    private fun startMeasure(){
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filter = ScanFilter.Builder()
            .setDeviceName("IF_B7")
            .build()
        var devfilters: MutableList<ScanFilter> = ArrayList()
        devfilters.add(filter)

        if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isDiscovering) {
            bluetoothLeScanner.startScan(devfilters, scanSettings, scanCallback)
        }
    }

    private fun stopMeasure() {
        bluetoothLeScanner!!.stopScan(scanCallback)
    }

    /**
     *
     * A scanCallback to override the onScanResult and manage the found devices as preferred
     *
     */
    private val scanCallback = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceDataText = findViewById<TextView>(R.id.deviceData)
            val measureBar = findViewById<ProgressBar>(R.id.measureBar)

            var divWeight: Double = 0.0

            val indexQuery = MainActivity.scanResults.indexOfFirst { it.device.address == result.device.address }

                Log.d("scan result: ", "Found BLE device! Name: ${result.device.name ?: "Unnamed"}, result: $result")

                    if( result.device.name == "IF_B7") {
                        var manufacturerData = result.scanRecord?.manufacturerSpecificData

                        if (result.scanRecord?.advertiseFlags != -1 && (result.scanRecord?.manufacturerSpecificData?.isEmpty() != true)) {
                            val secondByte = manufacturerData!!.valueAt(0).get(11)
                            val firstByte = manufacturerData!!.valueAt(0).get(10)
                            divWeight =
                                bytesToUnsignedShort(firstByte, secondByte, true).toDouble() / 100
                            if(maxMeasure <= divWeight)
                            {
                                maxMeasure = divWeight
                            }
                        }

                        runOnUiThread(Runnable {
                            deviceDataText.setText(divWeight.toString().format("%.2f"))
                            deviceDataText.setTextColor(Color.parseColor("#808080"))
                            measureBar.setProgress(divWeight.toInt())
                        })
                    }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("scan result: ", "onScanFailed: code $errorCode")
        }
    }

    public fun bytesToUnsignedShort(byte1 : Byte, byte2 : Byte, bigEndian : Boolean) : Int {
        if (bigEndian)
            return (((byte1.toInt() and 255) shl 8) or (byte2.toInt() and 255))

        return (((byte2.toInt() and 255) shl 8) or (byte1.toInt() and 255))
    }
}