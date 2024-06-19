package com.example.getmap.airwatch;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class utils {
    final String ORGANIZATION_GROUP = "organizationGroup";
    final String REACT_NATIVE_SHARED_PREFS = "wit_player_shared_preferences";

    public void saveOrganizationGroupInSharedPreferences(Context context,String serialNumber){
        SharedPreferences sharedPreferences = context.getSharedPreferences(REACT_NATIVE_SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String organizationGroup=new AirwatchApi().getOrganizationGroupFromApi(serialNumber,context);

        editor.putString(ORGANIZATION_GROUP,organizationGroup);
        editor.apply();
    }
}
