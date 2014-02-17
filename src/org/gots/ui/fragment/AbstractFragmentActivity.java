package org.gots.ui.fragment;

import org.gots.analytics.GotsAnalytics;
import org.gots.garden.GardenManager;
import org.gots.garden.provider.GardenProvider;
import org.gots.preferences.GotsPreferences;
import org.gots.seed.GotsSeedManager;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class AbstractFragmentActivity extends FragmentActivity {
    protected GotsPreferences gotsPref;
    protected GotsSeedManager seedProvider;
    protected GardenManager gardenProvider;

    @Override
    protected void onCreate(Bundle arg0) {
        GotsAnalytics.getInstance(getApplication()).incrementActivityCount();
        GoogleAnalyticsTracker.getInstance().trackPageView(getClass().getSimpleName());
        gotsPref=GotsPreferences.getInstance();
        gotsPref.initIfNew(this);
        
        seedProvider = GotsSeedManager.getInstance();
        seedProvider.initIfNew(getApplicationContext());
        gardenProvider = GardenManager.getInstance();
        gardenProvider.initIfNew(getApplicationContext());
        super.onCreate(arg0);
    }

    @Override
    protected void onDestroy() {
        GotsAnalytics.getInstance(getApplication()).decrementActivityCount();
        super.onDestroy();
    }
}
