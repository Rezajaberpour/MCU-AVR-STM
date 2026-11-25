package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiAgentPersona
import com.example.data.model.ChatMessageItem
import com.example.data.model.McuFamily
import com.example.data.model.PromptTargetAi
import com.example.data.model.TargetSimulator
import com.example.data.repository.GeminiEngineeringRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sin

sealed interface AiTaskState {
    data object Idle : AiTaskState
    data object Loading : AiTaskState
    data class Streaming(val text: String) : AiTaskState
    data class Success(val text: String) : AiTaskState
    data class Error(val message: String) : AiTaskState
}

data class HmiTelemetryPoint(
    val timestampLabel: String,
    val voltageV: Float,
    val currentMa: Float,
    val tempC: Float,
    val adcRaw: Int,
    val uartFrame: String
)

data class EngineeringStudioUiState(
    val selectedFamily: McuFamily = McuFamily.AVR,
    val selectedModel: String = McuFamily.AVR.defaultModels.first(),
    val selectedClock: String = McuFamily.AVR.defaultClocks.first(),
    val selectedPeripheral: String = "Timer1 PWM + UART",
    val selectedSimulator: TargetSimulator = TargetSimulator.PROTEUS,
    val selectedPromptTarget: PromptTargetAi = PromptTargetAi.GEMINI,
    val taskSpecification: String = "راه‌اندازی PWM فرکانس 20kHz برای کنترل دور موتور DC همراه با ارسال سرعت روی UART 115200",
    val generatedAutoPrompt: String = "",
    val codeInputForAnalysis: String = """#include <avr/io.h>
#include <util/delay.h>

int main(void) {
    DDRB |= (1 << PB1);
    while (1) {
        PORTB ^= (1 << PB1);
        _delay_ms(500); // Blocking delay increases power consumption
    }
}""",
    val codeLabAiState: AiTaskState = AiTaskState.Idle,
    val selectedPersona: AiAgentPersona = AiAgentPersona.CO_ANALYST,
    val chatMessages: List<ChatMessageItem> = listOf(
        ChatMessageItem(
            id = "welcome",
            isUser = false,
            senderLabel = "ایجنت ترکیبی سخت‌افزار و نرم‌افزار",
            content = "سلام مهندس! من دستیار تخصصی AVR، STM32، STM8 و Holtek هستم. سوال یا کد یا مشکل سخت‌افزاری خود را بنویسید تا با تحلیل عمیق پاسخ دهم."
        )
    ),
    val isChatStreaming: Boolean = false,
    val chatError: String? = null,
    val visionAnalysisState: AiTaskState = AiTaskState.Idle,
    val standardsQuery: String = "",
    val standardsAiState: AiTaskState = AiTaskState.Idle,
    // HMI & Python Telemetry Simulator State
    val hmiRunning: Boolean = true,
    val pwmDutyPercent: Int = 45,
    val loadResistanceOhms: Int = 120,
    val samplingRateHz: Int = 10,
    val baudRate: Int = 115200,
    val lowPowerSleepActive: Boolean = false,
    val telemetryHistory: List<HmiTelemetryPoint> = emptyList(),
    val hmiBundleAiState: AiTaskState = AiTaskState.Idle
)

