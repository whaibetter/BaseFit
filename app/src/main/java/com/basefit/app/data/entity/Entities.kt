package com.basefit.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 动作分类枚举
enum class ExerciseCategory {
    BODYWEIGHT,  // 自重训练
    STRENGTH,    // 力量训练
    CARDIO       // 有氧运动
}

// 媒体类型枚举
enum class MediaType {
    IMAGE,  // 图片
    GIF,    // 动图
    VIDEO   // 视频
}

// 动作实体
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// 周计划实体
@Entity(tableName = "week_plans")
data class WeekPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val dayOfWeek: Int, // 1-7 (周一到周日)
    val targetSets: Int,
    val targetReps: Int,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// 挑战计划实体
@Entity(tableName = "challenge_plans")
data class ChallengePlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val name: String,
    val startDate: Long,
    val endDate: Long,
    val targetTotalReps: Int, // 总目标次数（比如1000次）
    val targetSets: Int,      // 每天建议组数
    val targetReps: Int,      // 每组建议次数
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// 打卡记录实体
@Entity(tableName = "check_ins")
data class CheckIn(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val date: Long, // 时间戳，只保留日期部分
    val completedSets: Int,
    val completedReps: Int,
    val weight: Float? = null,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// 成就记录
data class Achievement(
    val exerciseId: Long,
    val exerciseName: String,
    val category: ExerciseCategory,
    val totalCheckIns: Int,
    val totalSets: Int,
    val totalReps: Int,
    val maxWeight: Float?,
    val maxDuration: Int?,
    val currentStreak: Int,
    val bestStreak: Int,
    val firstCheckInDate: Long = 0,
    val lastCheckInDate: Long = 0
)

enum class AchievementDifficulty {
    EASY,       // 简单：单日或短周期可完成
    MEDIUM,     // 中等：数周积累
    HARD,       // 困难：数月坚持
    EXTREME     // 极难：长期坚持
}

data class MilestoneInfo(
    val name: String,
    val targetValue: Int,
    val currentValue: Int,
    val unit: String,
    val difficulty: AchievementDifficulty,
    val isCompleted: Boolean = false
) {
    val progress: Float get() = if (targetValue > 0) (currentValue.toFloat() / targetValue).coerceAtMost(1f) else 0f
    val remaining: Int get() = (targetValue - currentValue).coerceAtLeast(0)
}

data class MilestoneStats(
    val totalMilestones: Int,
    val completedMilestones: Int,
    val milestones: List<MilestoneInfo>
)

data class CategoryDistribution(
    val category: ExerciseCategory,
    val count: Int,
    val totalCheckIns: Int,
    val percentage: Float
)

data class DifficultyDistribution(
    val difficulty: AchievementDifficulty,
    val count: Int,
    val label: String
)

data class TrendDataPoint(
    val label: String,
    val value: Int,
    val date: Long = 0
)

data class ComparisonData(
    val thisPeriod: Int,
    val lastPeriod: Int,
    val periodType: String
) {
    val change: Int get() = thisPeriod - lastPeriod
    val changePercent: Float get() = if (lastPeriod > 0) ((thisPeriod - lastPeriod).toFloat() / lastPeriod * 100) else 0f
    val isPositive: Boolean get() = change >= 0
}

// 统计数据
data class DailyStats(
    val date: Long,
    val totalExercises: Int,
    val completedExercises: Int,
    val totalSets: Int,
    val totalReps: Int
)

data class WeeklyStats(
    val weekStart: Long,
    val totalDays: Int,
    val activeDays: Int,
    val totalCheckIns: Int,
    val totalSets: Int,
    val totalReps: Int
)

data class MonthlyStats(
    val month: Int,
    val year: Int,
    val totalDays: Int,
    val activeDays: Int,
    val totalCheckIns: Int,
    val totalSets: Int,
    val totalReps: Int
)

// 动作媒体资源实体
@Entity(
    tableName = "exercise_media",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["exerciseId"])]
)
data class ExerciseMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseId: Long,
    val type: MediaType,
    val fileName: String,        // 文件名
    val localPath: String? = null,    // 本地存储路径
    val remoteUrl: String? = null,    // 远程URL（未来扩展云存储）
    val thumbnailPath: String? = null, // 缩略图路径（视频）
    val orderIndex: Int = 0,      // 排序索引
    val description: String? = null, // 媒体描述
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Long = 1,
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val avatarPath: String? = null,
    val birthDate: Long? = null,
    val gender: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_edit_history")
data class ProfileEditHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fieldName: String,
    val oldValue: String?,
    val newValue: String?,
    val editedAt: Long = System.currentTimeMillis()
)

enum class BodyMetricType {
    WEIGHT,       // 体重 (kg)
    HEIGHT,       // 身高 (cm)
    BMI,          // 身体质量指数
    BODY_FAT,     // 体脂率 (%)
    MUSCLE_MASS,  // 肌肉量 (kg)
    BMR,          // 基础代谢率 (kcal)
    STEPS,        // 步数
    SLEEP,        // 睡眠时长 (小时)
    HEART_RATE,   // 心率 (bpm)
    BLOOD_PRESSURE_SYSTOLIC,  // 收缩压 (mmHg)
    BLOOD_PRESSURE_DIASTOLIC  // 舒张压 (mmHg)
}

@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: BodyMetricType,
    val value: Float,
    val unit: String,
    val recordDate: Long,
    val source: String = "manual",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class BodyMetricWithRange(
    val metric: BodyMetric,
    val normalMin: Float?,
    val normalMax: Float?,
    val isNormal: Boolean
)

data class BodyMetricTrend(
    val type: BodyMetricType,
    val unit: String,
    val dataPoints: List<BodyMetricDataPoint>,
    val normalMin: Float?,
    val normalMax: Float?,
    val latestValue: Float?,
    val change: Float?,
    val changePercent: Float?
)

data class BodyMetricDataPoint(
    val date: Long,
    val value: Float
)

data class NormalRange(
    val type: BodyMetricType,
    val min: Float,
    val max: Float,
    val unit: String
) {
    companion object {
        val NORMAL_RANGES = listOf(
            NormalRange(BodyMetricType.WEIGHT, 45f, 90f, "kg"),
            NormalRange(BodyMetricType.HEIGHT, 150f, 200f, "cm"),
            NormalRange(BodyMetricType.BMI, 18.5f, 24.9f, ""),
            NormalRange(BodyMetricType.BODY_FAT, 10f, 25f, "%"),
            NormalRange(BodyMetricType.MUSCLE_MASS, 25f, 40f, "kg"),
            NormalRange(BodyMetricType.BMR, 1200f, 2000f, "kcal"),
            NormalRange(BodyMetricType.STEPS, 6000f, 10000f, "步"),
            NormalRange(BodyMetricType.SLEEP, 7f, 9f, "小时"),
            NormalRange(BodyMetricType.HEART_RATE, 60f, 100f, "bpm"),
            NormalRange(BodyMetricType.BLOOD_PRESSURE_SYSTOLIC, 90f, 140f, "mmHg"),
            NormalRange(BodyMetricType.BLOOD_PRESSURE_DIASTOLIC, 60f, 90f, "mmHg")
        )

        fun getRange(type: BodyMetricType): NormalRange? {
            return NORMAL_RANGES.find { it.type == type }
        }
    }
}
