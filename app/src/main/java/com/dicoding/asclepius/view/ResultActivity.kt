package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding
import kotlin.math.roundToInt

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.

        // Menampilkan hasil gambar, prediksi, dan confidence score
        try {
            showResult()
        } catch (e: Exception) {

            Log.e("ResultActivity", "Error displaying result", e)
            binding.resultText.text = getString(R.string.warning_result)

        }

        binding.BacjButton.setOnClickListener{
            finish()

        }
    }

    private fun showResult() {
        val classificationResult = intent.getStringExtra("classificationResult")
        val imageUri = intent.getStringExtra("imageUri")
        val inferenceTime = intent.getLongExtra("inferenceTime", 0)

        Log.d("ResultActivity", "Classification Result: $classificationResult")
        Log.d("ResultActivity", "Image URI: $imageUri")
        Log.d("ResultActivity", "Inference Time: $inferenceTime ms")

        imageUri?.let {
            val uri = Uri.parse(it)
            binding.resultImage.setImageURI(uri)
        }

        // Mengekstrak hasil klasifikasi dan menambahkan teks tambahan
        classificationResult?.let {
            val categories = it.split("\n") // Memisahkan berdasarkan baris
            val resultText = StringBuilder()

            var largestCategory = ""
            var largestPercentage = 0.0

            categories.forEach { category ->
                val parts = category.split(":")
                if (parts.size == 2) {
                    val categoryName = parts[0].trim()
                    val percentage = parts[1].trim().removeSuffix("%").toDouble()

                    resultText.append("$categoryName: $percentage%\n")

                    // Menentukan kategori dengan persentase terbesar
                    if (percentage > largestPercentage) {
                        largestCategory = categoryName
                        largestPercentage = percentage
                    }
                }
            }

            // Menambahkan teks tambahan berdasarkan kategori terbesar
            val roundedPercentage = largestPercentage.roundToInt()
            resultText.append("\nHasilnya adalah gambar ini termasuk kedalam kategori $largestCategory\n\n" +
                    "Dengan persentase $roundedPercentage%\n\n" +
                    "Waktu Inferensi: ${inferenceTime}ms")

            // Tampilkan hasil ke dalam TextView atau komponen UI lainnya
            binding.resultText.text = resultText.toString()
        }
    }


}