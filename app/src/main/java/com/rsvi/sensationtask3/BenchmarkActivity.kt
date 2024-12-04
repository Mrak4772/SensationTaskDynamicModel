package com.rsvi.sensationtask3

import android.os.Bundle
import android.os.Environment
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import kotlin.random.Random

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.FileWriter

class BenchmarkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benchmark)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val statusText = findViewById<TextView>(R.id.statusText)
        val recyclerView = findViewById<RecyclerView>(R.id.resultsRecyclerView)
       // val completionMessage = findViewById<TextView>(R.id.completionMessage)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        val modelsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "models")
        if (!modelsDir.exists() || modelsDir.listFiles().isNullOrEmpty()) {
            Toast.makeText(this, "No models found in the 'models' directory.", Toast.LENGTH_LONG).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val results = mutableListOf<BenchmarkResult>()
            val ortEnvironment = OrtEnvironment.getEnvironment()
            val totalModels = modelsDir.listFiles()?.count { it.extension == "onnx" } ?: 0

            modelsDir.listFiles()?.forEachIndexed { index, modelFile ->
                if (modelFile.extension == "onnx") {
                    try {
                        val startTime = System.nanoTime()
                        val result = benchmarkModel(ortEnvironment, modelFile)
                        val elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000.0

                        withContext(Dispatchers.Main) {
                            statusText.text = "Benchmarking model: ${modelFile.name} (${index + 1}/$totalModels)"
                            progressBar.progress = ((index + 1) * 100) / totalModels
                        }

                        results.add(result)
                    } catch (e: Exception) {
                        results.add(
                            BenchmarkResult(
                                modelFile.name,
                                "Error",
                                "Error",
                                "Error"
                            )
                        )
                    }
                }
            }

            // Save results to a CSV file
            saveResultsToCsv(results)

            withContext(Dispatchers.Main) {
                recyclerView.adapter = BenchmarkResultsAdapter(results)
                progressBar.visibility = ProgressBar.GONE
                statusText.text = "Benchmarking complete! Results saved to Downloads/${Build.MODEL.replace(" ", "_")}.csv"
                Toast.makeText(
                    this@BenchmarkActivity,
                    "Results saved to Downloads/${Build.MODEL.replace(" ", "_")}.csv",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }


    private fun saveResultsToCsv(results: List<BenchmarkResult>) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val deviceModel = Build.MODEL.replace(" ", "_") // Replace spaces with underscores
        val fileName = "${deviceModel}.csv" // File name like Pixel_7.csv
        val csvFile = File(downloadsDir, fileName)

        FileWriter(csvFile).use { writer ->
            // Write the header
            writer.append("Model Name,Processing Time per Image (seconds),FPS,Total Time for 100 Images (seconds)\n")
            // Write each result
            results.forEach { result ->
                writer.append("${result.modelName},${result.processingTime},${result.fps},${result.totalTime}\n")
            }
        }
    }

    private fun benchmarkModel(env: OrtEnvironment, modelFile: File): BenchmarkResult {
        val session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        val sessionOptions = OrtSession.SessionOptions()

        // Add execution providers if available
        try {
            // For Neural Networks API (NNAPI)
            sessionOptions.addNnapi()
        } catch (e: Exception) {
            println("NNAPI not supported on this device, falling back to default execution provider.")
        }

        try {
            // For GPU (if supported)
            sessionOptions.addCPU(false)
        } catch (e: Exception) {
            println("GPU not supported on this device, falling back to default execution provider.")
        }

        val inputShape = longArrayOf(1, 3, 640, 800)
        val totalElements = inputShape.reduce { acc, i -> acc * i }.toInt()
        val inputData = FloatArray(totalElements) { Random.nextFloat() }
        val floatBuffer = FloatBuffer.wrap(inputData)
        val inputTensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)

        val startTime = System.nanoTime()
        repeat(10) {
            session.run(mapOf("input" to inputTensor))
        }
        val endTime = System.nanoTime()

        val totalTime = (endTime - startTime) / 1_000_000_000.0
        val averageTime = totalTime / 10
        val fps = 1 / averageTime

        inputTensor.close()
        session.close()

        return BenchmarkResult(
            modelName = modelFile.name,
            processingTime = "${"%.4f".format(averageTime)} seconds",
            fps = "${"%.2f".format(fps)}",
            totalTime = "${"%.4f".format(totalTime)} seconds"
        )
    }
}
