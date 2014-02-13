/*******************************************************************************
 * Copyright (c) 2012 sfleury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     sfleury - initial API and implementation
 ******************************************************************************/
package org.gots.ui;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.gots.R;
import org.gots.action.service.ActionNotificationService;
import org.gots.action.service.ActionTODOBroadcastReceiver;
import org.gots.ads.GotsAdvertisement;
import org.gots.authentication.ParrotAuthentication;
import org.gots.garden.GardenInterface;
import org.gots.preferences.GotsPreferences;
import org.gots.seed.BaseSeedInterface;
import org.gots.seed.provider.parrot.ParrotSeedProvider;
import org.gots.seed.service.SeedNotification;
import org.gots.seed.service.SeedUpdateService;
import org.gots.weather.WeatherManager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashScreenActivity extends AbstractActivity {
    private static final class SplashHandler extends Handler {

        private WeakReference<Activity> that;

        public SplashHandler(WeakReference<Activity> that) {
            this.that = that;
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
            case STOPSPLASH:
                // remove SplashScreen from view
                if (that.get() != null) {
                    Intent intent = new Intent(that.get(), DashboardActivity.class);
                    that.get().startActivityForResult(intent, 3);
                }
                // that.get().finish();

                break;
            }
            super.handleMessage(msg);
        }
    }

    private static final int STOPSPLASH = 0;

    // private static final long SPLASHTIME = 3000;
    private static final long SPLASHTIME = 1000;

    private static Handler splashHandler;

    private View progressSeed;

    private View progressWeather;

    private View progressAction;

    private View progressGarden;

    private int asyncCounter;

    private TextView textprogressWeather;

    private TextView textprogressAction;

    private TextView textprogressSeed;

    private TextView textprogressGarden;

    private Handler getSplashHandler() {
        if (splashHandler == null) {
            WeakReference<Activity> that = new WeakReference<Activity>(this);
            splashHandler = new SplashHandler(that);
        }
        return splashHandler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        new AsyncTask<Void, Integer, String>() {
            private TextView name;

            @Override
            protected void onPreExecute() {
                name = (TextView) findViewById(R.id.textVersion);
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(Void... params) {
                PackageInfo pInfo;
                String version = "";
                try {
                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    version = pInfo.versionName;

                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
                setRecurringAlarm(getApplicationContext());

                return version;
            }

            protected void onPostExecute(String version) {
                name.setText("version " + version);
            };
        }.execute();

        setButtonClickable(R.id.webArtmoni, GotsPreferences.URL_ARTMONI);
        setButtonClickable(R.id.webSauterDansLesFlaques, GotsPreferences.URL_SAUTERDANSLESFLAQUES);
        setButtonClickable(R.id.idSocialGoogle, GotsPreferences.URL_GOOGLEPLUS_GARDENING_MANAGER);
        setButtonClickable(R.id.idSocialFacebook, GotsPreferences.URL_FACEBOOK_GARDENING_MANAGER);
        setButtonClickable(R.id.idTranslateButton, GotsPreferences.URL_TRANSLATE_GARDENING_MANAGER);

        ImageView flag = (ImageView) findViewById(R.id.imageTranslateFlag);
        int flagRessource = getResources().getIdentifier("org.gots:drawable/" + Locale.getDefault().getLanguage(),
                null, null);
        flag.setImageResource(flagRessource);

        progressWeather = findViewById(R.id.imageProgressWeather);
        progressSeed = findViewById(R.id.imageProgressSeed);
        progressAction = findViewById(R.id.imageProgressAction);
        progressGarden = findViewById(R.id.imageProgressGarden);

        textprogressWeather = (TextView) findViewById(R.id.textProgressWeather);
        textprogressSeed = (TextView) findViewById(R.id.textProgressSeed);
        textprogressAction = (TextView) findViewById(R.id.textProgressAction);
        textprogressGarden = (TextView) findViewById(R.id.textProgressGarden);

        if (!gotsPrefs.isPremium()) {
            GotsAdvertisement ads = new GotsAdvertisement(this);

            LinearLayout layout = (LinearLayout) findViewById(R.id.idAdsTop);
            layout.addView(ads.getAdsLayout());
        }

    }

    protected void setButtonClickable(int viewId, final String urlTranslateGardeningManager) {
        View buttonTranslate = (View) findViewById(viewId);
        buttonTranslate.setOnClickListener(new LinearLayout.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlTranslateGardeningManager));
                startActivity(browserIntent);

            }
        });
    }

    protected void addProgress() {
        asyncCounter++;

    };

    protected void removeProgress() {
        asyncCounter--;
        if (asyncCounter == 0) {
            Message msg = new Message();
            msg.what = STOPSPLASH;
            getSplashHandler().sendMessageDelayed(msg, SPLASHTIME);
        }
    };

    protected void launchProgress() {
        asyncCounter = 0;
        // weatherServiceIntent = new Intent(getApplicationContext(), WeatherUpdateService.class);

        // getApplicationContext().startService(weatherServiceIntent);

        /*
         * Synchronize Weather
         */
        new AsyncTask<Void, Integer, Void>() {

            protected void onPreExecute() {
                addProgress();
                Animation myFadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.tween);
                progressWeather.startAnimation(myFadeInAnimation);
                textprogressWeather.setText(getResources().getString(R.string.synchro_weather_checking));

            };

            @Override
            protected Void doInBackground(Void... params) {
                WeatherManager manager = new WeatherManager(getApplicationContext());
                manager.getConditionSet(2);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                progressWeather.clearAnimation();
                progressWeather.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_state_ok));
                removeProgress();
                textprogressWeather.setText(getResources().getString(R.string.synchro_weather_ok));

                super.onPostExecute(result);

            }
        }.execute();

        /*
         * Synchronize Seeds
         */
        new AsyncTask<Void, Integer, Void>() {
            // Intent startServiceIntent2 = new Intent(getApplicationContext(), SeedUpdateService.class);

            protected void onPreExecute() {
                Animation myFadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.tween);
                progressSeed.startAnimation(myFadeInAnimation);
                textprogressSeed.setText(getResources().getString(R.string.synchro_seeds_checking));

                addProgress();
            };

            @Override
            protected Void doInBackground(Void... params) {

                // getApplicationContext().startService(startServiceIntent2);
                seedManager.force_refresh(true);
                seedManager.getMyStock(gardenManager.getCurrentGarden());
                seedManager.getVendorSeeds(true);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                List<BaseSeedInterface> newSeeds = seedManager.getNewSeeds();
                if (newSeeds.size() > 0) {
                    SeedNotification notification = new SeedNotification(getApplicationContext());
                    notification.createNotification(newSeeds);
                }
                progressSeed.clearAnimation();
                progressSeed.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_state_ok));
                // getApplicationContext().stopService(startServiceIntent2);
                removeProgress();
                textprogressSeed.setText(getResources().getString(R.string.synchro_seeds_ok));

                super.onPostExecute(result);

            }
        }.execute();

        /*
         * Synchronize Actions
         */
        new AsyncTask<Void, Integer, Void>() {
            Intent startServiceIntent3 = new Intent(getApplicationContext(), ActionNotificationService.class);

            protected void onPreExecute() {
                Animation myFadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.tween);
                progressAction.startAnimation(myFadeInAnimation);
                textprogressAction.setText(getResources().getString(R.string.synchro_actions_checking));

                addProgress();
            };

            @Override
            protected Void doInBackground(Void... params) {
                getApplicationContext().startService(startServiceIntent3);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                progressAction.clearAnimation();
                progressAction.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_state_ok));
                textprogressAction.setText(getResources().getString(R.string.synchro_actions_ok));
                getApplicationContext().stopService(startServiceIntent3);
                removeProgress();
                super.onPostExecute(result);
            }
        }.execute();

        new AsyncTask<Void, Integer, Void>() {

            protected void onPreExecute() {
            };

            @Override
            protected Void doInBackground(Void... params) {

                ParrotAuthentication authentication = new ParrotAuthentication();
                authentication.getToken();
                ParrotSeedProvider provider = new ParrotSeedProvider(getApplicationContext());
                provider.getVendorSeeds(true);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
            }
        }.execute();

        /*
         * Synchronize Server
         */
        if (gotsPrefs.isConnectedToServer()) {
            findViewById(R.id.layoutProgressGarden).setVisibility(View.VISIBLE);

            new AsyncTask<Context, Void, List<GardenInterface>>() {
                ProgressDialog dialog;

                private List<GardenInterface> myGardens;

                @Override
                protected void onPreExecute() {
                    Animation myFadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.tween);
                    progressGarden.startAnimation(myFadeInAnimation);
                    textprogressGarden.setText(getResources().getString(R.string.synchro_garden_checking));

                    super.onPreExecute();
                }

                @Override
                protected List<GardenInterface> doInBackground(Context... params) {

                    myGardens = gardenManager.getMyGardens(true);
                    return myGardens;
                }

                @Override
                protected void onPostExecute(List<GardenInterface> result) {
                    progressGarden.clearAnimation();
                    progressGarden.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_state_ok));
                    textprogressGarden.setText(getResources().getString(R.string.synchro_garden_ok));

                    super.onPostExecute(result);
                }

            }.execute();
        } else {
            findViewById(R.id.layoutProgressGarden).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {

        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        if (gardenManager.getCurrentGarden() == null) {
            Intent intent = new Intent(this, FirstLaunchActivity.class);
            startActivityForResult(intent, 0);
        } else
            launchProgress();
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int currentGardenId = gotsPrefs.getCurrentGardenId();
        if (requestCode == 3) {
            finish();
            return;
        }
        if (currentGardenId > -1 || gotsPrefs.isConnectedToServer()) {
            Message msg = new Message();
            msg.what = STOPSPLASH;
            getSplashHandler().sendMessageDelayed(msg, SPLASHTIME);
        } else
            finish();

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setRecurringAlarm(Context context) {
        // we know mobiletuts updates at right around 1130 GMT.
        // let's grab new stuff at around 11:45 GMT, inexactly
        Calendar updateTime = Calendar.getInstance();

        Intent actionBroadcastReceiver = new Intent(context, ActionTODOBroadcastReceiver.class);
        PendingIntent actionTODOIntent = PendingIntent.getBroadcast(context, 0, actionBroadcastReceiver,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (GotsPreferences.isDevelopment())
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), 120000, actionTODOIntent);
        else {
            updateTime.set(Calendar.HOUR_OF_DAY, 20);
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, actionTODOIntent);
        }

        Intent seedBroadcastReceiver = new Intent(context, SeedUpdateService.class);
        PendingIntent seedIntent = PendingIntent.getBroadcast(context, 0, seedBroadcastReceiver,
                PendingIntent.FLAG_UPDATE_CURRENT);

        if (GotsPreferences.isDevelopment())
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), 120000, seedIntent);
        else {
            updateTime.set(Calendar.HOUR_OF_DAY, 12);
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, seedIntent);
        }
    }

}
