package com.basefit.app.data.repository

import android.content.Context
import android.util.Log
import com.basefit.app.data.dao.*
import com.basefit.app.data.database.AppDatabase
import com.basefit.app.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class FitRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val exerciseDao = database.exerciseDao()
    private val weekPlanDao = database.weekPlanDao()
    private val challengePlanDao = database.challengePlanDao()
    private val checkInDao = database.checkInDao()

    // Exercise operations
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAll()
    
    fun getAllActiveExercises(): Flow<List<Exercise>> = exerciseDao.getAllActive()

    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)

    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)

    // WeekPlan operations
    fun getWeekPlansForDay(dayOfWeek: Int): Flow<List<WeekPlan>> = 
        weekPlanDao.getActiveByDayOfWeek(dayOfWeek)

    suspend fun insertWeekPlan(plan: WeekPlan): Long = weekPlanDao.insert(plan)

    suspend fun updateWeekPlan(plan: WeekPlan) = weekPlanDao.update(plan)

    suspend fun deleteWeekPlan(plan: WeekPlan) = weekPlanDao.delete(plan)

    // ChallengePlan operations
    fun getAllActiveChallenges(): Flow<List<ChallengePlan>> = challengePlanDao.getAllActive()

    suspend fun getChallengeById(id: Long): ChallengePlan? = challengePlanDao.getById(id)

    suspend fun insertChallengePlan(plan: ChallengePlan): Long = challengePlanDao.insert(plan)

    suspend fun updateChallengePlan(plan: ChallengePlan) = challengePlanDao.update(plan)

    suspend fun deleteChallengePlan(plan: ChallengePlan) = challengePlanDao.delete(plan)

    // CheckIn operations
    fun getCheckInsByDate(date: Long): Flow<List<CheckIn>> = checkInDao.getByDate(date)

    fun getAllCheckIns(): Flow<List<CheckIn>> = checkInDao.getAll()

    suspend fun insertCheckIn(checkIn: CheckIn): Long = checkInDao.insert(checkIn)

    suspend fun updateCheckIn(checkIn: CheckIn) = checkInDao.update(checkIn)

    suspend fun deleteCheckIn(checkIn: CheckIn) = checkInDao.delete(checkIn)

    // Statistics
    fun getTotalCheckInDays(): Flow<Int> = checkInDao.getTotalCheckInDays()

    suspend fun getMaxWeight(exerciseId: Long): Float? = checkInDao.getMaxWeight(exerciseId)

    suspend fun getMaxDuration(exerciseId: Long): Int? = checkInDao.getMaxDuration(exerciseId)

    // Today's plan
    suspend fun getTodayPlans(): List<TodayPlanItem> {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert to Monday=1 format
        val adjustedDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

        val weekPlans = weekPlanDao.getActiveByDayOfWeek(adjustedDay).first()
        val results = mutableListOf<TodayPlanItem>()

        for (plan in weekPlans) {
            val exercise = exerciseDao.getById(plan.exerciseId) ?: continue
            val todayStart = getDayStart(System.currentTimeMillis())
            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
            val checkIns = checkInDao.getByExerciseAndDateRange(plan.exerciseId, todayStart, todayEnd)
            
            val completed = checkIns.isNotEmpty()
            val totalSets = checkIns.sumOf { it.completedSets }
            val totalReps = checkIns.sumOf { it.completedReps }

            results.add(TodayPlanItem(
                weekPlan = plan,
                exercise = exercise,
                isCompleted = completed,
                completedSets = totalSets,
                completedReps = totalReps
            ))
        }

        return results
    }

    // Achievements
    suspend fun getAchievements(): List<Achievement> {
        val exercises = exerciseDao.getAllActive().first()
        return exercises.mapNotNull { exercise ->
            val checkIns = checkInDao.getByExercise(exercise.id).first()
            if (checkIns.isEmpty()) return@mapNotNull null

            val totalSets = checkIns.sumOf { it.completedSets }
            val totalReps = checkIns.sumOf { it.completedReps }
            val maxWeight = checkInDao.getMaxWeight(exercise.id)
            val maxDuration = checkInDao.getMaxDuration(exercise.id)
            
            // Calculate streak
            val sortedDates = checkIns.map { it.date }.distinct().sortedDescending()
            val streak = calculateStreak(sortedDates)

            Achievement(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                totalCheckIns = checkIns.size,
                totalSets = totalSets,
                totalReps = totalReps,
                maxWeight = maxWeight,
                maxDuration = maxDuration,
                currentStreak = streak.first,
                bestStreak = streak.second
            )
        }
    }

    private fun calculateStreak(sortedDates: List<Long>): Pair<Int, Int> {
        if (sortedDates.isEmpty()) return Pair(0, 0)

        val calendar = Calendar.getInstance()
        var currentStreak = 0
        var bestStreak = 1
        var tempStreak = 1

        val today = getDayStart(System.currentTimeMillis())
        val yesterday = today - 24 * 60 * 60 * 1000

        // Check if the most recent date is today or yesterday
        val firstDate = sortedDates.first()
        if (firstDate >= yesterday) {
            currentStreak = 1
            var lastDate = firstDate
            
            for (i in 1 until sortedDates.size) {
                val currentDate = sortedDates[i]
                val diffDays = ((lastDate - currentDate) / (24 * 60 * 60 * 1000)).toInt()
                
                if (diffDays == 1) {
                    currentStreak++
                    tempStreak++
                } else {
                    break
                }
                lastDate = currentDate
            }
        }

        // Calculate best streak
        tempStreak = 1
        for (i in 1 until sortedDates.size) {
            val diffDays = ((sortedDates[i - 1] - sortedDates[i]) / (24 * 60 * 60 * 1000)).toInt()
            if (diffDays == 1) {
                tempStreak++
                bestStreak = maxOf(bestStreak, tempStreak)
            } else {
                tempStreak = 1
            }
        }

        return Pair(currentStreak, bestStreak)
    }

    // Calendar data
    suspend fun getCalendarData(year: Int, month: Int): Map<Long, Int> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis - 1

        val checkIns = checkInDao.getByDateRange(monthStart, monthEnd).first()
        return checkIns.groupingBy { getDayStart(it.date) }
            .eachCount()
    }

    // Weekly stats
    suspend fun getWeeklyStats(): List<WeeklyStats> {
        val checkIns = checkInDao.getAll().first()
        if (checkIns.isEmpty()) return emptyList()

        // Group by week
        val weekGroups = mutableMapOf<Long, MutableList<CheckIn>>()
        val calendar = Calendar.getInstance()

        for (checkIn in checkIns) {
            calendar.timeInMillis = checkIn.date
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val weekStart = calendar.timeInMillis

            weekGroups.getOrPut(weekStart) { mutableListOf() }.add(checkIn)
        }

        return weekGroups.map { (weekStart, items) ->
            val uniqueDays = items.map { getDayStart(it.date) }.distinct().size
            WeeklyStats(
                weekStart = weekStart,
                totalDays = 7,
                activeDays = uniqueDays,
                totalCheckIns = items.size,
                totalSets = items.sumOf { it.completedSets },
                totalReps = items.sumOf { it.completedReps }
            )
        }.sortedByDescending { it.weekStart }
    }

    // Data export
    suspend fun exportData(context: Context, fileName: String): Boolean {
        return try {
            val exercises = exerciseDao.getAll().first()
            val weekPlans = weekPlanDao.getAllActive().first()
            val challenges = challengePlanDao.getAllActive().first()
            val checkIns = checkInDao.getAll().first()

            val json = JSONObject().apply {
                put("version", 1)
                put("exportDate", System.currentTimeMillis())
                
                put("exercises", JSONArray().apply {
                    exercises.forEach { exercise ->
                        put(JSONObject().apply {
                            put("id", exercise.id)
                            put("name", exercise.name)
                            put("category", exercise.category.name)
                            put("isActive", exercise.isActive)
                            put("createdAt", exercise.createdAt)
                        })
                    }
                })

                put("weekPlans", JSONArray().apply {
                    weekPlans.forEach { plan ->
                        put(JSONObject().apply {
                            put("id", plan.id)
                            put("exerciseId", plan.exerciseId)
                            put("dayOfWeek", plan.dayOfWeek)
                            put("targetSets", plan.targetSets)
                            put("targetReps", plan.targetReps)
                            put("isActive", plan.isActive)
                        })
                    }
                })

                put("challenges", JSONArray().apply {
                    challenges.forEach { challenge ->
                        put(JSONObject().apply {
                            put("id", challenge.id)
                            put("exerciseId", challenge.exerciseId)
                            put("name", challenge.name)
                            put("startDate", challenge.startDate)
                            put("endDate", challenge.endDate)
                            put("targetSets", challenge.targetSets)
                            put("targetReps", challenge.targetReps)
                        })
                    }
                })

                put("checkIns", JSONArray().apply {
                    checkIns.forEach { checkIn ->
                        put(JSONObject().apply {
                            put("id", checkIn.id)
                            put("exerciseId", checkIn.exerciseId)
                            put("date", checkIn.date)
                            put("completedSets", checkIn.completedSets)
                            put("completedReps", checkIn.completedReps)
                            put("weight", checkIn.weight)
                            put("durationMinutes", checkIn.durationMinutes)
                            put("notes", checkIn.notes)
                            put("createdAt", checkIn.createdAt)
                        })
                    }
                })
            }

            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { output ->
                output.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e("FitRepository", "Export failed", e)
            false
        }
    }

    // Data import
    suspend fun importData(context: Context, fileName: String): Boolean {
        return try {
            val file = File(context.getExternalFilesDir(null), fileName)
            if (!file.exists()) return false

            val json = JSONObject(FileInputStream(file).use { 
                String(it.readBytes(), Charsets.UTF_8) 
            })

            // Clear existing data
            // Note: In production, you might want to ask user confirmation

            // Import exercises
            val exercisesArray = json.getJSONArray("exercises")
            for (i in 0 until exercisesArray.length()) {
                val item = exercisesArray.getJSONObject(i)
                exerciseDao.insert(Exercise(
                    id = 0, // Let Room generate new IDs
                    name = item.getString("name"),
                    category = ExerciseCategory.valueOf(item.getString("category")),
                    isActive = item.optBoolean("isActive", true),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis())
                ))
            }

            // Import check-ins
            val checkInsArray = json.optJSONArray("checkIns")
            checkInsArray?.let { array ->
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    // Note: exerciseId mapping would need to be handled properly
                    // For simplicity, assuming IDs match or user re-creates plans
                }
            }

            true
        } catch (e: Exception) {
            Log.e("FitRepository", "Import failed", e)
            false
        }
    }

    // Helper to get day start timestamp
    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        @Volatile
        private var INSTANCE: FitRepository? = null

        fun getRepository(context: Context): FitRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FitRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

// Helper class for today's plan
data class TodayPlanItem(
    val weekPlan: WeekPlan,
    val exercise: Exercise,
    val isCompleted: Boolean,
    val completedSets: Int,
    val completedReps: Int
)
