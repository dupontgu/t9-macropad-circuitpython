package com.dupontgu.t9.web

import com.dupontgu.t9.KeyboardLayout
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import kotlin.io.path.createTempFile

suspend fun circuitPythonBundle(libraryFile: File, keyMapFile: File, layout: KeyboardLayout): Result<File> =
    runCatching {
        val client = HttpClient(CIO)
        val layoutsZip = createTempFile("layout").toFile()
        client.downloadToFile(
            "https://github.com/Neradoc/Circuitpython_Keyboard_Layouts/releases/download/20221209/circuitpython-keyboard-layouts-py-20221209.zip",
            layoutsZip
        )
        ZipUtil.explode(layoutsZip)
        val layoutsDir = layoutsZip.resolve(layoutsZip.list()!!.first()).resolve("lib")
        println(layoutsDir.list()?.toList())

        return@runCatching layoutsZip
    }

suspend fun HttpClient.downloadToFile(url: String, file: File) {
    prepareGet(url).execute { httpResponse ->
        val channel: ByteReadChannel = httpResponse.body()
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.isEmpty) {
                val bytes = packet.readBytes()
                file.appendBytes(bytes)
            }
        }
    }
}