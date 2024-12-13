# Benchmarking Performance Tool
A benchmarking tool designed to evaluate the performance of ONNX models on mobile devices, using UNet++ models with MobileNetV3, EfficientNet-B0, and EfficientNet-B4 encoders.

## Features
Benchmarks models based on:
Processing Time per Image (seconds)
Frames Per Second (FPS)
Total Time for 100 Images (seconds)
Results displayed in a tabular format using RecyclerView.
Results saved to a .csv file in the Downloads directory (e.g., Pixel_7.csv).
Supports hardware acceleration via NNAPI or CPU fallback.
## How It Works
Upload Models: Place ONNX models in the models folder in the Downloads directory.
Run Benchmark: Click "Start Test" to measure the performance of all models in the directory.
## View Results:
Results displayed in the app in a tabular format.
Results saved in Downloads as a .csv file.
## Project Structure
MainActivity.kt: Handles downloading and unzipping ONNX models.
BenchmarkActivity.kt: Benchmarks models and displays results.
RecyclerView: Displays tabular results with model performance metrics.
## Requirements
Android device (tested on Pixel 7).
ONNX models compatible with input shape (1, 3, 640, 800) or similar.
## Demo
Prepare Models: Upload ONNX models to Downloads/models folder.
Run the App: Benchmark results are displayed and saved as a .csv file.
