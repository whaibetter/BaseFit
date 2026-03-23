package com.basefit.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Build
import com.basefit.app.data.entity.*
import com.basefit.app.data.repository.FitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class BaseFitApp : Application() {
    
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("basefit_prefs", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initSampleDataIfNeeded()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "checkin_reminder",
                "打卡提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "健身打卡提醒通知"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun initSampleDataIfNeeded() {
        // 检查数据库版本，如果版本不匹配则重新初始化
        val dbVersion = prefs.getInt("db_version", 0)
        val currentDbVersion = 2
        
        if (dbVersion >= currentDbVersion && prefs.getBoolean("sample_data_initialized", false)) {
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            val repository = FitRepository.getRepository(this@BaseFitApp)
            
            // 添加示例动作
            val pushUpId = repository.insertExercise(
                Exercise(name = "俯卧撑", category = ExerciseCategory.BODYWEIGHT)
            )
            val squatId = repository.insertExercise(
                Exercise(name = "深蹲", category = ExerciseCategory.BODYWEIGHT)
            )
            val pullUpId = repository.insertExercise(
                Exercise(name = "引体向上", category = ExerciseCategory.STRENGTH)
            )
            val plankId = repository.insertExercise(
                Exercise(name = "平板支撑", category = ExerciseCategory.BODYWEIGHT)
            )
            val runId = repository.insertExercise(
                Exercise(name = "跑步", category = ExerciseCategory.CARDIO)
            )
            val dumbbellCurlId = repository.insertExercise(
                Exercise(name = "哑铃弯举", category = ExerciseCategory.STRENGTH)
            )
            
            // 添加周计划
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_WEEK)
            val adjustedToday = if (today == Calendar.SUNDAY) 7 else today - 1
            
            // 周一到周五的计划
            for (day in 1..5) {
                repository.insertWeekPlan(
                    WeekPlan(
                        exerciseId = pushUpId,
                        dayOfWeek = day,
                        targetSets = 3,
                        targetReps = 15
                    )
                )
                repository.insertWeekPlan(
                    WeekPlan(
                        exerciseId = squatId,
                        dayOfWeek = day,
                        targetSets = 3,
                        targetReps = 20
                    )
                )
            }
            
            // 周三、周五加强训练
            repository.insertWeekPlan(
                WeekPlan(
                    exerciseId = pullUpId,
                    dayOfWeek = 3,
                    targetSets = 4,
                    targetReps = 8
                )
            )
            repository.insertWeekPlan(
                WeekPlan(
                    exerciseId = pullUpId,
                    dayOfWeek = 5,
                    targetSets = 4,
                    targetReps = 10
                )
            )
            
            // 添加挑战计划 - 30天挑战
            val now = System.currentTimeMillis()
            val startDate = now - 10 * 24 * 60 * 60 * 1000 // 10天前开始
            val endDate = now + 20 * 24 * 60 * 60 * 1000 // 20天后结束
            
            repository.insertChallengePlan(
                ChallengePlan(
                    exerciseId = plankId,
                    name = "30天平板支撑挑战",
                    startDate = startDate,
                    endDate = endDate,
                    targetTotalReps = 90, // 总目标90次
                    targetSets = 3,
                    targetReps = 1
                )
            )
            
            // 添加一些历史打卡记录
            for (i in 1..7) {
                val recordDate = now - i * 24 * 60 * 60 * 1000
                val dayStart = getDayStart(recordDate)

                repository.insertCheckIn(
                    CheckIn(
                        exerciseId = pushUpId,
                        date = dayStart,
                        completedSets = 3,
                        completedReps = (12..18).random(),
                        notes = if (i % 2 == 0) "感觉不错！" else null
                    )
                )

                repository.insertCheckIn(
                    CheckIn(
                        exerciseId = squatId,
                        date = dayStart,
                        completedSets = 3,
                        completedReps = (18..22).random()
                    )
                )

                if (i % 2 == 0) {
                    repository.insertCheckIn(
                        CheckIn(
                            exerciseId = runId,
                            date = dayStart,
                            completedSets = 1,
                            completedReps = 1,
                            durationMinutes = (20..35).random()
                        )
                    )
                }
            }

            // 添加用户个人资料
            repository.saveUserProfile(
                UserProfile(
                    id = 1,
                    name = "健身爱好者",
                    phone = "138****8888",
                    email = "fitness@example.com",
                    gender = "male",
                    birthDate = 946656000000, // 2000-01-01
                    avatarPath = null,
                    updatedAt = now
                )
            )

            // 添加身体指标数据
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
            val weightBase = 70f
            val bodyFatBase = 18f
            val stepsBase = 8000

            for (dayOffset in 0..30) {
                val recordDate = now - dayOffset * 24 * 60 * 60 * 1000

                // 体重数据 (波动下降)
                repository.saveBodyMetric(
                    BodyMetric(
                        type = BodyMetricType.WEIGHT,
                        value = weightBase - (30 - dayOffset) * 0.1f + (Math.random() * 2 - 1).toFloat(),
                        unit = "kg",
                        recordDate = recordDate,
                        source = "manual",
                        notes = null
                    )
                )

                // 体脂率数据 (缓慢下降)
                repository.saveBodyMetric(
                    BodyMetric(
                        type = BodyMetricType.BODY_FAT,
                        value = bodyFatBase - (30 - dayOffset) * 0.08f + (Math.random() * 1 - 0.5).toFloat(),
                        unit = "%",
                        recordDate = recordDate,
                        source = "manual",
                        notes = null
                    )
                )

                // 步数数据 (每天波动)
                if (dayOffset % 7 != 6) { // 非周日
                    repository.saveBodyMetric(
                        BodyMetric(
                            type = BodyMetricType.STEPS,
                            value = (stepsBase + (Math.random() * 4000 - 2000).toInt()).toFloat(),
                            unit = "步",
                            recordDate = recordDate,
                            source = "manual",
                            notes = null
                        )
                    )
                }

                // 睡眠数据 (周末多睡)
                repository.saveBodyMetric(
                    BodyMetric(
                        type = BodyMetricType.SLEEP,
                        value = (7.5f + (Math.random() * 1.5 - 0.75).toFloat() + if (dayOffset % 7 >= 5) 1f else 0f),
                        unit = "小时",
                        recordDate = recordDate,
                        source = "manual",
                        notes = null
                    )
                )

                // 心率数据
                repository.saveBodyMetric(
                    BodyMetric(
                        type = BodyMetricType.HEART_RATE,
                        value = (68 + Math.random() * 12 - 6).toFloat(),
                        unit = "bpm",
                        recordDate = recordDate,
                        source = "manual",
                        notes = null
                    )
                )

                // 每周记录一次肌肉量和BMI
                if (dayOffset % 7 == 0) {
                    repository.saveBodyMetric(
                        BodyMetric(
                            type = BodyMetricType.MUSCLE_MASS,
                            value = (35 + (30 - dayOffset) * 0.05f + (Math.random() * 1 - 0.5).toFloat()),
                            unit = "kg",
                            recordDate = recordDate,
                            source = "manual",
                            notes = null
                        )
                    )
                }
            }

            // 标记已初始化
            prefs.edit()
                .putBoolean("sample_data_initialized", true)
                .putInt("db_version", currentDbVersion)
                .apply()
        }
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
}
