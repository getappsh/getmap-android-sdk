package com.example.getmap

import GetApp.Client.models.MapConfigDto
import PasswordDialog
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.getmap.matomo.MatomoTracker
import com.example.getmap.models.ConfigParam.NebulaParamAdapter
import com.example.getmap.models.ConfigParam.NebulaParam
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.R)
class SettingsActivity : AppCompatActivity() {
    private lateinit var nebulaParamAdapter: NebulaParamAdapter

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        val instance = MapServiceManager.getInstance()
        val service = instance.service
        val recyclerView: RecyclerView = findViewById(R.id.nebula_recycler)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        var params = emptyArray<NebulaParam>()
        val tracker: Tracker?
        tracker = MatomoTracker.getTracker(this)
        nebulaParamAdapter = NebulaParamAdapter(params)

        recyclerView.adapter = nebulaParamAdapter
        loadConfig(service)
        params = nebulaParamAdapter.getParams()
        val version = findViewById<TextView>(R.id.textView)
        try {
            version.text = "version: " + applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName
        } catch (e: Exception) {
            version.text = ""
        }
        val lastInventory = findViewById<TextView>(R.id.last_inventory)
        val cancelButton = findViewById<Button>(R.id.cancel_button)
        val lastConfig = findViewById<TextView>(R.id.last_config)
        val lastServerConfig = findViewById<TextView>(R.id.last_server_config)
        val editConf = findViewById<ToggleButton>(R.id.Edit_toggle)
        val applyServerConfig = findViewById<Switch>(R.id.apply_server_config)
        TrackHelper.track().screen("/מסך טכנאי").with(tracker)
        applyServerConfig.isEnabled = false
        applyServerConfig.isChecked = service.config.applyServerConfig
        applyServerConfig.setOnCheckedChangeListener { _, isChecked ->
            service.config.applyServerConfig = isChecked
        }
        cancelButton.setOnClickListener {
            loadConfig(service)
            hideKeyboard()
            for (i in params.indices) {
                nebulaParamAdapter.setIsEditing(false, i, params[i])
            }
            cancelButton.visibility = View.INVISIBLE
            editConf.isChecked = false
            applyServerConfig.isEnabled = false
        }
        editConf.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                TrackHelper.track().screen("/מסך טכנאי").with(tracker)
                val passwordDialog =
                    PasswordDialog(
                        this, params, nebulaParamAdapter, true, editConf,
                        cancelButton, tracker, applyServerConfig
                    )
                passwordDialog.show()
            } else {
                hideKeyboard()
                params = nebulaParamAdapter.getParams()
                val hasChanged: LinkedHashMap<String, String> = hasChanged(service, params)
                if (hasChanged.isNotEmpty()) {
                    hasChanged.forEach { e ->
                        TrackHelper.track()
                            .event("מיפוי ענן", "שינוי הגדרות")
                            .name(" נתונים השתנו ב${e.key}")
                            .with(tracker)
                    }
                }
                val url = params[0].value
                if (url != service.config.baseUrl && url != "") {
                    try {
                        instance.resetService()
                        instance.initService(this, SaveConfiguration(params))
                    } catch (_: Exception) {
                        Log.i("There is a BIG problem", "There is a problem with the sdk instance")
                    }
                }
                saveLocalToService(params, service, this)
                service.config.applyServerConfig = applyServerConfig.isChecked
                loadConfig(service)
                for (i in params.indices) {
                    nebulaParamAdapter.setIsEditing(false, i, params[i])
                }
                cancelButton.visibility = View.INVISIBLE
                applyServerConfig.isEnabled = false
            }
        }

        lastConfig.text = "lastInventory: ${dateFormat(service.config.lastConfigCheck)}"
        lastServerConfig.text =
            "lastServerConfig: ${dateFormat(service.config.lastServerConfigUpdate)}"
        lastInventory.text = "lastConfig: ${dateFormat(service.config.lastInventoryCheck)}"

        val refreshButton = findViewById<ImageButton>(R.id.refresh_button_conf)
        refreshButton.setOnClickListener {
            TrackHelper.track().event("מיפוי ענן", "שינוי הגדרות")
                .name("רענון הגדרות")
                .with(tracker)
            rotateInfinitely(refreshButton)
            refreshButton.isEnabled = false
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    withContext(Dispatchers.Main){
                        lastConfig.text = "Loading..."
                        lastInventory.text = "Loading..."
                        lastServerConfig.text = "Loading..."
                    }
                    try {
                        service.fetchConfigUpdates()
                        service.fetchInventoryUpdates()
                    } catch (e: Exception) {
                        TrackHelper.track().dimension(instance.service.config.matomoDimensionId.toInt(), "הגדרות").event("מיפוי ענן", "שינוי הגדרות")
                            .name("ניסיון חיבור לשרת נכשל - תקלת רשת")
                            .with(tracker)
                        withContext(Dispatchers.Main) {
                        lastConfig.text = "lastConfig error: Network error occured"
                        Log.e("Fetch Config", e.message.toString())
                        lastServerConfig.text ="lastServerConfig error: Network error occured"
                        lastInventory.text ="lastInventory error: Network error occured"
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (!(lastConfig.text.contains("error"))) {
                        lastConfig.text =
                            "lastConfig: ${dateFormat(service.config.lastConfigCheck)}"
                    }
                    if (!(lastServerConfig.text.contains("error"))) {
                        lastServerConfig.text =
                            "lastServerConfig: ${dateFormat(service.config.lastServerConfigUpdate) }"
                    }
                    if (!(lastInventory.text.contains("error"))) {
                        lastInventory.text =
                            "lastInventory: ${dateFormat(service.config.lastInventoryCheck)}"
                    }
                    loadConfig(service)
                    refreshButton.isEnabled = true
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
//        val tracker = MatomoTracker.getTracker(this)
//        TrackHelper.track().screen("הגדרות").with(tracker)
    }

    private fun loadConfig(service: GetMapService) {
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
                "Periodic Config interval in",
                service.config.periodicConfIntervalMins.toString()
            ),
            NebulaParam(
                "Periodic Inventory interval in mins",
                service.config.periodicInventoryIntervalMins.toString()
            ),
            NebulaParam("Matomo site id", service.config.matomoSiteId),
            NebulaParam("Matomo dimension id", service.config.matomoDimensionId),
            NebulaParam("Min inclusion needed", service.config.mapMinInclusionPct.toString()),
            NebulaParam("Download Path", service.config.downloadPath),
            NebulaParam("Flash Storage Path", service.config.flashStoragePath),
            NebulaParam("Target Storage Policy", service.config.targetStoragePolicy.value, true),

            )
        nebulaParamAdapter.updateAll(params)
    }

    private fun SaveConfiguration(
        serviceParams: Array<NebulaParam>,
//        instance: MapServiceManager,
//        context: Context,
    ): Configuration {
//        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        val imei = telephonyManager.imei
        val cfg = Configuration(
            serviceParams[0].value,
            "rony@example.com",
            "rony123",
            16,
//            imei //Talk with Ronny and with asio
            null
        )
        return cfg
    }

    // Block that allow to hide the keyboard with touch on the screen
    fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (!inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)) {
            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            hideKeyboard()
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            hideKeyboard()
        }
        return super.onTouchEvent(event)
    }

}

