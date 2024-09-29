package com.example.getmap.airwatch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.airwatch.sdk.AirWatchSDKException;
import com.airwatch.sdk.SDKManager;
import com.arcgismaps.portal.PortalItemType;
import com.example.getmap.BuildConfig;

import org.json.JSONObject;

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

    public String getImei() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(REACT_NATIVE_SHARED_PREFS, MODE_PRIVATE);
        String serialNumber = sharedPreferences.getString(SERIAL_NUMBER, "");

        String userName = "getmap";
        String password = "260824!harelush";
        String credentials = userName + ":" + password;
        String encodedCredential = "";
        String imei = "";
        encodedCredential = Base64.getEncoder().encodeToString(credentials.getBytes());
        String url = "https://wsconsole.evendigitals.com/API/mdm/devices";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url + "/?searchby=Serialnumber&id=" + serialNumber)
                .header("Content-Type", "application/json")
                .header("aw-tenant-code", "Q3FQ9DDGTsNtSYFhY074KNVxUvT6VKWjf/rtcx06OT8=")
                .header("Authorization", "Basic " + encodedCredential)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                String airWatchData = response.body().string();
                JSONObject jsonObject = new JSONObject(airWatchData);
                imei = jsonObject.getString("Imei");
                Log.d("Imei", imei);
            } else {
                Log.d("Error", "imei failed");
            }
        } catch (Exception e) {
            Toast.makeText(context, "Imei" + e, Toast.LENGTH_LONG).show();
            Log.d("Error", "crushed");
            Log.d("Error", e.toString());
            e.printStackTrace();
        }

        return imei;
    }
}