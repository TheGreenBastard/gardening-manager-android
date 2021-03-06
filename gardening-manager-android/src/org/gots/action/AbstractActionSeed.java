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
package org.gots.action;

import android.content.Context;
import android.content.Intent;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import org.gots.broadcast.BroadCastMessages;
import org.gots.seed.GrowingSeed;

import java.util.Calendar;

public abstract class AbstractActionSeed extends AbstractAction implements ActionOnSeed {

    private int growingSeedId = -1;

    private int actionSeedId = -1;

    public AbstractActionSeed(Context context) {
        super(context);
    }

    public AbstractActionSeed(String name) {
        this.name = name;
    }

    @Override
    public int getGrowingSeedId() {
        return growingSeedId;
    }

    @Override
    public void setGrowingSeedId(int seedId) {
        growingSeedId = seedId;
    }

    @Override
    public int getActionSeedId() {
        return actionSeedId;
    }

    @Override
    public void setActionSeedId(int actionSeedId) {
        this.actionSeedId = actionSeedId;
    }

    @Override
    public int execute(GrowingSeed seed) {
        setDateActionDone(Calendar.getInstance().getTime());
        seed.getPlant().getActionToDo().remove(this);
        seed.getPlant().getActionDone().add(this);
        actionSeedManager.doAction(this, seed);

        GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
        tracker.trackEvent("Seed", getName(), seed.getPlant().getSpecie(), 0);

        mContext.sendBroadcast(new Intent(BroadCastMessages.ACTION_EVENT));
        return 1;
    }
}
