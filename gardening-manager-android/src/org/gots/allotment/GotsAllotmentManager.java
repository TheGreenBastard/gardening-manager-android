package org.gots.allotment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gots.allotment.provider.AllotmentProvider;
import org.gots.allotment.provider.local.LocalAllotmentProvider;
import org.gots.allotment.provider.nuxeo.NuxeoAllotmentProvider;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.broadcast.BroadCastMessages;
import org.gots.nuxeo.NuxeoManager;
import org.gots.preferences.GotsPreferences;
import org.gots.utils.NotConfiguredException;
import org.nuxeo.android.broadcast.NuxeoBroadcastMessages;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GotsAllotmentManager extends BroadcastReceiver implements AllotmentProvider {

    private static final String TAG = "GotsAllotmentManager";

    private static GotsAllotmentManager instance;

    private static Exception firstCall;

    private Context mContext;

    private AllotmentProvider allotmentProvider = null;

    private boolean initDone = false;

    Map<Integer, BaseAllotmentInterface> allotments;

    private boolean haschanged = false;

    private NuxeoManager nuxeoManager;

    private GotsAllotmentManager() {

    }

    /**
     * After first call, {@link #initIfNew(Context)} must be called else a {@link NotConfiguredException} will be thrown
     * on the second call attempt.
     */
    public static synchronized GotsAllotmentManager getInstance() {
        if (instance == null) {
            instance = new GotsAllotmentManager();
            firstCall = new Exception();

        } else if (!instance.initDone) {
            throw new NotConfiguredException(firstCall);
        }
        return instance;
    }

    /**
     * If it was already called once, the method returns without any change.
     */
    public synchronized GotsAllotmentManager initIfNew(Context context) {
        if (initDone) {
            return this;
        }
        this.mContext = context;
        // mContext.registerReceiver(this, new IntentFilter(BroadCastMessages.CONNECTION_SETTINGS_CHANGED));
        nuxeoManager = NuxeoManager.getInstance().initIfNew(context);
        setAllotmentProvider();
        initDone = true;
        return this;
    }

    public void reset() {
        initDone = false;
    }

    public void finalize() {

        // mContext.unregisterReceiver(this);
        initDone = false;
        mContext = null;
        instance = null;
    }

    private void setAllotmentProvider() {
        if (NuxeoManager.getInstance().getNuxeoClient().isOffline())
            Log.i(TAG, "isOffline");
        else
            Log.i(TAG, "isOnline");

        if (GotsPreferences.getInstance().isConnectedToServer() && !nuxeoManager.getNuxeoClient().isOffline()) {
            allotmentProvider = new NuxeoAllotmentProvider(mContext);
        } else {
            allotmentProvider = new LocalAllotmentProvider(mContext);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BroadCastMessages.CONNECTION_SETTINGS_CHANGED.equals(intent.getAction())
                || NuxeoBroadcastMessages.NUXEO_SERVER_CONNECTIVITY_CHANGED.equals(intent.getAction())) {
            setAllotmentProvider();
        }
        if (BroadCastMessages.GARDEN_CURRENT_CHANGED.equals(intent.getAction())) {
            haschanged = true;
        }
    }

    // public void setCurrentGarden(GardenInterface garden) {
    // GotsPreferences.getInstance().set(GotsPreferences.ORG_GOTS_CURRENT_GARDENID, (int) garden.getId());
    // Log.d("setCurrentGarden", "[" + garden.getId() + "] " + garden.getLocality()
    // + " has been set as current workspace");
    // changeDatabase((int) garden.getId());
    // }

    @Override
    public BaseAllotmentInterface getCurrentAllotment() {
        return allotmentProvider.getCurrentAllotment();
    }

    @Override
    public List<BaseAllotmentInterface> getMyAllotments(boolean force) {
        if (allotments == null || force || haschanged) {
            haschanged = false;
            allotments = new HashMap<Integer, BaseAllotmentInterface>();
            for (BaseAllotmentInterface allotment : allotmentProvider.getMyAllotments(false)) {
                allotments.put(allotment.getId(), allotment);
            }
        }
        return new ArrayList<BaseAllotmentInterface>(allotments.values());
    }

    @Override
    public BaseAllotmentInterface createAllotment(BaseAllotmentInterface allotment) {
        allotments.put(allotment.getId(), allotment);
        return allotmentProvider.createAllotment(allotment);
    }

    @Override
    public int removeAllotment(BaseAllotmentInterface allotment) {
        allotments.remove(allotment.getId());
        return allotmentProvider.removeAllotment(allotment);
    }

    @Override
    public BaseAllotmentInterface updateAllotment(BaseAllotmentInterface allotment) {
        allotments.put(allotment.getId(), allotment);
        return allotmentProvider.updateAllotment(allotment);
    }

    @Override
    public void setCurrentAllotment(BaseAllotmentInterface allotmentInterface) {

        allotmentProvider.setCurrentAllotment(allotmentInterface);
    }
}