package com.example.getmap.airwatch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.airwatch.sdk.AirWatchSDKException;
import com.airwatch.sdk.SDKManager;
import com.example.getmap.BuildConfig;

import java.io.IOException;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AirWatchSdkManager {

    private SDKManager sdkManager = null;
    private final String SERIAL_NUMBER = "serialNumber";
    private final String ORGANIZATION_GROUP = "organizationGroup";
    private final String REACT_NATIVE_SHARED_PREFS = "wit_player_shared_preferences";
    private int retryCount = 0;
    private Timer retryTimer;
    private Context context;

    public AirWatchSdkManager(Context context) {
        this.context = context;
        this.retryTimer = new Timer();
    }

    public void startSDK() {
        new Thread(() -> {
            try {
                Log.d("Airwatch - STARTSDK", "Starting SDK initialization");
                sdkManager = SDKManager.init(context);

                if (sdkManager != null) {
                    saveSerialNumberAndOrganizationGroupInSharedPreferences();
                }
            } catch (Exception exception) {
                sdkManager = null;
                Log.e("Airwatch", "Workspace ONE SDK initialization failed", exception);
            }
        }).start();
    }

    private boolean isInitialized() {
        return sdkManager != null;
    }

    public void startRetrying() {
        retryCount = 0;
        retryTimer.scheduleAtFixedRate(new RetryTask(), 0, 60000);
    }

    private class RetryTask extends TimerTask {
        @Override
        public void run() {
            retryCount++;
            Log.d("Airwatch - RetryTask", String.format("RetryCount: %d", retryCount));
            startSDK();
            if (isInitialized() || retryCount >= 3) {
                retryTimer.cancel();
            }
        }
    }

    private void saveSerialNumberAndOrganizationGroupInSharedPreferences() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(REACT_NATIVE_SHARED_PREFS, MODE_PRIVATE);
        String serialNumber = sharedPreferences.getString(SERIAL_NUMBER, "");
        String organizationGroup = sharedPreferences.getString(ORGANIZATION_GROUP, "");

        if (!serialNumber.isEmpty() && !organizationGroup.isEmpty()) {
            Log.d("Airwatch - saveSerialNumberAndOrganizationGroup", "Serial number and organization group are already saved.");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        try {
            serialNumber = sdkManager.getDeviceSerialId();

            String userName = BuildConfig.AW_USER_NAME;
            String password = BuildConfig.AW_PASSWORD;
            String credentials = userName + ":" + password;
            String encodedCredential = Base64.getEncoder().encodeToString(credentials.getBytes());
            String url = BuildConfig.AW_API;

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url + "/searchby-Serialnumber?id=" + serialNumber)
                    .header("Content-Type", "application/json")
                    .header("aw-tenant-code", BuildConfig.AIRWATCH_TENANT)
                    .header("Authorization", "Basic " + encodedCredential)
                    .build();

            String airWatchData = null;
            try (Response response = client.newCall(request).execute()) {
                airWatchData = response.body() != null ? response.body().string() : null;
                // Process the retrieved data here if needed
            } catch (IOException e) {
                Log.e("Airwatch", "Error making network request", e);
            }

            editor.putString(SERIAL_NUMBER, airWatchData);
            editor.putString(ORGANIZATION_GROUP, organizationGroup);
            editor.apply();

            new utils().saveOrganizationGroupInSharedPreferences(context, serialNumber);

        } catch (AirWatchSDKException e) {
            Log.e("Airwatch", "Failed to get serial number from SDK", e);
        }
    }
}
