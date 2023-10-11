package com.example.print.async

import android.content.Context

class AsyncUsbEscPosPrint : AsyncEscPosPrint {
    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, onPrintFinished: OnPrintFinished?) : super(
        context!!, onPrintFinished
    ) {
    }
}