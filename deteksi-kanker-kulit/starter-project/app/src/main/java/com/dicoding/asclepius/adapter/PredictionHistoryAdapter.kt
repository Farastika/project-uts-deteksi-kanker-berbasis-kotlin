package com.dicoding.asclepius.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dicoding.asclepius.R
import com.dicoding.asclepius.datariwayat.PredictionHistory

class PredictionHistoryAdapter(private val predictionList: List<PredictionHistory>) :
    RecyclerView.Adapter<PredictionHistoryAdapter.ViewHolder>() {

    private var onDeleteClickListener: OnDeleteClickListener? = null

    interface OnDeleteClickListener {
        fun onDeleteClick(position: Int)
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        onDeleteClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = predictionList[position]
        holder.bind(currentItem)
    }

    override fun getItemCount() = predictionList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.savedImg)
        private val resultTextView: TextView = itemView.findViewById(R.id.tvLabel)
        private val deleteImageView: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(prediction: PredictionHistory) {
            if (prediction.result.isNotEmpty()) {
                itemView.visibility = View.VISIBLE
                itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                Glide.with(itemView.context)
                    .load(prediction.imagePath)
                    .placeholder(R.drawable.ic_place_holder)
                    .error(R.drawable.ic_launcher_background)
                    .into(imageView)

                resultTextView.text = prediction.result
            } else {
                itemView.visibility = View.GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            }

            deleteImageView.setOnClickListener {
                onDeleteClickListener?.onDeleteClick(adapterPosition)
            }
        }
    }
}