package com.getapp.technician

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.SwitchCompat
import com.getapp.technician.mockserver.MockServer
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {


    protected lateinit var failedValidation: SwitchCompat
    protected lateinit var fastDownload: SwitchCompat
    lateinit var mockServer: MockServer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        failedValidation = findViewById(R.id.failed_validation)
        fastDownload = findViewById(R.id.fast_download)

        thread {
            mockServer = MockServer.getInstance(this)
            runOnUiThread {
                failedValidation.isChecked = mockServer.config.failedValidation
                fastDownload.isChecked = mockServer.config.fastDownload
            }
        }

        failedValidation.setOnCheckedChangeListener { _, isChecked ->
            mockServer.config.failedValidation = isChecked
        }

        fastDownload.setOnCheckedChangeListener { _, isChecked ->
            mockServer.config.fastDownload = isChecked
        }
    }
}