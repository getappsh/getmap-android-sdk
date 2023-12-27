package com.example.example_app

import android.app.Dialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapDeliveryState
import com.ngsoft.getapp.sdk.models.MapDownloadData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.qualifiedName

    private var progressDialog: ProgressDialog? = null

    private var downloadDialog: Dialog? = null
    private var progressBar: ProgressBar? = null

    private lateinit var service: GetMapService
    private lateinit var updateDate: LocalDateTime
    private lateinit var selectedProduct: DiscoveryItem

    private lateinit var selectedProductView: TextView
    private lateinit var deliveryButton: Button

    private lateinit var downoadnTestButton: Button

    private var downloadId: String? = null


    private lateinit var recyclerView: RecyclerView
    private lateinit var downloadListAdapter: DownloadListAdapter
    private val downloadList = mutableListOf<MapDownloadData>()


    private val onDelete: (String) -> Unit = { id ->
        GlobalScope.launch(Dispatchers.IO) {
            service.deleteMap(id)
        }
    }

    private val onCancel: (String) -> Unit = { id ->
        GlobalScope.launch(Dispatchers.IO) {
            service.cancelDownload(id)
        }
    }

    private val onResume: (String) -> Unit = { id ->
        GlobalScope.launch(Dispatchers.IO) {
            service.resumeDownload(id, downloadStatusHandler)
        }
    }
    private val downloadStatusHandler :(MapDownloadData) -> Unit = { data ->
        Log.d(TAG, "onDelivery data id: ${data.id}")

        runOnUiThread {
            val position = downloadListAdapter.getPositionById(data.id)
            if (position != -1) {

                if(data.deliveryStatus == MapDeliveryState.DELETED){
                    downloadList.removeAt(position)
                    downloadListAdapter.notifyItemRemoved(position)
                }else{
                    downloadList[position] = data
                    downloadListAdapter.notifyItemChanged(position)
                }
            } else {
                downloadList.add(0, data)
                downloadListAdapter.notifyItemInserted(0)
                print(downloadList[0])
            }

        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        if (!Environment.isExternalStorageManager()){
//            val intent = Intent()
//            intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//            val uri = Uri.fromParts("package", this.packageName, null)
//            intent.data = uri
//            startActivity(intent)
//        }

        val cfg = Configuration(
            "http://getapp-dev.getapp.sh:3000",
//            "http://getapp-test.getapp.sh:3000",
//            "http://localhost:3333",
//            "http://192.168.2.26:3000",
            "rony@example.com",
            "rony123",
//            File("/storage/1115-0C18/com.asio.gis").path,
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).path,
            16,
            5,5,
            null
        )


        service = GetMapServiceFactory.createAsioSdkSvc(this@MainActivity, cfg)
        dismissLoadingDialog()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        downloadListAdapter = DownloadListAdapter(downloadList, onDelete, onCancel, onResume)
        recyclerView.adapter = downloadListAdapter



        selectedProductView = findViewById<TextView>(R.id.selectedProduct)

        val discovery = findViewById<Button>(R.id.discovery)
        discovery.setOnClickListener {
            this.onDiscovery()
        }


        deliveryButton = findViewById<Button>(R.id.delivery)
        deliveryButton.isEnabled = false
        deliveryButton.setOnClickListener {
            this.onDelivery()
        }

        downoadnTestButton = findViewById<Button>(R.id.d_test)

        downoadnTestButton.setOnClickListener{

            GlobalScope.launch(Dispatchers.IO) {
                var downloads = service.getDownloadedMaps()
                Log.d(TAG, "onCreate - downloads size before ${downloads.size}")
                service.synchronizeMapData()
                refreshAll()
                downloads = service.getDownloadedMaps()
                Log.d(TAG, "onCreate - downloads size after ${downloads.size}")

            }
        }

    }
    private fun refreshAll(){
        Log.d(TAG, "refreshAll")
        GlobalScope.launch(Dispatchers.IO){
            val maps = service.getDownloadedMaps()
            downloadList.clear()
            downloadList.addAll(maps)
            runOnUiThread { downloadListAdapter.notifyDataSetChanged() }

            maps.forEach{map->
                service.registerDownloadHandler(map.id!!, downloadStatusHandler)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        refreshAll()
    }

    private fun onDiscovery(){
        Log.d(TAG, "onDiscovery");
        showLoadingDialog("Discovery")
        GlobalScope.launch (Dispatchers.IO) {
            val props = MapProperties("dummy product","1,2,3,4",false)
            try {
                val products = service.getDiscoveryCatalog(props)
                Log.d(TAG, "discovery products: " + products);

                launch(Dispatchers.Main) {
                    // Display the response in an AlertDialog
                    dismissLoadingDialog()
                    discoveryDialogPicker(products)

                }
            }catch (e: Exception) {
                // Handle any exceptions here
                Log.e(TAG, "error: "+ e );
                launch(Dispatchers.Main) {
                    dismissLoadingDialog()

                }
            }

        }

    }


    private fun onDelivery(){
        Log.d(TAG, "onDelivery: ");
        GlobalScope.launch(Dispatchers.IO){


            val props = MapProperties(
                selectedProduct.id,
//                "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
                "34.46264651,31.48939470,34.46454410,31.49104920",
//                "34.33390515,31.39424664,34.33937683,31.39776380",
//                "34.46087927,31.48921097,34.47834067,31.50156334"
                false
            )
            val id = service.downloadMap(props, downloadStatusHandler);

            downloadId = id
            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
//            GlobalScope.launch(Dispatchers.Main){
//                showLoadingDialog("Download file id: $id", id)
//            }
        }

    }

    private fun discoveryDialogPicker(products: List<DiscoveryItem>){
        Log.d(TAG, "dialogPicker")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose product")

        val productsStrings = products.map { it.productName }.toTypedArray()
        val checkedItem = -1
        builder.setSingleChoiceItems(productsStrings, checkedItem) { dialog, which ->
            selectedProduct = products[which]
            Log.d(TAG, "dialogPicker: selected item " + selectedProduct.productName)

            selectedProductView.setText("Selected Product: " + selectedProduct.productName)
            deliveryButton.isEnabled = true
            updateDate = selectedProduct.ingestionDate!!.toLocalDateTime()

        }


// add OK and Cancel buttons
        builder.setPositiveButton("OK") { dialog, which ->

        }
        builder.setNegativeButton("Cancel", null)

// create and show the alert dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun showMessageDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Response Data")

        // Set the response data as the message in the AlertDialog
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }


    private fun showLoadingDialog(title: String, id: String? = null) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle(title)
        progressDialog?.setMessage("Loading...") // Set the message to be displayed
        progressDialog?.setCancelable(false) // Prevent users from dismissing the dialog
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_SPINNER) // Use a spinner-style progress indicator
        if(id != null){
            progressDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel Download",
                DialogInterface.OnClickListener { dialog, which ->
                    this.service.cancelDownload(id)
                    progressDialog?.dismiss() //dismiss dialog
                })
        }
        progressDialog?.show()

    }

    // Call this function to dismiss the loading dialog
    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }
}