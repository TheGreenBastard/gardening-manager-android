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
package org.gots.seed.adapter;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.gots.action.BaseActionInterface;
import org.gots.action.GardeningActionInterface;
import org.gots.action.GotsActionManager;
import org.gots.action.SeedActionInterface;
import org.gots.action.bean.ReduceQuantityAction;
import org.gots.action.util.ActionState;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.broadcast.BroadCastMessages;
import org.gots.seed.BaseSeedInterface;
import org.gots.seed.GotsGrowingSeedManager;
import org.gots.seed.GrowingSeedInterface;
import org.gots.seed.SeedUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class MySeedsListAdapter extends SeedListAdapter {

    private BaseAllotmentInterface allotment;

    public MySeedsListAdapter(Context context, BaseAllotmentInterface allotment, List<BaseSeedInterface> seeds) {
        super(context, seeds);
        this.allotment = allotment;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View vi = super.getView(position, convertView, parent);
        Holder holder = (Holder) vi.getTag();
        final BaseSeedInterface currentSeed = getItem(position);

        holder.seedWidgetLong.setSeed(currentSeed);
        BaseActionInterface action = null;
        if (allotment != null) {
            // action = new SowingAction(mContext);
            GotsActionManager helper = GotsActionManager.getInstance().initIfNew(mContext);
            action = helper.getActionByName("sow");
            holder.actionWidget.setAction(action);

            if (Calendar.getInstance().get(Calendar.MONTH) >= currentSeed.getDateSowingMin()
                    && Calendar.getInstance().get(Calendar.MONTH) <= currentSeed.getDateSowingMax())
                holder.actionWidget.setState(ActionState.NORMAL);
            else if (Calendar.getInstance().get(Calendar.MONTH) + 1 >= currentSeed.getDateSowingMin())
                holder.actionWidget.setState(ActionState.WARNING);
            else
                holder.actionWidget.setState(ActionState.UNDEFINED);
            holder.actionWidget.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new AsyncTask<Void, Integer, GrowingSeedInterface>() {
                        @Override
                        protected GrowingSeedInterface doInBackground(Void... params) {
                            GotsGrowingSeedManager provider = GotsGrowingSeedManager.getInstance().initIfNew(mContext);
                            // NuxeoGrowingSeedProvider provider = new NuxeoGrowingSeedProvider(mContext);
                            GrowingSeedInterface growingSeed = (GrowingSeedInterface) currentSeed;
                            growingSeed.setDateSowing(Calendar.getInstance().getTime());

                            return provider.insertSeed(growingSeed, allotment);
                        }

                        @Override
                        protected void onPostExecute(GrowingSeedInterface seed) {
                            // notifyDataSetChanged();
                            Toast.makeText(mContext, "Sowing" + " " + SeedUtil.translateSpecie(mContext, seed),
                                    Toast.LENGTH_LONG).show();
                            mContext.sendBroadcast(new Intent(BroadCastMessages.SEED_DISPLAYLIST));
                            ((Activity) mContext).finish();
                        }
                    }.execute();

                }
            });

        } else {
            action = new ReduceQuantityAction(mContext);
//            action.setState(ActionState.NORMAL);

            holder.actionWidget.setAction(action);
            final BaseActionInterface baseActionInterface = action;
            holder.actionWidget.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    new AsyncTask<Void, Integer, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            if (allotment != null) {
                                GardeningActionInterface action = (GardeningActionInterface) baseActionInterface;
                                action.execute(allotment, (GrowingSeedInterface) currentSeed);
                                ((Activity) mContext).finish();
                            } else {
                                SeedActionInterface action = (SeedActionInterface) baseActionInterface;
                                action.execute((GrowingSeedInterface) currentSeed);
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void result) {
                            // notifyDataSetChanged();
                            Toast.makeText(
                                    mContext,
                                    SeedUtil.translateAction(mContext, baseActionInterface) + " - "
                                            + SeedUtil.translateSpecie(mContext, currentSeed), Toast.LENGTH_LONG).show();
                            mContext.sendBroadcast(new Intent(BroadCastMessages.SEED_DISPLAYLIST));
                            super.onPostExecute(result);
                        }
                    }.execute();

                }
            });
        }

        try {

            Calendar sowTime = Calendar.getInstance();
            if (sowTime.get(Calendar.MONTH) > currentSeed.getDateSowingMin())
                sowTime.set(Calendar.YEAR, sowTime.get(Calendar.YEAR) + 1);
            sowTime.set(Calendar.MONTH, currentSeed.getDateSowingMin());

            Calendar harvestTime = new GregorianCalendar();
            harvestTime.setTime(sowTime.getTime());
            harvestTime.add(Calendar.DAY_OF_MONTH, currentSeed.getDurationMin());

        } catch (Exception e) {
            // holder.seedSowingDate.setText("--");
        }

        return vi;

    }

}