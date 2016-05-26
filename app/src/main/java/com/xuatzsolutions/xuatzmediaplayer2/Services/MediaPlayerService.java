package com.xuatzsolutions.xuatzmediaplayer2.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.xuatzsolutions.xuatzmediaplayer2.HelperClasses.MySongManager;
import com.xuatzsolutions.xuatzmediaplayer2.MainActivity;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Migration;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Track;
import com.xuatzsolutions.xuatzmediaplayer2.Models.TrackStats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import hirondelle.date4j.DateTime;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;

/**
 * Created by xuatz on 25/9/2015.
 */
public class MediaPlayerService extends Service {

    public static final int SESSION_TYPE_GENERAL = 1;
    public static final int SESSION_TYPE_NEW = 2;
    public static final int SESSION_TYPE_UNDERRATED = 3;
    public static final int SESSION_TYPE_TRASH = 4;

    private final String TAG = "MediaPlayerService";
    private static final String INTENT_BASE_NAME = "com.xuatzsolutions.xuatzmediaplayer2.MediaPlayerService";


    public static final String INTENT_MP_READY = INTENT_BASE_NAME + ".MP_READY";
    public static final String INTENT_SESSION_TRACKS_GENERATING = INTENT_BASE_NAME + ".SESSION_TRACKS_GENERATING";
    public static final String INTENT_SESSION_TRACKS_GENERATED = INTENT_BASE_NAME + ".SESSION_TRACKS_GENERATED";

    public static final String INTENT_DISMISS_NOTIFICATION = INTENT_BASE_NAME + ".DISMISS_NOTIFICATION";

    public static final String INTENT_NOT_ENOUGH_DATA_FOR_NON_GENERAL_SESSION = INTENT_BASE_NAME + ".NOT_ENOUGH_DATA";
    public static final String INTENT_THERE_IS_NO_SONGS = INTENT_BASE_NAME + ".NO_SONGS";

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


    private int sessionType = 1;

    private static final double PASSING_GRADE = 49;

    //====================

    private NotificationManager mNotificationManager;
    private Notification.Builder mNotificationBuilder;
    private Notification mNotification;
    private static final int NOTIFICATION_ID = 1;

    private PendingIntent activityIntent;
    private PendingIntent dismissPendingIntent;
    private PendingIntent playPausePendingIntent;
    private PendingIntent nextPendingIntent;

    private void initNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        mNotificationManager = (NotificationManager) getSystemService(ns);

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        activityIntent = PendingIntent.getActivity(this, 0, notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);

        Intent dismissIntent = new Intent(INTENT_DISMISS_NOTIFICATION);
        dismissPendingIntent = PendingIntent.getBroadcast(this, 0, dismissIntent, 0);

