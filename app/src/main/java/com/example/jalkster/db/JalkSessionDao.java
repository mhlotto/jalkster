package com.example.jalkster.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface JalkSessionDao {

    @Insert
    long insert(JalkSessionEntity session);

    @Delete
    int deleteSession(JalkSessionEntity session);

    @Query("SELECT * FROM jalk_sessions ORDER BY startTime DESC")
    List<JalkSessionEntity> getAllSessions();

    @Query("DELETE FROM jalk_sessions WHERE id IN (:ids)")
    int deleteByIds(List<Integer> ids);

    @Query("SELECT * FROM jalk_sessions ORDER BY startTime DESC LIMIT :limit")
    List<JalkSessionEntity> getRecentSessions(int limit);

    @Query("SELECT COUNT(*) FROM jalk_sessions")
    int getTotalSessions();

    @Query("SELECT SUM((walkRepTime * walkRepsDone + jogRepTime * jogRepsDone + restRepTime * restRepsDone) * 1000) FROM jalk_sessions")
    Long getTotalTimeMillis();

    @Query("SELECT MAX((walkRepTime * walkRepsDone + jogRepTime * jogRepsDone + restRepTime * restRepsDone) * 1000) FROM jalk_sessions")
    Long getLongestSessionMillis();

    @Query("SELECT SUM(jogRepTime * jogRepsDone * 1000) FROM jalk_sessions")
    Long getTotalJogMillis();

    @Query("SELECT SUM(walkRepTime * walkRepsDone * 1000) FROM jalk_sessions")
    Long getTotalWalkMillis();

    @Query("SELECT * FROM jalk_sessions WHERE id = :id LIMIT 1")
    JalkSessionEntity getSessionById(int id);

    @Update
    void updateSession(JalkSessionEntity session);

    @Query("SELECT COUNT(*) FROM jalk_sessions WHERE startTime >= :sinceMillis")
    int getTotalSessionsSince(long sinceMillis);

    @Query("SELECT SUM(jogRepTime * jogRepsDone * 1000) FROM jalk_sessions WHERE startTime >= :sinceMillis")
    Long getTotalJogMillisSince(long sinceMillis);

    @Query("SELECT SUM(walkRepTime * walkRepsDone * 1000) FROM jalk_sessions WHERE startTime >= :sinceMillis")
    Long getTotalWalkMillisSince(long sinceMillis);

    @Query("SELECT SUM(restRepTime * restRepsDone * 1000) FROM jalk_sessions WHERE startTime >= :sinceMillis")
    Long getTotalRestMillisSince(long sinceMillis);

    @Query("SELECT SUM(xcski_duration_ms) FROM jalk_sessions WHERE session_type = 'XCSKI' AND startTime >= :sinceMillis")
    Long getTotalXcSkiMillisSince(long sinceMillis);
}
