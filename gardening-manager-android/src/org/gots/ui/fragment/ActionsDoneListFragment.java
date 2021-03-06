/*******************************************************************************
 * Copyright (c) 2012 sfleury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p/>
 * Contributors:
 * sfleury - initial API and implementation
 ******************************************************************************/
package org.gots.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.gots.action.ActionOnSeed;
import org.gots.action.adapter.ListAllActionAdapter;
import org.gots.seed.GrowingSeed;
import org.gots.ui.GrowingPlantDescriptionActivity;

import java.util.ArrayList;
import java.util.List;

public class ActionsDoneListFragment extends BaseGotsListFragment {

//    public static final String ORG_GOTS_GROWINGSEED_ID = "org.gots.growingseed.id";

    protected TextView mDialogText;
    protected boolean mShowing;
    Handler mHandler = new Handler();
    int seedid = -1;
    boolean force_sync = false;
    private ListAllActionAdapter listAllActionAdapter;

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        Bundle bundle = this.getArguments();
        if (bundle != null)
            seedid = bundle.getInt(GrowingPlantDescriptionActivity.GOTS_GROWINGSEED_ID);
        super.onViewCreated(v, savedInstanceState);
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
    }

    @Override
    protected Object retrieveNuxeoData() throws Exception {

        if (seedid == -1)
            return null;

        GrowingSeed seed = growingSeedManager.getGrowingSeedById(seedid);

        List<ActionOnSeed> seedActions = new ArrayList<ActionOnSeed>();
        seedActions = actionseedProvider.getActionsDoneBySeed(seed, force_sync);
        seedActions.addAll(actionseedProvider.getActionsToDoBySeed(seed, force_sync));

        force_sync = false;
        return seedActions;
    }

    @Override
    protected void onNuxeoDataRetrieved(Object data) {
        listAllActionAdapter = new ListAllActionAdapter(getActivity(), (List<ActionOnSeed>) data,
                ListAllActionAdapter.STATUS_DONE);
        listView.setAdapter(listAllActionAdapter);
        super.onNuxeoDataRetrieved(data);
    }

    @Override
    protected void onNuxeoDataRetrieveFailed() {
        Log.e(getTag(), "Error retrieving actions list");
        super.onNuxeoDataRetrieveFailed();
    }

    @Override
    protected void onListItemClicked(int i) {
    }

    @Override
    protected void doRefresh() {

    }

    public void update() {
        force_sync = true;
        runAsyncDataRetrieval();
    }
}
