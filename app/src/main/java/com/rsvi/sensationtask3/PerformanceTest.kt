package com.rsvi.sensationtask3

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import java.nio.FloatBuffer
import kotlin.random.Random

data class PerformanceResult(
    val averageTime: Double,
    val fps: Double,
    val totalTime: Double
)

object PerformanceTest {

    fun runBenchmark(modelPath: String): PerformanceResult {
        // Initialize ONNX Runtime environment
        val ortEnvironment = OrtEnvironment.getEnvironment()

        // Load the ONNX model and create a session
        val sessionOptions = SessionOptions()
        val session: OrtSession = ortEnvironment.createSession(modelPath, sessionOptions)

        // Define the input shape
        val inputShape = longArrayOf(1, 512, 512, 3)

        // Generate random input data
        val totalElements = inputShape.reduce { acc, i -> acc * i }.toInt()
        val inputData = FloatArray(totalElements) { Random.nextFloat() }

        // Convert FloatArray to FloatBuffer
        val floatBuffer = FloatBuffer.wrap(inputData)

        // Create ONNX tensor
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, inputShape)

        // Debug: Print tensor information
        println("Tensor created with shape: ${inputShape.joinToString()}")

        // Measure performance
        val startTime = System.nanoTime()
        repeat(100) {
            session.run(mapOf("input" to inputTensor))
        }
        val endTime = System.nanoTime()

        // Calculate performance metrics
        val totalTime = (endTime - startTime) / 1_000_000_000.0
        val averageTime = totalTime / 100
        val fps = 1 / averageTime

        // Cleanup resources
        inputTensor.close()
        session.close()
        ortEnvironment.close()

        return PerformanceResult(averageTime, fps, totalTime)
    }
}
