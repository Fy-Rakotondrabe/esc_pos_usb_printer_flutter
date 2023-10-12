package com.example.print.async

import android.content.Context
import android.os.AsyncTask
import com.dantsu.escposprinter.EscPosCharsetEncoding
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.example.print.async.AsyncEscPosPrint.PrinterStatus
import java.lang.ref.WeakReference
import io.flutter.plugin.common.MethodChannel

abstract class AsyncEscPosPrint @JvmOverloads constructor(
    context: Context,
    onPrintFinished: OnPrintFinished? = null,
    methodChannel: MethodChannel,
) : AsyncTask<AsyncEscPosPrinter?, Int?, PrinterStatus>() {
    protected var weakContext: WeakReference<Context>
    protected var onPrintFinished: OnPrintFinished?
    protected var methodChannel: MethodChannel?

    init {
        weakContext = WeakReference(context)
        this.onPrintFinished = onPrintFinished
        this.methodChannel = methodChannel
    }

    override fun doInBackground(vararg printersData: AsyncEscPosPrinter?): PrinterStatus? {
        if (printersData.size == 0) {
            return PrinterStatus(null, FINISH_NO_PRINTER)
        }
        if (printersData != null) {
            publishProgress(PROGRESS_CONNECTING)
            val printerData = printersData[0]
            try {
                val deviceConnection = printerData?.printerConnection
                    ?: return PrinterStatus(null, FINISH_NO_PRINTER)
                val printer = EscPosPrinter(
                    deviceConnection,
                    printerData.printerDpi,
                    printerData.printerWidthMM,
                    printerData.printerNbrCharactersPerLine,
                    EscPosCharsetEncoding("windows-1252", 16)
                )

                // printer.useEscAsteriskCommand(true);
                publishProgress(PROGRESS_PRINTING)
                val textsToPrint = printerData?.textsToPrint ?: arrayOf();
                for (textToPrint in textsToPrint) {
                    printer.printFormattedTextAndCut(textToPrint)
                    Thread.sleep(500)
                }
                publishProgress(PROGRESS_PRINTED)
            } catch (e: EscPosConnectionException) {
                e.printStackTrace()
                return PrinterStatus(printerData, FINISH_PRINTER_DISCONNECTED)
            } catch (e: EscPosParserException) {
                e.printStackTrace()
                return PrinterStatus(printerData, FINISH_PARSER_ERROR)
            } catch (e: EscPosEncodingException) {
                e.printStackTrace()
                return PrinterStatus(printerData, FINISH_ENCODING_ERROR)
            } catch (e: EscPosBarcodeException) {
                e.printStackTrace()
                return PrinterStatus(printerData, FINISH_BARCODE_ERROR)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return PrinterStatus(printerData, FINISH_SUCCESS)
        }
        return null;
    }

    override fun onPreExecute() {
        val context = weakContext.get() ?: return
        methodChannel?.invokeMethod("printProgress", "Printing in progress")
    }

    protected override fun onProgressUpdate(vararg progress: Int?) {
        val progressValue = progress[0] ?: 0  // Use 0 as the default if progress[0] is null

        when (progressValue) {
            PROGRESS_CONNECTING -> methodChannel?.invokeMethod("printProgress", "Connecting printer...")
            PROGRESS_CONNECTED -> methodChannel?.invokeMethod("printProgress", "Printer is connected...")
            PROGRESS_PRINTING -> methodChannel?.invokeMethod("printProgress", "Printer is printing...")
            PROGRESS_PRINTED -> methodChannel?.invokeMethod("printProgress", "Printer has finished...")
        }
    }


    override fun onPostExecute(result: PrinterStatus) {
        val context = weakContext.get() ?: return

        val printerStatusMessage = when (result.printerStatus) {
            FINISH_SUCCESS -> "Success"
            FINISH_NO_PRINTER -> "No printer"
            FINISH_PRINTER_DISCONNECTED -> "Broken connection"
            FINISH_PARSER_ERROR -> "Invalid formatted text"
            FINISH_ENCODING_ERROR -> "Bad selected encoding"
            FINISH_BARCODE_ERROR -> "Invalid barcode"
            else -> "Unknown error"
        }

        val resultMessage = when (result.printerStatus) {
            FINISH_SUCCESS -> "Congratulation! The texts are printed!"
            FINISH_NO_PRINTER -> "The application can't find any printer connected."
            FINISH_PRINTER_DISCONNECTED -> "Unable to connect the printer."
            FINISH_PARSER_ERROR -> "It seems to be an invalid syntax problem."
            FINISH_ENCODING_ERROR -> "The selected encoding character returning an error."
            FINISH_BARCODE_ERROR -> "Data sent to be converted to barcode or QR code seems to be invalid."
            else -> ""
        }

        methodChannel?.invokeMethod("printResult", mapOf("status" to printerStatusMessage, "message" to resultMessage))

        if (onPrintFinished != null) {
            if (result.printerStatus == FINISH_SUCCESS) {
                onPrintFinished?.onSuccess(result.asyncEscPosPrinter)
            } else {
                onPrintFinished?.onError(result.asyncEscPosPrinter, result.printerStatus)
            }
        }
    }

    class PrinterStatus(val asyncEscPosPrinter: AsyncEscPosPrinter?, val printerStatus: Int)
    abstract class OnPrintFinished {
        abstract fun onError(asyncEscPosPrinter: AsyncEscPosPrinter?, codeException: Int)
        abstract fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter?)
    }

    companion object {
        const val FINISH_SUCCESS = 1
        const val FINISH_NO_PRINTER = 2
        const val FINISH_PRINTER_DISCONNECTED = 3
        const val FINISH_PARSER_ERROR = 4
        const val FINISH_ENCODING_ERROR = 5
        const val FINISH_BARCODE_ERROR = 6
        protected const val PROGRESS_CONNECTING = 1
        protected const val PROGRESS_CONNECTED = 2
        protected const val PROGRESS_PRINTING = 3
        protected const val PROGRESS_PRINTED = 4
    }
}