package com.example.example_app

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.DownloadProgress
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.DownloadHebStatus
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showLoadingDialog("Login")


        val cfg = Configuration(
            "http://getapp-dev.getapp.sh:3000",
            "rony@example.com",
            "rony123",
            //currently downloads file to a path within the public external storage directory
            Environment.DIRECTORY_DOWNLOADS,
            16,
            5,5
        )

        service = GetMapServiceFactory.createAsioSdkSvc(this@MainActivity, cfg)
        dismissLoadingDialog()

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
        showLoadingDialog("Download file")
        GlobalScope.launch(Dispatchers.IO){

//            service.purgeCache()

            val props = MapProperties(
                selectedProduct.productId,
//                "34.76177215576172,31.841297149658207,34.76726531982422,31.8464469909668",
                "34.218144483292285,31.16202819895893,34.65786854261468,31.748690379853482",

                false
            )
            val downloadStatusHandler :(MapDownloadData) -> Unit = { data ->
                Log.d(TAG, "onDelivery: status ${data.deliveryStatus}, progress ${data.downloadProgress}");
            }

            service.downloadMap(props, downloadStatusHandler);
//            val tilesUpdates = service.getExtentUpdates(props, updateDate)
//            Log.d(TAG, "onDelivery: tilesUpdates " + tilesUpdates.toString());

//            var downloadedCount = 0
//            val downloadProgressHandler: (DownloadProgress) -> Unit = {
//                Log.d(TAG, "processing download progress=$it event...")
//                downloadedCount = it.packagesProgress.count { pkg ->  pkg.isCompleted }
//            }
//
//            val delivered = service.deliverExtentTiles(tilesUpdates, downloadProgressHandler)
//            Log.d(TAG, "onDelivery: delivered " + delivered)

            launch(Dispatchers.Main) {
                dismissLoadingDialog();
//                showMessageDialog(delivered.toString())
            }


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
            updateDate = selectedProduct.updateDate.toLocalDateTime()!!

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


    private fun showLoadingDialog(title: String) {
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle(title)
        progressDialog?.setMessage("Loading...") // Set the message to be displayed
        progressDialog?.setCancelable(false) // Prevent users from dismissing the dialog
        progressDialog?.setProgressStyle(ProgressDialog.STYLE_SPINNER) // Use a spinner-style progress indicator
        progressDialog?.show()
    }

    // Call this function to dismiss the loading dialog
    private fun dismissLoadingDialog() {
        progressDialog?.dismiss()
    }
}