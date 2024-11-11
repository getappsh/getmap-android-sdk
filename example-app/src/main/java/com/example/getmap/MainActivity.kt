package com.example.getmap

//import com.arcgismaps.geometry.Point
import GetApp.Client.models.MapConfigDto
import MapDataMetaData
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.getmap.airwatch.AirWatchSdkManager
import com.example.getmap.matomo.MatomoTracker
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.ngsoft.getapp.sdk.BuildConfig
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.MapFileManager
import com.ngsoft.getapp.sdk.Pref
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.matomo.sdk.Matomo
import org.matomo.sdk.Tracker
import org.matomo.sdk.TrackerBuilder
import org.matomo.sdk.extra.TrackHelper
import java.time.LocalDateTime
import java.util.Base64

@RequiresApi(Build.VERSION_CODES.R)
class MainActivity : AppCompatActivity(), DownloadListAdapter.SignalListener {

    private var tracker: Tracker? = null
    private val TAG = MainActivity::class.qualifiedName
    private lateinit var mapServiceManager: MapServiceManager
    private var progressDialog: ProgressDialog? = null
    private lateinit var updateDate: LocalDateTime
    private lateinit var selectedProduct: DiscoveryItem
    private var availableSpaceInMb: Double = 0.0
    private var isReplacingActivity = false
    private val phoneNumberPermissionCode = 100
    private var phoneNumber = ""
    private val sdkAirWatchSdkManager = AirWatchSdkManager(this)
    private var imeiEven = ""

    companion object {
        var count = 0
    }

    //    private lateinit var selectedProductView: TextView
    private lateinit var deliveryButton: Button
    private lateinit var scanQRButton: Button
    private lateinit var syncButton: ImageButton

    //    private lateinit var mapServiceManager: MapServiceManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadListAdapter: DownloadListAdapter

    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }
    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (mapServiceManager.isInit) {
            CoroutineScope(Dispatchers.Default).launch { mapServiceManager.service.synchronizeMapData() }
        }
    }

    private val popUp = PopUp()

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapServiceManager = MapServiceManager.getInstance()

        if (!Environment.isExternalStorageManager()) {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            val uri = Uri.fromParts("package", this.packageName, null)
            intent.data = uri
            startForResult.launch(intent)
        }
        val availableSpace = findViewById<TextView>(R.id.AvailableSpace)
        availableSpace.text = getAvailableSpace()
        if (!mapServiceManager.isInit) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)

            sdkAirWatchSdkManager.startRetrying()
            var serialNumber: String? =
                getSharedPreferences("wit_player_shared_preferences", 0).getString(
                    "serialNumber",
                    "serialNumber"
                ).toString()
            Log.i("AIRWATCH SERIAL_NUMBER", serialNumber.toString())

            val imeiSharedPref = getSharedPreferences("imeiValue", Context.MODE_PRIVATE)
            imeiEven = imeiSharedPref.getString("imei_key", "").toString()
            if (imeiEven == "") {
                try {
                    imeiEven = sdkAirWatchSdkManager.imei
                    imeiSharedPref.edit().putString("imei_key", imeiEven).apply()
                } catch (e: Exception) {
                    Log.d("Error", "Error getting Imei")
                    Log.d("Error", e.toString())
                }
                Log.d("AIRWATCH", "The Imei from airwatch is : $imeiEven")
            } else {
                Log.d("AIRWATCH", "The Imei from sharedpref is : $imeiEven")
            }

            var url = Pref.getInstance(this).baseUrl
            Log.i("$TAG - AIRWATCH", "Url of AIRWATCH: $url")
            if (url.isEmpty()) {
                url = BuildConfig.BASE_URL
                Log.d("$TAG - AIRWATCH", "URL is empty, new url is $url")
            }

            val cfg = Configuration(
                url,
                BuildConfig.USERNAME,
                BuildConfig.PASSWORD,
                16,
                imeiEven
            )

            try {
                mapServiceManager.initService(applicationContext, cfg)
            } catch (_: Exception) {
            }
        }

        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes;
        val tp = mapServiceManager.service.config.targetStoragePolicy
        if ((tp == MapConfigDto.TargetStoragePolicy.sDOnly || tp == MapConfigDto.TargetStoragePolicy.sDThenFlash) && storageList.getOrNull(
                1
            )?.directory?.absoluteFile == null
        ) {
            Toast.makeText(applicationContext, "Please insert a SdCard !", Toast.LENGTH_SHORT)
                .show()
        }
        tracker = MatomoTracker.getTracker(this)
        tracker?.userId = imeiEven
