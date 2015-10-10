package com.xuatzsolutions.xuatzmediaplayer2.Models;

import java.util.Calendar;

import hirondelle.date4j.DateTime;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by xuatz on 24/9/2015.
 */
public class Track extends RealmObject {

    @PrimaryKey
    private String local_id;

    private String title;
    private String artist;
    private String album;

    private int duration;
    private int trackNo;

    private long id;
    private String displayName;
    private String titleKey;
    private String path;
    private String albumId;

    private String albumKey;

    private int completedCount = 0;
    private int skippedCount = 0;
    private int selectedCount = 0;
    private int likedCount = 0;
    private int dislikedCount = 0;

    private String statsUpdatedAt = DateTime.now(Calendar.getInstance().getTimeZone()).toString();
    private String dateAdded = DateTime.now(Calendar.getInstance().getTimeZone()).toString();

    //============

    public Track() {
    }

    public Track(String title, String artist, String album, String path) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;

        this.local_id = getLocalId(title, artist, album);
    }

    public static String getLocalId(String title, String artist, String album) {
        StringBuilder sb = new StringBuilder();

        if (title == null) {
            sb.append("null");
        } else {
            sb.append(title);
        }

        if (artist == null) {
            sb.append("null");
        } else {
            sb.append(artist);
        }

        if (album == null) {
            sb.append("null");
        } else {
            sb.append(album);
        }

        return sb.toString();
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getLocal_id() {
        return local_id;
    }

    public void setLocal_id(String local_id) {
        this.local_id = local_id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getTrackNo() {
        return trackNo;
    }

    public void setTrackNo(int trackNo) {
        this.trackNo = trackNo;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public void setSelectedCount(int selectedCount) {
        this.selectedCount = selectedCount;
    }

    public int getLikedCount() {
        return likedCount;
    }

    public void setLikedCount(int likedCount) {
        this.likedCount = likedCount;
    }

    public int getDislikedCount() {
        return dislikedCount;
    }

    public void setDislikedCount(int dislikedCount) {
        this.dislikedCount = dislikedCount;
    }

    public String getStatsUpdatedAt() {
        return statsUpdatedAt;
    }

    public void setStatsUpdatedAt(String statsUpdatedAt) {
        this.statsUpdatedAt = statsUpdatedAt;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getAlbumKey() {
        return albumKey;
    }

    public void setAlbumKey(String albumKey) {
        this.albumKey = albumKey;
    }
}
