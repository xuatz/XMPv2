package com.xuatzsolutions.xuatzmediaplayer2.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by xuatz on 25/9/2015.
 */
public class MediaPlayerService extends Service {


    private final String TAG = "MediaPlayerService";
    private static final String INTENT_BASE_NAME = "com.xuatzsolutions.xuatzmediaplayer2.MediaPlayerService";


    public static final String INTENT_MP_READY = INTENT_BASE_NAME + ".MP_READY";
    public static final String INTENT_SESSION_TRACKS_GENERATING = INTENT_BASE_NAME + ".SESSION_TRACKS_GENERATING";
    public static final String INTENT_SESSION_TRACKS_GENERATED = INTENT_BASE_NAME + ".SESSION_TRACKS_GENERATED";

    public static final String INTENT_DISMISS_NOTIFICATION = INTENT_BASE_NAME + ".DISMISS_NOTIFICATION";



    String android_id = null;

    Realm realm = null;
    MediaPlayer mp = null;
    AudioManager am = null;

    AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = null;

    boolean audioFocus = false;
    private boolean wasPlaying = false;

    RealmResults<Track> tracks = null;

    private Track currentTrack = null;
    private List<Track> currentPlaylist = null;

    private BroadcastReceiver mReceiver;
    private IntentFilter intentFilter;
    private long mStartTime = 0;
    private int accumulatedPlaytimeForThisTrack = 0;
    private int globalTrackNo;

    private static final double PASSING_GRADE = 49;

    //====================

    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private Notification mNotification;
    private static final int NOTIFICATION_ID = 1;

