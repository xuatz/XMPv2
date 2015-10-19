package com.xuatzsolutions.xuatzmediaplayer2.HelperClasses;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import com.xuatzsolutions.xuatzmediaplayer2.MainActivity;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Migration;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Track;
import com.xuatzsolutions.xuatzmediaplayer2.Models.TrackStats;

import java.util.Calendar;

import hirondelle.date4j.DateTime;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by xuatz on 24/9/2015.
 */
public class MySongManager {
    private static final String TAG = "MySongManager";

    private final static String[] TRACK_COLUMNS = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TITLE_KEY,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM_KEY
    };

    public static void updateLibrary(Context context, boolean rebuildStats) {
        Log.d(TAG, "updateLibrary() start");

        Realm realm = Realm.getInstance(Migration.getConfig(context));

        //songs = new HashMap<String, Track>();

        String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";
        String[] selectionArgs = null;
        String orderBy = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        ContentResolver contentResolver = context.getContentResolver();

        //MediaStore.Audio.Media.INTERNAL_CONTENT_URI
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS,
                where, selectionArgs, orderBy);

        int idColumn 			= cursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int displayNameColumn 	= cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
        int titleColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int titleKeyColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY);
        int durationColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int trackColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int artistColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        //int dateAddedColumn 	= cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int dataColumn 			= cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int albumColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int albumKeyColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);

        realm.beginTransaction();

        while (cursor.moveToNext()) {
            Track track;

            String local_id = Track.getLocalId(cursor.getString(titleColumn), cursor.getString(artistColumn), cursor.getString(albumColumn));

            Track res2 = realm.where(Track.class)
                    .equalTo("local_id", local_id)
                    .findFirst();

            if (res2 != null) {
                track = res2;

                if (rebuildStats) {
                    RealmResults<TrackStats> statsRes =
                            realm.where(TrackStats.class)
                                    .equalTo("title", track.getTitle())
                                    .findAll();

                    if (!statsRes.isEmpty()) {
//                        Log.d(TAG, "hi5");
//                        Log.d(TAG, "==================");
//                        Log.d(TAG, "track.getTitle(): " + track.getTitle());
//                        Log.d(TAG, "track.getCompletedCount(): " + track.getCompletedCount());
//                        Log.d(TAG, "track.getSkippedCount(): " + track.getSkippedCount());
//                        Log.d(TAG, "track.getLikedCount(): " + track.getLikedCount());
//                        Log.d(TAG, "track.getDislikedCount(): " + track.getDislikedCount());

                        //res.where().equalTo("type", TrackStats.SONG_SELECTED) //TODO not implemented yet
                        int completedCount = statsRes.where().equalTo("type", TrackStats.SONG_COMPLETED).findAll().size();
                        int skippedCount = statsRes.where().equalTo("type", TrackStats.SONG_SKIPPED).findAll().size();
                        int likedCount = statsRes.where().equalTo("type", TrackStats.SONG_LIKED).findAll().size();
                        int dislikedCount = statsRes.where().equalTo("type", TrackStats.SONG_DISLIKED).findAll().size();

                        track.setCompletedCount(completedCount);
                        track.setSkippedCount(skippedCount);
                        track.setLikedCount(likedCount);
                        track.setDislikedCount(dislikedCount);
                        track.setStatsUpdatedAt(DateTime.now(Calendar.getInstance().getTimeZone()).toString());
                    }
                }
            } else {
//                Log.d(TAG, "hi6 - means its a new record");
//                Log.d(TAG, "cursor.getString(titleColumn): " + cursor.getString(titleColumn));
//                Log.d(TAG, "cursor.getString(artistColumn): " + cursor.getString(artistColumn));
//                Log.d(TAG, "cursor.getString(albumColumn): " + cursor.getString(albumColumn));

                track = new Track(
                        cursor.getString(titleColumn),
                        cursor.getString(artistColumn),
                        cursor.getString(albumColumn),
                        cursor.getString(dataColumn)
                );

                track.setId(cursor.getLong(idColumn));
                track.setDisplayName(cursor.getString(displayNameColumn));
                track.setTitleKey(cursor.getString(titleKeyColumn));
                track.setDuration(cursor.getInt(durationColumn));
                track.setTrackNo(cursor.getInt(trackColumn));
                track.setAlbumId(cursor.getString(albumIdColumn));
                track.setAlbumKey(cursor.getString(albumKeyColumn));

                track = realm.copyToRealm(track);
            }

            track.setPath(cursor.getString(dataColumn));
        }
        realm.commitTransaction();

        cursor.close();
        Log.d(TAG, "updateLibrary() end");
    }
}
