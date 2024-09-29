package com.example.getmap.airwatch;

import android.content.Context;
import android.util.Base64;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.example.getmap.BuildConfig;


public class AirwatchApi {

    String airwatchUrl = BuildConfig.AW_API;
    private OkHttpClient client;

    public AirwatchApi() {
        client = new OkHttpClient();
    }

    public String getOrganizationGroupFromApi(String serialNumber, Context context) {
        String url = airwatchUrl + "/?searchby=serialNumber&id=" + serialNumber;
        String userName = BuildConfig.AW_USER_NAME;
        String password = BuildConfig.AW_PASSWORD;

        if (userName == null || password == null) {
            return "";
        }

        String credential = userName + ":" + password;
        String encodedCredentialDEFAULT = Base64.encodeToString(credential.getBytes(), Base64.NO_WRAP);
        String awTenantCode = BuildConfig.AIRWATCH_TENANT;

        Request request = null;
        try {
            request = new Request.Builder()
                    .url(url)
                    .header("Content-Type", "application/json")
                    .header("aw-tenant-code", awTenantCode)
                    .header("Authorization", "Basic " + encodedCredentialDEFAULT)
                    .build();
        } catch (Exception e) {
            Toast.makeText(context, "Serial Number" + e, Toast.LENGTH_LONG).show();
            return "";
        }

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.get("LocationGroupName").getAsString();

        } catch (Exception e) {
            Toast.makeText(context, "Serial Number" + e, Toast.LENGTH_LONG).show();
            return "";
        }
    }
}
