package org.gots.ui.fragment;

import org.gots.allotment.GotsAllotmentManager;
import org.gots.garden.GotsGardenManager;
import org.gots.seed.GotsSeedManager;
import org.nuxeo.android.fragments.BaseNuxeoFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class AbstractListFragment extends BaseNuxeoFragment {
    protected GotsSeedManager seedProvider;

    protected GotsAllotmentManager allotmentManager;

    protected GotsGardenManager gardenManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        seedProvider = GotsSeedManager.getInstance();
        seedProvider.initIfNew(getActivity());
        allotmentManager = GotsAllotmentManager.getInstance();
        allotmentManager.initIfNew(getActivity());
        gardenManager = GotsGardenManager.getInstance();
        gardenManager.initIfNew(getActivity());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return false;
    }


}
