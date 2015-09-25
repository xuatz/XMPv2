package com.xuatzsolutions.xuatzmediaplayer2.Models;

import java.util.Date;

import io.realm.RealmObject;

/**
 * Created by xuatz on 25/9/2015.
 */
public class TrackStats extends RealmObject {

    public static final int SONG_COMPLETED = 1;
    public static final int SONG_SKIPPED = 2;
    public static final int SONG_SELECTED = 3;

    private String title;
    private int type;
    private String createdBy;
    private Date createdAt;

    public TrackStats() {

    }

    /**
     *
     * @param title
     * @param type either SONG_COMPLETED, SONG_SKIPPED, etc
     * @param createdBy
     * @param createdAt
     */
    public TrackStats(String title, int type, String createdBy, Date createdAt) {
        this.title = title;
        this.type = type;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
