package com.dicoding.asclepius.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import org.tensorflow.lite.task.vision.classifier.Classifications

class MainActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null
    private var isToastShown = false
    private var imageClassifierHelper: ImageClassifierHelper? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            Log.d("Permission", "Permission granted: $isGranted")
            if (isGranted) {
                showToast("Permission request granted")
            } else {
                showToast("Permission request denied")
            }
        }

    // Launcher for PickVisualMedia (Android 13 and above)
    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    // Launcher for GetContent (Android below 13)
    private val pickImageLauncherLegacy =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentImageUri = it
                showImage()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Restore image URI if it exists
        savedInstanceState?.let {
            isToastShown = it.getBoolean("isToastShown", false)
            val uriString = it.getString("imageUri")
            if (!uriString.isNullOrEmpty()) {
                currentImageUri = Uri.parse(uriString)
                showImage()
            }
        }

        checkPermission()

        binding.analyzeButton.setOnClickListener {
            analyzeImage()
        }

        binding.galleryButton.setOnClickListener {
            checkPermission()
            startGallery()
        }

        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = this
        )
    }

    private fun checkPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13 and above, check for READ_MEDIA_IMAGES
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("Permission", "Permission granted")
                    showToast("Storage permission granted")
                    isToastShown = true

                } else {
                    Log.d("Permission", "Permission denied, requesting now")
                    // Show explanation dialog if needed, then request permission
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.READ_MEDIA_IMAGES
                        )
                    ) {
                        AlertDialog.Builder(this)
                            .setMessage("This permission is needed to access your media gallery.")
                            .setPositiveButton("OK") { _, _ ->
                                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show()
                    } else {
                        // Request the permission directly
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            else -> {
                // For devices running Android 12 and below
                if (ContextCompat.checkSelfPermission(
                        this, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("Permission", "Storage permission granted")

                        showToast("Storage permission granted")
                        isToastShown = true

                } else {
                    Log.d("Permission", "Storage permission denied, requesting now")
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }


    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 or higher, use PickVisualMedia (ImageOnly) for selecting images
            launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            // Below Android 13, use GetContent
            pickImageLauncherLegacy.launch("image/*")
        }
    }

    private fun showImage() {
        // TODO: Menampilkan gambar sesuai Gallery yang dipilih.
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
            binding.analyzeButton.isEnabled = true

        }

    }

    private fun analyzeImage() {
        isToastShown=false

        // Cek apakah gambar ada (currentImageUri tidak null)
        currentImageUri?.let { uri ->
            val bitmap = uriToBitmap(uri)
            imageClassifierHelper?.classifyImage(bitmap)
        } ?: run {
            showToast("Silakan masukkan gambar terlebih dahulu")
            isToastShown = true
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        // Cek apakah input stream bisa dibuka
        contentResolver.openInputStream(uri)?.use { inputStream ->
            return BitmapFactory.decodeStream(inputStream)
        } ?: run {
            throw IllegalArgumentException("Cannot convert Uri to Bitmap")
        }
    }

    override fun onError(error: String) {
        // Menampilkan pesan error jika ada masalah dalam proses klasifikasi
        showToast("Error: $error")
    }

    override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
        if (results.isNullOrEmpty()) {
            showToast("No results found")
        } else {
            // Menampilkan hasil klasifikasi
            val classificationResult = results[0].categories.joinToString("\n") {
                "${it.label}: ${it.score * 100}%"
            }
            moveToResult(classificationResult, inferenceTime)
        }
    }

    private fun moveToResult(classificationResult: String, inferenceTime: Long) {
        // Pindah ke ResultActivity dengan data hasil klasifikasi
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("classificationResult", classificationResult)
        intent.putExtra("inferenceTime", inferenceTime)
        intent.putExtra("imageUri", currentImageUri.toString()) // Kirimkan URI gambar juga
        startActivity(intent)
    }


    private fun showToast(message: String) {
        if (!isToastShown) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            isToastShown = true
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentImageUri?.let {
            outState.putString("imageUri", it.toString())
        }
        outState.putBoolean("isToastShown", isToastShown)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isToastShown = savedInstanceState.getBoolean("isToastShown", false)
    }


}