package com.rsvi.sensationtask3

import ai.onnxruntime.NodeInfo
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        // Create session options and specify execution providers
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4) // Use 4 threads for intra-operator parallelism
            try {
                addNnapi()  // Attempt to add NNAPI execution provider for Android devices
            } catch (e: Exception) {
                println("NNAPI not supported. Using CPU execution provider.")
            }
        }

        // Load the model into the session
        val session = try {
            env.createSession(modelFile.absolutePath, sessionOptions)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to load model: ${modelFile.name}, Error: ${e.message}")
        }

        // Dynamically retrieve the input shape
        val inputInfo = session.inputInfo.entries.first() // Get first input entry
        val inputShape = getInputShape(inputInfo.value) // Get the input shape dynamically

        // Generate random data matching the input shape
        val totalElements = inputShape.filter { it > 0 }.reduce { acc, i -> acc * i }.toInt()
        val inputData = FloatArray(totalElements) { Random.nextFloat() }
        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), inputShape)

        // Start performance benchmarking
        val startTime = System.nanoTime()
        repeat(10) {
            session.run(mapOf(inputInfo.key to inputTensor))  // Run inference
        }
        val endTime = System.nanoTime()

        inputTensor.close()
        session.close()

        // Calculate performance metrics
        val totalTime = (endTime - startTime) / 1_000_000_000.0
        val averageTime = totalTime / 10
        val fps = 1 / averageTime

        return BenchmarkResult(
            modelName = modelFile.name,
            processingTime = "${"%.4f".format(averageTime)} seconds",
            fps = "${"%.2f".format(fps)}",
            totalTime = "${"%.4f".format(totalTime)} seconds"
        )
    }

    // Helper function to dynamically fetch the input shape from the input metadata
    private fun getInputShape(value: Any): LongArray {
        return when (value) {
            is NodeInfo -> {
                val shape = value.info.javaClass.getMethod("getShape").invoke(value.info) as LongArray?
                shape ?: throw IllegalArgumentException("Input shape is null.")
            }
            else -> throw IllegalArgumentException("Unsupported input info type: ${value.javaClass.name}")
        }
    }





}