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
        // 只在首次启动时添加示例数据
        if (prefs.getBoolean("sample_data_initialized", false)) {
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
            
            // 标记已初始化
            prefs.edit().putBoolean("sample_data_initialized", true).apply()
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
