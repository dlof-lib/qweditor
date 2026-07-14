package com.qw.editor;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends BaseActivity {

    private boolean initializing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        RadioGroup themeGroup = findViewById(R.id.themeGroup);
        RadioGroup tabSizeGroup = findViewById(R.id.tabSizeGroup);
        SeekBar fontSeekBar = findViewById(R.id.fontSeekBar);
        TextView fontSizeLabel = findViewById(R.id.fontSizeLabel);
        Switch switchWordWrap = findViewById(R.id.switchWordWrap);
        Switch switchAutoSave = findViewById(R.id.switchAutoSave);
        TextView versionText = findViewById(R.id.versionText);

        versionText.setText(getString(R.string.settings_version, getAppVersionName()));

        // تهيئة القيم الحالية
        String theme = AppPreferences.getThemeMode(this);
        if (AppPreferences.THEME_LIGHT.equals(theme)) {
            themeGroup.check(R.id.radioLight);
        } else if (AppPreferences.THEME_SYSTEM.equals(theme)) {
            themeGroup.check(R.id.radioSystem);
        } else {
            themeGroup.check(R.id.radioDark);
        }

        int tabSize = AppPreferences.getTabSize(this);
        if (tabSize == 2) tabSizeGroup.check(R.id.tab2);
        else if (tabSize == 8) tabSizeGroup.check(R.id.tab8);
        else tabSizeGroup.check(R.id.tab4);

        float fontSize = AppPreferences.getFontSize(this);
        fontSeekBar.setProgress(Math.round(fontSize) - 8); // نطاق 8..34
        fontSizeLabel.setText(Math.round(fontSize) + "sp");

        switchWordWrap.setChecked(AppPreferences.isWordWrapEnabled(this));
        switchAutoSave.setChecked(AppPreferences.isAutoSaveEnabled(this));

        initializing = false;

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioLight) {
                AppPreferences.setThemeMode(this, AppPreferences.THEME_LIGHT);
            } else if (checkedId == R.id.radioSystem) {
                AppPreferences.setThemeMode(this, AppPreferences.THEME_SYSTEM);
            } else {
                AppPreferences.setThemeMode(this, AppPreferences.THEME_DARK);
            }
            recreate();
        });

        tabSizeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.tab2) AppPreferences.setTabSize(this, 2);
            else if (checkedId == R.id.tab8) AppPreferences.setTabSize(this, 8);
            else AppPreferences.setTabSize(this, 4);
        });

        fontSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = progress + 8;
                fontSizeLabel.setText(size + "sp");
                if (fromUser) AppPreferences.setFontSize(SettingsActivity.this, size);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchWordWrap.setOnCheckedChangeListener((btn, checked) -> {
            if (!initializing) AppPreferences.setWordWrapEnabled(this, checked);
        });

        switchAutoSave.setOnCheckedChangeListener((btn, checked) -> {
            if (!initializing) AppPreferences.setAutoSaveEnabled(this, checked);
        });
    }

    private String getAppVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
