package org.gots.authentication.syncadapter;

import java.util.List;

import org.gots.authentication.GotsSyncAdapter;
import org.gots.broadcast.BroadCastMessages;
import org.gots.garden.GardenManager;
import org.gots.seed.BaseSeedInterface;
import org.gots.seed.GotsSeedManager;
import org.gots.seed.service.SeedNotification;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

public class SeedSyncAdapter extends GotsSyncAdapter {
    protected GotsSeedManager seedManager;

    public SeedSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
            SyncResult syncResult) {
        Log.d("SeedSyncAdapter", "onPerformSync for account[" + account.name + "]");

        final Intent intent = new Intent();
        intent.setAction(BroadCastMessages.PROGRESS_UPDATE);
        intent.putExtra("AUTHORITY", authority);
        getContext().sendBroadcast(intent);

        seedManager = GotsSeedManager.getInstance().initIfNew(getContext());

        seedManager.force_refresh(true);

        seedManager.getVendorSeeds(true);

        GardenManager gardenManager = GardenManager.getInstance().initIfNew(getContext());
        seedManager.getMyStock(gardenManager.getCurrentGarden());

        List<BaseSeedInterface> newSeeds = seedManager.getNewSeeds();
        if (newSeeds != null && newSeeds.size() > 0) {
            SeedNotification notification = new SeedNotification(getContext());
            notification.createNotification(newSeeds);
        }
        
        intent.setAction(BroadCastMessages.PROGRESS_FINISHED);
        getContext().sendBroadcast(intent);

    }
}