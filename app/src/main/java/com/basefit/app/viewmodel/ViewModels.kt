package com.basefit.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.basefit.app.data.entity.*
import com.basefit.app.data.repository.FitRepository
import com.basefit.app.data.repository.TodayPlanItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class HomeState(
    val todayPlans: List<TodayPlanItem> = emptyList(),
    val activeChallenges: List<ChallengePlanWithExercise> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

data class ExerciseState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true
)

data class PlanState(
    val weekPlans: List<WeekPlanWithExercise> = emptyList(),
    val challenges: List<ChallengePlanWithExercise> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true
)

data class WeekPlanWithExercise(
    val weekPlan: WeekPlan,
    val exercise: Exercise
)

data class ChallengePlanWithExercise(
    val challenge: ChallengePlan,
    val exercise: Exercise,
    val completedReps: Int = 0 // 已完成次数
)

data class RecordState(
    val checkIns: List<CheckInWithExercise> = emptyList(),
    val calendarData: Map<Long, Int> = emptyMap(),
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedDate: Long? = null,
    val isLoading: Boolean = true
)

data class CheckInWithExercise(
    val checkIn: CheckIn,
    val exercise: Exercise
)

data class StatsState(
    val achievements: List<Achievement> = emptyList(),
    val totalCheckInDays: Int = 0,
    val weeklyStats: List<WeeklyStats> = emptyList(),
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitRepository.getRepository(application)
    
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    init {
        loadTodayPlans()
    }

    fun loadTodayPlans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val plans = repository.getTodayPlans()
            
            // 加载进行中的挑战
            val challenges = repository.getAllActiveChallenges().first()
            val exercises = repository.getAllActiveExercises().first()
            val now = System.currentTimeMillis()
            
            val activeChallenges = challenges
                .filter { it.isActive && now in it.startDate..it.endDate }
                .mapNotNull { challenge ->
                    val exercise = exercises.find { it.id == challenge.exerciseId }
                    if (exercise != null) {
                        val completedReps = repository.getChallengeProgress(
                            exerciseId = challenge.exerciseId,
                            startDate = challenge.startDate,
                            endDate = challenge.endDate + 24 * 60 * 60 * 1000 - 1
                        )
                        ChallengePlanWithExercise(challenge, exercise, completedReps)
                    } else null
                }
            
            _state.update { 
                it.copy(
                    todayPlans = plans,
                    activeChallenges = activeChallenges,
                    completedCount = plans.count { p -> p.isCompleted },
                    totalCount = plans.size,
                    isLoading = false
                )
            }
        }
    }

    fun quickCheckIn(exerciseId: Long, targetSets: Int, targetReps: Int) {
        viewModelScope.launch {
            val today = getDayStart(System.currentTimeMillis())
            repository.insertCheckIn(
                CheckIn(
                    exerciseId = exerciseId,
                    date = today,
                    completedSets = targetSets,
                    completedReps = targetReps
                )
            )
            loadTodayPlans()
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

class ExerciseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitRepository.getRepository(application)
    
    private val _state = MutableStateFlow(ExerciseState())
    val state: StateFlow<ExerciseState> = _state

    init {
        loadExercises()
    }

    fun loadExercises() {
        viewModelScope.launch {
            repository.getAllActiveExercises()
                .catch { _state.update { it.copy(isLoading = false) } }
                .collect { exercises ->
                    _state.update { it.copy(exercises = exercises, isLoading = false) }
                }
        }
    }

    fun addExercise(name: String, category: ExerciseCategory) {
        viewModelScope.launch {
            repository.insertExercise(
                Exercise(name = name.trim(), category = category)
            )
        }
    }

    suspend fun addExerciseWithMedia(
        name: String,
        category: ExerciseCategory,
        mediaItems: List<MediaItem>,
        mediaStorage: com.basefit.app.data.storage.MediaStorage
    ): Long {
        // 先插入动作
        val exerciseId = repository.insertExercise(
            Exercise(name = name.trim(), category = category)
        )
        
        // 再保存媒体文件
        mediaItems.forEachIndexed { index, item ->
            val result = mediaStorage.saveMedia(exerciseId, item.uri, item.type)
            
            result.getOrNull()?.let { mediaResource ->
                repository.insertMedia(
                    ExerciseMedia(
                        exerciseId = exerciseId,
                        type = item.type,
                        fileName = mediaResource.fileName,
                        localPath = mediaResource.localPath,
                        thumbnailPath = mediaResource.thumbnailPath,
                        orderIndex = index
                    )
                )
            }
        }
        
        return exerciseId
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }
}

// 临时媒体项（用于UI展示）
data class MediaItem(
    val uri: Uri,
    val type: com.basefit.app.data.entity.MediaType
)

class PlanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitRepository.getRepository(application)
    
    private val _state = MutableStateFlow(PlanState())
    val state: StateFlow<PlanState> = _state

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            // Load exercises first
            val exercises = repository.getAllActiveExercises().first()
            
            // Load week plans
            val allWeekPlans = mutableListOf<WeekPlanWithExercise>()
            for (day in 1..7) {
                val plans = repository.getWeekPlansForDay(day).first()
                plans.forEach { plan ->
                    val exercise = exercises.find { it.id == plan.exerciseId }
                    if (exercise != null) {
                        allWeekPlans.add(WeekPlanWithExercise(plan, exercise))
                    }
                }
            }

            // Load challenges
            val challenges = repository.getAllActiveChallenges().first()
            val challengesWithExercise = challenges.mapNotNull { challenge ->
                val exercise = exercises.find { it.id == challenge.exerciseId }
                if (exercise != null) {
                    // 获取该动作在挑战期间内的已完成次数
                    val completedReps = repository.getChallengeProgress(
                        exerciseId = challenge.exerciseId,
                        startDate = challenge.startDate,
                        endDate = challenge.endDate + 24 * 60 * 60 * 1000 - 1 // 包含结束日期当天
                    )
                    ChallengePlanWithExercise(challenge, exercise, completedReps)
                } else null
            }

            _state.update { 
                it.copy(
                    weekPlans = allWeekPlans.sortedBy { p -> p.weekPlan.dayOfWeek },
                    challenges = challengesWithExercise,
                    exercises = exercises,
                    isLoading = false
                )
            }
        }
    }

    fun addWeekPlan(exerciseId: Long, dayOfWeek: Int, targetSets: Int, targetReps: Int) {
        viewModelScope.launch {
            repository.insertWeekPlan(
                WeekPlan(
                    exerciseId = exerciseId,
                    dayOfWeek = dayOfWeek,
                    targetSets = targetSets,
                    targetReps = targetReps
                )
            )
            loadData()
        }
    }

    fun deleteWeekPlan(plan: WeekPlan) {
        viewModelScope.launch {
            repository.deleteWeekPlan(plan)
            loadData()
        }
    }

    fun addChallenge(
        exerciseId: Long,
        name: String,
        startDate: Long,
        endDate: Long,
        targetTotalReps: Int,
        targetSets: Int,
        targetReps: Int
    ) {
        viewModelScope.launch {
            repository.insertChallengePlan(
                ChallengePlan(
                    exerciseId = exerciseId,
                    name = name,
                    startDate = startDate,
                    endDate = endDate,
                    targetTotalReps = targetTotalReps,
                    targetSets = targetSets,
                    targetReps = targetReps
                )
            )
            loadData()
        }
    }

    fun deleteChallenge(challenge: ChallengePlan) {
        viewModelScope.launch {
            repository.deleteChallengePlan(challenge)
            loadData()
        }
    }
}

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitRepository.getRepository(application)
    
    private val _state = MutableStateFlow(RecordState())
    val state: StateFlow<RecordState> = _state

    init {
        loadCurrentMonth()
    }

    fun loadCurrentMonth() {
        viewModelScope.launch {
            loadMonth(_state.value.currentYear, _state.value.currentMonth)
        }
    }

    fun loadMonth(year: Int, month: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val calendarData = repository.getCalendarData(year, month)
            val exercises = repository.getAllActiveExercises().first()
            
            // Get all check-ins for the month
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val monthEnd = calendar.timeInMillis - 1

            val allCheckIns = checkNotNull(
                repository.getAllCheckIns().first()
                    .filter { it.date in monthStart..monthEnd }
            )

            val checkInsWithExercise = allCheckIns.mapNotNull { checkIn ->
                val exercise = exercises.find { it.id == checkIn.exerciseId }
                if (exercise != null) CheckInWithExercise(checkIn, exercise) else null
            }.sortedByDescending { it.checkIn.date }

            _state.update {
                it.copy(
                    calendarData = calendarData,
                    checkIns = checkInsWithExercise,
                    currentYear = year,
                    currentMonth = month,
                    isLoading = false
                )
            }
        }
    }

    fun previousMonth() {
        val newState = if (_state.value.currentMonth == 1) {
            _state.value.copy(currentMonth = 12, currentYear = _state.value.currentYear - 1)
        } else {
            _state.value.copy(currentMonth = _state.value.currentMonth - 1)
        }
        _state.update { newState }
        loadMonth(newState.currentYear, newState.currentMonth)
    }

    fun nextMonth() {
        val newState = if (_state.value.currentMonth == 12) {
            _state.value.copy(currentMonth = 1, currentYear = _state.value.currentYear + 1)
        } else {
            _state.value.copy(currentMonth = _state.value.currentMonth + 1)
        }
        _state.update { newState }
        loadMonth(newState.currentYear, newState.currentMonth)
    }
    
    fun selectDate(date: Long?) {
        _state.update { it.copy(selectedDate = date) }
    }
}

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitRepository.getRepository(application)
    
    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            val achievements = repository.getAchievements()
            val weeklyStats = repository.getWeeklyStats()
            
            repository.getTotalCheckInDays()
                .catch { emit(0) }
                .collect { days ->
                    _state.update {
                        it.copy(
                            achievements = achievements,
                            totalCheckInDays = days,
                            weeklyStats = weeklyStats,
                            isLoading = false
                        )
                    }
                }
        }
    }
}

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FitRepository.getRepository(application)

    fun saveCheckIn(
        exerciseId: Long,
        date: Long,
        completedSets: Int,
        completedReps: Int,
        weight: Float?,
        durationMinutes: Int?,
        notes: String?
    ) {
        viewModelScope.launch {
            repository.insertCheckIn(
                CheckIn(
                    exerciseId = exerciseId,
                    date = date,
                    completedSets = completedSets,
                    completedReps = completedReps,
                    weight = weight,
                    durationMinutes = durationMinutes,
                    notes = notes
                )
            )
        }
    }

    suspend fun getExercise(id: Long): Exercise? = repository.getExerciseById(id)
}
