package com.example.example_app

import PasswordDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.example_app.models.NebulaParam.NebulaParamAdapter
import com.example.example_app.models.NebulaParam.NebulaParam
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

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
            NebulaParam("URL", service.config.baseUrl),
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
                val passwordDialog =
                    PasswordDialog(this, params, nebulaParamAdapter, isChecked, editConf)
                passwordDialog.show()
            } else {

                if (params.get(6).value == "" || service.config.maxMapAreaSqKm != params.get(6).value.toLong()){
                    Toast.makeText(this,"You can't change the maxMapAreaSqKm field ! ",Toast.LENGTH_LONG).show()
                    finish()
                }
                if ( params.get(13).value == "" || service.config.mapMinInclusionPct != params.get(13).value.toInt()){
                    Toast.makeText(this,"You can't change the Inclusion field ! ",Toast.LENGTH_LONG).show()
                    finish()
                }
                if (params.get(1).value != "")service.config.downloadRetry = params.get(1).value.toInt()
                if (params.get(2).value != "")service.config.deliveryTimeoutMins = params.get(2).value.toInt()
                if (params.get(3).value != "")service.config.matomoUrl = params.get(3).value
               if (params.get(4).value != "") service.config.matomoUpdateIntervalMins = params.get(4).value.toInt()
               if (params.get(5).value != "") service.config.maxMapSizeInMB = params.get(5).value.toLong()
               if (params.get(7).value != "") service.config.maxParallelDownloads = params.get(7).value.toInt()
               if (params.get(8).value != "") service.config.minAvailableSpaceMB = params.get(8).value.toLong()
               if (params.get(9).value != "") service.config.periodicConfIntervalMins = params.get(9).value.toInt()
               if (params.get(10).value != "") service.config.periodicInventoryIntervalMins = params.get(10).value.toInt()
               if (params.get(11).value != "") service.config.matomoSiteId = params.get(11).value
               if (params.get(12).value != "") service.config.matomoDimensionId = params.get(12).value

                    if (params.get(0).value != service.config.baseUrl) {
                        try {
                            instance.resetService()
                            instance.initService(this, SaveConfiguration(params))
                        } catch (_: Exception) {
                            Log.i("There is a BIG problem", "There is a problem")
                        }
                    }

//                service = SaveConfiguration(params, instance, this).service
                for (i in 0..(params.size - 1)) {
                    nebulaParamAdapter.setIsEditing(isChecked,i)
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
        serviceparams: Array<NebulaParam>,
//        instance: MapServiceManager,
//        context: Context,
    ): Configuration {
        var pathSd: String = intent.getStringExtra("pathSd").toString()
//        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        val imei = telephonyManager.imei
        val cfg = Configuration(
            serviceparams.get(0).value,
            "rony@example.com",
            "rony123",
            //            File("/storage/1115-0C18/com.asio.gis").path,
            //            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
            pathSd,
            16,
//            imei //Talk with Ronny and with asio
            null
        )
        return cfg
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


//Make an interface that will manage all the config and save it into the sharedpreference,
// We will just have to call the function to do the job, like the sdk