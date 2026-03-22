package com.basefit.app.data.dao

import androidx.room.*
import com.basefit.app.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActive(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE category = :category AND isActive = 1 ORDER BY name ASC")
    fun getByCategory(category: ExerciseCategory): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercises WHERE name = :name AND isActive = 1 LIMIT 1")
    suspend fun getByName(name: String): Exercise?

    @Query("SELECT * FROM exercises WHERE name = :name AND id != :excludeId AND isActive = 1 LIMIT 1")
    suspend fun getByNameExcludeId(name: String, excludeId: Long): Exercise?
}

@Dao
interface WeekPlanDao {
    @Query("SELECT * FROM week_plans WHERE isActive = 1 ORDER BY dayOfWeek ASC")
    fun getAllActive(): Flow<List<WeekPlan>>

    @Query("SELECT * FROM week_plans WHERE dayOfWeek = :dayOfWeek AND isActive = 1")
    fun getByDayOfWeek(dayOfWeek: Int): Flow<List<WeekPlan>>

    @Query("SELECT * FROM week_plans WHERE exerciseId = :exerciseId AND isActive = 1")
    fun getByExercise(exerciseId: Long): Flow<List<WeekPlan>>

    @Query("""
        SELECT wp.* FROM week_plans wp
        INNER JOIN exercises e ON wp.exerciseId = e.id
        WHERE wp.dayOfWeek = :dayOfWeek AND wp.isActive = 1 AND e.isActive = 1
    """)
    fun getActiveByDayOfWeek(dayOfWeek: Int): Flow<List<WeekPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WeekPlan): Long

    @Update
    suspend fun update(plan: WeekPlan)

    @Delete
    suspend fun delete(plan: WeekPlan)

    @Query("DELETE FROM week_plans WHERE exerciseId = :exerciseId")
    suspend fun deleteByExercise(exerciseId: Long)
}

@Dao
interface ChallengePlanDao {
    @Query("SELECT * FROM challenge_plans WHERE isActive = 1 ORDER BY startDate DESC")
    fun getAllActive(): Flow<List<ChallengePlan>>

    @Query("SELECT * FROM challenge_plans WHERE id = :id")
    suspend fun getById(id: Long): ChallengePlan?

    @Query("""
        SELECT * FROM challenge_plans 
        WHERE exerciseId = :exerciseId 
        AND :date BETWEEN startDate AND endDate 
        AND isActive = 1
    """)
    suspend fun getActiveForExerciseOnDate(exerciseId: Long, date: Long): ChallengePlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: ChallengePlan): Long

    @Update
    suspend fun update(plan: ChallengePlan)

    @Delete
    suspend fun delete(plan: ChallengePlan)
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE date = :date ORDER BY createdAt DESC")
    fun getByDate(date: Long): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE exerciseId = :exerciseId ORDER BY date DESC")
    fun getByExercise(exerciseId: Long): Flow<List<CheckIn>>

    @Query("SELECT * FROM check_ins WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByDateRange(startDate: Long, endDate: Long): Flow<List<CheckIn>>

    @Query("""
        SELECT * FROM check_ins 
        WHERE exerciseId = :exerciseId 
        AND date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC
    """)
    suspend fun getByExerciseAndDateRange(exerciseId: Long, startDate: Long, endDate: Long): List<CheckIn>

    @Query("SELECT COUNT(DISTINCT date) FROM check_ins")
    fun getTotalCheckInDays(): Flow<Int>

    @Query("SELECT COUNT(*) FROM check_ins WHERE exerciseId = :exerciseId")
    fun getCheckInCountForExercise(exerciseId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM check_ins 
        WHERE exerciseId = :exerciseId 
        AND date >= :startDate
    """)
    suspend fun getCheckInCountSince(exerciseId: Long, startDate: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: CheckIn): Long

    @Update
    suspend fun update(checkIn: CheckIn)

    @Delete
    suspend fun delete(checkIn: CheckIn)

    @Query("SELECT MAX(weight) FROM check_ins WHERE exerciseId = :exerciseId AND weight IS NOT NULL")
    suspend fun getMaxWeight(exerciseId: Long): Float?

    @Query("SELECT MAX(durationMinutes) FROM check_ins WHERE exerciseId = :exerciseId AND durationMinutes IS NOT NULL")
    suspend fun getMaxDuration(exerciseId: Long): Int?

    @Query("""
        SELECT SUM(completedSets) FROM check_ins 
        WHERE exerciseId = :exerciseId 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalSetsInRange(exerciseId: Long, startDate: Long, endDate: Long): Int?

    @Query("""
        SELECT SUM(completedReps) FROM check_ins 
        WHERE exerciseId = :exerciseId 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalRepsInRange(exerciseId: Long, startDate: Long, endDate: Long): Int?
}

@Dao
interface ExerciseMediaDao {
    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId ORDER BY orderIndex ASC, createdAt ASC")
    fun getByExercise(exerciseId: Long): Flow<List<ExerciseMedia>>

    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId AND type = :type ORDER BY orderIndex ASC")
    fun getByExerciseAndType(exerciseId: Long, type: MediaType): Flow<List<ExerciseMedia>>

    @Query("SELECT * FROM exercise_media WHERE id = :id")
    suspend fun getById(id: Long): ExerciseMedia?

    @Query("SELECT * FROM exercise_media WHERE exerciseId = :exerciseId ORDER BY orderIndex ASC LIMIT 1")
    suspend fun getFirstMedia(exerciseId: Long): ExerciseMedia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: ExerciseMedia): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(media: List<ExerciseMedia>)

    @Update
    suspend fun update(media: ExerciseMedia)

    @Delete
    suspend fun delete(media: ExerciseMedia)

    @Query("DELETE FROM exercise_media WHERE exerciseId = :exerciseId")
    suspend fun deleteByExercise(exerciseId: Long)

    @Query("DELETE FROM exercise_media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM exercise_media WHERE exerciseId = :exerciseId")
    fun getCountByExercise(exerciseId: Long): Flow<Int>

    @Query("SELECT MAX(orderIndex) FROM exercise_media WHERE exerciseId = :exerciseId")
    suspend fun getMaxOrderIndex(exerciseId: Long): Int?
}
