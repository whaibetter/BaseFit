package com.basefit.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// 动作分类枚举
enum class ExerciseCategory {
    BODYWEIGHT,  // 自重训练
    STRENGTH,    // 力量训练
    CARDIO       // 有氧运动
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
    val totalCheckIns: Int,
    val totalSets: Int,
    val totalReps: Int,
    val maxWeight: Float?,
    val maxDuration: Int?,
    val currentStreak: Int,
    val bestStreak: Int
)

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
