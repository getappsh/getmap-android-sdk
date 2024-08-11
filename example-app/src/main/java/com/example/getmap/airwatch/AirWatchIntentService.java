package com.example.getmap.airwatch;

import android.content.Context;
import android.os.Bundle;

import com.airwatch.event.WS1AnchorEvents;
import com.airwatch.sdk.profile.AnchorAppStatus;
import com.airwatch.sdk.profile.ApplicationProfile;
import com.airwatch.sdk.shareddevice.ClearReasonCode;


public class AirWatchIntentService implements WS1AnchorEvents {
    @Override
    public void onClearAppDataCommandReceived(Context context, ClearReasonCode clearReasonCode) {

    }

    @Override
    public void onApplicationConfigurationChange(Bundle bundle, Context context) {

    }

    @Override
    public void onApplicationProfileReceived(Context context, String s, ApplicationProfile applicationProfile) {

    }

    @Override
    public void onAnchorAppStatusReceived(Context context, AnchorAppStatus anchorAppStatus) {

    }

    @Override
    public void onAnchorAppUpgrade(Context context, boolean b) {

    }
}