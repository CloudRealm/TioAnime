package com.axiel7.tioanime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private TinyDB tinyDB;
    private SharedPreferences preferences;
    private SettingsFragment settingsFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsFragment = new SettingsFragment();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        //change status and nav bar color
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        //setup preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        tinyDB = new TinyDB(this);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("playWithVlc")) {
            boolean playWithVlc = sharedPreferences.getBoolean("playWithVlc", false);
            if (playWithVlc) {
                tinyDB.putBoolean("playWithVlc", true);
            }
            else {
                tinyDB.putBoolean("playWithVlc", false);
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference about = findPreference("about");
            assert about != null;
            about.setSummary("Versión " + BuildConfig.VERSION_NAME);
            Preference github = findPreference("github");
            assert github != null;
            github.setOnPreferenceClickListener(preference -> {
                String githubUrl = "https://github.com/axiel7/TioAnime";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(githubUrl));
                startActivity(intent);
                return true;
            });
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);
    }
}