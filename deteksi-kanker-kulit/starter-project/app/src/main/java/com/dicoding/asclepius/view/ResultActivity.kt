package com.dicoding.asclepius.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.datariwayat.AppDatabase
import com.dicoding.asclepius.datariwayat.PredictionHistory
import com.dicoding.asclepius.helper.ImageClassifierHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.io.FileOutputStream

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private val viewModel: ResultViewModel by viewModels()

    companion object {
        const val IMAGE_URI = "img_uri"
        const val TAG = "ResultActivity"
        const val RESULT_TEXT = "result_text" // Added this line
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Periksa apakah URI gambar sudah ada di ViewModel
        if (viewModel.imageUri == null) {
            val imageUriString = intent.getStringExtra(IMAGE_URI)
            if (imageUriString != null) {
                viewModel.imageUri = Uri.parse(imageUriString)
            } else {
                Log.e(TAG, "No image URI provided")
                finish()
            }
        }

        // Tampilkan gambar dan analisis
        viewModel.imageUri?.let { imageUri ->
            displayImage(imageUri)

            if (viewModel.analysisResult == null) {
                // Lakukan analisis gambar jika hasil belum ada di ViewModel
                val imageClassifierHelper = ImageClassifierHelper(
                    contextValue = this,
                    classifierListenerValue = object : ImageClassifierHelper.ClassifierListener {
                        override fun onError(errorMessage: String) {
                            Toast.makeText(this@ResultActivity, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                        }

                        override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                            results?.let { showResults(it) }
                        }
                    }
                )
                imageClassifierHelper.classifyImage(imageUri)
            } else {
                // Menampilkan hasil yang sudah disimpan di ViewModel
                binding.resultText.text = viewModel.analysisResult
            }
        }

        // Tombol Simpan
        binding.saveButton.setOnClickListener {
            val result = binding.resultText.text.toString()
            viewModel.imageUri?.let { imageUri ->
                showToast("Data saved")
                savePredictionToDatabase(imageUri, result)
            } ?: run {
                showToast("No image URI provided")
                finish()
            }
        }
    }

    // Fungsi untuk menampilkan gambar di ImageView
    private fun displayImage(uri: Uri) {
        Log.d(TAG, "Displaying image: $uri")
        binding.resultImage.setImageURI(uri)
    }

    // Fungsi untuk menampilkan hasil analisis dan menyimpannya di ViewModel
    private fun showResults(results: List<Classifications>) {
        val topResult = results[0]
        val label = topResult.categories[0].label
        val score = topResult.categories[0].score

        // Fungsi ekstensi untuk format persentase
        fun Float.formatToString(): String {
            return String.format("%.2f%%", this * 100)
        }

        val resultText = "$label ${score.formatToString()}"
        binding.resultText.text = resultText
        viewModel.analysisResult = resultText // Simpan hasil ke ViewModel
    }

    private fun moveToHistory(imageUri: Uri, result: String) {
        val intent = Intent(this, HistoryActivity::class.java)
        intent.putExtra(RESULT_TEXT, result)
        intent.putExtra(IMAGE_URI, imageUri.toString())
        setResult(RESULT_OK, intent)
        startActivity(intent)
        finish()
    }

    private fun savePredictionToDatabase(imageUri: Uri, result: String) {
        if (result.isNotEmpty()) {
            val fileName = "cropped_image_${System.currentTimeMillis()}.jpg"
            val destinationUri = Uri.fromFile(File(cacheDir, fileName))
            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(File(cacheDir, fileName)).use { output ->
                    input.copyTo(output)
                }
            }
            val prediction = PredictionHistory(imagePath = destinationUri.toString(), result = result)
            GlobalScope.launch(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(applicationContext)
                try {
                    database.predictionHistoryDao().insertPrediction(prediction)
                    Log.d(TAG, "Prediction saved successfully: $prediction")
                    val predictions = database.predictionHistoryDao().getAllPredictions()
                    Log.d(TAG, "All predictions after save: $predictions")
                    moveToHistory(destinationUri, result)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save prediction: $prediction", e)
                }
            }
        } else {
            Log.e(TAG, "Result is empty, cannot save prediction to database.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}