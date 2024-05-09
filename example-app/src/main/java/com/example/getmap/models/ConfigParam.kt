package com.example.getmap.models

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.InputType.TYPE_CLASS_TEXT
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.getmap.R
import com.google.android.material.textfield.TextInputEditText

class ConfigParam {

    data class NebulaParam(var name: String, var value: String, var isDropdown: Boolean = false)

    // ViewHolder Class

    class NebulaParamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.param_name)
        val valueTextView: TextView = itemView.findViewById(R.id.value_nebula)
        val dropdownButton: ImageButton = itemView.findViewById(R.id.dropdownButton)
    }

    // Adapter Class
    class NebulaParamAdapter(
        private var Params: Array<NebulaParam>,
        private val itemClickListener: (Int, String) -> Unit,
    ) : RecyclerView.Adapter<NebulaParamViewHolder>() {
        private var isEditing = false
        private var context: Context? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NebulaParamViewHolder {
            this.context = parent.context
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_param_config, parent, false)
            return NebulaParamViewHolder(view)
        }


        override fun onBindViewHolder(holder: NebulaParamViewHolder, position: Int) {

            val nebulaParam = Params[position]
            holder.nameTextView.text = nebulaParam.name
            holder.valueTextView.text = nebulaParam.value
            holder.valueTextView.isEnabled = isEditing

            // The following values and code will treat the special values (unactive and password like UI)
            val valItemView = holder.itemView.findViewById<TextInputEditText>(R.id.value_nebula)
            val itemViewLayout = holder.itemView.findViewById<CardView>(R.id.card)
            val itemNameLayout = holder.itemView.findViewById<TextView>(R.id.param_name)
            val dropdownButton = holder.itemView.findViewById<ImageButton>(R.id.dropdownButton)
            defineType(holder)
            if (nebulaParam.isDropdown && holder.nameTextView.text == "Target Storage Policy" && isEditing) {
                // Show the button only for the last item
                dropdownButton.visibility = View.VISIBLE
                dropdownButton.setOnClickListener {
                    showDropdownMenu(holder.itemView.context, holder.nameTextView.text.toString()) { selectedValue ->
                        // Update the value with the value selected
                        nebulaParam.value = selectedValue
                        holder.valueTextView.text = selectedValue
                    }
                }
            } else {
                dropdownButton.visibility = View.GONE
                holder.valueTextView.visibility = View.VISIBLE
            }

            if ((holder.nameTextView.text == "URL" || holder.nameTextView.text == "Matomo Url" || holder.nameTextView.text == "Download Path" || holder.nameTextView.text == "Flash Storage Path")
                && !isEditing
            ) {
                holder.valueTextView.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            } else
                holder.valueTextView.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()

            if ((holder.nameTextView.text == "Max MapArea in SqKm" || holder.nameTextView.text == "Min inclusion needed")
                || holder.nameTextView.text == "Download Path" || holder.nameTextView.text == "Flash Storage Path" && isEditing
            ) {
                holder.valueTextView.isEnabled = false
                itemNameLayout.setOnClickListener {
                    itemClickListener(position, holder.nameTextView.text.toString())
                }
                itemViewLayout.setOnClickListener {
                    itemClickListener(position, holder.nameTextView.text.toString())
                }
            }
            valItemView.setOnClickListener {
                itemClickListener(position, holder.nameTextView.text.toString())
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
        private fun showDropdownMenu(context: Context, title: String, onItemSelected: (String) -> Unit) {
            val dropdownItems = arrayOf("SDOnly","FlashThenSD","SDThenFlash","FlashOnly")
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                .setItems(dropdownItems) { dialog, which ->
                    val selectedValue = dropdownItems[which]
                    onItemSelected(selectedValue)
                    dialog.dismiss()
                }
            val dialog = builder.create()
            dialog.show()
        }

        private fun defineType(holder: NebulaParamViewHolder) {

            val valItemView = holder.itemView.findViewById<TextInputEditText>(R.id.value_nebula)
            val stringNames = arrayOf("Matomo dimension id", "Matomo site id")
            val passwordNames = arrayOf("URL", "Matomo Url", "Download Path", "Flash Storage Path")
            if (passwordNames.contains(Params[holder.adapterPosition].name))
                valItemView.inputType = TYPE_TEXT_VARIATION_PASSWORD
            else if (stringNames.contains(Params[holder.adapterPosition].name))
                valItemView.inputType = TYPE_CLASS_TEXT
            else valItemView.inputType = TYPE_CLASS_NUMBER
        }

        override fun getItemCount(): Int {
            return Params.size
        }

        fun updateAll(params: Array<NebulaParam>) {
            this.Params = params
            notifyDataSetChanged()
        }

        fun setIsEditing(editing: Boolean, position: Int, param: NebulaParam) {
            isEditing = editing
            notifyItemChanged(position, param)
        }

        fun getParams(): Array<NebulaParam> {
            return Params
        }
    }
}