private fun dateFormat(date: OffsetDateTime?): String? {
    return date?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd - HH:mm:ss"))
}

private fun saveLocalToService(
    params: Array<NebulaParam>,
    service: GetMapService,
    context: Context,
) {

    var targetTypes: HashMap<String, MapConfigDto.TargetStoragePolicy> = hashMapOf()
    targetTypes["SDOnly"] = MapConfigDto.TargetStoragePolicy.sDOnly
    targetTypes["FlashThenSD"] = MapConfigDto.TargetStoragePolicy.flashThenSD
    targetTypes["SDThenFlash"] = MapConfigDto.TargetStoragePolicy.sDThenFlash
    targetTypes["FlashOnly"] = MapConfigDto.TargetStoragePolicy.flashOnly

    var notifValidation: Toast? = null
    val reg = Regex("[a-zA-Z]")
    if (params[1].value != "")
        if (!params[1].value.contains(regex = reg))
            service.config.downloadRetry = params[1].value.toInt()
        else {
            NotifyValidity(notifValidation, context)
            params[1].value = service.config.downloadRetry.toString()
        }
    else {
        params[1].value = service.config.downloadRetry.toString()
        NotifyValidity(notifValidation, context)
    }
    if (params[2].value != "")
        if (!params[2].value.contains(regex = reg))
            service.config.deliveryTimeoutMins = params[2].value.toInt()
        else {
            NotifyValidity(notifValidation, context)
            params[2].value = service.config.deliveryTimeoutMins.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[2].value = service.config.deliveryTimeoutMins.toString()
    }
    if (params[3].value != "") service.config.matomoUrl = params[3].value
    if (params[4].value != "")
        if (!params.get(4).value.contains(regex = reg))
            service.config.matomoUpdateIntervalMins = params[4].value.toInt()
        else {
            NotifyValidity(notifValidation, context)
            params[4].value = service.config.matomoUpdateIntervalMins.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[4].value = service.config.matomoUpdateIntervalMins.toString()
    }
    if (params[5].value != "")
        if (!params[5].value.contains(regex = reg))
            service.config.maxMapSizeInMB = params[5].value.toLong()
        else {
            NotifyValidity(notifValidation, context)
            params[5].value = service.config.maxMapSizeInMB.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[5].value = service.config.maxMapSizeInMB.toString()
    }
    if (params[7].value != "")
        if (!params[7].value.contains(regex = reg))
            service.config.maxParallelDownloads = params[7].value.toInt()
        else {
            NotifyValidity(notifValidation, context)
            params[7].value = service.config.maxParallelDownloads.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[7].value = service.config.maxParallelDownloads.toString()
    }
    if (params[8].value != "")
        if (!params[8].value.contains(regex = reg))
            service.config.minAvailableSpaceMB = params[8].value.toLong()
        else {
            NotifyValidity(notifValidation, context)
            params[8].value = service.config.minAvailableSpaceMB.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[8].value = service.config.minAvailableSpaceMB.toString()
    }
    if (params[9].value != "")
        if (!params[9].value.contains(regex = reg)) {
            service.config.periodicConfIntervalMins = params[9].value.toInt()
        } else {
            NotifyValidity(notifValidation, context)
            params[9].value = service.config.periodicConfIntervalMins.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[9].value = service.config.periodicConfIntervalMins.toString()

    }
    if (params[10].value != "")
        if (!params[10].value.contains(regex = reg))
            service.config.periodicInventoryIntervalMins = params[10].value.toInt()
        else {
            NotifyValidity(notifValidation, context)
            params[10].value = service.config.periodicInventoryIntervalMins.toString()
        }
    else {
        NotifyValidity(notifValidation, context)
        params[10].value = service.config.periodicInventoryIntervalMins.toString()
    }
    if (params[16].value != "") {
        service.config.targetStoragePolicy = targetTypes[params[16].value]!!
    } else {
        NotifyValidity(notifValidation, context)
        params[16].value = service.config.targetStoragePolicy.toString()
    }

    if (params[11].value != "")
        service.config.matomoSiteId = params[11].value
    if (params[12].value != "")
        service.config.matomoDimensionId = params[12].value

}

