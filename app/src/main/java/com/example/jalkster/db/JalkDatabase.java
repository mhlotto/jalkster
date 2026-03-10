package com.example.jalkster.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {JalkSessionEntity.class}, version = 2, exportSchema = false)
public abstract class JalkDatabase extends RoomDatabase {

    private static volatile JalkDatabase INSTANCE;
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE jalk_sessions "
                    + "ADD COLUMN session_type TEXT NOT NULL DEFAULT 'JALK'");
            database.execSQL("ALTER TABLE jalk_sessions "
                    + "ADD COLUMN xcski_duration_ms INTEGER NOT NULL DEFAULT 0");
        }
    };

    public abstract JalkSessionDao jalkSessionDao();

    public static synchronized JalkDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                    JalkDatabase.class, "jalk_db")
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        return INSTANCE;
    }
}
