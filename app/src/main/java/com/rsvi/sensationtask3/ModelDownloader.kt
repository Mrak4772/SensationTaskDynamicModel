package com.rsvi.sensationtask3

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.zip.ZipInputStream

object ModelDownloader {

    fun downloadAndExtract(url: String, outputDir: File, callback: (Boolean) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                callback(false)
                return
            }

            val zipInputStream = ZipInputStream(response.body!!.byteStream())
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val outputFile = File(outputDir, entry.name)
                outputFile.outputStream().use { output ->
                    zipInputStream.copyTo(output)
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            callback(true)
        }
    }
}
