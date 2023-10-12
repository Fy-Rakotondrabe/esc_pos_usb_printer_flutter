package com.example.print.async

import android.content.Context
import io.flutter.plugin.common.MethodChannel

class AsyncUsbEscPosPrint : AsyncEscPosPrint {
    constructor(context: Context?, methodChannel: MethodChannel) : super(context!!, methodChannel = methodChannel) {
    }

    constructor(context: Context?, methodChannel: MethodChannel, onPrintFinished: OnPrintFinished?) : super(
        context!!, onPrintFinished, methodChannel,
    ) {
    }
}