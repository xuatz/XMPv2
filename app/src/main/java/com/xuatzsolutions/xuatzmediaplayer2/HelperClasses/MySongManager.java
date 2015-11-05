package com.xuatzsolutions.xuatzmediaplayer2.HelperClasses;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.provider.MediaStore;
import android.util.Log;

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

        realm.beginTransaction();
        RealmResults<Track> res = realm.where(Track.class).findAll();
        for (int x = 0; x < res.size(); x++) {
            res.get(0).setIsAvailable(false);
        }
        realm.commitTransaction();

        String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";
        String[] selectionArgs = null;
        String orderBy = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        ContentResolver contentResolver = context.getContentResolver();

        Cursor[] cursors = new Cursor[2];
        cursors[0] = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, TRACK_COLUMNS,
                where, selectionArgs, orderBy);

        cursors[1] = contentResolver.query(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, TRACK_COLUMNS,
                where, selectionArgs, orderBy);

        Cursor cursor = new MergeCursor(cursors);

        int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
        int displayNameColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int titleKeyColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY);
        int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        //int dateAddedColumn 	= cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int albumKeyColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);

        realm.beginTransaction();

        String dateTimeNowToString = DateTime.now(Calendar.getInstance().getTimeZone()).toString();

        while (cursor.moveToNext()) {
            Log.d(TAG, "duration: " + cursor.getInt(durationColumn));
            int minimumDurationToConsiderAsSong = 60000;

            Track track = null;

            String local_id = Track.getLocalId(cursor.getString(titleColumn), cursor.getString(artistColumn), cursor.getString(albumColumn));

            Track res2 = realm.where(Track.class)
                    .equalTo("local_id", local_id)
                    .findFirst();

            if (res2 != null) {
                if (cursor.getInt(durationColumn) < minimumDurationToConsiderAsSong) {
                    res2.removeFromRealm();
                } else {
                    track = res2;
                    track.setIsAvailable(true);

                    if (!track.isHidden()) {
                        if (rebuildStats) {
                            rebuildStats(context, track.getLocal_id());
                        }
                    }
                }
            } else {
                if (cursor.getInt(durationColumn) < minimumDurationToConsiderAsSong) {
                    //do nothing, skip next song
                } else {
//                    Log.d(TAG, "hi6 - means its a new record");
//                    Log.d(TAG, "cursor.getString(titleColumn): " + cursor.getString(titleColumn));
//                    Log.d(TAG, "cursor.getString(artistColumn): " + cursor.getString(artistColumn));
//                    Log.d(TAG, "cursor.getString(albumColumn): " + cursor.getString(albumColumn));

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
            }

            if (track != null) {
                track.setPath(cursor.getString(dataColumn));
            }
        }
        realm.commitTransaction();

        cursor.close();
        Log.d(TAG, "updateLibrary() end");
    }

    public static void rebuildStats(Context context, String localId) {
        Realm realm = Realm.getInstance(Migration.getConfig(context));

        RealmResults<TrackStats> statsRes = realm.where(TrackStats.class).equalTo("local_id", localId).findAll();

        if (!statsRes.isEmpty()) {
            /*
            Log.d(TAG, "hi5");
            Log.d(TAG, "==================");
            Log.d(TAG, "track.getTitle(): " + track.getTitle());
            Log.d(TAG, "track.getCompletedCount(): " + track.getCompletedCount());
            Log.d(TAG, "track.getSkippedCount(): " + track.getSkippedCount());
            Log.d(TAG, "track.getLikedCount(): " + track.getLikedCount());
            Log.d(TAG, "track.getDislikedCount(): " + track.getDislikedCount());

            res.where().equalTo("type", TrackStats.SONG_SELECTED) //TODO not implemented yet
             */

            int completedCount = 0;
            int skippedCount = 0;
            int likedCount = 0;
            int dislikedCount = 0;
            int halfPlayedCount = 0;
            int selectedCount = 0;

            for (TrackStats ts : statsRes) {
                switch (ts.getType()) {
                    case TrackStats.SONG_COMPLETED:
                        completedCount++;
                        break;
                    case TrackStats.SONG_HALF_PLAYED:
                        halfPlayedCount++;
                        break;
                    case TrackStats.SONG_SELECTED:
                        selectedCount++;
                        break;
                    case TrackStats.SONG_SKIPPED:
                        skippedCount++;
                        break;
                    case TrackStats.SONG_LIKED:
                        likedCount++;
                        break;
                    case TrackStats.SONG_DISLIKED:
                        dislikedCount++;
                        break;

                }
            }

            Track track = realm.where(Track.class).equalTo("local_id", localId).findFirst();

            realm.beginTransaction();

            track.setCompletedCount(completedCount);
            track.setHalfPlayedCount(halfPlayedCount);
            track.setSelectedCount(selectedCount);

            track.setSkippedCount(skippedCount);

            track.setLikedCount(likedCount);
            track.setDislikedCount(dislikedCount);

            track.setStatsUpdatedAt(DateTime.now(Calendar.getInstance().getTimeZone()).toString());

            realm.commitTransaction();
        }
    }
}
