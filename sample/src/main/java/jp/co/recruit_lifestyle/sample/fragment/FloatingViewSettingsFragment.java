package jp.co.recruit_lifestyle.sample.fragment;


import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import jp.co.recruit.floatingview.R;

public class FloatingViewSettingsFragment extends PreferenceFragmentCompat {

    public static FloatingViewSettingsFragment newInstance() {
        final FloatingViewSettingsFragment fragment = new FloatingViewSettingsFragment();
        return fragment;
    }

    public FloatingViewSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_floatingview, null);
    }
}
