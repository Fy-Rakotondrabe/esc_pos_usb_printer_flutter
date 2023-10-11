package com.example.print.async

import android.app.AlertDialog
import android.app.ProgressDialog
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

abstract class AsyncEscPosPrint @JvmOverloads constructor(
    context: Context,
    onPrintFinished: OnPrintFinished? = null
) : AsyncTask<AsyncEscPosPrinter?, Int?, PrinterStatus>() {
    protected var dialog: ProgressDialog? = null
    protected var weakContext: WeakReference<Context>
    protected var onPrintFinished: OnPrintFinished?

    init {
        weakContext = WeakReference(context)
        this.onPrintFinished = onPrintFinished
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
        if (dialog == null) {
            val context = weakContext.get() ?: return
            dialog = ProgressDialog(context)
            dialog!!.setTitle("Printing in progress...")
            dialog!!.setMessage("...")
            dialog!!.setProgressNumberFormat("%1d / %2d")
            dialog!!.setCancelable(false)
            dialog!!.isIndeterminate = false
            dialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            dialog!!.show()
        }
    }

    protected override fun onProgressUpdate(vararg progress: Int?) {
        val progressValue = progress[0] ?: 0  // Use 0 as the default if progress[0] is null

        when (progressValue) {
            PROGRESS_CONNECTING -> dialog!!.setMessage("Connecting printer...")
            PROGRESS_CONNECTED -> dialog!!.setMessage("Printer is connected...")
            PROGRESS_PRINTING -> dialog!!.setMessage("Printer is printing...")
            PROGRESS_PRINTED -> dialog!!.setMessage("Printer has finished...")
        }
        dialog!!.progress = progressValue
        dialog!!.max = 4
    }


    override fun onPostExecute(result: PrinterStatus) {
        dialog!!.dismiss()
        dialog = null
        val context = weakContext.get() ?: return
        when (result.printerStatus) {
            FINISH_SUCCESS -> AlertDialog.Builder(context)
                .setTitle("Success")
                .setMessage("Congratulation ! The texts are printed !")
                .show()
            FINISH_NO_PRINTER -> AlertDialog.Builder(context)
                .setTitle("No printer")
                .setMessage("The application can't find any printer connected.")
                .show()
            FINISH_PRINTER_DISCONNECTED -> AlertDialog.Builder(context)
                .setTitle("Broken connection")
                .setMessage("Unable to connect the printer.")
                .show()
            FINISH_PARSER_ERROR -> AlertDialog.Builder(context)
                .setTitle("Invalid formatted text")
                .setMessage("It seems to be an invalid syntax problem.")
                .show()
            FINISH_ENCODING_ERROR -> AlertDialog.Builder(context)
                .setTitle("Bad selected encoding")
                .setMessage("The selected encoding character returning an error.")
                .show()
            FINISH_BARCODE_ERROR -> AlertDialog.Builder(context)
                .setTitle("Invalid barcode")
                .setMessage("Data send to be converted to barcode or QR code seems to be invalid.")
                .show()
        }
        if (onPrintFinished != null) {
            if (result.printerStatus == FINISH_SUCCESS) {
                onPrintFinished!!.onSuccess(result.asyncEscPosPrinter)
            } else {
                onPrintFinished!!.onError(result.asyncEscPosPrinter, result.printerStatus)
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