    private void initNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);

        // long when = System.currentTimeMillis();

        Intent notificationIntent = new Intent(this,MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT), PendingIntent.FLAG_UPDATE_CURRENT);

        Intent dismissIntent = new Intent(INTENT_DISMISS_NOTIFICATION);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, 0);

        Intent playPauseIntent = new Intent(MainActivity.INTENT_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, 0);

        Intent nextIntent = new Intent(MainActivity.INTENT_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);

        mNotificationBuilder = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentTitle("My notification")
                .setContentText("Hello World!").setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_media_play, "PlayPause", playPausePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
                .addAction(0, "Dismiss", dismissPendingIntent);

        mNotification = mNotificationBuilder.build();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;

        mNotificationManager.notify(NOTIFICATION_ID, mNotification);

    }

    @Override
    public void onCreate() {
        super.onCreate();

        currentPlaylist = new ArrayList<Track>();

        realm = Realm.getInstance(MediaPlayerService.this);
        tracks = realm.where(Track.class).findAll();

        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        audioFocus = true;

                        mp.setVolume(1f, 1f);

                        if (wasPlaying) {
                            playPause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        audioFocus = false;
                        wasPlaying = false;

                        if(mp.isPlaying()) {
                            mp.stop();
                            am.abandonAudioFocus(mOnAudioFocusChangeListener);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        audioFocus = false;
                        if(mp.isPlaying()) {
                            playPause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if(mp.isPlaying()) {
                            mp.setVolume(0.2f, 0.2f);
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

                mStartTime = System.currentTimeMillis();
                accumulatedPlaytimeForThisTrack = 0;

                playPause();
            }
        });

        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "onCompletion is called! I want to monitor in the future to see if when i skip a song, will this method be played");

                if (currentTrack != null) {
                    createTrackStats(currentTrack.getTitle(), TrackStats.SONG_COMPLETED);
                    prepNextSong();
                }
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
                        checkIfRegisterAsSkip();
                        prepNextSong();
                        break;
                    case MainActivity.INTENT_LIKED:
                        if (currentTrack != null) {
                            createTrackStats(currentTrack.getTitle(), TrackStats.SONG_LIKED);
                        }
                        break;
                    case MainActivity.INTENT_DISLIKED:
                        if (currentTrack != null) {
                            createTrackStats(currentTrack.getTitle(), TrackStats.SONG_DISLIKED);
                        }
                        prepNextSong();
                        break;
                    case INTENT_SESSION_TRACKS_GENERATED:
                        prepNextSong();
                        break;
                    case INTENT_DISMISS_NOTIFICATION:
                        stopMediaPlayerService();
                        break;
                }
            }
        };

        intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.INTENT_PLAY_PAUSE);
        intentFilter.addAction(MainActivity.INTENT_NEXT);
        intentFilter.addAction(MainActivity.INTENT_LIKED);
        intentFilter.addAction(MainActivity.INTENT_DISLIKED);
        intentFilter.addAction(INTENT_DISMISS_NOTIFICATION);

        //intentFilter.addAction(INTENT_SESSION_TRACKS_GENERATED);

        registerReceiver(mReceiver, intentFilter);

        startSession();
        initNotification();


    }

    private void stopMediaPlayerService() {
        this.stopService(new Intent(this, MediaPlayerService.class));
    }

    private List<Track> generateNewTracks() {
        Log.d(TAG, "generateNewTracks()");

        List<Track> tempList = new ArrayList<Track>();
        List<Track> tempTrackList = new ArrayList<Track>(realm.where(Track.class).findAll());

        Log.d(TAG, "tempTrackList.isEmpty(): " + tempTrackList.isEmpty());
        if (!tempTrackList.isEmpty()) {
            int count= 0;

            Collections.shuffle(tempTrackList);

            for (Track t : tempTrackList) {
//                try {
//                    Thread.sleep(20);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                Log.d(TAG, "generateNewTracks() iteration no.: " +count++ + ":mytest");
                Log.d(TAG, "t.getTitle(): " + t.getTitle());

                RealmResults<TrackStats> res =
                        this.realm.where(TrackStats.class)
                                .equalTo("createdBy", android_id)
                                .equalTo("title", t.getTitle())
                                .findAll();

                Log.d(TAG, "res.size(): " + res.size());

                //res.where().equalTo("type", TrackStats.SONG_SELECTED) //TODO not implemented yet
                int completedCount = res.where().equalTo("type", TrackStats.SONG_COMPLETED).findAll().size();
                int skippedCount = res.where().equalTo("type", TrackStats.SONG_SKIPPED).findAll().size();
                int likedCount = res.where().equalTo("type", TrackStats.SONG_LIKED).findAll().size();
                int dislikedCount = res.where().equalTo("type", TrackStats.SONG_DISLIKED).findAll().size();

                Log.d(TAG, "completedCount: " + completedCount);
                Log.d(TAG, "skippedCount: " + skippedCount);
                Log.d(TAG, "likedCount: " + likedCount);
                Log.d(TAG, "dislikedCount: " + dislikedCount);

                /*
                TODO need to make the ratio more representative of the respective counts
                i.e.    if i completed the song once, it should only give like 0.6
                        but if i completed the song 100 times, it should give like 0.9?
                 */
                int comMinusSkip = completedCount - skippedCount;
                double cmsRatio = 0;
                if (comMinusSkip > 0) {
                    cmsRatio = 0.9;
                } else {
                    if (comMinusSkip == 0) {
                        cmsRatio = 0.5;
                    } else {
                        cmsRatio = 0.1;
                    }
                }

                /*
                TODO similarly as above, this algo needs rework in future
                 */
                int likeMinusDislike = likedCount - dislikedCount;
                double lmdRatio = 0;
                if (likeMinusDislike > 0) {
                    lmdRatio = 0.9;
                } else {
                    if (likeMinusDislike == 0) {
                        lmdRatio = 0.5;
                    } else {
                        lmdRatio = 0.1;
                    }
                }

                //XZ: it should add up to 100 in total
                final double chanceContributionWeightage        = 50;
                final double completedVsSkippedContributionWeightage     = 20;
                final double likeDislikeContributionWeightage   = 30;

                double chanceContribution               = chanceContributionWeightage * Math.random();
                double completedVsSkippedContribution   = completedVsSkippedContributionWeightage * cmsRatio;
                double likeDislikeContribution          = likeDislikeContributionWeightage * lmdRatio;

                //double selectedContribution    = 20; //TODO *WIP*
                //double freshnessContribution    = 20; //TODO *WIP*

                double points = chanceContribution + completedVsSkippedContribution + likeDislikeContribution;


                if (points > PASSING_GRADE) {
                    tempList.add(t);
                }

                if (tempList.size() == 10) {
                    break;
                }
            }
        }

        Log.d(TAG, "tempList.size(): " + tempList.size());
        return tempList;
    }



    public void startSession() {
        Log.d(TAG, "startSession()");

        //TODO might wanna consider to fetch fresh tracks in future

        currentPlaylist.clear();
        globalTrackNo = 0;

        /*
        TODO it shld be an async task in future
        refer to https://github.com/realm/realm-java/issues/1208
         */
        currentPlaylist.addAll(generateNewTracks());

        //sendBroadcast(new Intent(INTENT_SESSION_TRACKS_GENERATING));
        //sendBroadcast(new Intent().setAction(INTENT_SESSION_TRACKS_GENERATED));
        prepNextSong();
    }

    private void checkIfRegisterAsSkip() {
        if (currentTrack != null) {
            long millis = System.currentTimeMillis() - mStartTime;
            accumulatedPlaytimeForThisTrack += millis;

            if (accumulatedPlaytimeForThisTrack > currentTrack.getDuration()/2) {
                //dun consider as skip, just "next"
            } else {
                //its a skip, the guy dun like this song1

                createTrackStats(currentTrack.getTitle(), TrackStats.SONG_SKIPPED);
            }
        }
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

        if (currentPlaylist.size() > 0) {
            currentTrack = currentPlaylist.get(globalTrackNo++);

            try {
                mp.setDataSource(currentTrack.getPath());
                mp.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            int i = Math.max(currentPlaylist.size() - 5, globalTrackNo);

            if (currentPlaylist.size() - 5 <= globalTrackNo) {
                currentPlaylist.addAll(generateNewTracks());
            }
            return true;
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

                mStartTime = System.currentTimeMillis();
            }
            Log.d(TAG, "playPause() 3");
        } else {
            Log.d(TAG, "playPause() 4");
            wasPlaying = true;
            mp.pause();

            long millis = System.currentTimeMillis() - mStartTime;
            accumulatedPlaytimeForThisTrack += millis;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        //cancelNotification();
        mNotificationManager.cancel(NOTIFICATION_ID);
        unregisterReceiver(mReceiver);

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

        release();
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

    private void createTrackStats(String trackTitle, int type) {
        TrackStats stats = new TrackStats(trackTitle, type, android_id, Calendar.getInstance().getTime());

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
