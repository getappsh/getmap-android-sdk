package com.example.example_app.models

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.example_app.R

class NebulaParam {

    // Classe représentant l'objet
    data class NebulaParam(val name: String, val value: String)

    // Classe ViewHolder
    class NebulaParamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nomTextView: TextView = itemView.findViewById(R.id.param_name)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.value_nebula)

        fun bind(objet: NebulaParam) {
            nomTextView.text = objet.name
            descriptionTextView.text = objet.value
            descriptionTextView.isEnabled = false
        }
    }

    // Classe Adapter
    class NebulaParamAdapter(private val Params: Array<NebulaParam?>, private var isEditing: Boolean) : RecyclerView.Adapter<NebulaParamViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NebulaParamViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_param_nebula, parent, false)
            return NebulaParamViewHolder(view)
        }

        override fun onBindViewHolder(holder: NebulaParamViewHolder, position: Int) {
            val nebulaParam = Params[position]
            nebulaParam?.let { holder.bind(it) }

            nebulaParam?.let {
                holder.bind(it)
                holder.itemView.isEnabled = isEditing
            }
        }
        override fun getItemCount(): Int {
            return Params.size
        }

        companion object {
            fun setIsEditing(checked: Boolean) {
                
            }
        }
    }

}