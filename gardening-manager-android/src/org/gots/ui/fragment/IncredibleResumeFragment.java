package org.gots.ui.fragment;

import org.gots.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class IncredibleResumeFragment extends BaseGotsFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.incredible_resume, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        displayIncredibleInformation();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected void onCurrentGardenChanged() {
        displayIncredibleInformation();
    }

    @Override
    protected void onWeatherChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onActionChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return false;
    }

    private void displayIncredibleInformation() {
        if (getCurrentGarden().isIncredibleEdible()) {
            getView().findViewById(R.id.layoutIncredibleDescription).setVisibility(View.VISIBLE);
        } else
            getView().findViewById(R.id.layoutIncredibleDescription).setVisibility(View.GONE);
    }
}
