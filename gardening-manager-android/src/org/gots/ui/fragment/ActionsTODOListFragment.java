package org.gots.ui.fragment;

import java.util.List;

import org.gots.action.ActionOnSeed;
import org.gots.action.adapter.ListAllActionAdapter;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.seed.BaseSeedInterface;

import android.os.Bundle;

public class ActionsTODOListFragment extends AbstractListFragment {
    private boolean force = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
    }

    @Override
    protected Object retrieveNuxeoData() throws Exception {
        return actionseedProvider.getActionsToDo(force);
    }

    @Override
    protected void onNuxeoDataRetrieved(Object data) {
        getListView().setAdapter(
                new ListAllActionAdapter(getActivity(), (List<ActionOnSeed>) data, ListAllActionAdapter.STATUS_TODO));
        super.onNuxeoDataRetrieved(data);
    }
}