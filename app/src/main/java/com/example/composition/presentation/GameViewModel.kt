package com.example.composition.presentation

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.composition.R
import com.example.composition.data.GameRepositoryImpl
import com.example.composition.domain.entity.GameResult
import com.example.composition.domain.entity.GameSettings
import com.example.composition.domain.entity.Level
import com.example.composition.domain.entity.Question
import com.example.composition.domain.usecases.GenerateQuestionUseCase
import com.example.composition.domain.usecases.GetGameSettingsUseCase

class GameViewModel(
    private val application: Application,
    private val level: Level,
) : ViewModel() {

    private lateinit var gameSettings: GameSettings

    private val repository = GameRepositoryImpl


    private val generateQuestionUseCase = GenerateQuestionUseCase(repository)
    private val getGameSettingsUseCase = GetGameSettingsUseCase(repository)

    private var timer: CountDownTimer? = null

    private var countOfRightAnswer = 0
    private var countOfQuestion = 0

    private val _percentOfRightAnswer = MutableLiveData<Int>()
    val percentOfRightAnswer: LiveData<Int>
        get() = _percentOfRightAnswer

    private val _progressAnswer = MutableLiveData<String>()
    val progressAnswer: LiveData<String>
        get() = _progressAnswer

    private val _formattedTime = MutableLiveData<String>()
    val formattedTimer: LiveData<String>
        get() = _formattedTime

    private val _question = MutableLiveData<Question>()
    val question: LiveData<Question>
        get() = _question

    private val _enoughCountOfRightAnswer = MutableLiveData<Boolean>()
    val enoughCountOfRightAnswer: LiveData<Boolean>
        get() = _enoughCountOfRightAnswer

    private val _enoughPercentOfRightAnswer = MutableLiveData<Boolean>()
    val enoughPercentOfRightAnswer: LiveData<Boolean>
        get() = _enoughPercentOfRightAnswer

    private val _minPercent = MutableLiveData<Int>()
    val minPercent: LiveData<Int>
        get() = _minPercent

    private val _gameResult = MutableLiveData<GameResult>()
    val gameResult: LiveData<GameResult>
        get() = _gameResult

    init {
        startGame()
    }

    private fun startGame() {
        getGameSettings()
        startTimer()
        generateQuestion()
        updateProgress()
    }

    fun chooseAnswer(number: Int) {
        checkAnswer(number)
        updateProgress()
        generateQuestion()
    }

    private fun updateProgress() {
        val percent = calculatePercentOfRightAnswer()
        _percentOfRightAnswer.value = percent
        _progressAnswer.value = String.format(
            application.resources.getString(R.string.progress_answers),
            countOfRightAnswer,
            gameSettings.minCountOfRightAnswers
        )
        _enoughCountOfRightAnswer.value = countOfRightAnswer >= gameSettings.minCountOfRightAnswers
        _enoughPercentOfRightAnswer.value = percent >= gameSettings.minPercentOfRightAnswers
    }

    private fun calculatePercentOfRightAnswer(): Int {
        if (countOfQuestion == 0) {
            return 0
        }
        return ((countOfRightAnswer / countOfQuestion.toDouble()) * 100).toInt()
    }

    private fun checkAnswer(number: Int) {
        val rightAnswer = _question.value?.rightAnswer
        if (number == rightAnswer) {
            countOfRightAnswer++
        }
        countOfQuestion++
    }

    private fun getGameSettings() {
        this.gameSettings = getGameSettingsUseCase(level)
        _minPercent.value = gameSettings.minPercentOfRightAnswers
    }

    private fun startTimer() {
        timer = object : CountDownTimer(
            gameSettings.gameTimeInSecond * MILLIS_IN_SECONDS, MILLIS_IN_SECONDS
        ) {
            override fun onTick(millisUtilFinished: Long) {
                _formattedTime.value = formatTime(millisUtilFinished)
            }

            override fun onFinish() {
                finishGame()
            }
        }
        timer?.start()
    }

    private fun formatTime(millisUtilFinished: Long): String {
        val seconds = millisUtilFinished / MILLIS_IN_SECONDS
        val minutes = seconds / SECOND_IN_MINUTES
        val leftSecond = seconds - (minutes - SECOND_IN_MINUTES)
        return String.format("%02d:%02d", minutes, leftSecond)
    }

    override fun onCleared() {
        super.onCleared()
        timer?.cancel()
    }

    private fun generateQuestion() {
        _question.value = generateQuestionUseCase(gameSettings.maxSumValue)
    }

    private fun finishGame() {
        _gameResult.value = GameResult(
            winner = enoughCountOfRightAnswer.value == true
                    && enoughPercentOfRightAnswer.value == true,
            countOfRightAnswers = countOfRightAnswer,
            countOfQuestions = countOfQuestion,
            gameSettings = gameSettings
        )
    }

    companion object {
        private const val MILLIS_IN_SECONDS = 1000L
        private const val SECOND_IN_MINUTES = 60
    }

}