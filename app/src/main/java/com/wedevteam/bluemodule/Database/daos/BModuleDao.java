package com.wedevteam.bluemodule.Database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.wedevteam.bluemodule.Database.tables.BModule;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface BModuleDao {
    @Query("select * from bmodule_ts order by id desc")
    List<BModule> getAll();

    @Query("select * from BMODULE_TS order by id desc")
    LiveData<List<BModule>> getAllLiveData();

    @Query("select COUNT(*) from bmodule_ts")
    int getNumber();

    @Insert
    void insert(BModule tMain);

    @Update
    void update(BModule tMain);

    @Delete
    void delete(BModule tMain);

    @Query("delete from bmodule_ts")
    void deleteAll();
}
