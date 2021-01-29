package com.wedevteam.bluemodule.Database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.wedevteam.bluemodule.Database.daos.BModuleDao;
import com.wedevteam.bluemodule.Database.tables.BModule;

@Database(entities = {BModule.class},version = 1,exportSchema = false)
public abstract class _Database extends RoomDatabase {
    private static final String DB_NAME = "bmod_db";

    private static _Database instance;

    // Sync db
    public static synchronized _Database getInstance(Context context) {
        if(instance==null){
            // Genera db
            instance= Room.databaseBuilder(context.getApplicationContext(),_Database.class,DB_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build();
        }
        return instance;
    }

    // interfaces
    public abstract BModuleDao bModuleDao();
    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }
    };

}
