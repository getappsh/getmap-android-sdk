package com.example.getmap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.getmap.matomo.MatomoTracker
import com.ngsoft.getapp.sdk.BuildConfig
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.SystemTest
import com.ngsoft.getapp.sdk.exceptions.VpnClosedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper

class SystemTestActivity : AppCompatActivity() {


    private lateinit var tracker: Tracker;

    private lateinit var testDiscoveryIcon: ImageView
    private lateinit var testDiscoveryName: TextView

    private lateinit var testConfigIcon: ImageView
    private lateinit var testConfigName: TextView

    private lateinit var testImportIcon: ImageView
    private lateinit var testImportName: TextView

    private lateinit var testDownloadIcon: ImageView
    private lateinit var testDownloadName: TextView

    private lateinit var testFileMoveIcon: ImageView
    private lateinit var testFileMoveName: TextView

    private lateinit var testInventoryUpdatesIcon: ImageView
    private lateinit var testInventoryUpdatesName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_test)

        val cfg = Configuration(
            BuildConfig.BASE_URL,
            BuildConfig.USERNAME,
            BuildConfig.PASSWORD,
            16,
            null)

        tracker = MatomoTracker.getTracker(this)

        val systemTest = SystemTest.getInstance(this, cfg)

        testDiscoveryIcon = findViewById(R.id.testDiscoveryIcon)
        testDiscoveryName = findViewById(R.id.testDiscoveryName)

        testConfigIcon = findViewById(R.id.testConfigIcon)
        testConfigName = findViewById(R.id.testConfigName)

        testImportIcon = findViewById(R.id.testImportIcon)
        testImportName = findViewById(R.id.testImportName)

        testDownloadIcon = findViewById(R.id.testDownloadIcon)
        testDownloadName = findViewById(R.id.testDownloadName)

        testFileMoveIcon = findViewById(R.id.testFileMoveIcon)
        testFileMoveName = findViewById(R.id.testFileMoveName)

        testInventoryUpdatesIcon = findViewById(R.id.testInventoryUpdatesIcon)
        testInventoryUpdatesName = findViewById(R.id.testInventoryUpdatesName)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                systemTest.run { testReport ->
                    runOnUiThread {
                        updateTestResults(testReport)
                    }
                }
            }catch (e: VpnClosedException){

                runOnUiThread {
                    showVpnErrorAndCloseActivity()
                }
            }

        }

    }


    private fun updateTestResults(testReport: HashMap<Int, SystemTest.TestResults?>) {
        updateTestResult(testDiscoveryIcon, testDiscoveryName, testReport[SystemTest.TEST_DISCOVERY])
        updateTestResult(testConfigIcon, testConfigName, testReport[SystemTest.TEST_CONFIG])
        updateTestResult(testImportIcon, testImportName, testReport[SystemTest.TEST_IMPORT])
        updateTestResult(testDownloadIcon, testDownloadName, testReport[SystemTest.TEST_DOWNLOAD])
        updateTestResult(testFileMoveIcon, testFileMoveName, testReport[SystemTest.TEST_FILE_MOVE])
        updateTestResult(testInventoryUpdatesIcon, testInventoryUpdatesName, testReport[SystemTest.TEST_INVENTORY_UPDATES])

        if (testReport[SystemTest.TEST_DISCOVERY]?.success == false){
//            בחירת תחום failed
            TrackHelper.track()
                .event("מיפוי ענן", "ניהול שגיאות").name("בחירת תיחום נכשל")
                .with(tracker)
        }
        if (testReport[SystemTest.TEST_CONFIG]?.success == false){
//            קבלת קונפיגורציה failed
            TrackHelper.track()
                .event("מיפוי ענן", "ניהול שגיאות").name("קבלת קונפיגורציה נכשל")
                .with(tracker)
        }
        if (testReport[SystemTest.TEST_IMPORT]?.success == false){
//            הפקת מפה failed
            TrackHelper.track()
                .event("מיפוי ענן", "ניהול שגיאות").name("הפקת מפה נכשל")
                .with(tracker)
        }
        if (testReport[SystemTest.TEST_DOWNLOAD]?.success == false){
//            הורדת מפה failed
            TrackHelper.track()
                .event("מיפוי ענן", "ניהול שגיאות").name("הורדת מפה נכשל")
                .with(tracker)
        }
        if (testReport[SystemTest.TEST_FILE_MOVE]?.success == false){
//            העברת קבצים failed
            TrackHelper.track()
                .event("מיפוי ענן", "ניהול שגיאות").name("העברת קבצים נכשל")
                .with(tracker)
        }
        if (testReport[SystemTest.TEST_INVENTORY_UPDATES]?.success == false){
//            סטטוס עדכנוית מפות failed
            TrackHelper.track()
                .event("מיפוי ענן", "ניהול שגיאות").name("סטטוס עדכנוית מפות נכשל")
                .with(tracker)
        }
    }

    private fun updateTestResult(iconImageView: ImageView, testNameTextView: TextView, testResult: SystemTest.TestResults?) {
        // Update the icon based on the test result
        when {
            testResult?.success == null -> {
                iconImageView.setImageResource(R.drawable.ic_loading)
            }
            testResult.success == true -> {
                iconImageView.setImageResource(R.drawable.ic_check)
            }
            else -> {
                iconImageView.setImageResource(R.drawable.ic_cross)
            }
        }

        // Update the TextView with the test name
        testNameTextView.text = testResult?.name ?: "טוען..."
    }

    private fun showVpnErrorAndCloseActivity() {
        // Create an AlertDialog builder
        val builder = AlertDialog.Builder(this)
        builder.setMessage("אין תקשורת, אנא וודא חיבור VPN תקין")
            .setCancelable(false) // Prevent dialog from being dismissed by back button
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
                finish() // Close the activity
            }

        val dialog = builder.create()
        dialog.show()

    }
}