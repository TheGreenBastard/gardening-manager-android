package org.gots.garden;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gots.broadcast.BroadCastMessages;
import org.gots.garden.provider.GardenProvider;
import org.gots.garden.provider.local.LocalGardenProvider;
import org.gots.garden.provider.nuxeo.NuxeoGardenProvider;
import org.gots.preferences.GotsPreferences;
import org.gots.utils.NotConfiguredException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class GardenManager extends BroadcastReceiver {
    private static final String TAG = "GardenManager";

    private static GardenManager instance;

    private static Exception firstCall;

    private Context mContext;

    private GardenProvider gardenProvider = null;

    private boolean initDone = false;

    private Map<Long, GardenInterface> myGardens;

    private GardenInterface currentGarden;

    private GardenManager() {
    }

    /**
     * After first call, {@link #initIfNew(Context)} must be called else a {@link NotConfiguredException} will be thrown
     * on the second call attempt.
     */
    public static synchronized GardenManager getInstance() {
        if (instance == null) {
            instance = new GardenManager();
            firstCall = new Exception();

        } else if (!instance.initDone) {
            throw new NotConfiguredException(firstCall);
        }
        return instance;
    }

    public void reset() {
        initDone = false;
    }

    /**
     * If it was already called once, the method returns without any change.
     * 
     * @return TODO
     */
    public synchronized GardenManager initIfNew(Context context) {
        if (initDone) {
            return this;
        }
        this.mContext = context;
        setGardenProvider();
        initDone = true;
        return instance;
    }

    public void finalize() {
        initDone = false;
        mContext = null;
        instance = null;
    }

    private void setGardenProvider() {
        // new AsyncTask<Void, Integer, Void>() {
        // @Override
        // protected Void doInBackground(Void... params) {
        if (GotsPreferences.getInstance().isConnectedToServer()) {
            gardenProvider = new NuxeoGardenProvider(mContext);
        } else {
            // return null;
            // }
            // }.execute();
            gardenProvider = new LocalGardenProvider(mContext);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BroadCastMessages.CONNECTION_SETTINGS_CHANGED.equals(intent.getAction())
                || BroadCastMessages.GARDEN_SETTINGS_CHANGED.equals(intent.getAction())) {
            setGardenProvider();
        }
    }

    public GardenInterface addGarden(GardenInterface garden) {
        GardenInterface newGarden = gardenProvider.createGarden(garden);
        // gardenProvider.setCurrentGarden(newGarden);
        // GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
        // tracker.trackEvent("Garden", "location", result.getLocality(), 0);
        return newGarden;
    }

    public GardenInterface getCurrentGarden() {
        if(currentGarden == null)
            currentGarden = gardenProvider.getCurrentGarden();
        
        return currentGarden;
    }

    public void setCurrentGarden(GardenInterface garden) {
        currentGarden = garden;
        gardenProvider.setCurrentGarden(garden);
        // mContext.sendBroadcast(new Intent(BroadCastMessages.GARDEN_EVENT));

        Log.d(TAG, "[" + garden.getId() + "] " + garden.getLocality() + " has been set as current garden");
    }

    public void removeGarden(GardenInterface garden) {
        gardenProvider.removeGarden(garden);
        myGardens.remove(garden.getId());
    }

    public void updateCurrentGarden(GardenInterface garden) {
        gardenProvider.updateGarden(garden);
        myGardens.put(garden.getId(), garden);

    }

    public List<GardenInterface> getMyGardens(boolean force) {
        if (myGardens == null || force) {
            myGardens = new HashMap<Long, GardenInterface>();
            for (GardenInterface garden : gardenProvider.getMyGardens(force)) {
                myGardens.put(garden.getId(), garden);
            }
        }

        // myGardens = gardenProvider.getMyGardens(force);
        return new ArrayList<GardenInterface>(myGardens.values());
    }

}
