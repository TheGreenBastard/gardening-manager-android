package org.gots.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.vending.billing.util.Purchase;

import org.gots.R;
import org.gots.inapp.GotsPurchaseItem;
import org.gots.inapp.HolderSku;
import org.gots.inapp.OnPurchaseFinished;
import org.gots.sensor.SensorChartFragment;
import org.gots.sensor.SensorLoginDialogFragment;
import org.gots.sensor.fragment.AllSensorResumeFragment;
import org.gots.sensor.fragment.SensorResumeFragment.OnSensorClickListener;
import org.gots.sensor.parrot.ParrotLocation;
import org.gots.ui.fragment.TutorialFragment;

import java.util.ArrayList;
import java.util.List;

public class SensorActivity extends BaseGotsActivity implements OnSensorClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Parrot Flower Power");
//        setContentView(R.layout.sensor);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        registerReceiver(sensorBroadcast, new IntentFilter(SensorLoginDialogFragment.EVENT_AUTHENTICATE));

//        FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
//        AllSensorResumeFragment allSensorResumeFragment = new AllSensorResumeFragment();
//        transaction2.setCustomAnimations(R.anim.push_left_in, R.anim.push_right_out);
//        transaction2.replace(R.id.idFragmentSensorList, allSensorResumeFragment).commit();

//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.replace(R.id.idFragmentSensorContent, new TutorialFragment(R.layout.tutorial_f));
////        transaction.addToBackStack(null);
//        transaction.commit();


    }

//    BroadcastReceiver sensorBroadcast = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (SensorLoginDialogFragment.EVENT_AUTHENTICATE.equals(intent.getAction())) {
//                runAsyncDataRetrieval();
//            }
//        }
//    };

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
//        unregisterReceiver(sensorBroadcast);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.premium:
                openPurchaseFragment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openPurchaseFragment() {
        List<HolderSku> skus = new ArrayList<>();
        skus.add(new HolderSku(GotsPurchaseItem.SKU_FEATURE_PARROT, false));
        displayPurchaseFragment(skus, new OnPurchaseFinished() {
            @Override
            public void onPurchaseSucceed(Purchase purchase) {
                if (GotsPurchaseItem.SKU_FEATURE_PARROT.equals(purchase.getSku())) {
                    gotsPurchase.setFeatureParrot(true);
                }
            }

            @Override
            public void onPurchaseFailed(Purchase purchase) {

            }
        });
    }

    @Override
    public void OnSensorClick(ParrotLocation locationSensor) {

//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out);
//        ft.addToBackStack(null);
//        ft.replace(R.id.idFragmentSensorContent, new SensorChartFragment(locationSensor));
//        ft.commit();
        addContentLayout(new SensorChartFragment(locationSensor), null);
    }

    @Override
    protected boolean requireFloatingButton() {
        return true;
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return true;
    }

    @Override
    protected Object retrieveNuxeoData() throws Exception {
        return "";
    }

    @Override
    protected void onNuxeoDataRetrieved(Object data) {
        if (gotsPrefs.getParrotToken() == null)
            addMainLayout(new TutorialFragment(R.layout.tutorial_f), null);
        else
            addMainLayout(new AllSensorResumeFragment(), null);
        super.onNuxeoDataRetrieved(data);
    }

    @Override
    protected List<FloatingItem> onCreateFloatingMenu() {
        ArrayList<FloatingItem> items = new ArrayList<>();
        if (gotsPrefs.getParrotToken() == null) {
            FloatingItem item = new FloatingItem();
            if (gotsPurchase.getFeatureParrot()) {
                item.setRessourceId(R.drawable.ic_login);
                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SensorLoginDialogFragment login = new SensorLoginDialogFragment(new SensorLoginDialogFragment.SensorLoginEvent() {
                            @Override
                            public void onSensorLoginSuccess() {
                                runAsyncDataRetrieval();
                            }

                            @Override
                            public void onSensorLoginFailed() {
                                Log.w(TAG, "onSensorLoginFailed");
                            }
                        });
                        login.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialog);
                        login.show(getSupportFragmentManager(), "sensor_login");
                    }
                });
            } else {
                item.setTitle(getResources().getString(R.string.inapp_purchase_buy));
                item.setRessourceId(R.drawable.action_buy_online);
                item.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        openPurchaseFragment();
                    }
                });
            }
            items.add(item);
        }


        return items;
    }
}
