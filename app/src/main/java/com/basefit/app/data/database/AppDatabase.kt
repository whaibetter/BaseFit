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
        ExerciseMedia::class,
        UserProfile::class,
        ProfileEditHistory::class,
        BodyMetric::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun weekPlanDao(): WeekPlanDao
    abstract fun challengePlanDao(): ChallengePlanDao
    abstract fun checkInDao(): CheckInDao
    abstract fun exerciseMediaDao(): ExerciseMediaDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun profileEditHistoryDao(): ProfileEditHistoryDao
    abstract fun bodyMetricDao(): BodyMetricDao

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
    fun fromMediaType(value: MediaType): String {
        return value.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return MediaType.valueOf(value)
    }

    @TypeConverter
    fun fromBodyMetricType(value: BodyMetricType): String {
        return value.name
    }

    @TypeConverter
    fun toBodyMetricType(value: String): BodyMetricType {
        return BodyMetricType.valueOf(value)
    }
}
