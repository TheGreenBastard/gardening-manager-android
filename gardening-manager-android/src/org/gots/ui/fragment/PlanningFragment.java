package org.gots.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import org.gots.R;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by sfleury on 10/07/15.
 */
public class PlanningFragment extends SeedContentFragment implements DatePicker.OnDateChangedListener {

    private DatePicker planningSowMin;
    private DatePicker planningSowMax;
    private DatePicker planningHarvestMin;
    private DatePicker planningHarvestMax;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.input_seed_planning, null);
        planningSowMin = (DatePicker) v.findViewById(R.id.IdSeedDateSowingPlanningMin);
        planningSowMax = (DatePicker) v.findViewById(R.id.IdSeedDateSowingPlanningMax);
        planningHarvestMin = (DatePicker) v.findViewById(R.id.IdSeedDateHarvestPlanningMin);
        planningHarvestMax = (DatePicker) v.findViewById(R.id.IdSeedDateHarvestPlanningMax);
        return v;
    }

    @Override
    public void update() {
        runAsyncDataRetrieval();
    }

    @Override
    protected void onNuxeoDataRetrievalStarted() {
        Calendar sowTimeMin = Calendar.getInstance();
        Calendar sowTimeMax = Calendar.getInstance();
        Calendar harvestTimeMin = Calendar.getInstance();
        Calendar harvestTimeMax = Calendar.getInstance();

        if (mSeed.getDateSowingMin() > 0)
            sowTimeMin.set(Calendar.MONTH, mSeed.getDateSowingMin() - 1);
        if (mSeed.getDateSowingMax() > 0)
            sowTimeMax.set(Calendar.MONTH, mSeed.getDateSowingMax() - 1);

        if (mSeed.getDateSowingMin() > 0)
            harvestTimeMin.set(Calendar.MONTH, mSeed.getDateSowingMin() - 1 + mSeed.getDurationMin() / 30);
        planningSowMin.init(sowTimeMin.get(Calendar.YEAR), sowTimeMin.get(Calendar.MONTH),
                sowTimeMin.get(Calendar.DAY_OF_MONTH), this);
        monthFilter(planningSowMin);

        planningSowMax.init(sowTimeMax.get(Calendar.YEAR), sowTimeMax.get(Calendar.MONTH),
                sowTimeMax.get(Calendar.DAY_OF_MONTH), this);
        monthFilter(planningSowMax);

        planningHarvestMin.init(harvestTimeMin.get(Calendar.YEAR), harvestTimeMin.get(Calendar.MONTH),
                harvestTimeMin.get(Calendar.DAY_OF_MONTH), this);
        monthFilter(planningHarvestMin);

        planningHarvestMax.init(harvestTimeMax.get(Calendar.YEAR), harvestTimeMax.get(Calendar.MONTH),
                harvestTimeMax.get(Calendar.DAY_OF_MONTH), this);
        monthFilter(planningHarvestMax);

        super.onNuxeoDataRetrievalStarted();
    }


    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
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

    @Override
    protected void onNuxeoDataRetrieved(Object data) {
        updatePlanning();
        super.onNuxeoDataRetrieved(data);
    }

    @Override
    public void onDateChanged(DatePicker datePicker, int i, int i1, int i2) {

        updatePlanning();
    }

    private void updatePlanning() {
        int durationmin = planningHarvestMin.getMonth() - planningSowMin.getMonth();

        int durationmax;
        if (planningHarvestMin.getMonth() <= planningHarvestMax.getMonth())
            // [0][1][min][3][4][5][6][7][max][9][10][11]
            durationmax = planningHarvestMax.getMonth() - planningSowMax.getMonth();
        else
            // [0][1][max][3][4][5][6][7][min][9][10][11]
            durationmax = 12 - planningSowMax.getMonth() + planningHarvestMax.getMonth();
        mSeed.setDateSowingMin(planningSowMin.getMonth() + 1);
        mSeed.setDateSowingMax(planningSowMax.getMonth() + 1);
        mSeed.setDurationMin(durationmin * 30);
        mSeed.setDurationMax(durationmax * 30);
        notifyObservers();
    }
}