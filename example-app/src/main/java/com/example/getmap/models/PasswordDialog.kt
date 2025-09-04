import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import android.widget.ToggleButton
import com.example.getmap.R
import com.example.getmap.models.ConfigParam
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

class PasswordDialog(
    private val context: Context, private val params: Array<ConfigParam.NebulaParam>,
    private val configParamAdapter: ConfigParam.NebulaParamAdapter, private val isChecked: Boolean,
    private var editConf: ToggleButton,
    private var cancelButton: Button,
    private var resetMapButton: Button,
    private val sendBugButton: Button,
    private val tracker: Tracker,
    private val applyServerConfig: Switch
) {

    fun show() {
        val builder = AlertDialog.Builder(context)
        // Inflate the personalized layout
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.config_password, null)

        // Configure the vue of the dialog
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextNumberPassword)
        builder.setView(dialogView)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val password = editTextPassword.text.toString()
            if (password == "10052") {
                TrackHelper.track().event("מיפוי ענן","שינוי הגדרות").name("הכנסת סיסמא תקינה").with(tracker)
                sendBugButton.visibility = View.INVISIBLE
                resetMapButton.visibility = View.INVISIBLE
                cancelButton.visibility = View.VISIBLE
                applyServerConfig.isEnabled = true
                for (i in 0..(params.size - 1)) {
                    configParamAdapter.setIsEditing(isChecked, i, params[i])
                }
            } else {
                TrackHelper.track().event("מיפוי ענן","שינוי הגדרות").name("הכנסת סיסמא לא תקינה").with(tracker)
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
        TrackHelper.track().screen("/הזנת סיסמה").with(tracker)
        dialog.show()
    }
}
