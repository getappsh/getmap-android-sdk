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

    data class NebulaParam(var name: String, var value: String)

    // ViewHolder Class
    class NebulaParamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.param_name)
        val valueTextView: TextView = itemView.findViewById(R.id.value_nebula)
    }

    // Adapter Class
    class NebulaParamAdapter(
        private val Params: Array<NebulaParam>,
    ) : RecyclerView.Adapter<NebulaParamViewHolder>() {
        private var isEditing = false
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NebulaParamViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_param_nebula, parent, false)
            return NebulaParamViewHolder(view)
        }

        override fun onBindViewHolder(holder: NebulaParamViewHolder, position: Int) {
            val nebulaParam = Params[holder.adapterPosition]
            holder.nameTextView.text = nebulaParam.name
            holder.valueTextView.text = nebulaParam.value
            holder.valueTextView.isEnabled = isEditing
//            holder.descriptionTextView.isEnabled = isEditingList[holder.adapterPosition]
            holder.valueTextView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // Update the value in the Params array when the user edits the EditText
                    Params[holder.adapterPosition].value = s.toString()
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

        fun setIsEditing(editing: Boolean,position: Int) {
            isEditing = editing
            notifyItemChanged(position)
//            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return Params.size
        }
    }

}