package com.xuatzsolutions.xuatzmediaplayer2.Models;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.internal.ColumnType;

/**
 * Created by xuatz on 24/9/2015.
 */
public class Track extends RealmObject {

    //    @Ignore
    //    private int             sessionId;

    @PrimaryKey
    private String title;

    private long id;
    private String displayName;
    private String titleKey;
    private String artist;

    private int duration;
    private int trackNo;
    private String dateAdded;
    private String path;

    private String album;
    private String albumId;

    //==============
    //v1-start

    private int completedCount;
    private int skippedCount;
    private int selectedCount;
    private int likedCount;
    private int dislikedCount;

    private String statsUpdatedAt;

    public String getStatsUpdatedAt() {
        return statsUpdatedAt;
    }

    public void setStatsUpdatedAt(String statsUpdatedAt) {
        this.statsUpdatedAt = statsUpdatedAt;
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

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbumKey() {
        return albumKey;
    }

    public void setAlbumKey(String albumKey) {
        this.albumKey = albumKey;
    }

    private String albumKey;

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}