package com.example.example_app.models

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.example_app.R
import com.google.android.material.textfield.TextInputEditText

class NebulaParam {

    data class NebulaParam(var name: String, var value: String)

    // ViewHolder Class
    class NebulaParamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.param_name)
        val valueTextView: TextView = itemView.findViewById(R.id.value_nebula)
    }

    // Adapter Class
    class NebulaParamAdapter(
        private var Params: Array<NebulaParam>,
        private val itemClickListener: (Int, String) -> Unit,
    ) : RecyclerView.Adapter<NebulaParamViewHolder>() {
        private var isEditing = false

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NebulaParamViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_param_nebula, parent, false)
            return NebulaParamViewHolder(view)
        }

        override fun onBindViewHolder(holder: NebulaParamViewHolder, position: Int) {

            val nebulaParam = Params[position]
            holder.nameTextView.text = nebulaParam.name
            holder.valueTextView.text = nebulaParam.value
            holder.valueTextView.isEnabled = isEditing

            val itemViewLayout = holder.itemView.findViewById<CardView>(R.id.card)
            val itemNameLayout = holder.itemView.findViewById<TextView>(R.id.param_name)
            val valItemView = holder.itemView.findViewById<TextInputEditText>(R.id.value_nebula)
            if ((holder.nameTextView.text == "Max MapArea in SqKm" || holder.nameTextView.text == "Min inclusion needed")
                && isEditing
            ) {
                holder.valueTextView.isEnabled = false
                itemNameLayout.setOnClickListener {
                    itemClickListener(position, holder.nameTextView.text.toString())
                }
                itemViewLayout.setOnClickListener {
                    itemClickListener(position, holder.nameTextView.text.toString())
                }
                valItemView.setOnClickListener {
                    itemClickListener(position, holder.nameTextView.text.toString())
                }
            }

//            holder.descriptionTextView.isEnabled = isEditingList[holder.adapterPosition]
            holder.valueTextView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    // Update the value in the Params array when the user edits the EditText
                    Params[holder.adapterPosition].value = s.toString()
                }

                override fun beforeTextChanged(s: CharSequence?,start: Int,count: Int,after: Int,) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            })
        }

        fun setIsEditing(editing: Boolean, position: Int, param: NebulaParam) {
            isEditing = editing
            notifyItemChanged(position, param)
        }

        override fun getItemCount(): Int {
            return Params.size
        }

        fun updateAll(params:Array<NebulaParam>){
            this.Params = params
            notifyDataSetChanged()
        }
        fun getParams(): Array<NebulaParam>{
            return Params
        }
    }

}