//        service = GetMapServiceFactory.createAsioSdkSvc(this@MainActivity, cfg)
//        service.setOnInventoryUpdatesListener {
//            val data = it.joinToString()
//            runOnUiThread { Toast.makeText(this, data, Toast.LENGTH_LONG).show() }
//            Log.d(TAG, "onCreate - setOnInventoryUpdatesListener: $data")
//
//        }

        dismissLoadingDialog()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        downloadListAdapter = DownloadListAdapter({ bId, mapId, pathSd ->
            when (bId) {
                DownloadListAdapter.RESUME_BUTTON_CLICK -> onResume(mapId)
                DownloadListAdapter.CANCEL_BUTTON_CLICK -> onCancel(mapId)
                DownloadListAdapter.DELETE_BUTTON_CLICK -> onDelete(mapId)
                DownloadListAdapter.QR_CODE_BUTTON_CLICK -> generateQrCode(mapId)
                DownloadListAdapter.UPDATE_BUTTON_CLICK -> updateMap(mapId)
                DownloadListAdapter.ITEM_VIEW_CLICK -> itemViewClick(
                    mapId
                )
            }
        }, mapServiceManager, this)
        //Set the adapter to listen to changes
        downloadListAdapter.addListener(this)

        recyclerView.adapter = downloadListAdapter
        //Separator code between each download, optional
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                baseContext,
                layoutManager.orientation
            )
        )
        mapServiceManager.service.getDownloadedMapsLive().observe(this, Observer {
            Log.d(TAG, "onCreate - data changed ${it.size}")
            downloadListAdapter.saveData(it)
            var atLeastOneUpdated = it.any {
                it.isUpdated == false
            }
            if (atLeastOneUpdated) {
                syncButton.visibility = View.VISIBLE
            } else {
                syncButton.visibility = View.INVISIBLE
            }
        })
        val swipeRecycler = findViewById<SwipeRefreshLayout>(R.id.refreshRecycler)
