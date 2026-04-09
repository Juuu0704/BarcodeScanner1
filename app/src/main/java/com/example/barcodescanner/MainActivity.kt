package com.example.barcodescanner

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var adresseIP: TextView
    private lateinit var bouton: Button
    private lateinit var new_ip: EditText
    private lateinit var tcpClient: TcpClient

    private lateinit var bouton_envoi: Button

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else resultText.text = "Permission caméra refusée"
    }
    private fun loadIP(): String {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getString("saved_ip", "192.168.2.50") ?: "192.168.2.50"
    }
    private fun saveIP(ip: String) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putString("saved_ip", ip).apply()
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var isScanning = true

    private fun isValidIP(ip: String): Boolean {
        return try {
            java.net.InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.resultText)
        adresseIP = findViewById(R.id.Text)
        bouton = findViewById(R.id.button)
        new_ip = findViewById(R.id.ipEdit)
        bouton_envoi = findViewById(R.id.sendbutton)

        val savedIP = loadIP()
        tcpClient = TcpClient(savedIP, 1234)

        adresseIP.text = "Dernière IP sauvegardée : $savedIP"

        // Connexion TCP au démarrage
        tcpClient = TcpClient(savedIP, 1234)
        CoroutineScope(Dispatchers.IO).launch {
            tcpClient.connect()
        }

        requestPermission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = CameraPreview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            CoroutineScope(Dispatchers.Main).launch{
                withContext(Dispatchers.Main) {
                    adresseIP.text = "Dernière IP sauvegardée : ${loadIP()}"
                }
            }
/*_______________________________________________________BOUTON IP_____________________________________________________________________*/
            val button = findViewById<Button>(R.id.button)
            //val newIp = findViewById<EditText>(R.id.new_IP)

            button.setOnClickListener {
                val new_IP = new_ip.text.toString()

                // ✅ Vérification IP
                if (new_IP.isBlank()) {
                    Toast.makeText(this, "Veuillez entrer une IP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!isValidIP(new_IP)) {
                    Toast.makeText(this, "IP invalide", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                saveIP(new_IP)          //sauvegarde

                adresseIP.text = "Nouvelle IP : $new_IP "
                Toast.makeText(this, "IP changée !", Toast.LENGTH_SHORT).show()

                // Reconnexion propre
                tcpClient = TcpClient(new_IP, 1234)
                CoroutineScope(Dispatchers.IO).launch {
                    tcpClient.connect()
                }
            }
 /*_______________________________________________________FIN BOUTON IP_____________________________________________________________________*/

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        BarcodeAnalyzer { code ->
                            if (isScanning) {
                                isScanning = false
                            val code_coupe = code.substringBefore(";")
                                // affiche le résultat dans la fenetre de scan
                                CoroutineScope(Dispatchers.Main).launch{
                                    withContext(Dispatchers.Main) {

                                        resultText.text = "Résultat : ${code_coupe} "
                                        resultText.postDelayed({ isScanning = true }, 2000)
                                    }
                                }
   /*_______________________________________________________ENVOI_____________________________________________________________________*/
                                val bouton = findViewById<Button>(R.id.sendbutton)
                                bouton.setOnClickListener {
                                    // ✅ Envoi TCP du code scanné
                                    CoroutineScope(Dispatchers.IO).launch {
                                        tcpClient.sendMessage(code)
                                        // Toast confirmation envoi
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@MainActivity, "Code envoyé : $code_coupe", Toast.LENGTH_SHORT).show()
                                        }
/*_______________________________________________________REPONSE_____________________________________________________________________*/
                                        val reponse = tcpClient.receiveMessage()
                                        // Toast avec la réponse
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(this@MainActivity, "Réponse : $reponse", Toast.LENGTH_SHORT).show()
                                        }
                                    }
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

/*_______________________________________________________DESTRUCTION DE LA CONNEXION_____________________________________________________________________*/
    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch{
            tcpClient.disconnect()}
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
