import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import android.widget.ToggleButton
import com.example.example_app.R
import com.example.example_app.models.NebulaParam

class PasswordDialog(
    private val context: Context, private val params: Array<NebulaParam.NebulaParam>,
    private val nebulaParamAdapter: NebulaParam.NebulaParamAdapter, private val isChecked: Boolean,
    private var editConf: ToggleButton,
) {

    fun show() {
        val builder = AlertDialog.Builder(context)

        // Inflate the personalized layout
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.nebula_password, null)

        // Configure the vue of the dialog
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextNumberPassword)
        builder.setView(dialogView)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val password = editTextPassword.text.toString()
            if (password == "200") {
                for (i in 0..(params.size - 1)) {
                    nebulaParamAdapter.setIsEditing(isChecked,i,params[i] )
                }
            } else {
                Toast.makeText(context, "Incorrect Password", Toast.LENGTH_SHORT).show()
                editConf.isChecked = false
                dialog.dismiss()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            editConf.isChecked = false
            dialog.cancel()
        }
        builder.setOnCancelListener { editConf.isChecked = false }

        val dialog = builder.create()
        dialog.show()
    }
}
