package com.rsvi.sensationtask3

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val modelUrlInput = findViewById<EditText>(R.id.modelUrlInput)
        val downloadButton = findViewById<Button>(R.id.downloadButton)
        val startTestButton = findViewById<Button>(R.id.startTestButton)

        downloadButton.setOnClickListener {
            val url = modelUrlInput.text.toString()
            if (url.isNotBlank()) {
                downloadAndUnzipModels(url)
            } else {
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }

        startTestButton.setOnClickListener {
            val intent = Intent(this, BenchmarkActivity::class.java)
            startActivity(intent)
        }
    }

    private fun downloadAndUnzipModels(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Prepare paths
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val modelsDir = File(downloadsDir, "models")
                if (!modelsDir.exists()) modelsDir.mkdirs()

                val zipFile = File(modelsDir, "models.zip")

                // Download file
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    saveToFile(inputStream, zipFile)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "File saved to: ${zipFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    }

                    // Unzip the file
                    unzip(zipFile, modelsDir)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Download and extraction complete!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Failed to download models: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveToFile(inputStream: InputStream?, file: File) {
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    // Only save .onnx files
                    if (entry.name.endsWith(".onnx")) {
                        FileOutputStream(newFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                }
                entry = zis.nextEntry
            }
        }
    }


}
