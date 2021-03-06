package org.gots.allotment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.gots.allotment.provider.AllotmentProvider;
import org.gots.allotment.provider.local.LocalAllotmentProvider;
import org.gots.allotment.provider.nuxeo.NuxeoAllotmentProvider;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.broadcast.BroadCastMessages;
import org.gots.context.GotsContext;
import org.gots.nuxeo.NuxeoManager;
import org.gots.utils.NotConfiguredException;
import org.nuxeo.android.broadcast.NuxeoBroadcastMessages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GotsAllotmentManager extends BroadcastReceiver implements AllotmentProvider {

    private static final String TAG = "GotsAllotmentManager";

    private static GotsAllotmentManager instance;

    private static Exception firstCall;
    Map<Integer, BaseAllotmentInterface> allotments;
    private Context mContext;
    private AllotmentProvider allotmentProvider = null;
    private boolean initDone = false;
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

    protected GotsContext getGotsContext() {
        return GotsContext.get(mContext);
    }

    /**
     * If it was already called once, the method returns without any change.
     */
    public synchronized AllotmentProvider initIfNew(Context context) {
        if (initDone) {
            return this;
        }
        this.mContext = context;
        nuxeoManager = NuxeoManager.getInstance().initIfNew(context);
        setAllotmentProvider();
        initDone = true;
        return this;
    }

    public void reset() {
        initDone = false;
        mContext = null;
        instance = null;
    }

    private void setAllotmentProvider() {
        if (getGotsContext().getServerConfig().isConnectedToServer() && !nuxeoManager.getNuxeoClient().isOffline()) {
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
            setAllotmentProvider();
            haschanged = true;
        }
    }

    @Override
    public List<BaseAllotmentInterface> getMyAllotments(boolean force) {
        if (allotments == null || force || haschanged) {
            haschanged = false;
            allotments = new HashMap<Integer, BaseAllotmentInterface>();
            for (BaseAllotmentInterface allotment : allotmentProvider.getMyAllotments(force)) {
                allotments.put(allotment.getId(), allotment);
            }
        }
        return new ArrayList<BaseAllotmentInterface>(allotments.values());
    }

    @Override
    public BaseAllotmentInterface createAllotment(BaseAllotmentInterface allotment) {
        allotment = allotmentProvider.createAllotment(allotment);
        allotments.put(allotment.getId(), allotment);
        mContext.sendBroadcast(new Intent(BroadCastMessages.ALLOTMENT_EVENT));
        return allotment;
    }

    @Override
    public int removeAllotment(BaseAllotmentInterface allotment) {
        allotments.remove(allotment.getId());
        allotmentProvider.removeAllotment(allotment);
        mContext.sendBroadcast(new Intent(BroadCastMessages.ALLOTMENT_EVENT));
        return 0;
    }

    @Override
    public BaseAllotmentInterface updateAllotment(BaseAllotmentInterface allotment) {
        allotments.put(allotment.getId(), allotment);
        allotment = allotmentProvider.updateAllotment(allotment);
        mContext.sendBroadcast(new Intent(BroadCastMessages.ALLOTMENT_EVENT));
        return allotment;
    }

    @Override
    public BaseAllotmentInterface getCurrentAllotment() {
        return allotmentProvider.getCurrentAllotment();
    }

    @Override
    public void setCurrentAllotment(BaseAllotmentInterface allotmentInterface) {

        allotmentProvider.setCurrentAllotment(allotmentInterface);
    }

    @Override
    public BaseAllotmentInterface getAllotmentByID(Integer id) {
        if (allotments != null)
            return allotments.get(id);
        else
            return null;
    }
}
