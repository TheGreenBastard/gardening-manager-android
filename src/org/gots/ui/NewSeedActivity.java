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

import java.lang.reflect.Field;
import java.util.Calendar;

import org.gots.R;
import org.gots.action.bean.BuyingAction;
import org.gots.broadcast.BroadCastMessages;
import org.gots.help.HelpUriBuilder;
import org.gots.seed.BaseSeedInterface;
import org.gots.seed.GrowingSeed;
import org.gots.seed.GrowingSeedInterface;
import org.gots.seed.adapter.ListSpeciesAdapter;
import org.gots.seed.provider.GotsSeedProvider;
import org.gots.seed.provider.local.LocalSeedProvider;
import org.gots.seed.view.SeedWidgetLong;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class NewSeedActivity extends AbstractActivity implements OnClickListener {
    private static final String SELECTED_SPECIE = "selectedSpecie";

    private DatePicker planningSowMin;

    private DatePicker planningHarvestMin;

    private AutoCompleteTextView autoCompleteVariety;

    // private AutoCompleteTextView autoCompleteSpecie;
    private Gallery gallerySpecies;

    private SeedWidgetLong seedWidgetLong;

    private BaseSeedInterface newSeed;

    private TextView textViewBarCode;

    private boolean isNewSeed = true;

    private DatePicker planningSowMax;

    private DatePicker planningHarvestMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.inputseed);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.seed_register_title);

        findViewById(R.id.imageBarCode).setOnClickListener(this);
        findViewById(R.id.buttonStock).setOnClickListener(this);
        findViewById(R.id.buttonCatalogue).setOnClickListener(this);
        findViewById(R.id.buttonModify).setOnClickListener(this);

        textViewBarCode = (TextView) findViewById(R.id.textViewBarCode);

        if (getIntent().getIntExtra("org.gots.seedid", -1) != -1) {
            newSeed = seedManager.getSeedById(getIntent().getIntExtra("org.gots.seedid", -1));
            isNewSeed = false;

        } else {
            newSeed = new GrowingSeed();
        }

        // autoCompleteVariety.clearFocus();
        // gallerySpecies.post(new Runnable() {
        // public void run() {
        // gallerySpecies.requestFocus();
        //
        // }
        // });
        initview();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (gallerySpecies != null)
            outState.putInt(SELECTED_SPECIE, gallerySpecies.getSelectedItemPosition());
    }

    public void updatePlanning() {

        // if (planningSowMin.getMonth() > planningSowMax.getMonth())
        // planningSowMax.init(planningSowMin.getYear(), planningSowMin.getMonth(), planningSowMin.getDayOfMonth(),
        // new PlanningUpdater());

        newSeed.setDateSowingMin(planningSowMin.getMonth() + 1);
        newSeed.setDateSowingMax(planningSowMax.getMonth() + 1);

        int durationmin = planningHarvestMin.getMonth() - planningSowMin.getMonth();

        int durationmax;
        if (planningHarvestMin.getMonth() <= planningHarvestMax.getMonth())
            // [0][1][min][3][4][5][6][7][max][9][10][11]
            durationmax = planningHarvestMax.getMonth() - planningSowMax.getMonth();
        else
            // [0][1][max][3][4][5][6][7][min][9][10][11]
            durationmax = 12 - planningSowMax.getMonth() + planningHarvestMax.getMonth();
        newSeed.setDurationMin(durationmin * 30);
        newSeed.setDurationMax(durationmax * 30);

        seedWidgetLong.setSeed(newSeed);
    }

    private void monthFilter(DatePicker picker) {
        try {
            Field f[] = picker.getClass().getDeclaredFields();
            for (Field field : f) {
                if (field.getName().equals("mDaySpinner")) {
                    field.setAccessible(true);
                    Object dayPicker = new Object();
                    dayPicker = field.get(picker);
                    ((View) dayPicker).setVisibility(View.GONE);
                }
                if (field.getName().equals("mYearSpinner")) {
                    field.setAccessible(true);
                    Object dayPicker = new Object();
                    dayPicker = field.get(picker);
                    ((View) dayPicker).setVisibility(View.GONE);
                }
            }
        } catch (SecurityException e) {
            Log.d("ERROR", e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.d("ERROR", e.getMessage());
        } catch (IllegalAccessException e) {
            Log.d("ERROR", e.getMessage());
        }
    }

    private void initview() {

        /*
         * PLANNING
         */
        planningSowMin = (DatePicker) findViewById(R.id.IdSeedDateSowingPlanningMin);
        planningSowMax = (DatePicker) findViewById(R.id.IdSeedDateSowingPlanningMax);
        planningHarvestMin = (DatePicker) findViewById(R.id.IdSeedDateHarvestPlanningMin);
        planningHarvestMax = (DatePicker) findViewById(R.id.IdSeedDateHarvestPlanningMax);

        Calendar sowTimeMin = Calendar.getInstance();
        Calendar sowTimeMax = Calendar.getInstance();
        Calendar harvestTimeMin = Calendar.getInstance();
        Calendar harvestTimeMax = Calendar.getInstance();

        if (newSeed.getDateSowingMin() > 0)
            sowTimeMin.set(Calendar.MONTH, newSeed.getDateSowingMin() - 1);
        if (newSeed.getDateSowingMax() > 0)
            sowTimeMax.set(Calendar.MONTH, newSeed.getDateSowingMax() - 1);

        if (newSeed.getDateSowingMin() > 0)
            harvestTimeMin.set(Calendar.MONTH, newSeed.getDateSowingMin() - 1 + newSeed.getDurationMin() / 30);

        planningSowMin.init(sowTimeMin.get(Calendar.YEAR), sowTimeMin.get(Calendar.MONTH),
                sowTimeMin.get(Calendar.DAY_OF_MONTH), new PlanningUpdater());

        planningSowMax.init(sowTimeMax.get(Calendar.YEAR), sowTimeMax.get(Calendar.MONTH),
                sowTimeMax.get(Calendar.DAY_OF_MONTH), new PlanningUpdater());

        planningHarvestMin.init(harvestTimeMin.get(Calendar.YEAR), harvestTimeMin.get(Calendar.MONTH),
                harvestTimeMin.get(Calendar.DAY_OF_MONTH), new PlanningUpdater());

        planningHarvestMax.init(harvestTimeMax.get(Calendar.YEAR), harvestTimeMax.get(Calendar.MONTH),
                harvestTimeMax.get(Calendar.DAY_OF_MONTH), new PlanningUpdater());

        monthFilter(planningSowMin);
        monthFilter(planningSowMax);
        monthFilter(planningHarvestMin);
        monthFilter(planningHarvestMax);

        seedWidgetLong = (SeedWidgetLong) findViewById(R.id.idSeedWidgetLong);

        /*
         * VARIETIES
         */
        autoCompleteVariety = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewVariety);
        initVarietyList();
        autoCompleteVariety.setText(newSeed.getVariety());
        autoCompleteVariety.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                initVarietyList();
                if (autoCompleteVariety != null)
                    autoCompleteVariety.showDropDown();
            }
        });

        ImageButton clearVariety = (ImageButton) findViewById(R.id.buttonClearVariety);
        clearVariety.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                autoCompleteVariety.setText("");
            }
        });

        /*
         * SPECIES
         */
        gallerySpecies = (Gallery) findViewById(R.id.layoutSpecieGallery);
        initSpecieList();

        /*
         * BARCODE
         */
        textViewBarCode.setText(newSeed.getBareCode());

        // if (savedInstanceState != null &&
        // savedInstanceState.getInt(SELECTED_SPECIE) != 0)
        // gallerySpecies.setSelection(savedInstanceState.getInt(SELECTED_SPECIE));

        if (!isNewSeed) {
            findViewById(R.id.buttonCatalogue).setVisibility(View.GONE);
            findViewById(R.id.buttonStock).setVisibility(View.GONE);
            findViewById(R.id.buttonModify).setVisibility(View.VISIBLE);
        }

    }

    // private void showDropdown() {
    // autoCompleteSpecie.showDropDown();
    // }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.imageBarCode:
            scanBarCode();
            break;

        case R.id.buttonStock:
            if (validateSeed()) {
                newSeed = seedManager.createSeed(newSeed);
                seedManager.addToStock(newSeed, gardenManager.getCurrentGarden());
                finish();
            }
            break;

        case R.id.buttonModify:
            if (validateSeed()) {

                new AsyncTask<Void, Integer, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        seedManager.updateSeed(newSeed);
                        return null;
                    }

                    protected void onPostExecute(Void result) {
                        getApplicationContext().sendBroadcast(new Intent(BroadCastMessages.SEED_DISPLAYLIST));
                        NewSeedActivity.this.finish();

                    };
                }.execute();

            }
            break;
        case R.id.buttonCatalogue:
            if (validateSeed()) {
                seedManager.createSeed(newSeed);
                finish();
            }
            break;
        default:
            break;
        }

    }

    private void addToStock(BaseSeedInterface vendorseed) {
        GotsSeedProvider helper = new LocalSeedProvider(getApplicationContext());
        if (vendorseed.getSeedId() >= 0) {
            GrowingSeedInterface seed = (GrowingSeedInterface) helper.getSeedById(vendorseed.getSeedId());
            BuyingAction buy = new BuyingAction(this);
            buy.execute(seed);
        }

    }

    private boolean validateSeed() {
        if (newSeed.getFamily() == null || "".equals(newSeed.getFamily())) {
            Toast.makeText(this, getResources().getString(R.string.fillfields_specie), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newSeed.getVariety() == null || "".equals(newSeed.getVariety())) {
            Toast.makeText(this, getResources().getString(R.string.fillfields_variety), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (newSeed.getDateSowingMin() == -1 || newSeed.getDateSowingMax() == -1) {
            Toast.makeText(this, getResources().getString(R.string.fillfields_dates), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
	 *
	 */
    private void initSpecieList() {
        final LocalSeedProvider helper = new LocalSeedProvider(getApplicationContext());

        String[] specieList = helper.getArraySpecie();

        ListSpeciesAdapter listSpeciesAdapter = new ListSpeciesAdapter(this, specieList, newSeed);

        gallerySpecies.setAdapter(listSpeciesAdapter);
        gallerySpecies.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                gallerySpecies.dispatchSetSelected(false);
                if (((String) view.getTag()).equals(newSeed.getSpecie())) {
                    // clicked already selected item
                    return;
                }
                // Selected specie changed -> remove background on others
                for (int i = 0; i < parent.getChildCount(); i++) {
                    View childView = parent.getChildAt(i);
                    if (childView != view) {
                        childView.setBackgroundColor(0);
                    }
                }
                view.setSelected(true);
                view.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_state_warning));
                newSeed.setSpecie((String) view.getTag());
                String family = helper.getFamilyBySpecie(newSeed.getSpecie());
                newSeed.setFamily(family);
                seedWidgetLong.setSeed(newSeed);
                seedWidgetLong.invalidate();
            }
        });

    }

    /**
	 *
	 */
    private void initVarietyList() {
        LocalSeedProvider helper = new LocalSeedProvider(getApplicationContext());

        String[] referenceList = null;
        if (newSeed.getSpecie() != null)
            referenceList = helper.getArrayVarietyBySpecie(newSeed.getSpecie());
        else
            referenceList = helper.getArrayVariety();

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, referenceList);
        autoCompleteVariety.setAdapter(adapter);
        autoCompleteVariety.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String variety = autoCompleteVariety.getText().toString();
                newSeed.setVariety(variety);
                seedWidgetLong.setSeed(newSeed);
                seedWidgetLong.invalidate();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        autoCompleteVariety.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String variety = adapter.getItem(arg2);
                newSeed.setVariety(variety);
                seedWidgetLong.setSeed(newSeed);
                seedWidgetLong.invalidate();

            }
        });
        autoCompleteVariety.invalidate();
    }

    private void scanBarCode() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null && scanResult.getContents() != "") {
            Log.i("Scan result", scanResult.toString());
            textViewBarCode.setText(scanResult.getContents());
            newSeed.setBareCode(textViewBarCode.getText().toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.menu_newseed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;

        case R.id.help:
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(HelpUriBuilder.getUri(getClass().getSimpleName())));
            startActivity(browserIntent);

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.inputseed);
        //
        // seedWidgetLong.setSeed(newSeed);
        // seedWidgetLong.invalidate();
    }

    @Override
    protected void onResume() {
        // initview();

        super.onResume();
    }

    public class PlanningUpdater implements DatePicker.OnDateChangedListener {

        @Override
        public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            updatePlanning();
        }
    }
}
