package com.basefit.app.data.demo

import com.basefit.app.data.entity.*
import com.basefit.app.data.repository.TodayPlanItem
import com.basefit.app.viewmodel.ChallengePlanWithExercise
import com.basefit.app.viewmodel.CheckInWithExercise
import java.util.*

object DemoData {
    fun getDemoTodayPlans(): List<TodayPlanItem> {
        val now = System.currentTimeMillis()
        val dayStart = getDayStart(now)

        return listOf(
            TodayPlanItem(
                weekPlan = WeekPlan(id = 1, exerciseId = 1, dayOfWeek = getDayOfWeek(now), targetSets = 3, targetReps = 15),
                exercise = Exercise(id = 1, name = "俯卧撑", category = ExerciseCategory.BODYWEIGHT),
                isCompleted = false,
                completedSets = 0,
                completedReps = 0,
                mediaList = emptyList()
            ),
            TodayPlanItem(
                weekPlan = WeekPlan(id = 2, exerciseId = 2, dayOfWeek = getDayOfWeek(now), targetSets = 4, targetReps = 20),
                exercise = Exercise(id = 2, name = "深蹲", category = ExerciseCategory.BODYWEIGHT),
                isCompleted = true,
                completedSets = 4,
                completedReps = 20,
                mediaList = emptyList()
            ),
            TodayPlanItem(
                weekPlan = WeekPlan(id = 3, exerciseId = 3, dayOfWeek = getDayOfWeek(now), targetSets = 3, targetReps = 10),
                exercise = Exercise(id = 3, name = "引体向上", category = ExerciseCategory.STRENGTH),
                isCompleted = false,
                completedSets = 2,
                completedReps = 8,
                mediaList = emptyList()
            )
        )
    }

    fun getDemoActiveChallenges(): List<ChallengePlanWithExercise> {
        val now = System.currentTimeMillis()
        val weekInMillis = 7L * 24 * 60 * 60 * 1000

        return listOf(
            ChallengePlanWithExercise(
                challenge = ChallengePlan(
                    id = 1,
                    exerciseId = 1,
                    name = "30天俯卧撑挑战",
                    startDate = now - weekInMillis,
                    endDate = now + 23 * weekInMillis,
                    targetTotalReps = 500,
                    targetSets = 3,
                    targetReps = 15
                ),
                exercise = Exercise(id = 1, name = "俯卧撑", category = ExerciseCategory.BODYWEIGHT),
                completedReps = 180,
                mediaList = emptyList()
            ),
            ChallengePlanWithExercise(
                challenge = ChallengePlan(
                    id = 2,
                    exerciseId = 2,
                    name = "百次深蹲计划",
                    startDate = now - 3 * weekInMillis,
                    endDate = now + 11 * weekInMillis,
                    targetTotalReps = 100,
                    targetSets = 4,
                    targetReps = 25
                ),
                exercise = Exercise(id = 2, name = "深蹲", category = ExerciseCategory.BODYWEIGHT),
                completedReps = 45,
                mediaList = emptyList()
            )
        )
    }

    fun getDemoCheckIns(): List<CheckInWithExercise> {
        val now = System.currentTimeMillis()
        val dayInMillis = 24L * 60 * 60 * 1000

        return listOf(
            CheckInWithExercise(
                checkIn = CheckIn(
                    id = 1,
                    exerciseId = 1,
                    date = now - dayInMillis,
                    completedSets = 3,
                    completedReps = 15,
                    weight = null,
                    durationMinutes = null,
                    notes = "状态不错"
                ),
                exercise = Exercise(id = 1, name = "俯卧撑", category = ExerciseCategory.BODYWEIGHT)
            ),
            CheckInWithExercise(
                checkIn = CheckIn(
                    id = 2,
                    exerciseId = 2,
                    date = now - dayInMillis,
                    completedSets = 4,
                    completedReps = 20,
                    weight = null,
                    durationMinutes = null,
                    notes = null
                ),
                exercise = Exercise(id = 2, name = "深蹲", category = ExerciseCategory.BODYWEIGHT)
            ),
            CheckInWithExercise(
                checkIn = CheckIn(
                    id = 3,
                    exerciseId = 3,
                    date = now - 2 * dayInMillis,
                    completedSets = 3,
                    completedReps = 8,
                    weight = null,
                    durationMinutes = null,
                    notes = "有点累"
                ),
                exercise = Exercise(id = 3, name = "引体向上", category = ExerciseCategory.STRENGTH)
            ),
            CheckInWithExercise(
                checkIn = CheckIn(
                    id = 4,
                    exerciseId = 1,
                    date = now - 2 * dayInMillis,
                    completedSets = 3,
                    completedReps = 12,
                    weight = null,
                    durationMinutes = null,
                    notes = null
                ),
                exercise = Exercise(id = 1, name = "俯卧撑", category = ExerciseCategory.BODYWEIGHT)
            ),
            CheckInWithExercise(
                checkIn = CheckIn(
                    id = 5,
                    exerciseId = 4,
                    date = now - 3 * dayInMillis,
                    completedSets = 1,
                    completedReps = 1,
                    weight = null,
                    durationMinutes = 30,
                    notes = "5公里跑步"
                ),
                exercise = Exercise(id = 4, name = "跑步", category = ExerciseCategory.CARDIO)
            )
        )
    }

    fun getDemoCalendarData(): Map<Long, Int> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val map = mutableMapOf<Long, Int>()

