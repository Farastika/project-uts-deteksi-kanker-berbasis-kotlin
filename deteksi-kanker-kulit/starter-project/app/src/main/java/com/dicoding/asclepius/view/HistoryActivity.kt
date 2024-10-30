package com.dicoding.asclepius.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.asclepius.R
import com.dicoding.asclepius.adapter.PredictionHistoryAdapter
import com.dicoding.asclepius.datariwayat.AppDatabase
import com.dicoding.asclepius.datariwayat.PredictionHistory
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity(), PredictionHistoryAdapter.OnDeleteClickListener {

    private lateinit var predictionRecyclerView: RecyclerView
    private lateinit var predictionAdapter: PredictionHistoryAdapter
    private var predictionList: MutableList<PredictionHistory> = mutableListOf()
    private lateinit var tvNotFound: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    companion object {
        const val TAG = "historydata"
        const val REQUEST_HISTORY_UPDATE = 100 // Tambahkan konstanta untuk permintaan kode update
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        bottomNavigationView = findViewById(R.id.menuBar)
        predictionRecyclerView = findViewById(R.id.rvHistory)
        tvNotFound = findViewById(R.id.tvNotFound)

        bottomNavigationView.selectedItemId = R.id.history_menu
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.news -> {
                    startActivity(Intent(this, NewsActivity::class.java))
                    true
                }
                R.id.history_menu -> true
                else -> false
            }
        }

        predictionAdapter = PredictionHistoryAdapter(predictionList)
        predictionAdapter.setOnDeleteClickListener(this)
        predictionRecyclerView.adapter = predictionAdapter
        predictionRecyclerView.layoutManager = LinearLayoutManager(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Mengganti GlobalScope dengan lifecycleScope
        lifecycleScope.launch {
            loadPredictionHistoryFromDatabase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_HISTORY_UPDATE && resultCode == RESULT_OK) {
            lifecycleScope.launch {
                loadPredictionHistoryFromDatabase()
            }
        }
    }

    private suspend fun loadPredictionHistoryFromDatabase() {
        // Menggunakan IO dispatcher untuk query database
        withContext(Dispatchers.IO) {
            val predictions = AppDatabase.getDatabase(this@HistoryActivity).predictionHistoryDao().getAllPredictions()
            Log.d(TAG, "Number of predictions: ${predictions.size}")

            withContext(Dispatchers.Main) {
                predictionList.clear()
                predictionList.addAll(predictions)
                predictionAdapter.notifyDataSetChanged()
                showOrHideNoHistoryText()
            }
        }
    }

    private fun showOrHideNoHistoryText() {
        if (predictionList.isEmpty()) {
            tvNotFound.visibility = View.VISIBLE
            predictionRecyclerView.visibility = View.GONE
        } else {
            tvNotFound.visibility = View.GONE
            predictionRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        val prediction = predictionList[position]
        if (prediction.result.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getDatabase(this@HistoryActivity).predictionHistoryDao().deletePrediction(prediction)
                withContext(Dispatchers.Main) {
                    predictionList.removeAt(position)
                    predictionAdapter.notifyItemRemoved(position) // Gunakan notifyItemRemoved untuk performa lebih baik
                    showOrHideNoHistoryText()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}