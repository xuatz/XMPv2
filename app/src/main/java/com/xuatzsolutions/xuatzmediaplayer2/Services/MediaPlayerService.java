package com.xuatzsolutions.xuatzmediaplayer2.Services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.xuatzsolutions.xuatzmediaplayer2.MainActivity;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Track;
import com.xuatzsolutions.xuatzmediaplayer2.Models.TrackStats;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by xuatz on 25/9/2015.
 */
public class MediaPlayerService extends Service {

    private final String TAG = "MediaPlayerService";

    private static final String INTENT_BASE_NAME = "com.xuatzsolutions.xuatzmediaplayer2.MediaPlayerService";
    public static final String INTENT_MP_READY = INTENT_BASE_NAME + ".MP_READY";

    String android_id = null;

    Realm realm = null;
    MediaPlayer mp = null;
    AudioManager am = null;

    AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = null;

    boolean audioFocus = false;
    private int originalVolume = 0;
    private boolean wasPlaying = false;

    RealmResults<Track> tracks = null;

    private Track currentTrack = null;
    private BroadcastReceiver mReceiver;
    private IntentFilter intentFilter;

    @Override
    public void onCreate() {
        super.onCreate();

        realm = Realm.getInstance(MediaPlayerService.this);
        tracks = realm.where(Track.class).findAll();

        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        audioFocus = true;

                        //==================
                        int sb2value = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                        Log.d(TAG, "maxVol: " + sb2value);
                        Log.d(TAG, "originalVol: " + originalVolume);

                        if (originalVolume<am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                            sb2value = originalVolume;
                        }
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, sb2value, 0);
                        //===================

                        if (wasPlaying) {
                            playPause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        audioFocus = false;
                        wasPlaying = false;

                        if(mp.isPlaying()) {
                            originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                            mp.stop();

                            am.abandonAudioFocus(mOnAudioFocusChangeListener);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        audioFocus = false;
                        if(mp.isPlaying()) {
                            originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                            playPause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if(mp.isPlaying()) {
                            originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*0.1), 0);
                        }
                        break;
                }
            }
        };

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mp = new MediaPlayer();
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                sendBroadcast(new Intent().setAction(INTENT_MP_READY));
                playPause();
            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "onCompletion is called! I want to monitor in the future to see if when i skip a song, will this method be played");

                TrackStats stats = new TrackStats(currentTrack.getTitle(), TrackStats.SONG_COMPLETED, android_id, Calendar.getInstance().getTime());

                realm.beginTransaction();
                realm.copyToRealm(stats);
                realm.commitTransaction();

                RealmResults<TrackStats> res =
                        realm.where(TrackStats.class)
                                //.equalTo("title", currentTrack.getTitle())
                                .findAll();

                Log.d(TAG, "Kaypoh:res.size(): " + res.size());
                for (TrackStats ts : res) {
                    Log.d(TAG, "Kaypoh:ts.getTitle(): " + ts.getTitle());
                    Log.d(TAG, "Kaypoh:ts.getType(): " + ts.getType());
                    Log.d(TAG, "Kaypoh:ts.getCreatedAt(): " + ts.getCreatedAt());
                }

                prepNextSong();
            }
        });

        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "intent.getAction(): " + intent.getAction());
                switch (intent.getAction()) {
                    case MainActivity.INTENT_PLAY_PAUSE:
                        playPause();
                        break;
                    case MainActivity.INTENT_NEXT:
                        prepNextSong();
                        break;
                }
            }
        };

        intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.INTENT_PLAY_PAUSE);
        intentFilter.addAction(MainActivity.INTENT_NEXT);

        registerReceiver(mReceiver, intentFilter);

        prepNextSong();
    }

    public boolean isPlaying() {
        if (mp == null) {
            return false;
        } else {
            return mp.isPlaying();
        }
    }

    public boolean prepNextSong() {
        mp.reset();

        if (tracks.size() > 0) {
            Random random = new Random();

            currentTrack = tracks.get(random.nextInt(tracks.size()));

            try {
                mp.setDataSource(currentTrack.getPath());
                mp.prepare();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public void playPause() {
        Log.d(TAG, "playPause()!");

        if (!mp.isPlaying()) {
            Log.d(TAG, "playPause() 1");
            if (!audioFocus) {
                // Request audio focus for playback
                int result = am.requestAudioFocus(mOnAudioFocusChangeListener,
                        // Use the music stream.
                        AudioManager.STREAM_MUSIC,
                        // Request permanent focus.
                        AudioManager.AUDIOFOCUS_GAIN);

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioFocus = true;
                }
            }

            if (audioFocus) {
                Log.d(TAG, "playPause() 2");
                wasPlaying = false;
                mp.start();
            }
            //sendBroadcast(new Intent(PLAY_MP));
            Log.d(TAG, "playPause() 3");
        } else {
            Log.d(TAG, "playPause() 4");
            wasPlaying = true;
            mp.pause();

            if (audioFocus) {
                am.abandonAudioFocus(mOnAudioFocusChangeListener);
                audioFocus = false;
            }

//            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//            SharedPreferences.Editor editor = pref.edit();
//
//            editor.putString(XMPSQLiteHelper.COLUMN_SESSION_TITLE, sessionTitle);
//            editor.putInt("trackIndex", trackIndex);
//            editor.putInt("trackPosition", mp.getCurrentPosition());
//            editor.commit();
            Log.d(TAG, "playPause() 5");
        }
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        //TODO try start sticky see how
//        return START_STICKY;
//    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        //am.unregisterMediaButtonEventReceiver(mRemoteControlResponder);

//        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        SharedPreferences.Editor editor = pref.edit();
//
//        editor.putString(XMPSQLiteHelper.COLUMN_SESSION_TITLE, sessionTitle);
//        editor.putInt("trackIndex", trackIndex);
//        editor.putInt("trackPosition", mp.getCurrentPosition());
//        editor.commit();

        if (audioFocus) {
            am.abandonAudioFocus(mOnAudioFocusChangeListener);
            audioFocus = false;
        }

//        stopService(dataCollectionServiceIntent);
//
//        unregisterReceiver(receiver);
//        cancelNotification();

        release();

        unregisterReceiver(mReceiver);
    }

    private void release() {
        if (mp == null) {
            return;
        } else {
            if (mp.isPlaying()) {
                mp.stop();
            }
            mp.reset();
            mp.release();
            mp = null;
        }
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(Track currentTrack) {
        this.currentTrack = currentTrack;
    }
}
