package com.xuatzsolutions.xuatzmediaplayer2.Models;

import java.util.Calendar;
import java.util.Date;

import hirondelle.date4j.DateTime;
import io.realm.RealmObject;

/**
 * Created by xuatz on 25/9/2015.
 */
public class TrackStats extends RealmObject {

    public static final int SONG_COMPLETED = 1;
    public static final int SONG_SKIPPED = 2;
    public static final int SONG_SELECTED = 3;
    public static final int SONG_LIKED = 4;
    public static final int SONG_DISLIKED = 5;
    public static final int SONG_HALF_PLAYED = 6;

    private String local_id;

    private int type;
    private String createdBy;
    private String createdAt = DateTime.now(Calendar.getInstance().getTimeZone()).toString();

    public TrackStats() {

    }

    /**
     *
     * @param local_id
     * @param type either SONG_COMPLETED, SONG_SKIPPED, etc
     * @param createdBy
     */
    public TrackStats(String local_id, int type, String createdBy) {
        this.local_id = local_id;
        this.type = type;
        this.createdBy = createdBy;
    }

    public String getLocal_id() {
        return local_id;
    }

    public void setLocal_id(String local_id) {
        this.local_id = local_id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
