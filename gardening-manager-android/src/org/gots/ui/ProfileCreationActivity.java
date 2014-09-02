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

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.gots.R;
import org.gots.action.BaseActionInterface;
import org.gots.action.GardeningActionInterface;
import org.gots.action.GotsActionManager;
import org.gots.action.GotsActionSeedManager;
import org.gots.action.provider.GotsActionProvider;
import org.gots.action.provider.GotsActionSeedProvider;
import org.gots.allotment.provider.local.LocalAllotmentProvider;
import org.gots.bean.Allotment;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.bean.Garden;
import org.gots.broadcast.BroadCastMessages;
import org.gots.garden.GardenInterface;
import org.gots.seed.GrowingSeedInterface;
import org.gots.seed.provider.GotsSeedProvider;
import org.gots.seed.provider.local.LocalSeedProvider;

import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class ProfileCreationActivity extends AbstractActivity implements LocationListener, OnClickListener {
    public static final int OPTION_EDIT = 1;

    private LocationManager mlocManager;

    private Location location;

    private Address address;

    private String TAG = "ProfileActivity";

    GardenInterface garden = new Garden();

    private int mode = 0;

    private TextView editTextLocality;

    private TextView editTextName;

    private TextView editTextLatitude;

    private TextView editTextLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() != null)
            mode = getIntent().getExtras().getInt("option");

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.profilecreation);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setTitle(R.string.profile_menu_localize);
        // bar.setDisplayShowCustomEnabled(true);

        // getSupportActionBar().setIcon(R.drawable.bt_update);

        garden.setLocality("");

        findViewById(R.id.idButtonLocalize).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getPosition();
                buildProfile();
            }
        });
        buildProfile();

    }

    private void buildProfile() {
        editTextLocality = (TextView) findViewById(R.id.editTextLocality);
        editTextName = (TextView) findViewById(R.id.editTextGardenName);
        editTextLatitude = (TextView) findViewById(R.id.textGardenLatitude);
        editTextLongitude = (TextView) findViewById(R.id.textGardenLongitude);

        findViewById(R.id.buttonValidatePosition).setOnClickListener(this);

        // findViewById(R.id.buttonAddGarden).setOnClickListener(this);
        if (gardenManager.getCurrentGarden() != null)
            ((CheckBox) findViewById(R.id.checkboxSamples)).setChecked(false);

        if (mode == OPTION_EDIT && gardenManager.getCurrentGarden() != null
                && gardenManager.getCurrentGarden().getLocality() != null) {
            editTextLocality.setText(gardenManager.getCurrentGarden().getLocality());
            editTextName.setText(gardenManager.getCurrentGarden().getName());
            editTextLatitude.setText(String.valueOf(gardenManager.getCurrentGarden().getGpsLatitude()));
            editTextLongitude.setText(String.valueOf(gardenManager.getCurrentGarden().getGpsLongitude()));
        }

        editTextLocality.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CharSequence hint = ((TextView) findViewById(R.id.editTextLocality)).getHint();
                if (hint != null && "".equals(((TextView) findViewById(R.id.editTextLocality)).getText())) {
                    ((TextView) findViewById(R.id.editTextLocality)).setText(hint);
                }
            }
        });

    };

    private void getPosition() {
        // setProgressBarIndeterminateVisibility(true);
        setProgressRefresh(true);
        mlocManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        // pd = ProgressDialog.show(this, "", getResources().getString(R.string.gots_loading), false);
        // pd.setCanceledOnTouchOutside(true);

        // bestprovider can be null because we ask only for enabled providers
        // (getBestProvider(criteria, TRUE);)
        if (mlocManager == null)
            return;
        try {
            mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);

            String bestProvider = mlocManager.getBestProvider(criteria, true);
            if ("gps".equals(bestProvider))
                mlocManager.requestLocationUpdates(bestProvider, 60000, 0, this);
        } catch (Exception e) {
            Log.e(ProfileCreationActivity.class.getName(), e.getMessage());
        }

    }

    private void displayAddress() {

        // Le geocoder permet de récupérer ou chercher des adresses
        // gràce à un mot clé ou une position
        Geocoder geo = new Geocoder(ProfileCreationActivity.this);
        try {
            // Ici on récupère la premiere adresse trouvé gràce à la
            // position
            // que l'on a récupéré
            List<Address> adresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (adresses != null && adresses.size() == 1) {
                address = adresses.get(0);
                editTextLocality.setTag(address);

                // if ("".equals(editTextLocality.getText().toString()))
                // editTextLocality.setHint(String.format("%s", address.getLocality()));
                // else
                editTextLocality.setText(String.format("%s", address.getLocality()));
                editTextLatitude.setText(String.valueOf(address.getLatitude()));
                editTextLongitude.setText(String.valueOf(address.getLongitude()));

            } else {
                // sinon on affiche un message d'erreur
                ((TextView) findViewById(R.id.editTextLocality)).setHint(getResources().getString(
                        R.string.location_notfound));
            }
        } catch (IOException e) {
            e.printStackTrace();
            ((TextView) findViewById(R.id.editTextLocality)).setHint(getResources().getString(
                    R.string.location_notfound));
        }
        // on stop le cercle de chargement
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLocationChanged(Location location) {

        this.location = location;
        displayAddress();
        setProgressRefresh(false);
        mlocManager.removeUpdates(this);
    }

    @Override
    public void onProviderDisabled(String provider) {
        /* this is called if/when the GPS is disabled in settings */
        Log.v(TAG, "Disabled");

        /* bring up the GPS settings */
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.v(TAG, "Enabled");
        Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
        case LocationProvider.OUT_OF_SERVICE:
            Log.v(TAG, "Status Changed: Out of Service");
            Toast.makeText(this, "Status Changed: Out of Service", Toast.LENGTH_SHORT).show();
            break;
        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            Log.v(TAG, "Status Changed: Temporarily Unavailable");
            Toast.makeText(this, "Status Changed: Temporarily Unavailable", Toast.LENGTH_SHORT).show();
            break;
        case LocationProvider.AVAILABLE:
            Log.v(TAG, "Status Changed: Available");
            Toast.makeText(this, "Status Changed: Available", Toast.LENGTH_SHORT).show();
            break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        case R.id.buttonValidatePosition:
            if (mode == OPTION_EDIT)
                updateProfile();
            else
                createNewProfile();
            break;

        default:
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_profilecreation, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.help:
            Intent browserIntent = new Intent(this, WebHelpActivity.class);
            browserIntent.putExtra(WebHelpActivity.URL, getClass().getSimpleName());
            startActivity(browserIntent);
            return true;
        case R.id.localize_gaden:
            getPosition();
            buildProfile();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private GardenInterface buildGarden(GardenInterface originalGarden) {
        GardenInterface modifiedGarden = originalGarden;

        String locality = editTextLocality.getText().toString();
        modifiedGarden.setLocality(locality);

        if (editTextName.getText() != null && !"".equals(editTextName.getText()))
            modifiedGarden.setName(editTextName.getText().toString());
        else
            modifiedGarden.setName(locality.replace("\'", " "));

        if (location != null) {
            Address address = (Address) editTextLocality.getTag();
            if (address != null) {
                modifiedGarden.setAdminArea(address.getAdminArea());
                modifiedGarden.setCountryName(address.getCountryName());
                modifiedGarden.setGpsLatitude(address.getLatitude());
                modifiedGarden.setGpsLongitude(address.getLongitude());
                modifiedGarden.setGpsAltitude(location.getAltitude());
                modifiedGarden.setCountryCode(address.getCountryCode());
            } else {
                modifiedGarden.setCountryCode(Locale.getDefault().getCountry().toLowerCase());
            }
        }
        return modifiedGarden;
    }

    private boolean verifyForm() {
        if ("".equals(editTextLocality.getText().toString())) {
            Toast.makeText(getApplicationContext(), "Please enter your locality", Toast.LENGTH_LONG).show();
            findViewById(R.id.editTextLocality).setBackgroundDrawable(getResources().getDrawable(R.drawable.border_red));
            return false;
        }
        return true;
    }

    private void createNewProfile() {
        if (!verifyForm())
            return;

        new AsyncTask<Void, Void, GardenInterface>() {
            @Override
            protected GardenInterface doInBackground(Void... params) {
                garden = buildGarden(new Garden());
                if (((RadioGroup) findViewById(R.id.radioGardenType)).getCheckedRadioButtonId() == findViewById(
                        R.id.radioGardenIncredibleEdible).getId()) {
                    garden.setIncredibleEdible(true);
                }
                garden = gardenManager.addGarden(garden);
                if (garden.isIncredibleEdible())
                    gardenManager.share(garden, "Everyone", "Write");
                return garden;
            }

            protected void onPostExecute(GardenInterface result) {
                if (result != null)
                    gardenManager.setCurrentGarden(result);
                else
                    Log.e(TAG, "garden is null, no current garden changement");
                sendBroadcast(new Intent(BroadCastMessages.GARDEN_EVENT));
                sendBroadcast(new Intent(BroadCastMessages.GARDEN_CURRENT_CHANGED));
                ProfileCreationActivity.this.finish();
            };
        }.execute();

        // SAMPLE GARDEN
        CheckBox samples = (CheckBox) findViewById(R.id.checkboxSamples);
        if (samples.isChecked()) {
            GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
            tracker.trackEvent("Garden", "sample", garden.getLocality(), 0);

            // Allotment
            BaseAllotmentInterface newAllotment = new Allotment();
            newAllotment.setName("" + new Random().nextInt());

            LocalAllotmentProvider helper = new LocalAllotmentProvider(this);
            helper.createAllotment(newAllotment);

            // Seed
            GotsSeedProvider seedHelper = new LocalSeedProvider(getApplicationContext());

            int nbSeed = seedHelper.getVendorSeeds(false).size();
            Random random = new Random();
            for (int i = 1; i <= 5 && i < nbSeed; i++) {
                int alea = random.nextInt(nbSeed);

                GrowingSeedInterface seed = (GrowingSeedInterface) seedHelper.getSeedById(alea % nbSeed + 1);
                if (seed != null) {
                    seed.setNbSachet(alea % 3 + 1);
                    seedHelper.updateSeed(seed);

                    GotsActionProvider actionHelper = GotsActionManager.getInstance().initIfNew(getApplicationContext());
                    BaseActionInterface bakering = actionHelper.getActionByName("beak");
                    GardeningActionInterface sowing = (GardeningActionInterface) actionHelper.getActionByName("sow");

                    sowing.execute(newAllotment, seed);

                    Calendar cal = new GregorianCalendar();
                    cal.setTime(Calendar.getInstance().getTime());
                    cal.add(Calendar.MONTH, -3);
                    seed.setDateSowing(cal.getTime());

                    GotsActionSeedProvider actionsHelper = GotsActionSeedManager.getInstance().initIfNew(this);
                    actionsHelper.insertAction(seed, bakering);
                }
            }
        }
        finish();

    }

    private void updateProfile() {
        if (!verifyForm())
            return;

        new AsyncTask<String, Integer, Void>() {
            @Override
            protected Void doInBackground(String... params) {

                garden = buildGarden(gardenManager.getCurrentGarden());
                gardenManager.updateCurrentGarden(garden);
                return null;
            }

            protected void onPostExecute(Void result) {
                ProfileCreationActivity.this.finish();
                sendBroadcast(new Intent(BroadCastMessages.GARDEN_EVENT));
            };
        }.execute();

    }
}
