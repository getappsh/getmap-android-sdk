package com.example.getmap.airwatch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.airwatch.sdk.AirWatchSDKException;
import com.airwatch.sdk.SDKManager;

import java.util.Timer;
import java.util.TimerTask;

public class AirWatchSdkManager {
    SDKManager sdkManager = null;
    final String SERIAL_NUMBER = "serialNumber";
    final String ORGANIZATION_GROUP = "organizationGroup";
    final String REACT_NATIVE_SHARED_PREFS = "wit_player_shared_preferences";
    private int retryCount = 0;
    private Timer retryTimer;
    private Context context;

    public AirWatchSdkManager(Context context) {
        this.context = context;
        retryTimer = new Timer();
    }

    public void startRetrying() {
        retryCount = 0;
        // Schedule the task to run starting now and then every minute
        retryTimer.scheduleAtFixedRate(new RetryTask(), 0, 60000);
    }

    private class RetryTask extends TimerTask {
        @Override
        public void run() {
            retryCount++;
            startSDK();
            if (isInitialized() || retryCount >= 3) {
                retryTimer.cancel();
            }
        }
    }

    public void startSDK() {
        new Thread(() -> {
            try {
                final SDKManager initSDKManager = SDKManager.init(context);
                sdkManager = initSDKManager;

                if (sdkManager != null) {
                    saveSerialNumberAndOrganizationGroupInSharedPreferences();
                }
            } catch (Exception exception) {
                sdkManager = null;
                Log.d("airwatch", "Workspace ONE failed " + exception + "." + context);
            }
        }).start();
    }

    private boolean isInitialized() {
        return sdkManager != null;
    }

    private void saveSerialNumberAndOrganizationGroupInSharedPreferences() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(REACT_NATIVE_SHARED_PREFS, MODE_PRIVATE);
        String serialNumber = sharedPreferences.getString(SERIAL_NUMBER, "");
        String organizationGroup = sharedPreferences.getString(ORGANIZATION_GROUP, "");

        if (!serialNumber.isEmpty() && !organizationGroup.isEmpty()) {
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        try {

            serialNumber = sdkManager.getDeviceSerialId();
            editor.putString(SERIAL_NUMBER, serialNumber);
            editor.apply();
            new utils().saveOrganizationGroupInSharedPreferences(context, serialNumber);

        } catch (AirWatchSDKException ignored) {
        }
    }
}