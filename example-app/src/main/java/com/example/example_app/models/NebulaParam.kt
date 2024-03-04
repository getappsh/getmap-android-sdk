package com.example.example_app.models

import android.text.Editable
import android.text.TextWatcher
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
        val descriptionTextView: TextView = itemView.findViewById(R.id.value_nebula)

        fun bind(objet: NebulaParam, b: Boolean) {
            nomTextView.text = objet.name
            descriptionTextView.text = objet.value
            descriptionTextView.isEnabled = b
        }
    }

    // Classe Adapter
    class NebulaParamAdapter(
        private val Params: Array<NebulaParam?>,
        private var isEditing: Boolean,
    ) : RecyclerView.Adapter<NebulaParamViewHolder>() {
        private val isEditingList = BooleanArray(Params.size) { false }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NebulaParamViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_param_nebula, parent, false)
            return NebulaParamViewHolder(view)
        }

        override fun onBindViewHolder(holder: NebulaParamViewHolder, position: Int) {
            val nebulaParam = Params[holder.adapterPosition]
            nebulaParam?.let { holder.bind(it, isEditingList[holder.adapterPosition]) }
            holder.descriptionTextView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // Update the value in the Params array when the user edits the EditText
                    Params[holder.adapterPosition] = nebulaParam?.copy(value = s.toString())
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        fun setIsEditing(position: Int, editing: Boolean) {
            // Mettre à jour l'état d'édition pour l'élément à la position donnée
            isEditingList[position] = editing

            // Notifier l'adaptateur que l'ensemble de données a changé
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return Params.size
        }
    }

}