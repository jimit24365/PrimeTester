package com.jimit24365.primetester.activity;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.jimit24365.primetester.R;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    ListPreference gameLevel1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pereferences);
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        gameLevel1 = (ListPreference) getPreferenceManager().findPreference(getResources().getString(R.string.preferenece_key));

        gameLevel1.setOnPreferenceChangeListener(this);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(
                getResources().getString(R.string.preferenece_key),
                Integer.parseInt((String) newValue)).apply();
        finish();
        return true;
    }
}