class EngineeringAiViewModel(
    private val aiRepository: GeminiEngineeringRepository = GeminiEngineeringRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EngineeringStudioUiState())
    val uiState: StateFlow<EngineeringStudioUiState> = _uiState.asStateFlow()

    private var codeLabJob: Job? = null
    private var chatJob: Job? = null
    private var standardsJob: Job? = null
    private var hmiBundleJob: Job? = null
    private var telemetryStep = 0

    init {
        refreshAutoPrompt()
        startHmiTelemetryLoop()
    }

    fun selectFamily(family: McuFamily) {
        _uiState.update {
            it.copy(
                selectedFamily = family,
                selectedModel = family.defaultModels.first(),
                selectedClock = family.defaultClocks.first()
            )
        }
        refreshAutoPrompt()
    }

    fun selectModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
        refreshAutoPrompt()
    }

    fun selectClock(clock: String) {
        _uiState.update { it.copy(selectedClock = clock) }
        refreshAutoPrompt()
    }

    fun updatePeripheral(peripheral: String) {
        _uiState.update { it.copy(selectedPeripheral = peripheral) }
        refreshAutoPrompt()
    }

    fun selectSimulator(sim: TargetSimulator) {
        _uiState.update { it.copy(selectedSimulator = sim) }
        refreshAutoPrompt()
    }

    fun selectPromptTarget(target: PromptTargetAi) {
        _uiState.update { it.copy(selectedPromptTarget = target) }
        refreshAutoPrompt()
    }

    fun updateTaskSpecification(spec: String) {
        _uiState.update { it.copy(taskSpecification = spec) }
        refreshAutoPrompt()
    }

    fun updateCodeInput(code: String) {
        _uiState.update { it.copy(codeInputForAnalysis = code) }
    }

    fun selectPersona(persona: AiAgentPersona) {
        _uiState.update { it.copy(selectedPersona = persona) }
    }

    fun refreshAutoPrompt() {
        val s = _uiState.value
        val targetNotes = when (s.selectedPromptTarget) {
            PromptTargetAi.GEMINI -> "Structured Hardware+Firmware Deep Thinking (Register Table + Low-Power + Simulation Testbench)"
            PromptTargetAi.DEEPSEEK -> "Focus on bare-metal C99 register bitwise efficiency, zero HAL bloat, and exact ISR timing"
            PromptTargetAi.CHATGPT -> "Provide step-by-step schematic wiring list, Atmel/CubeIDE setup, and commented main.c"
            PromptTargetAi.CLAUDE -> "Deliver defensive C firmware with static checks, ring-buffer UART, and MISRA-friendly structure"
        }
        val base = aiRepository.buildSimulationPrompt(
            mcuFamily = s.selectedFamily,
            mcuModel = s.selectedModel,
            clock = s.selectedClock,
            peripheral = s.selectedPeripheral,
            simulator = s.selectedSimulator,
            userSpec = s.taskSpecification
        )
        val fullPrompt = "$base\n[TARGET_AI_STYLE (${s.selectedPromptTarget.badge})]: $targetNotes"
        _uiState.update { it.copy(generatedAutoPrompt = fullPrompt) }
    }

    fun runGeneratedPromptWithGemini() {
        val prompt = _uiState.value.generatedAutoPrompt
        if (prompt.isBlank()) return
        launchDeepStream(prompt) { state ->
            _uiState.update { it.copy(codeLabAiState = state) }
        }
    }

    fun runCodeOptimization(mode: String) {
        val s = _uiState.value
        if (s.codeInputForAnalysis.isBlank()) {
            _uiState.update { it.copy(codeLabAiState = AiTaskState.Error("لطفاً ابتدا کد میکروکنترلر را وارد کنید.")) }
            return
        }
        val prompt = """
            شما متخصص بهینه‌سازی سیستم‌های نهفته و تحلیل همزمان سخت‌افزار/نرم‌افزار هستید.
            میکروکنترلر: ${s.selectedFamily.displayName} (${s.selectedModel} @ ${s.selectedClock})
            هدف عملیات: $mode
            شبیه‌ساز هدف: ${s.selectedSimulator.label}

            کد ورودی:
            ```c
            ${s.codeInputForAnalysis}
            ```

            لطفاً به زبان فارسی دقیق مهندسی موارد زیر را ارائه دهید:
            1. **تحلیل باگ‌ها، مصرف انرژی و ریسک‌های سخت‌افزاری/نرم‌افزاری** (شناسایی تاخیرهای مسدودکننده، تداخل وقفه، خطای رجیستر یا سطح ولتاژ).
            2. **کد بازنویسی‌شده و بهینه‌سازی‌شده کامل** (تایمر غیرمسدودکننده / Sleep Mode / رجیستر کاهش توان).
            3. **چک‌لیست شبیه‌سازی در ${s.selectedSimulator.label}** برای کاهش زمان تست روی سخت‌افزار واقعی.
        """.trimIndent()
        launchDeepStream(prompt) { state ->
            _uiState.update { it.copy(codeLabAiState = state) }
        }
    }

    private fun launchDeepStream(prompt: String, onState: (AiTaskState) -> Unit) {
        codeLabJob?.cancel()
        codeLabJob = viewModelScope.launch {
            onState(AiTaskState.Loading)
            try {
                var latest = ""
                aiRepository.streamDeepEngineeringQuery(prompt).collect { text ->
                    latest = text
                    onState(AiTaskState.Streaming(text))
                }
                onState(AiTaskState.Success(latest))
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Deep stream timed out", e)
                onState(AiTaskState.Error("زمان پاسخگویی (۶۰ ثانیه) به پایان رسید. دوباره تلاش کنید."))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Deep stream failed", e)
                onState(AiTaskState.Error(e.localizedMessage ?: "خطا در ارتباط با جمنای"))
            }
        }
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _uiState.value.isChatStreaming) return

        val s = _uiState.value
        val historySnapshot = s.chatMessages.map { it.isUser to it.content }
        val userMsg = ChatMessageItem(
            id = UUID.randomUUID().toString(),
            isUser = true,
            senderLabel = "شما",
            content = trimmed
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessageItem(
            id = assistantId,
            isUser = false,
            senderLabel = s.selectedPersona.titleFa,
            content = "...",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMsg + assistantPlaceholder,
                isChatStreaming = true,
                chatError = null
            )
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                aiRepository.streamAgentChat(
                    persona = s.selectedPersona,
                    mcuFamily = s.selectedFamily,
                    mcuModel = s.selectedModel,
                    history = historySnapshot,
                    userMessage = trimmed
                ).collect { partial ->
                    _uiState.update { state ->
                        state.copy(
                            chatMessages = state.chatMessages.map { msg ->
                                if (msg.id == assistantId) msg.copy(content = partial, isStreaming = true) else msg
                            }
                        )
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        isChatStreaming = false,
                        chatMessages = state.chatMessages.map { msg ->
                            if (msg.id == assistantId) msg.copy(isStreaming = false) else msg
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errText = if (e is TimeoutCancellationException) {
                    "پاسخ ایجنت زمان‌بر شد (بیش از ۶۰ ثانیه)."
                } else {
                    e.localizedMessage ?: "خطا در گفتگو با ایجنت هوشمند"
                }
                Log.e(TAG, "Chat error", e)
                _uiState.update { state ->
                    state.copy(
                        isChatStreaming = false,
                        chatError = errText,
                        chatMessages = state.chatMessages.filterNot { it.id == assistantId && it.content == "..." }
                    )
                }
            }
        }
    }

    fun analyzeSchematic(
        bitmap: Bitmap,
        mcuFamily: String,
        analysisMode: String,
        circuitNotes: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(visionAnalysisState = AiTaskState.Loading) }
            try {
                val result = aiRepository.analyzeSchematicImage(
                    bitmap = bitmap,
                    mcuFamily = mcuFamily,
                    analysisMode = analysisMode,
                    circuitNotes = circuitNotes
                )
                _uiState.update { it.copy(visionAnalysisState = AiTaskState.Success(result)) }
            } catch (e: TimeoutCancellationException) {
                _uiState.update {
                    it.copy(visionAnalysisState = AiTaskState.Error("تحلیل تصویر بیش از ۶۰ ثانیه طول کشید. لطفاً دوباره امتحان کنید."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Vision failed", e)
                _uiState.update {
                    it.copy(visionAnalysisState = AiTaskState.Error(e.localizedMessage ?: "خطا در تحلیل تصویر شماتیک"))
                }
            }
        }
    }

    fun updateStandardsQuery(query: String) {
        _uiState.update { it.copy(standardsQuery = query) }
    }

    fun runDeepStandardSearch(customQuery: String? = null) {
        val query = (customQuery ?: _uiState.value.standardsQuery).trim()
        if (query.isEmpty()) return
        _uiState.update { it.copy(standardsQuery = query) }

        val fullPrompt = """
            شما پایگاه دانش فنی و استانداردهای الکترونیک (دیتاشیت میکروکنترلرهای AVR، STM32، STM8، Holtek و استانداردهای IPC/IEC) هستید.
            پرسش مهندسی کاربر: $query

            با استدلال عمیق، مقادیر دقیق رجیسترها، فرمول‌های محاسباتی، تلرانس‌های الکتریکی، پین‌اوت‌ها و هشدارهای کاربردی (Errata / Application Notes) را به زبان فارسی دقیق ارائه دهید.
        """.trimIndent()

        standardsJob?.cancel()
        standardsJob = viewModelScope.launch {
            _uiState.update { it.copy(standardsAiState = AiTaskState.Loading) }
            try {
                var latest = ""
                aiRepository.streamDeepEngineeringQuery(fullPrompt).collect { partial ->
                    latest = partial
                    _uiState.update { it.copy(standardsAiState = AiTaskState.Streaming(partial)) }
                }
                _uiState.update { it.copy(standardsAiState = AiTaskState.Success(latest)) }
            } catch (e: TimeoutCancellationException) {
                _uiState.update { it.copy(standardsAiState = AiTaskState.Error("مهلت زمانی جستجوی عمیق به پایان رسید.")) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Standards query failed", e)
                _uiState.update {
                    it.copy(standardsAiState = AiTaskState.Error(e.localizedMessage ?: "خطا در جستجوی استاندارد فنی"))
                }
            }
        }
    }

    // HMI & Python Telemetry Simulator Controls
    fun toggleHmiRunning() {
        _uiState.update { it.copy(hmiRunning = !it.hmiRunning) }
    }

    fun setPwmDuty(duty: Int) {
        _uiState.update { it.copy(pwmDutyPercent = duty.coerceIn(0, 100)) }
    }

    fun setLoadResistance(ohms: Int) {
        _uiState.update { it.copy(loadResistanceOhms = ohms.coerceIn(20, 500)) }
    }

    fun setBaudRate(baud: Int) {
        _uiState.update { it.copy(baudRate = baud) }
    }

    fun toggleLowPowerSleep(enabled: Boolean) {
        _uiState.update { it.copy(lowPowerSleepActive = enabled) }
    }

    fun generateHmiPythonBundle() {
        val s = _uiState.value
        val snapshot = s.telemetryHistory.takeLast(3).joinToString(" | ") { it.uartFrame }
            .ifBlank { "\$MCU,PWM=${s.pwmDutyPercent},V=3.30,I=18.2*7A" }

        hmiBundleJob?.cancel()
        hmiBundleJob = viewModelScope.launch {
            _uiState.update { it.copy(hmiBundleAiState = AiTaskState.Loading) }
            try {
                var latest = ""
                aiRepository.streamHmiAndPythonBundle(
                    mcuFamily = s.selectedFamily,
                    mcuModel = s.selectedModel,
                    baudRate = s.baudRate,
                    pwmDuty = s.pwmDutyPercent,
                    adcSamplingHz = s.samplingRateHz,
                    telemetrySnapshot = snapshot
                ).collect { partial ->
                    latest = partial
                    _uiState.update { it.copy(hmiBundleAiState = AiTaskState.Streaming(partial)) }
                }
                _uiState.update { it.copy(hmiBundleAiState = AiTaskState.Success(latest)) }
            } catch (e: TimeoutCancellationException) {
                _uiState.update { it.copy(hmiBundleAiState = AiTaskState.Error("تولید بسته HMI زمان‌بر شد.")) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(hmiBundleAiState = AiTaskState.Error(e.localizedMessage ?: "خطا در تولید کد HMI و پایتون"))
                }
            }
        }
    }

    private fun startHmiTelemetryLoop() {
        viewModelScope.launch {
            while (true) {
                val s = _uiState.value
                if (s.hmiRunning) {
                    telemetryStep++
                    val baseV = if (s.selectedFamily == McuFamily.STM32) 3.30f else 5.00f
                    val ripple = (sin(telemetryStep * 0.45) * 0.03).toFloat()
                    val v = (baseV + ripple).coerceAtLeast(1.8f)
                    val dutyFactor = if (s.lowPowerSleepActive) 0.05f else (s.pwmDutyPercent / 100f)
                    val loadMa = (v / s.loadResistanceOhms.toFloat()) * 1000f * dutyFactor
                    val coreMa = if (s.lowPowerSleepActive) 0.4f else 12.5f
                    val currentMa = coreMa + loadMa
                    val tempC = 27.0f + (currentMa * 0.14f) + (sin(telemetryStep * 0.2) * 0.4).toFloat()
                    val adcRaw = ((dutyFactor * 4095f) + (sin(telemetryStep * 0.6) * 18)).roundToInt().coerceIn(0, 4095)
                    val vStr = "%.2f".format(v)
                    val iStr = "%.1f".format(currentMa)
                    val tStr = "%.1f".format(tempC)
                    val crc = (s.pwmDutyPercent xor adcRaw xor telemetryStep) and 0xFF
                    val frame = "\$${s.selectedFamily.name},PWM=${s.pwmDutyPercent},V=$vStr,I=${iStr}mA,T=${tStr}C,ADC=$adcRaw*${crc.toString(16).uppercase().padStart(2, '0')}"

                    val point = HmiTelemetryPoint(
                        timestampLabel = "T+${telemetryStep}s",
                        voltageV = v,
                        currentMa = currentMa,
                        tempC = tempC,
                        adcRaw = adcRaw,
                        uartFrame = frame
                    )
                    _uiState.update { state ->
                        state.copy(telemetryHistory = (state.telemetryHistory + point).takeLast(24))
                    }
                }
                delay(700L)
            }
        }
    }

    private companion object {
        const val TAG = "EngineeringAiVM"
    }
}