//        getTracker()
        swipeRecycler.setOnRefreshListener {
            TrackHelper.track().event("מיפוי ענן", "ניהול בולים").name("רענון")
                .with(tracker)
            CoroutineScope(Dispatchers.IO).launch {
                CoroutineScope(Dispatchers.Default).launch {
//                    tracker?.dispatch()
                    mapServiceManager.service.synchronizeMapData()
                }
            }
            val deleteFail = findViewById<ImageButton>(R.id.deleteFail)
            deleteFail.visibility = View.INVISIBLE
            showDeleteFailedBtn(deleteFail)

            swipeRecycler.isRefreshing = false
        }

        val discovery = findViewById<Button>(R.id.discovery)
        discovery.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val sizeExceeded = withContext(Dispatchers.IO) {
                    MapFileManager(this@MainActivity).isInventorySizeExceedingPolicy()
                }
                Log.i("SIZE EXCEDEED", "$sizeExceeded")
                if (availableSpaceInMb > mapServiceManager.service.config.minAvailableSpaceMB && !sizeExceeded) {
                    val count = withContext(Dispatchers.IO) {
                        var count = 0
                        mapServiceManager.service.getDownloadedMaps().forEach { m ->
                            if (m.statusMsg == "בקשה נשלחה" || m.statusMsg == "בקשה בהפקה" || m.statusMsg == "בהורדה") {
                                count += 1
                            }
                        }
                        count
                    }

                    if (count < mapServiceManager.service.config.maxParallelDownloads) {
                        this@MainActivity.onDiscovery()
                    } else {
                        Toast.makeText(
                            applicationContext,
                            "יש כבר מספר הורדות מקסימלי",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "ניצלת את מכסת האחסון המקסימלית לבולים במכשיר, מחק בולים קיימים כדי להמשיך",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        //Matomo tracker
        // The `Tracker` instance from the previous step
        // Track a screen view
        TrackHelper.track().screen("מסך ראשי")
            .with(tracker)
        //Phone number
//        requestPhonePermission()

        // Monitor your app installs
        TrackHelper.track().download().with(tracker)
        //Example of an event for matomo, have to put differents per action
//        TrackHelper.track().event("current-time", "234").name("mills").value(System.currentTimeMillis().toFloat()).with(tracker);

        CoroutineScope(Dispatchers.Default).launch { mapServiceManager.service.synchronizeMapData() }
        syncButton = findViewById(R.id.Sync)
        syncButton.setOnClickListener {
            TrackHelper.track()
                .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), "מעדכן בול")
                .screen("/Popup/עדכון כלל הבולים")
                .with(tracker)
            popUp.recyclerView = recyclerView
            popUp.type = "update"
            popUp.textM = "האם אתה בטוח שאתה רוצה לעדכן את כל המפות?"
            popUp.tracker = tracker
            if (count == 0) {
                count += 1
                popUp.show(supportFragmentManager, "update")
            }

        }

        scanQRButton = findViewById(R.id.scanQR)
        scanQRButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val sizeExceeded = withContext(Dispatchers.IO) {
                    MapFileManager(this@MainActivity).isInventorySizeExceedingPolicy()
                }

                if (availableSpaceInMb > mapServiceManager.service.config.minAvailableSpaceMB && !sizeExceeded) {
                    barcodeLauncher.launch(ScanOptions())
                    TrackHelper.track().screen("/קבלת בול בסריקה").with(tracker)
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "ניצלת את מכסת האחסון המקסימלית לבולים במכשיר, מחק בולים קיימים כדי להמשיך",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }


        val settingButton = findViewById<ImageButton>(R.id.SettingsButton)
        settingButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            isReplacingActivity = true
            finish()
        }

        registerReceiver(
            SystemTestReceiver,
            IntentFilter(SystemTestReceiver.ACTION_RUN_SYSTEM_TEST)
        )

        val deleteFail = findViewById<ImageButton>(R.id.deleteFail)
        deleteFail.visibility = View.INVISIBLE
        showDeleteFailedBtn(deleteFail)

        deleteFail.setOnClickListener {
            val dialogBuilder = android.app.AlertDialog.Builder(this)
            TrackHelper.track().screen("/מחיקת תקולים").with(tracker)
            dialogBuilder.setMessage("האם למחוק את כל ההורדות שנכשלו בהורדה?")
            dialogBuilder.setPositiveButton("כן") { _, _ ->
                TrackHelper.track()
                    .event("מיפוי ענן", "ניהול בקשות").name("מחיקת כלל בקשות התקולות")
                    .with(tracker)
                GlobalScope.launch(Dispatchers.IO) {
                    mapServiceManager.service.getDownloadedMaps().forEach { map ->
                        if (map.statusMsg == "נכשל") {
                            mapServiceManager.service.deleteMap(map.id!!)
                        }
                    }
                }
                deleteFail.visibility = View.INVISIBLE
            }
            dialogBuilder.setNegativeButton("לא", null)
            val popUpMessage = dialogBuilder.create()
            popUpMessage.show()
        }

        val pdfView = findViewById<PDFView>(R.id.pdfView)
        pdfView.visibility = View.INVISIBLE
        val pdFile = findViewById<ImageButton>(R.id.pdfFile)
        pdFile.setOnClickListener {
            TrackHelper.track().screen("/מדריך למשתמש").with(tracker)
            pdfView.visibility = View.VISIBLE
            pdfView.fromAsset("strategy.pdf").load()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pdfView.visibility == View.VISIBLE) {
                    pdfView.visibility = View.INVISIBLE
                    TrackHelper.track().screen("/מסך ראשי").with(tracker)
                }
            }
        })
    }