        Intent playPauseIntent = new Intent(MainActivity.INTENT_PLAY_PAUSE);
        playPausePendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, 0);

        Intent nextIntent = new Intent(MainActivity.INTENT_NEXT);
        nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
    }

    public void showNotification() {
        if (currentTrack != null) {
            mNotificationBuilder = new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.sym_def_app_icon)
                    .setContentTitle(currentTrack.getTitle())
                    .setContentText(currentTrack.getArtist())
                    .setContentIntent(activityIntent)
                    .addAction(android.R.drawable.ic_media_play, "PlayPause", playPausePendingIntent)
                    .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
                    .addAction(0, "Dismiss", dismissPendingIntent);

            mNotification = mNotificationBuilder.build();
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;

            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    public void cancelNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        currentPlaylist = new ArrayList<Track>();

        realm = Realm.getInstance(Migration.getConfig(this));
        tracks = realm.where(Track.class)
                .equalTo("isHidden", false)
                .equalTo("isAvailable", true)
                .findAll();

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

                        if (mp.isPlaying()) {
                            mp.stop();
                            am.abandonAudioFocus(mOnAudioFocusChangeListener);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        audioFocus = false;
                        if (mp.isPlaying()) {
                            playPause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if (mp.isPlaying()) {
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

                Log.d(TAG, "mp.onPrepared!");

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
                    createTrackStats(currentTrack.getLocal_id(), TrackStats.SONG_COMPLETED);
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
                            createTrackStats(currentTrack.getLocal_id(), TrackStats.SONG_LIKED);
                        }
                        break;
                    case MainActivity.INTENT_DISLIKED:
                        if (currentTrack != null) {
                            createTrackStats(currentTrack.getLocal_id(), TrackStats.SONG_DISLIKED);
                        }
                        prepNextSong();
                        break;
                    case INTENT_SESSION_TRACKS_GENERATED:
                        prepNextSong();
                        break;
                    case INTENT_DISMISS_NOTIFICATION:
                        stopMediaPlayerService();
                        break;
                    case INTENT_THERE_IS_NO_SONGS:
                        stopMediaPlayerService();
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        pause();
                        break;
                    case android.content.Intent.ACTION_HEADSET_PLUG:
                        int state = intent.getIntExtra("state", 4);
                        if(state == 0){
                            //unplugged
                            pause();
                        } else if(state == 1){
                            //plugged in
                            if (currentTrack != null) {
                                play();
                            }
                        } else {
                            //others??
                        }
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
        intentFilter.addAction(INTENT_THERE_IS_NO_SONGS);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(android.content.Intent.ACTION_HEADSET_PLUG);
        //intentFilter.addAction(INTENT_SESSION_TRACKS_GENERATED);

        registerReceiver(mReceiver, intentFilter);

        initNotification();
    }

    private void stopMediaPlayerService() {
        this.stopService(new Intent(this, MediaPlayerService.class));
    }

    private List<Track> generateNewTracks(int sessionType) {
        Log.d(TAG, "generateNewTracks()");

        List<Track> tempList = new ArrayList<Track>();
        List<Track> tempTrackList = new ArrayList<Track>(tracks);

        Log.d(TAG, "tempTrackList.isEmpty(): " + tempTrackList.isEmpty());
        if (!tempTrackList.isEmpty()) {
            switch (sessionType) {
                case SESSION_TYPE_NEW:
                    tempList = getNewPlaylist(tempTrackList);
                    break;
                case SESSION_TYPE_UNDERRATED:
                    tempList = getUnderratedPlaylist(tempTrackList);
                    break;
                case SESSION_TYPE_TRASH:
                    tempList = getTrashPlaylist(tempTrackList);
                    break;
                case SESSION_TYPE_GENERAL:
                    //fall thru
                default:
                    tempList = getGeneralPlaylist(tempTrackList);
                    break;

            }

            if (sessionType != SESSION_TYPE_GENERAL) {
                if (tempList.isEmpty()) {
                    sendBroadcast(new Intent().setAction(INTENT_NOT_ENOUGH_DATA_FOR_NON_GENERAL_SESSION));
                    this.sessionType = SESSION_TYPE_GENERAL;
                    tempList = getGeneralPlaylist(tempTrackList);
                }
            }
        }

        Log.d(TAG, "tempList.size(): " + tempList.size());

        if (tempList.isEmpty()) {
            sendBroadcast(new Intent().setAction(INTENT_THERE_IS_NO_SONGS));
        }


        return tempList;
    }

    private List<Track> getTrashPlaylist(List<Track> tempTrackList) {
        //TODO
        return null;
    }

    private List<Track> getNewPlaylist(List<Track> tempTrackList) {
        //TODO
        return null;
    }

    private List<Track> getUnderratedPlaylist(List<Track> tempTrackList) {
        Log.d(TAG, "getUnderratedPlaylist()");
        List<Track> tempList = new ArrayList<Track>();

        /**
         * referring to how many times does a song need to be
         * played/selected before it is not a rookie
         * TODO XZ: in future, this should be a relative number
         * such as top played song is at 100 completeCount
         * hence an underrated song would be, say maxCount * 0.4?
         */
        int rookieThreshold = 10;

        if (!tempTrackList.isEmpty()) {
            Collections.shuffle(tempTrackList);

            for (Track t : tempTrackList) {

                int completedCount = t.getCompletedCount();
                int skippedCount = t.getSkippedCount();
                int selectedCount = t.getSelectedCount();
                int likedCount = t.getLikedCount();
                int dislikedCount = t.getDislikedCount();

                if (completedCount + selectedCount < rookieThreshold) {
                    if (selectedCount > 2) {
                        if (skippedCount <= completedCount) {
                            tempList.add(t);
                        }
                    } else {
                        if (completedCount > 2) {
                            tempList.add(t);
                        } else {
                            if (likedCount > dislikedCount) {
                                tempList.add(t);
                            }
                        }
                    }
                }

                if (tempList.size() == 10) {
                    break;
                }
            }
        }

        Log.d(TAG, "tempList.size():" + tempList.size());

        return tempList;
    }

    private List<Track> getGeneralPlaylist(List<Track> tempTrackList) {
        List<Track> tempList = new ArrayList<Track>();

        if (tempTrackList != null) {
            Collections.sort(tempTrackList, new Comparator<Track>() {
                @Override
                public int compare(Track left, Track right) {
                    return (left.getCompletedCount() + left.getSkippedCount()) - (right.getCompletedCount() + right.getSkippedCount());
                }
            });
            Iterator<Track> iter = tempTrackList.iterator();

            while (iter.hasNext()) {
                Track newTrack = iter.next();
                if (newTrack.getSkippedCount() + newTrack.getCompletedCount() > 3) {
                    break;
                }

                if (Math.random() > 0.2) {
                    tempList.add(newTrack);
                    iter.remove();

                    if (tempTrackList.size() > 3) {
                        break;
                    }
                }
            }

            Collections.shuffle(tempTrackList);

            for (Track t : tempTrackList) {
                // Log.d(TAG, "t.getTitle(): " + t.getTitle());

                int completedCount = t.getCompletedCount();
                int skippedCount = t.getSkippedCount();
                //res.where().equalTo("type", TrackStats.SONG_SELECTED) //TODO not implemented yet
                int likedCount = t.getLikedCount();
                int dislikedCount = t.getDislikedCount();

                /**
                 * TODO need to make the ratio more representative of the respective counts
                 * i.e. if i completed the song once, it should only give like 0.6
                 * but if i completed the song 100 times, it should give like 0.9?
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

                //TODO similarly as above, this algo needs rework in future
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
                final double chanceContributionWeightage = 50;
                final double completedVsSkippedContributionWeightage = 20;
                final double likeDislikeContributionWeightage = 30;

                double chanceContribution = chanceContributionWeightage * Math.random();
                double completedVsSkippedContribution = completedVsSkippedContributionWeightage * cmsRatio;
                double likeDislikeContribution = likeDislikeContributionWeightage * lmdRatio;

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
        return tempList;
    }


    public void startSession(int sessionType) {
        Log.d(TAG, "startSession()");
        this.sessionType = sessionType;

        List<Track> newTracks = generateNewTracks(sessionType);

        currentPlaylist.clear();
        globalTrackNo = 0;
        currentPlaylist.addAll(newTracks);

        prepNextSong();
        showNotification();
    }

    private void checkIfRegisterAsSkip() {
        if (currentTrack != null) {
            long millis = System.currentTimeMillis() - mStartTime;
            accumulatedPlaytimeForThisTrack += millis;

            if (accumulatedPlaytimeForThisTrack > currentTrack.getDuration() / 2) {
                //dun consider as skip, just "next"
                createTrackStats(currentTrack.getLocal_id(), TrackStats.SONG_HALF_PLAYED);
            } else {
                //its a skip, the guy dun like this song1
                createTrackStats(currentTrack.getLocal_id(), TrackStats.SONG_SKIPPED);
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

            MySongManager.rebuildStats(this, currentTrack.getLocal_id());

            try {
                mp.setDataSource(currentTrack.getPath());
                mp.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            int i = Math.max(currentPlaylist.size() - 5, globalTrackNo);

            if (currentPlaylist.size() - 5 <= globalTrackNo) {
                //TODO async
                currentPlaylist.addAll(generateNewTracks(sessionType));
            }
            return true;
        } else {
            return false;
        }
    }


    public void playPause() {
        if (!mp.isPlaying()) {
            play();
        } else {
            pause();
        }
    }

    /**
     * already did null checks for mp
     */
    public void pause() {
        if (mp == null) {
            Log.e(TAG, "mp is null");
        } else {
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
        }
    }

    public void play() {
        if (mp == null) {
            Log.e(TAG, "mp is null");
        } else {
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
                wasPlaying = false;

                if (currentTrack == null) {
                    Log.e(TAG, "there is no songs loaded");

                    prepNextSong();
                } else {
                    mp.start();
                    mStartTime = System.currentTimeMillis();
                }
            }
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

        cancelNotification();
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

    private void createTrackStats(String local_id, int type) {
        TrackStats stats = new TrackStats(local_id, type, android_id);

        realm.beginTransaction();
        realm.copyToRealm(stats);
        realm.commitTransaction();
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

    public int getSessionType() {
        return sessionType;
    }

    public String getSessionTypeString() {
        switch (sessionType) {
            default:
            case SESSION_TYPE_GENERAL:
                return "General Playlist";
            case SESSION_TYPE_NEW:
                return "New Songs";
            case SESSION_TYPE_UNDERRATED:
                return "Underrated Songs";
            case SESSION_TYPE_TRASH:
                return "Delete Candidates";
        }
    }

    public void setSessionType(int sessionType) {
        this.sessionType = sessionType;
    }
}
