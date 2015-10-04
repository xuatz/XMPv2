package com.xuatzsolutions.xuatzmediaplayer2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xuatzsolutions.xuatzmediaplayer2.HelperClasses.MySongManager;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Track;
import com.xuatzsolutions.xuatzmediaplayer2.Models.TrackStats;
import com.xuatzsolutions.xuatzmediaplayer2.Services.MediaPlayerService;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends Activity {

    private static final String INTENT_BASE_NAME = "com.xuatzsolutions.xuatzmediaplayer2.MainActivity";
    public static final String INTENT_PLAY_PAUSE = INTENT_BASE_NAME + ".PLAY_PAUSE_CLICKED";
    public static final String INTENT_NEXT = INTENT_BASE_NAME + ".NEXT_CLICKED";
    public static final String INTENT_LIKED =INTENT_BASE_NAME + ".LIKED";
    public static final String INTENT_DISLIKED = INTENT_BASE_NAME + ".DISLIKED";
    private final String TAG = "MainActivity";

    MediaPlayerService mService;
    boolean mBound = false;
    boolean isActivityVisible = false;

    Realm realm = null;

    private BroadcastReceiver mReceiver;
    private IntentFilter intentFilter;

    private TextView tvCurrentTrackTitle;
    private TextView tvCurrentTrackComCount;
    private TextView tvCurrentTrackSkipCount;
    private TextView tvCurrentTrackSelectCount;
    private TextView tvCurrentTrackLikeCount;
    private TextView tvCurrentTrackDislikeCount;


    private Button btnPlayPause;
    private Button btnNext;
    private Button btnLiked;
    private Button btnDisliked;
    private ProgressDialog pdNewSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        realm = Realm.getInstance(MainActivity.this);

        tvCurrentTrackTitle = (TextView) findViewById(R.id.tv_current_song_title);
        tvCurrentTrackComCount = (TextView) findViewById(R.id.tvCurrentTrackComCount);
        tvCurrentTrackSkipCount = (TextView) findViewById(R.id.tvCurrentTrackSkipCount);
        //tvCurrentTrackSelectCount = (TextView) findViewById(R.id.tvCurrentTrackSelectCount);
        tvCurrentTrackLikeCount = (TextView) findViewById(R.id.tvCurrentTrackLikeCount);
        tvCurrentTrackDislikeCount = (TextView) findViewById(R.id.tvCurrentTrackDislikeCount);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch(intent.getAction()) {
                    case MediaPlayerService.INTENT_MP_READY:
                        Log.d(TAG, "onReceive() INTENT_MP_READY");

                        if (mService.getCurrentTrack() != null) {
                            updateScreenAndNotification();
                        }

                        break;
                    case MediaPlayerService.INTENT_SESSION_TRACKS_GENERATING:
                        Log.d(TAG, "onReceive() INTENT_SESSION_TRACKS_GENERATING :mytest");
                        pdNewSession = new ProgressDialog(MainActivity.this);
                        pdNewSession.setMessage("Generating Session Songs...");
                        pdNewSession.setIndeterminate(false);
                        pdNewSession.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        pdNewSession.setCancelable(false);
                        pdNewSession.show();
                        break;
                    case MediaPlayerService.INTENT_SESSION_TRACKS_GENERATED:
                        Log.d(TAG, "onReceive() INTENT_SESSION_TRACKS_GENERATED :mytest");
                        pdNewSession.dismiss();
                        break;
                }
            }
        };

        btnPlayPause = (Button) findViewById(R.id.btn_play_pause);
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent().setAction(INTENT_PLAY_PAUSE));
            }
        });

        btnNext = (Button) findViewById(R.id.btn_next);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent().setAction(INTENT_NEXT));
            }
        });

        btnLiked = (Button) findViewById(R.id.btn_like);
        btnLiked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService.getCurrentTrack() != null) {
                    showShortToast("This song rocks man!");
                    sendBroadcast(new Intent().setAction(INTENT_LIKED));
                }
            }
        });

        btnDisliked = (Button) findViewById(R.id.btn_dislike);
        btnDisliked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService.getCurrentTrack() != null) {
                    showShortToast("This song... cui...!");
                    sendBroadcast(new Intent().setAction(INTENT_DISLIKED));
                }
            }
        });

        intentFilter = new IntentFilter();
        intentFilter.addAction(MediaPlayerService.INTENT_MP_READY);
        intentFilter.addAction(MediaPlayerService.INTENT_SESSION_TRACKS_GENERATING);
        intentFilter.addAction(MediaPlayerService.INTENT_SESSION_TRACKS_GENERATED);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();

        initService();
        new PopulateSongLibrary().execute();
    }

    @Override
    protected void onResume() {
        super.onResume();

        isActivityVisible = true;

        this.registerReceiver(mReceiver, intentFilter);

        Log.d(TAG, "mBound: " + mBound);
        if (mBound) {
            if (mService.isPlaying()) {
                updateScreenAndNotification();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
        updateScreenAndNotification();
    }

    private void updateScreenAndNotification() {
        Log.d(TAG, "updateScreenAndNotification()");

        if (mService != null) {
            if (mService.isPlaying()) {
                if (isActivityVisible) {
                    mService.cancelNotification();
                } else {
                    mService.showNotification();
                }
            }

            if (mService.getCurrentTrack() != null) {


                RealmResults<TrackStats> res =
                        realm.where(TrackStats.class)
                                .equalTo("title", mService.getCurrentTrack().getTitle())
                                .findAll();

                Log.d(TAG, "Kaypoh:res.size(): " + res.size());

                int completed = 0, skipped = 0, selected = 0, liked= 0, disliked = 0;

                for (TrackStats ts : res) {
                    switch (ts.getType()) {
                        case TrackStats.SONG_COMPLETED:
                            completed++;
                            break;
                        case TrackStats.SONG_SKIPPED:
                            skipped++;
                            break;
                        case TrackStats.SONG_SELECTED:
                            selected++;
                            break;
                        case TrackStats.SONG_LIKED:
                            liked++;
                            break;
                        case TrackStats.SONG_DISLIKED:
                            disliked++;
                            break;
                    }
                }

                tvCurrentTrackTitle.setText(mService.getCurrentTrack().getTitle());

                tvCurrentTrackComCount.setText(""+completed);
                tvCurrentTrackSkipCount.setText(""+skipped);
                //tvCurrentTrackSelectCount.setText(selected);
                tvCurrentTrackLikeCount.setText(""+liked);
                tvCurrentTrackDislikeCount.setText(""+disliked);

            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false; //this is necessary
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        //stopService(new Intent(this, MediaPlayerService.class));

        this.unregisterReceiver(mReceiver);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected() start");
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            //TODO-note
            //for dev purpose, auto-start MP upon bound
            if (!mService.isPlaying()) {
                mService.prepNextSong();
            } else {
                updateScreenAndNotification();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected()");
            mBound = false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class PopulateSongLibrary extends AsyncTask<Void, Void, Void> {


        private ProgressDialog progressDialog;

        private boolean isLibEmpty = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            RealmResults<Track> res = realm.where(Track.class).findAll();

            if (res.size() == 0) {
                isLibEmpty = true;

                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.setIndeterminate(false);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.show();
            } else {
                isLibEmpty = false;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            MySongManager.updateLibrary(getApplicationContext());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (isLibEmpty) {
                progressDialog.dismiss();

                if(mBound) {
                    mService.startSession();
                }
            }
        }

    }
    public void initService() {
        Log.d(TAG, "initService()");
        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }



    private void showShortToast(String s) {
        CharSequence text = s;
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