//    override fun onResume() {
//        super.onResume()
//        TrackHelper.track().screen("מסך ראשי")
//            .with(tracker)
//        tracker?.dispatch()
//        mapServiceManager = MapServiceManager.getInstance()
//        Log.d("a", "sa")
//    }

    private fun showDeleteFailedBtn(deleteFail: ImageButton) {
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.getDownloadedMaps().forEach { map ->
                if (map.statusMsg == "נכשל") {
                    withContext(Dispatchers.Main) {
                        deleteFail.visibility = View.VISIBLE
                    }
                    return@forEach
                }
            }
        }
    }

    override fun onDestroy() {
        if (!isReplacingActivity) {
            tracker?.dispatch()
            Log.d("getmap", "matomo send when on destroy")
        }
        super.onDestroy()
    }
    //Telephone Number of the Olar
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == phoneNumberPermissionCode) {
//            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
//                // Permissions granted
//                val telephonyManager = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
//                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ||
//                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED) {
//
//                    val phoneNumber = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)  ?: "אין למכשיר מספר טלפון"
//                    Log.i("PhoneNumber", "phoneNumber: $phoneNumber")
//                }
//            } else {
//                // Permissions denied
//                Log.i("PhoneNumber", "Permissions denied")
//            }
//        }
//    }
//
//
//    private fun requestPhonePermission() {
//        requestPermissions(
//            arrayOf(
//                android.Manifest.permission.READ_PHONE_STATE,
//                android.Manifest.permission.READ_SMS,
//                android.Manifest.permission.READ_PHONE_NUMBERS
//            ), phoneNumberPermissionCode
//        )
//    }


    private fun onDiscovery() {

        TrackHelper.track().screen("/בחירת תיחום").with(tracker)
        Log.d(TAG, "onDiscovery");
        showLoadingDialog("פותח את המפה")
        GlobalScope.launch(Dispatchers.IO) {
            val props = MapProperties("dummy product", "1,2,3,4", false)
            try {

                val products = mapServiceManager.service.getDiscoveryCatalog(props)
                Log.d(TAG, "discovery products: " + products);
                products.forEach { product ->
                    Log.d(
                        "products1",
                        "Id : ${product.id} And Coordinates : ${product.footprint}"
                    )
                }
                launch(Dispatchers.Main) {
                    // Display the response in an AlertDialog
                    dismissLoadingDialog()
                    DiscoveryProductsManager.getInstance().updateProducts(products)

                    val intent = Intent(this@MainActivity, MapActivity::class.java)
                    startActivity(intent)
                    isReplacingActivity = true
                    finish()
//                    discoveryDialogPicker(products)

                }
            } catch (e: Exception) {
                // Handle any exceptions here
                Log.e(TAG, "error: " + e);
                launch(Dispatchers.Main) {
                    dismissLoadingDialog()
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                    Log.i("hghfhffhg", e.message!!)
                }
                TrackHelper.track().event("מיפוי ענן", "ניהול שגיאות").name("תקלה בבחירת תיחום")
                    .with(tracker)
            }

        }

    }

    private fun onDelivery(first: Point, second: Point) {
        Log.d(TAG, "onDelivery: ");
        GlobalScope.launch(Dispatchers.IO) {

//            service.purgeCache()

            val props = MapProperties(
                "selectedProduct.id",
//                "34.46641783,31.55079535, 34.47001187,31.55095355, 34.4700189, 31.553150863,34.46641783, 31.55318508, 34.46641783, 31.55079535",
//                    "34.50724201341369,31.602641553384572,34.5180453565571,31.59509118055151,34.50855899068993,31.5815177494226,34.497755647546515,31.589068122255644,34.50724201341369,31.602641553384572",
//                "34.47956403,31.52202192,34.51125354,31.54650531",
//                "34.33390512,31.39424661,34.33937683,31.39776391",// json dose not exist on s3 for this bBox
                "${first.y},${first.x},${second.y},${second.x}",

                false
            )
            val id = mapServiceManager.service.downloadMap(props);
            if (id == null) {
                this@MainActivity.runOnUiThread {
                    // This is where your UI code goes.
                    Toast.makeText(
                        applicationContext,
                        "The map already exists, please choose another Bbox",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
//            val availableSpace = findViewById<TextView>(R.id.AvailableSpace)
//            availableSpace.text = GetAvailableSpaceInSdCard()

            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
        }

    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var index = 0
        while (size > 1024 && index < units.size - 1) {
            size /= 1024
            index++
        }
        return String.format("%.2f %s", size, units[index])
    }

    private fun getAvailableSpace(): String {
        val availableBytes = MapFileManager(this).getAvailableSpaceByPolicy()
        availableSpaceInMb = availableBytes.toDouble() / (1024 * 1024)
        return "מקום פנוי להורדה: ${formatBytes(availableBytes)}"
    }
//    fun GetAvailableSpaceInSdCard(): String {
//        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
//        val storageList = storageManager.storageVolumes;
//        val flashMem = storageList[0].directory
//        val externalFilesDirs = getExternalFilesDirs(null)
//        var sdCardDirectory: File? = null
//
//        for (file in externalFilesDirs) {
//            if (Environment.isExternalStorageRemovable(file)) {
//                sdCardDirectory = file
//                break
//            }
//        }
//        var availableSd: String = ""
//        var availableFlash: String = ""
//        sdCardDirectory?.let {
//            availableSd = AvailableSpace(it)
//        }
//        flashMem.let {
//            availableFlash = AvailableSpace(it)
//        }
//        val shorter = shorterSpace(availableSd, availableFlash)
//        GetAvailableSpaceInMb(shorter)
//        return shorter
//    }

//    private fun GetAvailableSpaceInMb(shorter: String) {
//        availableSpaceInMb =
//            shorter.substringAfter(":").substring(1, shorter.substringAfter(":").length - 3)
//                .toDouble()
//        val availableSpaceType = shorter.substringAfterLast(" ")
//        if (availableSpaceType != "MB") {
//            availableSpaceInMb *= 1024
//        }
//    }

//    private fun shorterSpace(mem1: String, mem2: String): String {
//
//        if (mem1.contains("MB") && mem2.contains("GB")) {
//            return mem1
//        } else if ((mem1.contains("GB") && mem2.contains("GB")) || (mem1.contains("MB") && mem2.contains(
//                "MB"
//            ))
//        ) {
//            val mem1Number =
//                mem1.substringAfter(":").substring(1, mem1.substringAfter(":").length - 3)
//                    .toDouble()
//            val mem2Number =
//                mem2.substringAfter(":").substring(1, mem1.substringAfter(":").length - 3)
//                    .toDouble()
//            if (mem1Number > mem2Number) {
//                return mem2
//            } else return mem1
//        }
//        return mem2
//    }

//    fun AvailableSpace(it: File?): String {
//        val stat = StatFs(it?.absolutePath)
//        val bytesAvailable: Long = stat.blockSizeLong * stat.availableBlocksLong
//        val gigabytesAvailable = bytesAvailable.toDouble() / (1024 * 1024 * 1024)
//        val megabytesAvailable = bytesAvailable.toDouble() / (1024 * 1024)
//
//        return if (gigabytesAvailable >= 1) {
//            String.format("מקום פנוי להורדה: %.2f GB", gigabytesAvailable)
//        } else {
//            String.format("מקום פנוי להורדה: %.2f MB", megabytesAvailable)
//        }
//    }

//    private fun discoveryDialogPicker(products: List<DiscoveryItem>) {
//        Log.d(TAG, "dialogPicker")
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Choose product")
//
//        val productsStrings = products.map { it.productName }.toTypedArray()
//        val checkedItem = -1
//        builder.setSingleChoiceItems(productsStrings, checkedItem) { dialog, which ->
//            selectedProduct = products[which]
//            Log.d(TAG, "dialogPicker: selected item " + selectedProduct.productName)
//
////            selectedProductView.text = ("Selected Product:\n" + selectedProduct.productName)
//            deliveryButton.isEnabled = true
//            updateDate = selectedProduct.ingestionDate!!.toLocalDateTime()
//
//        }
//
//        builder.setPositiveButton("OK") { dialog, which ->
//
//        }
////        builder.setNegativeButton("Cancel", null)
//
//        val dialog = builder.create()
//        dialog.show()
//    }

    @SuppressLint("LongLogTag")
    private fun onDelete(id: String) {
        Log.i("onCreate Tracker Refreshea", "${tracker}")
        TrackHelper.track().screen("/מחיקה").with(tracker)
        popUp.textM = "האם אתה בטוח שאתה רוצה למחוק את הבול הזו?"
        popUp.mapId = id
        popUp.type = "delete"
        val deleteFail = findViewById<ImageButton>(R.id.deleteFail)
        popUp.deleteFailImage = deleteFail
        popUp.deleteFailFun = { showDeleteFailedBtn(deleteFail) }
        GlobalScope.launch(Dispatchers.IO) {
            val map = mapServiceManager.service.getDownloadedMap(id)
            if (map!!.fileName != null) {
                val endName = map.getJson()?.getJSONArray("region")?.get(0).toString() +
                        map.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                popUp.bullName = endName
            } else {
                popUp.bullName = ""
            }
        }
        if (count == 0) {
            count += 1
            popUp.show(supportFragmentManager, "delete")
        }
    }

    private fun onResume(id: String) {
        GlobalScope.launch(Dispatchers.IO) {

            val map = mapServiceManager.service.getDownloadedMap(id)
            if (map?.deliveryState == MapDeliveryState.ERROR) {
                TrackHelper.track()
//                    .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), endName)
                    .event("מיפוי ענן", "ניהול בקשות").name("אתחל")
                    .with(tracker)
            } else {
            val endName = map?.getJson()?.getJSONArray("region")?.get(0).toString() +
                    map?.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                TrackHelper.track()
                    .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), endName)
                    .event("מיפוי ענן", "ניהול בקשות").name("חידוש הורדה")
                    .with(tracker)

            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.resumeDownload(id)
        }

    }

    private fun onCancel(id: String) {
        TrackHelper.track().screen("/עצירת בקשה").with(tracker)
        GlobalScope.launch(Dispatchers.IO) {
            val map = mapServiceManager.service.getDownloadedMap(id)
            if (map!!.fileName != null) {
                val endName = map.getJson()?.getJSONArray("region")?.get(0).toString() +
                        map.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                popUp.bullName = endName
            } else {
                popUp.bullName = ""
            }
        }
        popUp.mapId = id
        popUp.type = "cancelled"
        popUp.textM = "האם לעצור את ההורדה ?"
        popUp.tracker = tracker
        if (count == 0) {
            count += 1
            popUp.show(supportFragmentManager, "cancelled")
        }
//        TrackHelper.track().event("cancelButton", "cancel-download-map").with(tracker)
//        GlobalScope.launch(Dispatchers.IO) {
//            mapServiceManager.service.cancelDownload(id)
//        }
    }

    // Function that will update the AvailableSpace
    override fun onSignalSpace() {
        val availableSpace = findViewById<TextView>(R.id.AvailableSpace)
        availableSpace.text = getAvailableSpace()
    }


    override fun onSignalDownload() {
//        if (downloadListAdapter.availableUpdate > 0)
//            syncButton.visibility = View.VISIBLE
//        else
//            syncButton.visibility = View.INVISIBLE
    }


    private fun generateQrCode(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val map = mapServiceManager.service.getDownloadedMap(id)
                val qrCode = mapServiceManager.service.generateQrCode(id, 1000, 1000)
                val jsonText =
                    Gson().fromJson(map?.getJson().toString(), MapDataMetaData::class.java)
                val region = jsonText.region[0]
                val name =
                    region + map?.fileName?.substringAfterLast('_')?.substringBefore('Z') + "Z"
                runOnUiThread {
                    TrackHelper.track()
                        .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), name)
                        .event("מיפוי ענן", "שיתוף")
                        .name("שליחת בול בסריקה").with(tracker)
                    showQRCodeDialog(qrCode)
                }
            } catch (e: Exception) {
                val map = mapServiceManager.service.getDownloadedMap(id)
                val jsonText =
                    Gson().fromJson(map?.getJson().toString(), MapDataMetaData::class.java)
                val region = jsonText.region[0]
                val name =
                    region + map?.fileName?.substringAfterLast('_')?.substringBefore('Z') + "Z"
                TrackHelper.track()
                    .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), name)
                    .event("מיפוי ענן", "ניהול שגיאות").name("תקלה ביצירת qr")
                    .with(tracker)
                runOnUiThread { showErrorDialog(e.message.toString()) }
            }

        }
    }

    private fun updateMap(id: String) {
        GlobalScope.launch(Dispatchers.IO) {
            mapServiceManager.service.downloadUpdatedMap(id)
        }
    }

    private fun itemViewClick(id: String) {
        var currMap: MapData? = null
        GlobalScope.launch(Dispatchers.IO) {
            currMap = mapServiceManager.service.getDownloadedMap(id)!!
            withContext(Dispatchers.Main) {
                if (currMap?.isUpdated == false) {
                    TrackHelper.track().dimension(
                        mapServiceManager.service.config.matomoDimensionId.toInt(),
                        "עדכן בול"
                    ).screen(this@MainActivity)
                        .with(tracker)
                    GlobalScope.launch(Dispatchers.IO) {
                        val map = mapServiceManager.service.getDownloadedMap(id)
                        if (map!!.fileName != null) {
                            val endName = map.getJson()?.getJSONArray("region")?.get(0).toString() +
                                    map.fileName!!.substringAfterLast('_')
                                        .substringBefore('Z') + "Z"
                            popUp.bullName = endName
                        } else {
                            popUp.bullName = ""
                        }
                    }
                    popUp.mapId = id
                    popUp.type = "updateOne"
                    popUp.recyclerView = recyclerView
                    popUp.textM = "האם לבצע עדכון מפה ?"
                    if (count == 0) {
                        count += 1
                        popUp.show(supportFragmentManager, "updateOne")
                    }
                }
            }
        }

//        } else {
//
//            GlobalScope.launch(Dispatchers.IO) {
//                val map = mapServiceManager.service.getDownloadedMap(id)
//                val str = map?.let {
//                    "The id is =${it.id}, \n" +
////                        "footprint=${it.footprint}, \n" +
////                        "fileName=${it.fileName}, \n" +
////                        "jsonName=${it.jsonName}, \n" +
////                        "deliveryStatus=${it.deliveryStatus}, \n" +
////                        "url=${it.url}, \n" +
//                            "The status is:${it.statusMsg}, \n" +
//                            "The download is:${it.progress}, \n" +
////                        "errorContent=${it.errorContent}, \n" +
//                            "The bbox is updated:${it.isUpdated}, \n "
//                }.toString()
//                runOnUiThread { showDialog(str) }
//            }
//        }


    }

    private fun showErrorDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun showDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)
        val dialog = builder.create()
        dialog.show()
    }

    private fun showLoadingDialog(title: String, id: String? = null) {

//        var percentages = findViewById<TextView>(R.id.Percentages)
////        var percentages= R.id.Percentages
//
////        percentages = i
//        Log.i("PROGRESSBAR", "showLoadingDialog: ")
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle(title)
        progressDialog?.setMessage("מפה בטעינה...") // Set the message to be displayed
        progressDialog?.setCancelable(false) // Prevent users from dismissing the dialog
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_SPINNER) // Use a spinner-style progress indicator
        if (id != null) {
            progressDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, "בטל",
                DialogInterface.OnClickListener { dialog, which ->
                    mapServiceManager.service.cancelDownload(id)
                    progressDialog?.dismiss() //dismiss dialog
                })
        }
        progressDialog?.show()

    }

    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }


    private fun showQRCodeDialog(qrCodeBitmap: Bitmap) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_qr_code, null)

        val imageViewQRCode: ImageView = dialogView.findViewById(R.id.imageViewQRCode)
        imageViewQRCode.setImageBitmap(qrCodeBitmap)

        if (count == 0) {
            count = 1
            builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    count = 0
                    dialog.dismiss()
                }
                .show()
        }
    }


    private val barcodeLauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents == null) {
        } else {
            Toast.makeText(this, "Scanned: " + result.contents, Toast.LENGTH_LONG).show()
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    mapServiceManager.service.processQrCodeData(result.contents)
//                    val map = mapServiceManager.service.getDownloadedMap(mapServiceManager.service.processQrCodeData(result.contents))
//                    val jsonText = Gson().fromJson(map?.getJson().toString(), MapDataMetaData::class.java)
//                    val region = jsonText.region[0]
//                    val name = region + "-" + map?.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
//                    val coordinates = map.footprint
//                    Log.i("mapName", name + coordinates)
                    withContext(Dispatchers.Main) {
                        TrackHelper.track()
//                            .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), name)
//                            .dimension(mapServiceManager.service.config.matomoDimensionId.toInt(), coordinates)
                            .event("מיפוי ענן", "שיתוף")
                            .name("קבלת בול בסריקה").with(tracker)
                    }
                } catch (e: Exception) {
//                    val map = mapServiceManager.service.getDownloadedMap(mapServiceManager.service.processQrCodeData(result.contents))
//                    val jsonText = Gson().fromJson(map?.getJson().toString(), MapDataMetaData::class.java)
//                    val region = jsonText.region[0]
//                    val name = region + "-" + map?.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                    TrackHelper.track().event("מיפוי ענן", "ניהול שגיאות")
                        .name("תקלה בקבלת בול בסריקה").with(tracker)
                    runOnUiThread { showErrorDialog(e.message.toString()) }
                }

            }
        }
    }

    fun getTracker(): Tracker {
        if (tracker == null) {
            tracker = TrackerBuilder.createDefault(
                "https://matomo-matomo.apps.okd4-stage-getapp.getappstage.link/matomo.php", 1
            ).build(
                Matomo.getInstance(this)
            )
        }
        return tracker!!
    }
}