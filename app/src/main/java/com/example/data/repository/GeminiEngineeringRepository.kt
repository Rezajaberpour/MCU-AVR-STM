package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiAgentPersona
import com.example.data.model.McuFamily
import com.example.data.model.TargetSimulator
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiEngRepo"
private const val AI_TIMEOUT_MS = 60_000L

class GeminiEngineeringRepository {

    // Deep Thinking model (gemini-4-carbon, ThinkingLevel.HIGH, NO maxOutputTokens set)
    private val deepThinkingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            }
        )
    }

    // Image understanding model (gemini-4-neon) for schematic/PCB/circuit analysis
    private val schematicVisionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-neon"
        )
    }

    // Fast model for HMI telemetry anomaly checks & quick script generation
    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    fun streamDeepEngineeringQuery(prompt: String): Flow<String> = flow {
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                deepThinkingModel.generateContentStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("پاسخی از مدل هوشمند دریافت نشد.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Deep thinking stream failed", e)
            throw e
        }
    }

    fun streamAgentChat(
        persona: AiAgentPersona,
        mcuFamily: McuFamily,
        mcuModel: String,
        history: List<Pair<Boolean, String>>,
        userMessage: String
    ): Flow<String> = flow {
        val model = if (persona.useDeepThinking) deepThinkingModel else fastModel
        val systemPreamble = """
            شما "${persona.titleFa}" در محیط تخصصی MCU Studio AI هستید.
            میکروکنترلر هدف فعلی: ${mcuFamily.displayName} (${mcuModel}) | ابزار توسعه: ${mcuFamily.defaultToolchain}.
            پاسخ‌ها را به زبان فارسی دقیق مهندسی، همراه با اصطلاحات استاندارد سخت‌افزار/رجیسترها و بلوک‌های کد تمیز C/C++/Python بنویسید.
            همیشه نکات ایمنی سخت‌افزاری (سطح ولتاژ، Pull-up، تایمر، وقفه ISR و مصرف انرژی) را بررسی کنید.
        """.trimIndent()

        val chatHistory = mutableListOf<Content>()
        chatHistory.add(content("user") { text(systemPreamble) })
        chatHistory.add(content("model") { text("آماده‌ام. پرسش مهندسی درباره ${mcuModel} را بفرمایید.") })
        history.takeLast(8).forEach { (isUser, msg) ->
            chatHistory.add(content(if (isUser) "user" else "model") { text(msg) })
        }

        val chat = model.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        accumulated.append(chunkText)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("مدل پاسخی ارسال نکرد.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Agent chat failed", e)
            throw e
        }
    }

    suspend fun analyzeSchematicImage(
        bitmap: Bitmap,
        mcuFamily: String,
        analysisMode: String,
        circuitNotes: String
    ): String {
        val scaledBitmap = scaleBitmapForPayload(bitmap, maxDim = 1400)
        val prompt = """
            شما مهندس ارشد سخت‌افزار و طراحی برد PCB هستید. تصویر پیوست‌شده (شماتیک مدار، برد PCB یا تست بردبورد) را با دقت کامل بررسی کنید.
            خانواده میکروکنترلر هدف: $mcuFamily
            نوع تحلیل درخواستی: $analysisMode
            توضیحات کاربر: ${circuitNotes.ifBlank { "بررسی کامل مدار، خطاهای سیم‌کشی و پیشنهاد قطعات" }}

            لطفاً گزارش ساختاریافته به زبان فارسی و با سرفصل‌های زیر ارائه دهید:
            1. **تشخیص قطعات و بخش‌های مدار در تصویر**
            2. **عیب‌یابی خودکار و ریسک‌های سخت‌افزاری** (اتصال کوتاه، سطح ولتاژ 3.3V/5V، فقدان خازن دکوپلاژ، مقاومت Pull-up/Pull-down، پین Reset، حفاظت ESD/Flyback)
            3. **پیشنهاد قطعات بهینه و لیست BOM** (شماره قطعه واقعی مثل AMS1117، IRLZ44N، 1N5819، مقاومت و خازن دقیق)
            4. **توصیه‌های بهینه‌سازی PCB و کاهش نویز/مصرف انرژی**
        """.trimIndent()

        return try {
            withTimeout(AI_TIMEOUT_MS) {
                val response = schematicVisionModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(prompt)
                    }
                )
                response.text?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("تحلیل تصویر خروجی خالی برگرداند.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Schematic vision analysis failed", e)
            throw e
        }
    }

    fun streamHmiAndPythonBundle(
        mcuFamily: McuFamily,
        mcuModel: String,
        baudRate: Int,
        pwmDuty: Int,
        adcSamplingHz: Int,
        telemetrySnapshot: String
    ): Flow<String> = flow {
        val prompt = """
            یک بسته تلفیقی کامل شامل موارد زیر برای کنترل بلادرنگ HMI تولید کنید:
            - میکروکنترلر: ${mcuFamily.displayName} ($mcuModel) | BaudRate: $baudRate bps
            - پارامترهای فعلی HMI: PWM=$pwmDuty%, Sampling=$adcSamplingHz Hz
            - آخرین فریم‌های تله‌متری: $telemetrySnapshot

            خروجی شامل 3 بخش دقیق و قابل اجرا باشد:
            1. **کد C میکروکنترلر (UART Frame Parser & Telemetry Sender)** با پروتکل بسته‌بندی ایمن (`$...*CRC\n`) سازگار با ${mcuFamily.defaultToolchain}.
            2. **ساختار فلوچارت Flowcode HMI** (مقادیر Macro، کامپوننت UART/RS232 و نمایشگر Gauge/Chart بلادرنگ).
            3. **اسکریپت پایتون (`pyserial` + `threading` + تحلیل هوشمند خطا)** برای دریافت بلادرنگ داده‌ها، رسم نمودار و ارسال فرمان کنترل PWM.
            توضیحات کوتاه مهندسی را به فارسی و کدها را تمیز ارائه دهید.
        """.trimIndent()

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                fastModel.generateContentStream(prompt).collect { chunk ->
                    val t = chunk.text.orEmpty()
                    if (t.isNotEmpty()) {
                        accumulated.append(t)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("خروجی تولید نشد.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "HMI Python bundle failed", e)
            throw e
        }
    }

    fun buildSimulationPrompt(
        mcuFamily: McuFamily,
        mcuModel: String,
        clock: String,
        peripheral: String,
        simulator: TargetSimulator,
        userSpec: String
    ): String {
        return """
            [ROLE]: Senior Embedded Firmware & Hardware Co-Design Engineer
            [MCU]: ${mcuFamily.displayName} - $mcuModel ($clock, ${mcuFamily.voltageSpec})
            [TOOLCHAIN]: ${mcuFamily.defaultToolchain}
            [PERIPHERAL / MODULE]: $peripheral
            [SIMULATION TARGET]: ${simulator.label}
            [TASK SPECIFICATION]: $userSpec
            [REQUIREMENTS]:
            1. تولید کد C/C++ استاندارد، ماژولار و کاملاً کامپایل‌پذیر بدون توابع مسدودکننده (Non-blocking) در وقفه‌ها.
            2. تنظیم دقیق رجیسترها، Fuse Bit / Option Byte / Clock Tree و تعریف پین‌ها (Pinout Table).
            3. بهینه‌سازی مصرف انرژی (Sleep/Idle mode و خاموش‌کردن پریفرال‌های بلااستفاده).
            4. ارائه پیکربندی دقیق شبیه‌سازی در ${simulator.label} برای کاهش زمان تست سخت‌افزار قبل از تولید برد.
        """.trimIndent()
    }

    private fun scaleBitmapForPayload(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