        for (i in 0..6) {
            val dayStart = calendar.timeInMillis - i * 24L * 60 * 60 * 1000
            map[dayStart] = (1..4).random()
        }

        return map
    }

    fun getDemoAchievements(): List<Achievement> {
        return listOf(
            Achievement(
                exerciseId = 1,
                exerciseName = "俯卧撑",
                category = ExerciseCategory.BODYWEIGHT,
                totalCheckIns = 28,
                totalSets = 84,
                totalReps = 392,
                currentStreak = 7,
                bestStreak = 14,
                firstCheckInDate = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                lastCheckInDate = System.currentTimeMillis(),
                maxWeight = null,
                maxDuration = null
            ),
            Achievement(
                exerciseId = 2,
                exerciseName = "深蹲",
                category = ExerciseCategory.BODYWEIGHT,
                totalCheckIns = 21,
                totalSets = 84,
                totalReps = 420,
                currentStreak = 5,
                bestStreak = 10,
                firstCheckInDate = System.currentTimeMillis() - 25L * 24 * 60 * 60 * 1000,
                lastCheckInDate = System.currentTimeMillis(),
                maxWeight = null,
                maxDuration = null
            ),
            Achievement(
                exerciseId = 4,
                exerciseName = "跑步",
                category = ExerciseCategory.CARDIO,
                totalCheckIns = 12,
                totalSets = 12,
                totalReps = 12,
                currentStreak = 3,
                bestStreak = 7,
                firstCheckInDate = System.currentTimeMillis() - 20L * 24 * 60 * 60 * 1000,
                lastCheckInDate = System.currentTimeMillis(),
                maxWeight = null,
                maxDuration = 45
            )
        )
    }

    fun getDemoCategoryDistribution(): List<CategoryDistribution> {
        return listOf(
            CategoryDistribution(ExerciseCategory.BODYWEIGHT, 60, 100, 60f),
            CategoryDistribution(ExerciseCategory.STRENGTH, 25, 100, 25f),
            CategoryDistribution(ExerciseCategory.CARDIO, 15, 100, 15f)
        )
    }

    fun getDemoDifficultyDistribution(): List<DifficultyDistribution> {
        return listOf(
            DifficultyDistribution(AchievementDifficulty.EASY, 5, "简单"),
            DifficultyDistribution(AchievementDifficulty.MEDIUM, 8, "中等"),
            DifficultyDistribution(AchievementDifficulty.HARD, 3, "困难"),
            DifficultyDistribution(AchievementDifficulty.EXTREME, 1, "极限")
        )
    }

    fun getDemoWeeklyTrend(): List<TrendDataPoint> {
        val calendar = Calendar.getInstance()
        val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")

        return (0..6).map { i ->
            val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            TrendDataPoint(
                label = dayNames[dayOfWeek],
                value = (2..5).random(),
                date = calendar.timeInMillis
            )
        }
    }

    fun getDemoMonthlyTrend(): List<TrendDataPoint> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        return (1..30 step 3).map { day ->
            TrendDataPoint(
                label = "${day}日",
                value = (15..25).random(),
                date = calendar.timeInMillis
            )
        }
    }

    fun getDemoMilestoneStats(): MilestoneStats {
        return MilestoneStats(
            totalMilestones = 10,
            completedMilestones = 3,
            milestones = listOf(
                MilestoneInfo("首次打卡", 1, 1, "次", AchievementDifficulty.EASY, true),
                MilestoneInfo("连续3天", 3, 3, "天", AchievementDifficulty.EASY, true),
                MilestoneInfo("连续7天", 7, 7, "天", AchievementDifficulty.MEDIUM, true),
                MilestoneInfo("连续30天", 30, 7, "天", AchievementDifficulty.HARD, false),
                MilestoneInfo("百次打卡", 100, 28, "次", AchievementDifficulty.MEDIUM, false),
                MilestoneInfo("千次训练", 1000, 50, "组", AchievementDifficulty.EXTREME, false)
            )
        )
    }

    fun getDemoBodyMetrics(): Map<BodyMetricType, BodyMetric> {
        val now = System.currentTimeMillis()
        return mapOf(
            BodyMetricType.WEIGHT to BodyMetric(
                type = BodyMetricType.WEIGHT,
                value = 68.5f,
                unit = "kg",
                recordDate = now,
                source = "manual",
                notes = null
            ),
            BodyMetricType.BODY_FAT to BodyMetric(
                type = BodyMetricType.BODY_FAT,
                value = 16.8f,
                unit = "%",
                recordDate = now,
                source = "manual",
                notes = null
            ),
            BodyMetricType.STEPS to BodyMetric(
                type = BodyMetricType.STEPS,
                value = 8500f,
                unit = "步",
                recordDate = now,
                source = "manual",
                notes = null
            ),
            BodyMetricType.SLEEP to BodyMetric(
                type = BodyMetricType.SLEEP,
                value = 7.5f,
                unit = "小时",
                recordDate = now,
                source = "manual",
                notes = null
            ),
            BodyMetricType.HEART_RATE to BodyMetric(
                type = BodyMetricType.HEART_RATE,
                value = 68f,
                unit = "bpm",
                recordDate = now,
                source = "manual",
                notes = null
            )
        )
    }

    fun getDemoUserProfile(): UserProfile {
        return UserProfile(
            id = 1,
            name = "健身爱好者",
            phone = "138****8888",
            email = "fitness@example.com",
            gender = "male",
            birthDate = 946656000000,
            avatarPath = null,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getDayOfWeek(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    }
}
