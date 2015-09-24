/*******************************************************************************
 * Copyright (c) 2012 sfleury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * sfleury - initial API and implementation
 ******************************************************************************/
package org.gots.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;

import org.gots.R;
import org.gots.broadcast.BroadCastMessages;
import org.gots.seed.BaseSeed;
import org.gots.seed.adapter.SeedListAdapter;
import org.gots.seed.adapter.VendorSeedListAdapter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CatalogueFragment extends AbstractListFragment implements OnScrollListener,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    protected static final String FILTER_FAVORITES = "filter.favorites";

    protected static final String FILTER_THISMONTH = "filter.thismonth";

    // protected static final String FILTER_BARCODE = "filter.barcode";

//    protected static final String FILTER_VALUE = "filter.data";

    // public static final String FILTER_PARROT = "filter.parrot";

    protected static final String FILTER_STOCK = "filter.stock";

    public static final String BROADCAST_FILTER = "broadcast_filter";

    public static final String IS_SELECTABLE = "seed.selectable";

    public static final String TAG = CatalogueFragment.class.getSimpleName();

    public Context mContext;

    public SeedListAdapter listVendorSeedAdapter;

    private String filterValue;

    private boolean force = false;

    private GridView gridViewCatalog;

    private int pageSize = 25;

    private int page = 0;

    private int paddingPage = 10;

    private OnSeedSelected mCallback;
    private Spinner spinner;
    private String filter = null;

    public interface OnSeedSelected {
        public abstract void onPlantCatalogueClick(BaseSeed seed);

        public abstract void onPlantCatalogueLongClick(CatalogueFragment vendorListFragment, BaseSeed seed);

    }

    @Override
    public void onAttach(Activity activity) {
        try {
            mCallback = (OnSeedSelected) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnSeedSelected");
        }
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();

        listVendorSeedAdapter = new VendorSeedListAdapter(mContext, new ArrayList<BaseSeed>());
        View view = inflater.inflate(R.layout.list_seed_grid, container, false);
        spinner = (Spinner) view.findViewById(R.id.idSpinnerFilter);
        gridViewCatalog = (GridView) view.findViewById(R.id.seedgridview);
        gridViewCatalog.setAdapter(listVendorSeedAdapter);
        gridViewCatalog.setOnItemClickListener(this);
        gridViewCatalog.setOnItemLongClickListener(this);
        gridViewCatalog.setOnScrollListener(this);

        List<String> list = new ArrayList<String>();
        list.add(getResources().getString(R.string.hut_menu_vendorseeds));
        list.add(getResources().getString(R.string.hut_menu_favorites));
        list.add(getResources().getString(R.string.hut_menu_thismonth));
        list.add(getResources().getString(R.string.hut_menu_myseeds));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (getActivity(), android.R.layout.simple_spinner_item, list);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filter=null;
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        filter = FILTER_FAVORITES;
                        break;
                    case 2:
                        filter = FILTER_THISMONTH;
                        break;
                    case 3:
                        filter = FILTER_STOCK;
                        break;
                    default:
                        filter = null;
                        break;
                }
//                force=true;
                runAsyncDataRetrieval();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
//        runAsyncDataRetrieval();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
    }

    @Override
    protected void onNuxeoDataRetrievalStarted() {

        super.onNuxeoDataRetrievalStarted();
    }

    @Override
    protected Object retrieveNuxeoData() throws Exception {
        List<BaseSeed> catalogue = new ArrayList<BaseSeed>();
        if (FILTER_FAVORITES.equals(filter))
            catalogue = seedProvider.getMyFavorites();
        else if (FILTER_STOCK.equals(filter))
            catalogue = seedProvider.getMyStock(gardenManager.getCurrentGarden(), force);
        else if (FILTER_THISMONTH.equals(filter))
            catalogue = seedProvider.getSeedBySowingMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);
        else
            catalogue = seedProvider.getVendorSeeds(force, page, pageSize);
        force = false;
        return catalogue;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
        runAsyncDataRetrieval();
    }

    @Override
    protected void onNuxeoDataRetrieved(Object data) {
        List<BaseSeed> vendorSeeds = (List<BaseSeed>) data;
        listVendorSeedAdapter.setSeeds(vendorSeeds);
        listVendorSeedAdapter.notifyDataSetChanged();

        super.onNuxeoDataRetrieved(data);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem + visibleItemCount >= totalItemCount && firstVisibleItem != 0) {
            if (isReady()) {
                additems();
            }
        }
    }

    private void additems() {
        page = page + paddingPage;
        pageSize = pageSize + paddingPage;
        Log.d(TAG, "page=" + page + " - pageSize=" + pageSize);
        force = true;
        runAsyncDataRetrieval();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    SeedListAdapter getListAdapter() {
        return listVendorSeedAdapter;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        gridViewCatalog.setSelection(position);
        gridViewCatalog.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
        mCallback.onPlantCatalogueLongClick(this, listVendorSeedAdapter.getItem(position));
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> list, View container, int position, long id) {
        mCallback.onPlantCatalogueClick(listVendorSeedAdapter.getItem(position));
    }

    public void update() {
        runAsyncDataRetrieval();
    }
}
