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

    @Query("""
        SELECT SUM(completedReps) FROM check_ins
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalRepsInDateRange(startDate: Long, endDate: Long): Int?

    @Query("""
        SELECT SUM(completedSets) FROM check_ins
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalSetsInDateRange(startDate: Long, endDate: Long): Int?

    @Query("""
        SELECT COUNT(DISTINCT date) FROM check_ins
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getActiveDaysInDateRange(startDate: Long, endDate: Long): Int

    @Query("SELECT date FROM check_ins ORDER BY date ASC LIMIT 1")
    suspend fun getFirstCheckInDate(): Long?

    @Query("SELECT date FROM check_ins ORDER BY date DESC LIMIT 1")
    suspend fun getLastCheckInDate(): Long?

    @Query("""
        SELECT e.category, COUNT(*) as count, SUM(c.completedReps) as totalReps
        FROM check_ins c
        INNER JOIN exercises e ON c.exerciseId = e.id
        GROUP BY e.category
    """)
    suspend fun getCategoryDistribution(): List<CategoryDistributionRaw>

    @Query("""
        SELECT * FROM check_ins
        WHERE exerciseId = :exerciseId
        ORDER BY date ASC
    """)
    suspend fun getCheckInsByExerciseOrdered(exerciseId: Long): List<CheckIn>
}

data class CategoryDistributionRaw(
    val category: ExerciseCategory,
    val count: Int,
    val totalReps: Int
)

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

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Delete
    suspend fun delete(profile: UserProfile)
}

@Dao
interface ProfileEditHistoryDao {
    @Query("SELECT * FROM profile_edit_history ORDER BY editedAt DESC")
    fun getAllHistory(): Flow<List<ProfileEditHistory>>

    @Query("SELECT * FROM profile_edit_history WHERE fieldName = :fieldName ORDER BY editedAt DESC")
    fun getHistoryByField(fieldName: String): Flow<List<ProfileEditHistory>>

    @Query("SELECT * FROM profile_edit_history ORDER BY editedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<ProfileEditHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ProfileEditHistory)

    @Query("DELETE FROM profile_edit_history")
    suspend fun deleteAll()
}

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY recordDate DESC")
    fun getAllMetrics(): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE type = :type ORDER BY recordDate DESC")
    fun getMetricsByType(type: BodyMetricType): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE type = :type ORDER BY recordDate DESC LIMIT 1")
    suspend fun getLatestMetricByType(type: BodyMetricType): BodyMetric?

    @Query("SELECT * FROM body_metrics WHERE type = :type AND recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC")
    fun getMetricsByTypeAndDateRange(type: BodyMetricType, startDate: Long, endDate: Long): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate DESC")
    fun getMetricsByDateRange(startDate: Long, endDate: Long): Flow<List<BodyMetric>>

    @Query("SELECT * FROM body_metrics WHERE recordDate >= :startDate ORDER BY recordDate ASC")
    suspend fun getMetricsSince(startDate: Long): List<BodyMetric>

    @Query("SELECT DISTINCT recordDate FROM body_metrics WHERE recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC")
    suspend fun getRecordDatesInRange(startDate: Long, endDate: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: BodyMetric): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metrics: List<BodyMetric>)

    @Update
    suspend fun update(metric: BodyMetric)

    @Delete
    suspend fun delete(metric: BodyMetric)

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM body_metrics WHERE type = :type")
    suspend fun deleteByType(type: BodyMetricType)
}
