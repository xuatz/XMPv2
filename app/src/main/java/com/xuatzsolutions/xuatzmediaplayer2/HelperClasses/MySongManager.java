package com.xuatzsolutions.xuatzmediaplayer2.HelperClasses;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import com.xuatzsolutions.xuatzmediaplayer2.MainActivity;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Migration;
import com.xuatzsolutions.xuatzmediaplayer2.Models.Track;

import io.realm.Realm;

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

    public static void updateLibrary(Context context) {
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
        int dateAddedColumn 	= cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        int dataColumn 			= cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        int albumColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int albumIdColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        int albumKeyColumn 		= cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_KEY);

        int debugCount = 0;

        while (cursor.moveToNext()) {
            Track track = new Track();

            track.setId(cursor.getLong(idColumn));
            track.setDisplayName(cursor.getString(displayNameColumn));
            track.setTitle(cursor.getString(titleColumn));
            track.setTitleKey(cursor.getString(titleKeyColumn));
            track.setDuration(cursor.getInt(durationColumn));
            track.setTrackNo(cursor.getInt(trackColumn));
            track.setArtist(cursor.getString(artistColumn));
            track.setDateAdded(cursor.getString(dateAddedColumn));
            track.setPath(cursor.getString(dataColumn));

            track.setAlbum(cursor.getString(albumColumn));
            track.setAlbumId(cursor.getString(albumIdColumn));
            track.setAlbumKey(cursor.getString(albumKeyColumn));

            if (debugCount == 5) {
                Log.d(TAG, "MediaStore.Audio.Media._ID: " + track.getId());
                Log.d(TAG, "MediaStore.Audio.Media.DISPLAY_NAME: " + track.getDisplayName());
                Log.d(TAG, "MediaStore.Audio.Media.TITLE: " + track.getTitle());
                Log.d(TAG, "MediaStore.Audio.Media.TITLE_KEY: " + track.getTitleKey());
                Log.d(TAG, "MediaStore.Audio.Media.DATE_ADDED: " + track.getDateAdded());

                Log.d(TAG, "MediaStore.Audio.Media.ALBUM: " + track.getAlbum());
                Log.d(TAG, "MediaStore.Audio.Media.ALBUM_ID: " + track.getAlbumId());
                Log.d(TAG, "MediaStore.Audio.Media.ALBUM_KEY: " + track.getAlbumKey());
            }

            debugCount++;

            realm.beginTransaction();
            realm.copyToRealmOrUpdate(track);
            realm.commitTransaction();
        }

        Log.d(TAG, "no of cursor items: " + cursor.getCount());

        cursor.close();


        Log.d(TAG, "updateLibrary() end");
    }
}
