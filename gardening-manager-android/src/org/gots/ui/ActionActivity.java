/**
 * ****************************************************************************
 * Copyright (c) 2012 sfleury.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p>
 * Contributors:
 * sfleury - initial API and implementation
 * ****************************************************************************
 */
package org.gots.ui;

import java.util.ArrayList;
import java.util.List;

import org.gots.R;
import org.gots.action.ActionOnSeed;
import org.gots.action.BaseAction;
import org.gots.action.GotsActionManager;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.provider.ActionsContentProvider;
import org.gots.seed.GrowingSeed;
import org.gots.ui.fragment.ActionsDoneListFragment;
import org.gots.ui.fragment.ActionsTODOListFragment;
import org.gots.ui.fragment.AllotmentListFragment;
import org.gots.ui.fragment.AllotmentListFragment.OnAllotmentSelected;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

public class ActionActivity extends BaseGotsActivity implements OnAllotmentSelected {

    private FloatingActionsMenu menu;
    private ActionsTODOListFragment contentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleBar(R.string.dashboard_actions_name);
        contentFragment = new ActionsTODOListFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected String requireRefreshSyncAuthority() {
        return ActionsContentProvider.AUTHORITY;
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
    }

    @Override
    protected Object retrieveNuxeoData() throws Exception {
        return actionseedProvider.getActionsToDo(true);
    }

    @Override
    protected void onResume() {
        addMainLayout(contentFragment, null);
        super.onResume();
    }

    @Override
    protected boolean requireFloatingButton() {
        return true;
    }

    @Override
    protected List<FloatingItem> onCreateFloatingMenu() {
        List<FloatingItem> floatingItems = new ArrayList<>();
        FloatingItem floatingItem = new FloatingItem();
        floatingItem.setTitle(getResources().getString(R.string.action_planning));
        floatingItem.setRessourceId(R.drawable.action_schedule);
        floatingItem.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addContentLayout(new AllotmentListFragment(), null);
            }
        });
        floatingItems.add(floatingItem);
        return floatingItems;

    }

    @Override
    public void onAllotmentClick(BaseAllotmentInterface allotmentInterface) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAllotmentLongClick(BaseAllotmentInterface allotmentInterface) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAllotmentMenuClick(View v, BaseAllotmentInterface allotmentInterface) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGrowingSeedClick(View v, final GrowingSeed growingSeedInterface) {
        menu = new FloatingActionsMenu(getApplicationContext());

        GotsActionManager gotsActionManager = GotsActionManager.getInstance().initIfNew(getApplicationContext());
        ArrayList<BaseAction> actions = gotsActionManager.getActions(false);

        for (final BaseAction baseActionInterface : actions) {
            if (!(baseActionInterface instanceof ActionOnSeed))
                continue;
            FloatingItem floatingItem = new FloatingItem();
            floatingItem.setTitle(baseActionInterface.getName());
            int actionImageRessource = getResources().getIdentifier(
                    "org.gots:drawable/action_" + baseActionInterface.getName(), null, null);
            floatingItem.setRessourceId(actionImageRessource);
            floatingItem.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            baseActionInterface.setDuration(7);
                            actionseedProvider.insertAction(growingSeedInterface, (ActionOnSeed) baseActionInterface);
                            return null;
                        }

                        protected void onPostExecute(Void result) {
                            getSupportFragmentManager().popBackStack();
                            contentFragment.update();
                            menu.setVisibility(View.GONE);
                        }

                        ;
                    }.execute();
                    return true;
                }
            });
            floatingItem.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            actionseedProvider.insertAction(growingSeedInterface, (ActionOnSeed) baseActionInterface);
                            return null;
                        }

                        protected void onPostExecute(Void result) {
                            getSupportFragmentManager().popBackStack();
                            contentFragment.update();
                            menu.setVisibility(View.GONE);
                        }

                        ;
                    }.execute();
                }
            });

            FloatingActionButton button = new FloatingActionButton(getApplicationContext());
            button.setSize(FloatingActionButton.SIZE_NORMAL);
            button.setColorNormalResId(R.color.action_error_color);
            button.setColorPressedResId(R.color.action_warning_color);
            button.setIcon(floatingItem.getRessourceId());
            button.setTitle(floatingItem.getTitle());
            button.setSize(FloatingActionButton.SIZE_MINI);

            button.setStrokeVisible(false);
            button.setOnLongClickListener(floatingItem.getOnLongClickListener());
            button.setOnClickListener(floatingItem.getOnClickListener());
            menu.addButton(button);

        }

        menu.setColorNormalResId(R.color.green_light);
        menu.setColorPressedResId(R.color.green_dark);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        menu.setLayoutParams(params);
        ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
        ((ViewGroup) root.getChildAt(0)).addView(menu);
        menu.toggle();
    }

    @Override
    public void onGrowingSeedLongClick(View v, GrowingSeed growingSeedInterface) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onBackStackChanged() {
        if (menu != null)
            menu.setVisibility(View.GONE);

        super.onBackStackChanged();
    }
}
