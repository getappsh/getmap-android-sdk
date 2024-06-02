package com.elbit.system_test

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ngsoft.getapp.sdk.SystemTest
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver.ACTION_RUN_SYSTEM_TEST
import com.ngsoft.getapp.sdk.jobs.SystemTestReceiver.ACTION_SYSTEM_TEST_RESULTS

class MainActivity : AppCompatActivity() {

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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


        registerReceiver(SystemTestResReceiver, IntentFilter(ACTION_SYSTEM_TEST_RESULTS))

        val runSystemTestIntent = Intent(ACTION_RUN_SYSTEM_TEST)
        sendBroadcast(runSystemTestIntent)
    }

    private fun updateTestResults(testReport: HashMap<Int, com.ngsoft.getapp.sdk.SystemTest.TestResults?>) {
        updateTestResult(testDiscoveryIcon, testDiscoveryName, testReport[SystemTest.TEST_DISCOVERY])
        updateTestResult(testConfigIcon, testConfigName, testReport[SystemTest.TEST_CONFIG])
        updateTestResult(testImportIcon, testImportName, testReport[SystemTest.TEST_IMPORT])
        updateTestResult(testDownloadIcon, testDownloadName, testReport[SystemTest.TEST_DOWNLOAD])
        updateTestResult(testFileMoveIcon, testFileMoveName, testReport[SystemTest.TEST_FILE_MOVE])
        updateTestResult(testInventoryUpdatesIcon, testInventoryUpdatesName, testReport[SystemTest.TEST_INVENTORY_UPDATES])
    }

    private fun updateTestResult(iconImageView: ImageView, testNameTextView: TextView, testResult: com.ngsoft.getapp.sdk.SystemTest.TestResults?) {
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
        testNameTextView.text = testResult?.name ?: "Loading..."
    }
}