private fun hasChanged(
    service: GetMapService,
    params: Array<NebulaParam>,
): LinkedHashMap<String, String> {
    var toReturn = LinkedHashMap<String, String>()

    if (params[0].value != "")
        if (service.config.baseUrl != params[0].value)
            toReturn["URL"] = params[0].value
    if (params[1].value != "")
        if (service.config.downloadRetry != params[1].value.toInt())
            toReturn["DownloadRetry"] = params[1].value
    if (params[2].value != "")
        if (service.config.deliveryTimeoutMins != params[2].value.toInt())
            toReturn["deliveryTimeoutMins"] = params[2].value
    if (params[3].value != "")
        if (service.config.matomoUrl != params[3].value)
            toReturn["matomoUrl"] = params[3].value
    if (params[4].value != "")
        if (service.config.matomoUpdateIntervalMins != params[4].value.toInt())
            toReturn["matomoUpdateIntervalMins"] = params[4].value
    if (params[5].value != "")
        if (service.config.maxMapSizeInMB != params[5].value.toLong())
            toReturn["maxMapSizeInMB"] = params[5].value
    if (params[7].value != "")
        if (service.config.maxParallelDownloads != params[7].value.toInt())
            toReturn["maxParallelDownloads"] = params[7].value
    if (params[8].value != "")
        if (service.config.minAvailableSpaceMB != params[8].value.toLong())
            toReturn["minAvailableSpaceMB"] = params[8].value
    if (params[9].value != "")
        if (service.config.periodicConfIntervalMins != params[9].value.toInt())
            toReturn["periodicConfIntervalMins"] = params[9].value
    if (params[10].value != "")
        if (service.config.periodicInventoryIntervalMins != params[10].value.toInt())
            toReturn["periodicInventoryIntervalMins"] = params[10].value
    if (params[11].value != "" && params[11].value.toInt() > 0)
        if (service.config.matomoSiteId != params[11].value)
            toReturn["matomoSiteId"] = params[11].value
    if (params[12].value != "")
        if (service.config.matomoDimensionId != params[12].value)
            toReturn["matomoDimensionId"] = params[12].value


    return toReturn
}

private fun NotifyValidity(notification: Toast?, context: Context) {
    var notifValidation = notification
    notifValidation?.cancel()
    notifValidation = Toast.makeText(context, "Please enter valid entry", Toast.LENGTH_SHORT)
    notifValidation?.show()
}

fun rotateInfinitely(view: View, duration: Long = 2500L): ObjectAnimator {
    val rotation = ObjectAnimator.ofFloat(view, View.ROTATION, 0F, 360F)
    rotation.duration = duration
    rotation.interpolator = LinearInterpolator()
    rotation.start()
    return rotation
}