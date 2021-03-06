package com.android.imagerecordapp.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GridViewData::class], version = 1)
abstract class GridViewDatabase : RoomDatabase() {
    abstract fun gridViewDao() : GridViewDao
}