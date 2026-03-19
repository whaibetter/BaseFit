package com.basefit.app.data.database

import android.content.Context
import androidx.room.*
import com.basefit.app.data.dao.*
import com.basefit.app.data.entity.*

@Database(
    entities = [
        Exercise::class,
        WeekPlan::class,
        ChallengePlan::class,
        CheckIn::class,
        ExerciseResource::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun weekPlanDao(): WeekPlanDao
    abstract fun challengePlanDao(): ChallengePlanDao
    abstract fun checkInDao(): CheckInDao
    abstract fun exerciseResourceDao(): ExerciseResourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "basefit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromExerciseCategory(value: ExerciseCategory): String {
        return value.name
    }

    @TypeConverter
    fun toExerciseCategory(value: String): ExerciseCategory {
        return ExerciseCategory.valueOf(value)
    }

    @TypeConverter
    fun fromResourceType(value: ResourceType): String {
        return value.name
    }

    @TypeConverter
    fun toResourceType(value: String): ResourceType {
        return ResourceType.valueOf(value)
    }
}
