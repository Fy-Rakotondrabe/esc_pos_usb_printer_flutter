package com.example.print

import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.os.Bundle
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.usb.UsbConnection
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.example.print.async.AsyncEscPosPrint.OnPrintFinished
import com.example.print.async.AsyncEscPosPrinter
import com.example.print.async.AsyncUsbEscPosPrint
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : FlutterActivity() {
    private val CHANNEL = "print_channel"
    private var toPrint = ""
    private var methodChannel: MethodChannel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (flutterEngine != null) {
            methodChannel = MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL)
            methodChannel?.setMethodCallHandler { call, result ->
                methodChannel?.let { channel ->
                    if (call.method == "printUsb") {
                        val contentToPrint = call.arguments as String
                        toPrint = contentToPrint
                        printUsb() // Call your printing method with the content
                        result.success("Print job initiated") // You can send a success message back to Flutter
                    } else {
                        result.notImplemented() // Handle other method calls
                    }
                }
            }
        } else {
            Log.i("FlutterEngine", "Flutter engine is null")
        }
    }
    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/
    companion object {
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val usbManager = getSystemService(USB_SERVICE) as UsbManager
                    val usbDevice =
                        intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            AsyncUsbEscPosPrint(
                                context,
                                methodChannel!!,
                                object : OnPrintFinished() {
                                    override fun onError(
                                        asyncEscPosPrinter: AsyncEscPosPrinter?,
                                        codeException: Int
                                    ) {
                                        Log.e(
                                            "Async.OnPrintFinished",
                                            "AsyncEscPosPrint.OnPrintFinished : An error occurred !"
                                        )
                                    }

                                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter?) {
                                        Log.i(
                                            "Async.OnPrintFinished",
                                            "AsyncEscPosPrint.OnPrintFinished : Print is finished !"
                                        )
                                    }
                                },
                            )
                                .execute(
                                    getAsyncEscPosPrinter(
                                        UsbConnection(
                                            usbManager,
                                            usbDevice
                                        )
                                    )
                                )
                        }
                    }
                }
            }
        }
    }

    fun printUsb() {
        val usbConnection = UsbPrintersConnections.selectFirstConnected(this)
        val usbManager = this.getSystemService(USB_SERVICE) as UsbManager
        if (usbConnection == null || usbManager == null) {
            AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB printer found.")
                .show()
            return
        }
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
        usbManager.requestPermission(usbConnection.device, permissionIntent)
    }

    /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    fun getAsyncEscPosPrinter(printerConnection: DeviceConnection?): AsyncEscPosPrinter {
        val format = SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss")
        val printer = AsyncEscPosPrinter(printerConnection!!, 203, 48f, 32)
        return printer.addTextToPrint(

                /*[C]<img>${
                PrinterTextParserImg.bitmapToHexadecimalString(
                    printer,
                    this.applicationContext.resources.getDrawableForDensity(
                        R.drawable.logo,
                        DisplayMetrics.DENSITY_MEDIUM
                    )
                )
            }</img>*/
            toPrint.trimIndent()
        )
    }
}
