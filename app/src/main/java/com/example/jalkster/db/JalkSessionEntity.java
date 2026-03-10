package com.example.jalkster.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "jalk_sessions")
public class JalkSessionEntity {

    public static final String SESSION_TYPE_JALK = "JALK";
    public static final String SESSION_TYPE_XCSKI = "XCSKI";

    @PrimaryKey(autoGenerate = true)
    private int id;
    private long startTime;
    private long endTime;
    private int walkRepTime;
    private int jogRepTime;
    private int restRepTime;
    private int walkRepsDone;
    private int jogRepsDone;
    private int restRepsDone;
    @NonNull
    @ColumnInfo(name = "session_type", defaultValue = SESSION_TYPE_JALK)
    private String sessionType;
    @ColumnInfo(name = "xcski_duration_ms", defaultValue = "0")
    private long xcskiDurationMs;

    public JalkSessionEntity(long startTime,
                             long endTime,
                             int walkRepTime,
                             int jogRepTime,
                             int restRepTime,
                             int walkRepsDone,
                             int jogRepsDone,
                             int restRepsDone) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.walkRepTime = walkRepTime;
        this.jogRepTime = jogRepTime;
        this.restRepTime = restRepTime;
        this.walkRepsDone = walkRepsDone;
        this.jogRepsDone = jogRepsDone;
        this.restRepsDone = restRepsDone;
        this.sessionType = SESSION_TYPE_JALK;
        this.xcskiDurationMs = 0L;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getWalkRepTime() {
        return walkRepTime;
    }

    public void setWalkRepTime(int walkRepTime) {
        this.walkRepTime = walkRepTime;
    }

    public int getJogRepTime() {
        return jogRepTime;
    }

    public void setJogRepTime(int jogRepTime) {
        this.jogRepTime = jogRepTime;
    }

    public int getRestRepTime() {
        return restRepTime;
    }

    public void setRestRepTime(int restRepTime) {
        this.restRepTime = restRepTime;
    }

    public int getWalkRepsDone() {
        return walkRepsDone;
    }

    public void setWalkRepsDone(int walkRepsDone) {
        this.walkRepsDone = walkRepsDone;
    }

    public int getJogRepsDone() {
        return jogRepsDone;
    }

    public void setJogRepsDone(int jogRepsDone) {
        this.jogRepsDone = jogRepsDone;
    }

    public int getRestRepsDone() {
        return restRepsDone;
    }

    public void setRestRepsDone(int restRepsDone) {
        this.restRepsDone = restRepsDone;
    }

    @NonNull
    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(@NonNull String sessionType) {
        this.sessionType = sessionType;
    }

    public long getXcskiDurationMs() {
        return xcskiDurationMs;
    }

    public void setXcskiDurationMs(long xcskiDurationMs) {
        this.xcskiDurationMs = xcskiDurationMs;
    }

    @Override
    public String toString() {
        return "JalkSession{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", walkReps=" + walkRepsDone +
                ", jogReps=" + jogRepsDone +
                ", restReps=" + restRepsDone +
                ", sessionType=" + sessionType +
                ", xcskiDurationMs=" + xcskiDurationMs +
                '}';
    }
}
