package com.elbit.system_test

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.ngsoft.getapp.sdk.SystemTest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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

    private lateinit var savedTimeTextView: TextView

    private lateinit var isRunningImageView: ImageView;

    private var runningThread: Thread? = null;

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

        savedTimeTextView = findViewById(R.id.savedTimeTextView)

        isRunningImageView = findViewById(R.id.isRunning)


        TestResultsLiveData.LiveDataManager.testResults().observe(this) {
            Log.d("MainActivity", "Received test results: $it")
            updateTestResults(it)
            displaySavedTime()
        }
        updateRunningState()

    }


    override fun onStart() {
        super.onStart()
        if (runningThread == null || !runningThread!!.isAlive) {
            // Create a new instance of the thread and start it
            runningThread = Thread {
                while (true) {
                    try {
                        updateRunningState()
                        Thread.sleep(2000)
                    } catch (interruptedException: InterruptedException) {
                        return@Thread
                    }
                }
            }
            runningThread?.start()
        }
        displaySavedTime()
    }


    override fun onStop() {
        super.onStop()
        runningThread?.interrupt()
    }
    private fun displaySavedTime() {
        val savedTime = SharedPreferencesHelper.readCurrentTime(this)
        if (savedTime != 0L) {
            val formattedTime = formatTime(savedTime)
            savedTimeTextView.text = "Started At: $formattedTime"
        }
    }

    private fun formatTime(timeInMillis: Long): String {
        val date = Date(timeInMillis)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(date)
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

    fun updateRunningState(){
        val isRunning = TestForegroundService.isServiceRunning(this, TestForegroundService::class.java)
        if (isRunning) {
            isRunningImageView.setImageResource(android.R.drawable.presence_online)
        } else {
            isRunningImageView.setImageResource(android.R.drawable.presence_busy)

        }
    }


    fun stopTest(view: View){
        TestForegroundService.stop(this)
        Toast.makeText(this, "Stopping Test, it may take a few seconds...", Toast.LENGTH_SHORT).show()
        updateRunningState()
    }
    fun startTest(view: View) {
        if (!TestForegroundService.start(this)){
            Toast.makeText(this, "Test already running", Toast.LENGTH_SHORT).show()
        }
        updateRunningState()

    }
}