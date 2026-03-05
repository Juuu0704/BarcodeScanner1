package com.example.barcodescanner

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else resultText.text = "Permission caméra refusée"
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isScanning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.resultText)

        requestPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = CameraPreview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        BarcodeAnalyzer { code ->
                            if (isScanning) {
                                isScanning = false

                                // On met à jour uniquement le TextView — le PreviewView reste intact
                                runOnUiThread {
                                    resultText.text = "ISBN : $code"

                                    // Réactive le scan après 2 secondes
                                    resultText.postDelayed({ isScanning = true }, 2000)
                                }
                            }
                        }
                    )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("CameraX", "Erreur : ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }
}

/**
 * Classe responsable de l'analyse des images de la caméra en temps réel.
 */
class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { onBarcodeDetected(it) }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}