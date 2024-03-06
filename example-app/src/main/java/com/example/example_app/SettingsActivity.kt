package com.example.example_app

import PasswordDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.example_app.models.NebulaParam.NebulaParamAdapter
import com.example.example_app.models.NebulaParam.NebulaParam
import com.ngsoft.getapp.sdk.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.memberProperties

@RequiresApi(Build.VERSION_CODES.R)
class SettingsActivity : AppCompatActivity() {
    private lateinit var nebulaParamAdapter: NebulaParamAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        var instance = MapServiceManager.getInstance()
        var service = instance.service
//        if (savedInstanceState == null) {
//            supportFragmentManager
//                .beginTransaction()
//                .replace(R.id.settings, SettingsFragment())
//                .commit()
//        }
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val params = arrayOf(
            intent.getStringExtra("URL")?.let { NebulaParam("URL", it) },
            NebulaParam("DownloadRetry", service.config.downloadRetry.toString()),
            NebulaParam("DeliveryTimeout in mins", service.config.deliveryTimeoutMins.toString()),
            NebulaParam("Matomo Url", service.config.matomoUrl),
            NebulaParam(
                "MatomoUpdateInterval in mins",
                service.config.matomoUpdateIntervalMins.toString()
            ),
            NebulaParam("Max size for Map in Mb", service.config.maxMapSizeInMB.toString()),
            NebulaParam("Max MapArea in SqKm", service.config.maxMapAreaSqKm.toString()),
            NebulaParam("Max parallel downloads", service.config.maxParallelDownloads.toString()),
            NebulaParam("Min available space in Mb", service.config.minAvailableSpaceMB.toString()),
            NebulaParam(
                "Periodic Config interval in mins",
                service.config.periodicConfIntervalMins.toString()
            ),
            NebulaParam(
                "Periodic Inventory interval in mins",
                service.config.periodicInventoryIntervalMins.toString()
            ),
            NebulaParam("Matomo site id", service.config.matomoSiteId),
            NebulaParam("Matomo dimension id", service.config.matomoDimensionId),
            NebulaParam("Min inclusion needed", service.config.mapMinInclusionPct.toString()),
        )

        val recyclerView: RecyclerView = findViewById(R.id.nebula_recycler)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        nebulaParamAdapter = NebulaParamAdapter(params)
        recyclerView.adapter = nebulaParamAdapter

        val lastInventory = findViewById<TextView>(R.id.last_inventory)
        val lastConfig = findViewById<TextView>(R.id.last_config)
        val lastServerConfig = findViewById<TextView>(R.id.last_server_config)
        val editConf = findViewById<ToggleButton>(R.id.Edit_toggle)
        editConf.setOnCheckedChangeListener { _, isChecked ->
//            val passwordLayout = findViewById<ConstraintLayout>(R.id.password_layout)
//            val back = findViewById<ImageButton>(R.id.back_nebula_password)
////            passwordLayout.visibility = View.VISIBLE
//            back.setOnClickListener{
////                passwordLayout.visibility = View.GONE
//            }
            if (isChecked) {
                val passwordDialog = PasswordDialog(
                    this, params, nebulaParamAdapter,
                    isChecked, editConf
                )
                passwordDialog.show()
            } else {
                service = SaveConfiguration(params, instance, this).service
                for (i in 0..(params.size - 1)) {
                    nebulaParamAdapter.setIsEditing(isChecked)
                }
            }
        }

        lastConfig.text = "lastInventory: ${dateFormat(service.config.lastInventoryCheck)}"
        lastServerConfig.text =
            "lastServerConfig: ${dateFormat(service.config.lastServerConfigUpdate)}"
        lastInventory.text = "lastConfig: ${dateFormat(service.config.lastConfigCheck)}"

        val refresh_text = findViewById<ImageButton>(R.id.refresh_button_conf)
        refresh_text.setOnClickListener {
            CoroutineScope(Dispatchers.Default).launch {

                lastConfig.text = "Loading..."
                lastInventory.text = "Loading..."
                lastServerConfig.text = "Loading..."

                try {
                    service.fetchConfigUpdates()
                    delay(1500)
                    lastConfig.text =
                        "lastInventory: ${dateFormat(service.config.lastInventoryCheck)}"
                    lastServerConfig.text =
                        "lastServerConfig: ${dateFormat(service.config.lastServerConfigUpdate)}"
                    lastInventory.text = "lastConfig: ${dateFormat(service.config.lastConfigCheck)}"


                } catch (e: Exception) {
                    lastConfig.text = "Connection error !"
                    lastServerConfig.text = "Connection error ! "
                    lastInventory.text = "Connection error !"
                }
            }
        }

    }

    private fun SaveConfiguration(
        serviceparams: Array<NebulaParam?>,
        instance: MapServiceManager,
        context: Context,
    ): MapServiceManager {
        var pathSd: String = intent.getStringExtra("pathSd").toString()
        var url: String = " "
        for (param in serviceparams) {
            if (param?.name == "URL") {
                url = param.value
                break
            }
        }
//        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        val imei = telephonyManager.imei
        val cfg = Configuration(
            url,
            "rony@example.com",
            "rony123",
            //            File("/storage/1115-0C18/com.asio.gis").path,
            //            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
            pathSd,
            16,
//            imei //Talk with Ronny and with asio
            null
        )
        try {
            instance.initService(applicationContext, cfg)
        } catch (_: Exception) {
        }
        return instance
    }
}

private fun dateFormat(date: OffsetDateTime?): String? {
    return date?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd - HH:mm:ss"))
}

//    class SettingsFragment : PreferenceFragmentCompat() {
//        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
//            setPreferencesFromResource(R.xml.root_preferences, rootKey)
//        }
//    }