package com.xuatzsolutions.xuatzmediaplayer2;

import android.app.Fragment;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.xuatzsolutions.xuatzmediaplayer2.HelperClasses.MySongManager;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Track;
import com.xuatzsolutions.xuatzmediaplayer2.Services.MediaPlayerService;

import io.realm.Realm;
import io.realm.RealmResults;

public class MusicPlayerFragment extends Fragment {

    private static final String INTENT_BASE_NAME = "com.xuatzsolutions.xuatzmediaplayer2.MainActivity";
    public static final String INTENT_PLAY_PAUSE = INTENT_BASE_NAME + ".PLAY_PAUSE_CLICKED";
    public static final String INTENT_NEXT = INTENT_BASE_NAME + ".NEXT_CLICKED";
    public static final String INTENT_LIKED =INTENT_BASE_NAME + ".LIKED";
    public static final String INTENT_DISLIKED = INTENT_BASE_NAME + ".DISLIKED";
    private final String TAG = "MainActivity";

    MediaPlayerService mService;
    boolean mBound = false;

    Realm realm = null;

    private BroadcastReceiver mReceiver;
    private IntentFilter intentFilter;

    private TextView tvCurrentTrackTitle;
    private Button btnPlayPause;
    private Button btnNext;
    private Button btnLiked;
    private Button btnDisliked;
    private ProgressDialog pdNewSession;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.activity_main2, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCurrentTrackTitle = (TextView) getView().findViewById(R.id.tv_current_song_title);


        btnPlayPause = (Button) getView().findViewById(R.id.btn_play_pause);
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().sendBroadcast(new Intent().setAction(INTENT_PLAY_PAUSE));
            }
        });

        btnNext = (Button) getView().findViewById(R.id.btn_next);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().sendBroadcast(new Intent().setAction(INTENT_NEXT));
            }
        });

        btnLiked = (Button) getView().findViewById(R.id.btn_like);
        btnLiked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShortToast("This song rocks man!");
                getActivity().sendBroadcast(new Intent().setAction(INTENT_LIKED));
            }
        });

        btnDisliked = (Button) getView().findViewById(R.id.btn_dislike);
        btnDisliked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShortToast("This song... cui...!");
                getActivity().sendBroadcast(new Intent().setAction(INTENT_DISLIKED));
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getInstance(getActivity());

        new PopulateSongLibrary().execute();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch(intent.getAction()) {
                    case MediaPlayerService.INTENT_MP_READY:
                        Log.d(TAG, "onReceive() INTENT_MP_READY");
                        //updateScreenAndRemoveNotiIfVisible();

                        if (mService.getCurrentTrack() != null) {
                            updateScreen();
                        }

                        break;
                    case MediaPlayerService.INTENT_SESSION_TRACKS_GENERATING:
                        Log.d(TAG, "onReceive() INTENT_SESSION_TRACKS_GENERATING :mytest");
                        pdNewSession = new ProgressDialog(getActivity());
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

        intentFilter = new IntentFilter();
        intentFilter.addAction(MediaPlayerService.INTENT_MP_READY);
        intentFilter.addAction(MediaPlayerService.INTENT_SESSION_TRACKS_GENERATING);
        intentFilter.addAction(MediaPlayerService.INTENT_SESSION_TRACKS_GENERATED);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();

        initService();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mReceiver, intentFilter);

        Log.d(TAG, "mBound: " + mBound);
        if (mBound) {
            if (mService.isPlaying()) {
                updateScreen();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false; //this is necessary
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        //stopService(new Intent(this, MediaPlayerService.class));
        getActivity().unregisterReceiver(mReceiver);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            //TODO-note
            //for dev purpose, auto-start MP upon bound
            if (!mService.isPlaying()) {
                mService.prepNextSong();
            } else {
                updateScreen();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected()");
            mBound = false;
        }
    };

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        this.getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    private void updateScreen() {
        Log.d(TAG, "updateScreen()");

        tvCurrentTrackTitle.setText(mService.getCurrentTrack().getTitle());
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

                progressDialog = new ProgressDialog(getActivity());
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
            MySongManager.updateLibrary(getActivity());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (isLibEmpty) {
                progressDialog.dismiss();
                mService.startSession();
            }
        }

    }
    public void initService() {
        Log.d(TAG, "initService()");
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }



    private void showShortToast(String s) {
        CharSequence text = s;
        Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
