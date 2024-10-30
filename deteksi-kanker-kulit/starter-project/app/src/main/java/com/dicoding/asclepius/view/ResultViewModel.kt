package com.dicoding.asclepius.view

import android.net.Uri
import androidx.lifecycle.ViewModel

class ResultViewModel : ViewModel() {
    var imageUri: Uri? = null
    var analysisResult: String? = null
}