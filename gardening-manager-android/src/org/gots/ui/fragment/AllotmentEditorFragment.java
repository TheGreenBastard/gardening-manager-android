package org.gots.ui.fragment;

import org.gots.R;
import org.gots.allotment.GotsAllotmentManager;
import org.gots.allotment.provider.AllotmentProvider;
import org.gots.bean.Allotment;
import org.gots.bean.BaseAllotmentInterface;
import org.gots.seed.GrowingSeed;
import org.gots.seed.adapter.ListGrowingSeedAdapter;
import org.gots.utils.FileUtilities;

import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.Adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class AllotmentEditorFragment extends BaseGotsFragment {

    public interface OnAllotmentListener {
        public void onAllotmentCreated(BaseAllotmentInterface allotment);

        public void onAllotmentModified(BaseAllotmentInterface allotment);

        public void onAllotmentSeedClicked(BaseAllotmentInterface allotment, GrowingSeed seed);

        public void onAllotmentAddPlantClicked(BaseGotsFragment fragment, BaseAllotmentInterface allotment);

    }

    protected static final int REQUEST_ACTION_PICK = 10;

    private OnAllotmentListener mCallback;

    // private AllotmentProvider allotmentManager;

    private TextView textviewAllotmentName;

    // private Button buttonNewAllotment;

    private BaseAllotmentInterface allotment;

    private TextView textviewPlantCount;

    private GridView gridView;

    private ImageView imageViewAllotment;

    @Override
    public void onAttach(Activity activity) {
        try {
            mCallback = (OnAllotmentListener) activity;
        } catch (ClassCastException castException) {
            throw new ClassCastException(AllotmentEditorFragment.class.getSimpleName()
                    + " must implements OnActionSelectedListener");
        }
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.allotment_content, null);
        // allotmentManager = GotsAllotmentManager.getInstance().initIfNew(getActivity());

        textviewAllotmentName = (TextView) view.findViewById(R.id.editTextAllotmentName);
        // buttonNewAllotment = (Button) view.findViewById(R.id.buttonAllotmentNew);
        textviewPlantCount = (TextView) view.findViewById(R.id.textViewNbPlants);
        imageViewAllotment = (ImageView) view.findViewById(R.id.imageViewAllotment);
        gridView = (GridView) view.findViewById(R.id.IdGrowingSeedList);

        imageViewAllotment.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_ACTION_PICK);
            }
        });
        if (allotment == null) {
            allotment = new Allotment();
            // buttonNewAllotment.setOnClickListener(new View.OnClickListener() {
            //
            // @Override
            // public void onClick(View v) {
            // new AsyncTask<Void, Void, BaseAllotmentInterface>() {
            //
            // protected void onPreExecute() {
            // allotment = new Allotment();
            // allotment.setName(textviewAllotmentName.getText().toString());
            // };
            //
            // @Override
            // protected BaseAllotmentInterface doInBackground(Void... params) {
            // return allotmentManager.createAllotment(allotment);
            // }
            //
            // protected void onPostExecute(BaseAllotmentInterface newAllotment) {
            // mCallback.onAllotmentCreated(newAllotment);
            // };
            //
            // }.execute();
            // };
            // });
        }
        // else {
        // buttonNewAllotment.setText(getResources().getString(R.string.menu_edit));
        // buttonNewAllotment.setOnClickListener(new View.OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // new AsyncTask<Void, Void, BaseAllotmentInterface>() {
        //
        // protected void onPreExecute() {
        // allotment.setName(textviewAllotmentName.getText().toString());
        // };
        //
        // @Override
        // protected BaseAllotmentInterface doInBackground(Void... params) {
        // return allotmentManager.updateAllotment(allotment);
        // }
        //
        // protected void onPostExecute(BaseAllotmentInterface result) {
        // mCallback.onAllotmentCreated(allotment);
        // };
        //
        // }.execute();
        // };
        // });
        else {

        }
        // }

        textviewAllotmentName.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    allotment.setName(textviewAllotmentName.getText().toString());
                    if (allotment.getId() >= 0)
                        mCallback.onAllotmentModified(allotment);
                    else
                        mCallback.onAllotmentCreated(allotment);
                    hideKeyboard();

                }

            }
        });

        return view;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(textviewAllotmentName.getWindowToken(), 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ACTION_PICK && data != null) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);

            allotment.setImagePath(picturePath);
            Bitmap bitmap = FileUtilities.decodeScaledBitmapFromSdCard(picturePath, 100, 100);

            imageViewAllotment.setImageBitmap(bitmap);
            cursor.close();
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void update() {
        textviewPlantCount.setText(allotment != null ? "" + allotment.getSeeds().size() : "0");
        textviewAllotmentName.setText(allotment != null ? allotment.getName() : "");

        if (allotment.getImagePath() != null) {
            Bitmap bitmap = FileUtilities.decodeScaledBitmapFromSdCard(allotment.getImagePath(), 100, 100);
            imageViewAllotment.setImageBitmap(bitmap);
        }
        if (allotment != null) {
            textviewPlantCount.setText(allotment != null ? "" + allotment.getSeeds().size() : "0");
            textviewAllotmentName.setText(allotment != null ? allotment.getName() : "");
            if (allotment.getImagePath() != null) {
                Bitmap bitmap = FileUtilities.decodeScaledBitmapFromSdCard(allotment.getImagePath(), 100, 100);
                imageViewAllotment.setImageBitmap(bitmap);
            }
            if (allotment != null) {
                final ListGrowingSeedAdapter adapter = new ListGrowingSeedAdapter(getActivity(), allotment.getSeeds());
                gridView.setAdapter(adapter);
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position < adapter.getCount() - 1) {
                            mCallback.onAllotmentSeedClicked(allotment, adapter.getItem(position));

                        } else
                            mCallback.onAllotmentAddPlantClicked(AllotmentEditorFragment.this, allotment);
                    }
                });

            }
            final ListGrowingSeedAdapter adapter = new ListGrowingSeedAdapter(getActivity(), allotment.getSeeds());
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (position < adapter.getCount() - 1) {
                        mCallback.onAllotmentSeedClicked(allotment, adapter.getItem(position));

                    } else
                        mCallback.onAllotmentAddPlantClicked(AllotmentEditorFragment.this, allotment);
                }
            });

        }
    }

    public void setAllotment(BaseAllotmentInterface allotment) {
        this.allotment = allotment;
    }

    @Override
    protected boolean requireAsyncDataRetrieval() {
        return false;
    }

    @Override
    public void onResume() {
        update();
        super.onResume();
    }

}
