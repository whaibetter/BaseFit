package com.basefit.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.basefit.app.data.dao.*
import com.basefit.app.data.database.AppDatabase
import com.basefit.app.data.entity.*
import com.basefit.app.data.storage.LocalMediaStorage
import com.basefit.app.data.storage.MediaStorage
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
    private val exerciseMediaDao = database.exerciseMediaDao()
    private val userProfileDao = database.userProfileDao()
    private val profileEditHistoryDao = database.profileEditHistoryDao()
    private val bodyMetricDao = database.bodyMetricDao()

    // Exercise operations
    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAll()
    
    fun getAllActiveExercises(): Flow<List<Exercise>> = exerciseDao.getAllActive()

    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getById(id)

    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)

    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)

    suspend fun getExerciseByName(name: String): Exercise? = exerciseDao.getByName(name.trim())

    suspend fun getExerciseByNameExcludeId(name: String, excludeId: Long): Exercise? = 
        exerciseDao.getByNameExcludeId(name.trim(), excludeId)

    // WeekPlan operations
    fun getWeekPlansForDay(dayOfWeek: Int): Flow<List<WeekPlan>> = 
        weekPlanDao.getActiveByDayOfWeek(dayOfWeek)

    suspend fun insertWeekPlan(plan: WeekPlan): Long = weekPlanDao.insert(plan)

    suspend fun updateWeekPlan(plan: WeekPlan) = weekPlanDao.update(plan)

    suspend fun deleteWeekPlan(plan: WeekPlan) = weekPlanDao.delete(plan)

    // ExerciseMedia operations
    fun getMediaByExercise(exerciseId: Long): Flow<List<ExerciseMedia>> = 
        exerciseMediaDao.getByExercise(exerciseId)

    fun getMediaByExerciseAndType(exerciseId: Long, type: MediaType): Flow<List<ExerciseMedia>> =
        exerciseMediaDao.getByExerciseAndType(exerciseId, type)

    suspend fun getMediaById(id: Long): ExerciseMedia? = exerciseMediaDao.getById(id)

    suspend fun insertMedia(media: ExerciseMedia): Long = exerciseMediaDao.insert(media)

    suspend fun updateMedia(media: ExerciseMedia) = exerciseMediaDao.update(media)

    suspend fun deleteMedia(media: ExerciseMedia) = exerciseMediaDao.delete(media)

    suspend fun deleteMediaByExercise(exerciseId: Long) = exerciseMediaDao.deleteByExercise(exerciseId)

    fun getMediaCountByExercise(exerciseId: Long): Flow<Int> = exerciseMediaDao.getCountByExercise(exerciseId)

    // ChallengePlan operations
    fun getAllActiveChallenges(): Flow<List<ChallengePlan>> = challengePlanDao.getAllActive()

    suspend fun getChallengeById(id: Long): ChallengePlan? = challengePlanDao.getById(id)

    suspend fun insertChallengePlan(plan: ChallengePlan): Long = challengePlanDao.insert(plan)

    suspend fun updateChallengePlan(plan: ChallengePlan) = challengePlanDao.update(plan)

    suspend fun deleteChallengePlan(plan: ChallengePlan) = challengePlanDao.delete(plan)

    // Get challenge progress - returns total completed reps for an exercise in a date range
    suspend fun getChallengeProgress(exerciseId: Long, startDate: Long, endDate: Long): Int {
        val checkIns = checkInDao.getByExerciseAndDateRange(exerciseId, startDate, endDate)
        return checkIns.sumOf { it.completedSets * it.completedReps }
    }

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
            
            // 加载媒体列表
            val mediaList = exerciseMediaDao.getByExercise(plan.exerciseId).first()

            results.add(TodayPlanItem(
                weekPlan = plan,
                exercise = exercise,
                isCompleted = completed,
                completedSets = totalSets,
                completedReps = totalReps,
                mediaList = mediaList
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

            val sortedDatesAsc = checkIns.map { it.date }.distinct().sorted()
            val firstDate = sortedDatesAsc.firstOrNull() ?: 0L
            val lastDate = sortedDatesAsc.lastOrNull() ?: 0L

            Achievement(
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                category = exercise.category,
                totalCheckIns = checkIns.size,
                totalSets = totalSets,
                totalReps = totalReps,
                maxWeight = maxWeight,
                maxDuration = maxDuration,
                currentStreak = streak.first,
                bestStreak = streak.second,
                firstCheckInDate = firstDate,
                lastCheckInDate = lastDate
            )
        }
    }

    // Milestone Statistics
    suspend fun getMilestoneStats(): MilestoneStats {
        val achievements = getAchievements()
        if (achievements.isEmpty()) {
            return MilestoneStats(0, 0, emptyList())
        }

        val totalCheckIns = achievements.sumOf { it.totalCheckIns }
        val maxStreak = achievements.maxOfOrNull { it.bestStreak } ?: 0
        val maxReps = achievements.maxOfOrNull { it.totalReps } ?: 0
        val maxWeight = achievements.mapNotNull { it.maxWeight }.maxOrNull()?.toInt() ?: 0
        val totalReps = achievements.sumOf { it.totalReps }

        val milestones = listOf(
            MilestoneInfo("首次打卡", 1, if (totalCheckIns > 0) 1 else 0, "次", AchievementDifficulty.EASY, totalCheckIns >= 1),
            MilestoneInfo("10次打卡", 10, totalCheckIns.coerceAtMost(10), "次", AchievementDifficulty.MEDIUM, totalCheckIns >= 10),
            MilestoneInfo("30次打卡", 30, totalCheckIns.coerceAtMost(30), "次", AchievementDifficulty.HARD, totalCheckIns >= 30),
            MilestoneInfo("50次打卡", 50, totalCheckIns.coerceAtMost(50), "次", AchievementDifficulty.HARD, totalCheckIns >= 50),
            MilestoneInfo("100次打卡", 100, totalCheckIns.coerceAtMost(100), "次", AchievementDifficulty.EXTREME, totalCheckIns >= 100),
            MilestoneInfo("连续7天", 7, maxStreak.coerceAtMost(7), "天", AchievementDifficulty.MEDIUM, maxStreak >= 7),
            MilestoneInfo("连续30天", 30, maxStreak.coerceAtMost(30), "天", AchievementDifficulty.EXTREME, maxStreak >= 30),
            MilestoneInfo("累计1000次", 1000, totalReps.coerceAtMost(1000), "次", AchievementDifficulty.HARD, totalReps >= 1000),
            MilestoneInfo("累计5000次", 5000, totalReps.coerceAtMost(5000), "次", AchievementDifficulty.EXTREME, totalReps >= 5000)
        )

        val completed = milestones.count { it.isCompleted }
        return MilestoneStats(milestones.size, completed, milestones)
    }

    // Category Distribution
    suspend fun getCategoryDistribution(): List<CategoryDistribution> {
        val rawData = checkInDao.getCategoryDistribution()
        val totalCheckIns = rawData.sumOf { it.count }.toFloat()
        if (totalCheckIns == 0f) return emptyList()

        return rawData.map { raw ->
            CategoryDistribution(
                category = raw.category,
                count = raw.count,
                totalCheckIns = raw.totalReps,
                percentage = raw.count / totalCheckIns * 100
            )
        }
    }

    // Weekly Trend (last 8 weeks)
    suspend fun getWeeklyTrend(): List<TrendDataPoint> {
        val calendar = Calendar.getInstance()
        val today = getDayStart(System.currentTimeMillis())
        val result = mutableListOf<TrendDataPoint>()

        for (i in 7 downTo 0) {
            calendar.timeInMillis = today
            calendar.add(Calendar.WEEK_OF_YEAR, -i)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val weekEnd = calendar.timeInMillis + 24 * 60 * 60 * 1000 - 1

            val activeDays = checkInDao.getActiveDaysInDateRange(weekStart, weekEnd)
            val label = if (i == 0) "本周" else "第${8 - i}周"
            result.add(TrendDataPoint(label = label, value = activeDays, date = weekStart))
        }

        return result
    }

    // Monthly Trend (last 6 months)
    suspend fun getMonthlyTrend(): List<TrendDataPoint> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<TrendDataPoint>()

        for (i in 5 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, -i)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis - 1

            val activeDays = checkInDao.getActiveDaysInDateRange(monthStart, monthEnd)
            val monthLabel = calendar.get(Calendar.MONTH) + 1
            val label = "${monthLabel}月"
            result.add(TrendDataPoint(label = label, value = activeDays, date = monthStart))
        }

        return result
    }

    // Period Comparison (this week vs last week, this month vs last month)
    suspend fun getWeekComparison(): ComparisonData {
        val calendar = Calendar.getInstance()
        val today = getDayStart(System.currentTimeMillis())

        calendar.timeInMillis = today
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val thisWeekStart = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val lastWeekStart = calendar.timeInMillis
        val lastWeekEnd = thisWeekStart - 1

        val thisWeekDays = checkInDao.getActiveDaysInDateRange(thisWeekStart, today)
        val lastWeekDays = checkInDao.getActiveDaysInDateRange(lastWeekStart, lastWeekEnd)

        return ComparisonData(thisPeriod = thisWeekDays, lastPeriod = lastWeekDays, periodType = "周")
    }

    suspend fun getMonthComparison(): ComparisonData {
        val calendar = Calendar.getInstance()
        val today = getDayStart(System.currentTimeMillis())

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val thisMonthStart = calendar.timeInMillis

        calendar.add(Calendar.MONTH, -1)
        val lastMonthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        val lastMonthEnd = calendar.timeInMillis - 1

        val thisMonthDays = checkInDao.getActiveDaysInDateRange(thisMonthStart, today)
        val lastMonthDays = checkInDao.getActiveDaysInDateRange(lastMonthStart, lastMonthEnd)

        return ComparisonData(thisPeriod = thisMonthDays, lastPeriod = lastMonthDays, periodType = "月")
    }

    // Difficulty Distribution
    suspend fun getDifficultyDistribution(): List<DifficultyDistribution> {
        val achievements = getAchievements()
        if (achievements.isEmpty()) {
            return listOf(
                DifficultyDistribution(AchievementDifficulty.EASY, 0, "简单"),
                DifficultyDistribution(AchievementDifficulty.MEDIUM, 0, "中等"),
                DifficultyDistribution(AchievementDifficulty.HARD, 0, "困难"),
                DifficultyDistribution(AchievementDifficulty.EXTREME, 0, "极难")
            )
        }

        var easyCount = 0
        var mediumCount = 0
        var hardCount = 0
        var extremeCount = 0

        achievements.forEach { a ->
            when {
                a.totalCheckIns >= 100 || a.bestStreak >= 100 -> extremeCount++
                a.totalCheckIns >= 30 || a.bestStreak >= 30 -> hardCount++
                a.totalCheckIns >= 10 || a.bestStreak >= 7 -> mediumCount++
                else -> easyCount++
            }
        }

        return listOf(
            DifficultyDistribution(AchievementDifficulty.EASY, easyCount, "简单"),
            DifficultyDistribution(AchievementDifficulty.MEDIUM, mediumCount, "中等"),
            DifficultyDistribution(AchievementDifficulty.HARD, hardCount, "困难"),
            DifficultyDistribution(AchievementDifficulty.EXTREME, extremeCount, "极难")
        )
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

    // User Profile operations
    fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getProfile()

    suspend fun getUserProfileOnce(): UserProfile? = userProfileDao.getProfileOnce()

    suspend fun saveUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdate(profile)
    }

    suspend fun updateUserProfileWithHistory(
        oldProfile: UserProfile,
        newProfile: UserProfile
    ) {
        if (oldProfile.name != newProfile.name) {
            profileEditHistoryDao.insert(
                ProfileEditHistory(
                    fieldName = "name",
                    oldValue = oldProfile.name,
                    newValue = newProfile.name
                )
            )
        }
        if (oldProfile.phone != newProfile.phone) {
            profileEditHistoryDao.insert(
                ProfileEditHistory(
                    fieldName = "phone",
                    oldValue = oldProfile.phone,
                    newValue = newProfile.phone
                )
            )
        }
        if (oldProfile.email != newProfile.email) {
            profileEditHistoryDao.insert(
                ProfileEditHistory(
                    fieldName = "email",
                    oldValue = oldProfile.email,
                    newValue = newProfile.email
                )
            )
        }
        if (oldProfile.gender != newProfile.gender) {
            profileEditHistoryDao.insert(
                ProfileEditHistory(
                    fieldName = "gender",
                    oldValue = oldProfile.gender,
                    newValue = newProfile.gender
                )
            )
        }
        if (oldProfile.birthDate != newProfile.birthDate) {
            profileEditHistoryDao.insert(
                ProfileEditHistory(
                    fieldName = "birthDate",
                    oldValue = oldProfile.birthDate?.toString(),
                    newValue = newProfile.birthDate?.toString()
                )
            )
        }
        if (oldProfile.avatarPath != newProfile.avatarPath) {
            profileEditHistoryDao.insert(
                ProfileEditHistory(
                    fieldName = "avatar",
                    oldValue = oldProfile.avatarPath,
                    newValue = newProfile.avatarPath
                )
            )
        }
        userProfileDao.insertOrUpdate(newProfile.copy(updatedAt = System.currentTimeMillis()))
    }

    fun getProfileEditHistory(): Flow<List<ProfileEditHistory>> =
        profileEditHistoryDao.getAllHistory()

    fun getRecentProfileHistory(limit: Int = 10): Flow<List<ProfileEditHistory>> =
        profileEditHistoryDao.getRecentHistory(limit)

    // Body Metrics operations
    fun getAllBodyMetrics(): Flow<List<BodyMetric>> = bodyMetricDao.getAllMetrics()

    fun getBodyMetricsByType(type: BodyMetricType): Flow<List<BodyMetric>> =
        bodyMetricDao.getMetricsByType(type)

    suspend fun getLatestBodyMetric(type: BodyMetricType): BodyMetric? =
        bodyMetricDao.getLatestMetricByType(type)

    fun getBodyMetricsByDateRange(startDate: Long, endDate: Long): Flow<List<BodyMetric>> =
        bodyMetricDao.getMetricsByDateRange(startDate, endDate)

    fun getBodyMetricTrend(type: BodyMetricType, days: Int): Flow<List<BodyMetric>> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.timeInMillis
        return bodyMetricDao.getMetricsByTypeAndDateRange(type, startDate, endDate)
    }

    suspend fun getBodyMetricTrendData(type: BodyMetricType, days: Int): BodyMetricTrend {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startDate = calendar.timeInMillis

        val metrics = bodyMetricDao.getMetricsSince(startDate)
            .filter { it.type == type }
            .sortedBy { it.recordDate }

        val dataPoints = metrics.map { BodyMetricDataPoint(it.recordDate, it.value) }
        val latestValue = metrics.lastOrNull()?.value
        val firstValue = metrics.firstOrNull()?.value
        val change = if (latestValue != null && firstValue != null) latestValue - firstValue else null
        val changePercent = if (change != null && firstValue != null && firstValue != 0f)
            (change / firstValue * 100) else null

        val normalRange = NormalRange.getRange(type)

        return BodyMetricTrend(
            type = type,
            unit = metrics.firstOrNull()?.unit ?: "",
            dataPoints = dataPoints,
            normalMin = normalRange?.min,
            normalMax = normalRange?.max,
            latestValue = latestValue,
            change = change,
            changePercent = changePercent
        )
    }

    suspend fun saveBodyMetric(metric: BodyMetric): Long {
        return bodyMetricDao.insert(metric)
    }

    suspend fun saveMultipleBodyMetrics(metrics: List<BodyMetric>) {
        bodyMetricDao.insertAll(metrics)
    }

    suspend fun deleteBodyMetric(metric: BodyMetric) {
        bodyMetricDao.delete(metric)
    }

    suspend fun getAllLatestMetrics(): Map<BodyMetricType, BodyMetric> {
        val result = mutableMapOf<BodyMetricType, BodyMetric>()
        BodyMetricType.entries.forEach { type ->
            bodyMetricDao.getLatestMetricByType(type)?.let {
                result[type] = it
            }
        }
        return result
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
    val completedReps: Int,
    val mediaList: List<ExerciseMedia> = emptyList()
)
