package com.elbit.system_test

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

object BatteryOptimizationUtil {

    fun openBatteryOptimizationSettingsForApp(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle("Disable Battery Optimizations")
            setMessage("Please disable battery optimizations for target app (CloudMapping or Nebula) to ensure it runs smoothly.")
            setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                }
                context.startActivity(intent)
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }
}
