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
package org.gots.seed.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.gots.R;
import org.gots.context.GotsContext;
import org.gots.preferences.GotsPreferences;
import org.gots.seed.BaseSeed;
import org.gots.seed.LikeStatus;
import org.gots.seed.SeedUtil;

public class SeedWidgetTile extends LinearLayout {
    Context mContext;

    // private String TAG = SeedWidgetLong.class.getSimpleName();
    LinearLayout stockLayout;
    TextView stockValue;
    ImageView seedView;
    TextView seedSpecie;
    TextView seedVariety;
    private BaseSeed mSeed;
    private TextView likeCount;
    private ImageView like;
    private ImageView state;
    private View likeContainer;

    public SeedWidgetTile(Context context) {
        super(context);
        this.mContext = context;
        initView();

    }

    public SeedWidgetTile(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView();
    }

    private void initView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.seed_widget_tile, this);


        stockLayout = (LinearLayout) findViewById(R.id.idSeedStock);
        stockValue = (TextView) findViewById(R.id.textViewNbStock);
        seedView = (ImageView) findViewById(R.id.idSeedImage2);

        seedSpecie = (TextView) findViewById(R.id.IdSeedSpecie);
        seedVariety = (TextView) findViewById(R.id.IdSeedVariety);
        state = (ImageView) findViewById(R.id.imageStateValidation);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupView();
    }

    @SuppressWarnings("deprecation")
    private void setupView() {

        if (mSeed == null)
            return;

        Bitmap image = SeedUtil.getSeedBitmap(GotsContext.get(mContext).getServerConfig().getFilesDir(), mSeed);
        if (image != null)
            seedView.setImageBitmap(image);
        else {
            seedView.setImageResource(SeedUtil.getSeedDrawable(getContext(), mSeed));
        }

        seedSpecie.setText(SeedUtil.translateSpecie(mContext, mSeed));
        if (GotsPreferences.DEBUG)
            seedSpecie.setText("(" + mSeed.getSeedId() + ")" + SeedUtil.translateSpecie(mContext, mSeed));

        seedVariety.setText(mSeed.getVariety());

        if ("approved".equals(mSeed.getState()))
            state.setVisibility(View.VISIBLE);
        else
            state.setVisibility(View.GONE);

        if (mSeed.getNbSachet() > 0) {
            stockLayout.setVisibility(View.VISIBLE);
            stockValue.setText(String.valueOf(mSeed.getNbSachet()));
        } else
            stockLayout.setVisibility(View.GONE);


        if (mSeed.getLanguage() != null && !"".equals(mSeed.getLanguage())) {
            ImageView flag = (ImageView) findViewById(R.id.IdSeedLanguage);
            int flagRessource = getResources().getIdentifier("org.gots:drawable/" + mSeed.getLanguage().toLowerCase(),
                    null, null);
            flag.setImageResource(flagRessource);
        }

        likeContainer = (View) findViewById(R.id.layoutLikeContainer);
        likeCount = (TextView) findViewById(R.id.textSeedLike);
        like = (ImageView) findViewById(R.id.ImageSeedLike);

        displayLikeStatus(mSeed.getLikeStatus());
        if (mSeed.getUUID() == null)
            like.setVisibility(View.GONE);

//        like.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                new AsyncTask<Void, Void, LikeStatus>() {
//                    GotsException exception = null;
//
//                    @Override
//                    protected LikeStatus doInBackground(Void... params) {
//                        GotsSeedManager manager = GotsSeedManager.getInstance().initIfNew(mContext);
//                        try {
//                            return manager.like(mSeed, mSeed.getLikeStatus().getUserLikeStatus() == 1);
//                        } catch (GotsException e) {
//                            exception = e;
//                            return null;
//                        } catch (Exception e) {
//                            Log.e(getClass().getName(), "" + e.getMessage(), e);
//                            return new LikeStatus();
//                        }
//                    }
//
//                    protected void onPostExecute(LikeStatus result) {
//                        if (result == null && exception != null) {
//                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
//
//                            // set title
//                            alertDialogBuilder.setTitle(exception.getMessage());
//                            alertDialogBuilder.setMessage(exception.getMessageDescription()).setCancelable(false).setPositiveButton(
//                                    mContext.getResources().getString(R.string.login_connect),
//                                    new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int id) {
//                                            // Intent loginIntent = new Intent(mContext, LoginFragment.class);
//                                            // mContext.startActivity(loginIntent);
//                                            LoginFragment dialogFragment = new LoginFragment();
//                                            dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);
//                                            dialogFragment.show(
//                                                    ((FragmentActivity) mContext).getSupportFragmentManager(), "");
//                                        }
//                                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    dialog.cancel();
//                                }
//                            });
//
//                            AlertDialog alertDialog = alertDialogBuilder.create();
//
//                            alertDialog.show();
//
//                            return;
//                        }
//                        mSeed.setLikeStatus(result);
//                        displayLikeStatus(result);
//                        mContext.sendBroadcast(new Intent(BroadCastMessages.SEED_DISPLAYLIST));
//
//                    }
//
//                    ;
//                }.execute();
//
//            }
//        });
    }

    protected void displayLikeStatus(LikeStatus likeStatus) {
        if (likeStatus != null && likeStatus.getLikesCount() > 0) {
            likeCount.setText(String.valueOf(likeStatus.getLikesCount()));
            likeCount.setTextColor(getResources().getColor(R.color.text_color_dark));
        }

        if (likeStatus != null && likeStatus.getUserLikeStatus() > 0) {
            like.setImageDrawable(getResources().getDrawable(R.drawable.ic_like));
            likeCount.setTextColor(getResources().getColor(R.color.text_color_light));
            likeContainer.setVisibility(View.VISIBLE);

        } else {
            like.setImageDrawable(getResources().getDrawable(R.drawable.ic_like_unknown));
            likeContainer.setVisibility(View.GONE);

        }
    }

    public void setSeed(BaseSeed seed) {
        this.mSeed = seed;
        setupView();
    }

}
