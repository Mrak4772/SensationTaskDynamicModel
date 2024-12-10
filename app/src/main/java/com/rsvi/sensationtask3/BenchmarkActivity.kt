package com.rsvi.sensationtask3

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.nio.FloatBuffer
import kotlin.random.Random
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale



class BenchmarkActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_benchmark)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val statusText = findViewById<TextView>(R.id.statusText)
        val recyclerView = findViewById<RecyclerView>(R.id.resultsRecyclerView)

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
                                "Error", false
                            )
                        )
                    }
                }
            }

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
        val deviceModel = Build.MODEL.replace(" ", "_")
        val fileName = "${deviceModel}.csv"
        val csvFile = File(downloadsDir, fileName)

        // Set locale to US for consistent decimal and separator formatting
        val decimalFormat = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

        OutputStreamWriter(csvFile.outputStream(), StandardCharsets.UTF_8).use { writer ->
            writer.append("Model Name;Processing Time per Image (seconds);FPS;Total Time for 100 Images;Quantized\n") // Use semicolon as separator
            results.forEach { result ->
                writer.append(
                    "${result.modelName};" +
                            "${decimalFormat.format(result.processingTime)};" +
                            "${decimalFormat.format(result.fps)};" +
                            "${decimalFormat.format(result.totalTime)};" +
                            "${if (result.isQuantized) "Yes" else "No"}\n"
                )
            }
        }
    }

    private fun benchmarkModel(env: OrtEnvironment, modelFile: File): BenchmarkResult {
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
            try {
                addNnapi()  // Attempt to add NNAPI execution provider
            } catch (e: Exception) {
                println("NNAPI not supported. Using CPU execution provider.")
            }
        }




    val session = try {
            env.createSession(modelFile.absolutePath, sessionOptions)
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to load model: ${modelFile.name}, Error: ${e.message}")
        }

        val inputInfo = session.inputInfo.entries.first()
        val inputShape = getInputShape(inputInfo.value)
        val totalElements = inputShape.filter { it > 0 }.reduce { acc, i -> acc * i }.toInt()
        val inputData = FloatArray(totalElements) { Random.nextFloat() }
        val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), inputShape)

        val startTime = System.nanoTime()
        repeat(10) {
            session.run(mapOf(inputInfo.key to inputTensor))
        }
        val endTime = System.nanoTime()

        inputTensor.close()
        session.close()

        val totalTime = (endTime - startTime) / 1_000_000_000.0
        val averageTime = totalTime / 10
        val fps = 1 / averageTime

        return BenchmarkResult(
            modelName = modelFile.name,
            processingTime = "%.4f seconds".format(averageTime),
            fps = "%.2f".format(fps),
            totalTime = "%.4f seconds".format(totalTime),
            isQuantized = modelFile.name.contains("quantized")
        )
    }

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
