package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class McuFamily(
    val displayName: String,
    val persianTagline: String,
    val defaultModels: List<String>,
    val defaultClocks: List<String>,
    val defaultToolchain: String,
    val voltageSpec: String
) {
    AVR(
        displayName = "AVR",
        persianTagline = "معماری ۸ بیتی RISC — مناسب Atmel Studio و پروژه‌های صنعتی سریع",
        defaultModels = listOf("ATmega328P", "ATmega32A", "ATmega128A", "ATtiny85"),
        defaultClocks = listOf("8 MHz (Internal RC)", "16 MHz (External Crystal)", "1 MHz (Low Power)"),
        defaultToolchain = "Atmel / Microchip Studio 7 (AVR-GCC)",
        voltageSpec = "2.7V – 5.5V"
    ),
    STM32(
        displayName = "STM32",
        persianTagline = "ARM Cortex-M — پردازش بلادرنگ، DMA و مصرف انرژی فوق‌کم",
        defaultModels = listOf("STM32F103C8T6", "STM32F407VGT6", "STM32G030F6", "STM32L476RG"),
        defaultClocks = listOf("72 MHz (PLL)", "168 MHz (HSE PLL)", "64 MHz (HSI)", "16 MHz (MSI Low Power)"),
        defaultToolchain = "STM32CubeIDE (HAL / LL)",
        voltageSpec = "1.8V – 3.6V"
    ),
    STM8(
        displayName = "STM8",
        persianTagline = "میکروکنترلر اقتصادی و مقاوم صنعتی با رابط SWIM",
        defaultModels = listOf("STM8S003F3P6", "STM8S103F3", "STM8L151K4"),
        defaultClocks = listOf("16 MHz (HSI)", "8 MHz (HSI/2)", "2 MHz (Low Power)"),
        defaultToolchain = "STVD / Cosmic C / SDCC",
        voltageSpec = "2.95V – 5.5V"
    ),
    HOLTEK(
        displayName = "Holtek",
        persianTagline = "میکروکنترلرهای ۸ بیتی کم‌مصرف و مقاوم در برابر نویز برای لوازم خانگی",
        defaultModels = listOf("HT66F018", "HT66F2390", "HT68F002", "HT46R064"),
        defaultClocks = listOf("8 MHz (HIRC)", "16 MHz (HIRC)", "32 kHz (LIRC)"),
        defaultToolchain = "Holtek HT-IDE3000",
        voltageSpec = "2.2V – 5.5V"
    )
}

enum class PromptTargetAi(val label: String, val badge: String) {
    GEMINI("جمنای (Gemini Deep Think)", "Gemini"),
    DEEPSEEK("دیپ‌سیک (DeepSeek Coder)", "DeepSeek"),
    CHATGPT("چت‌جی‌پی‌تی (ChatGPT)", "ChatGPT"),
    CLAUDE("کلود (Claude Sonnet/Opus)", "Claude")
}

enum class TargetSimulator(val label: String) {
    PROTEUS("Proteus ISIS"),
    SIMULIDE("SimulIDE"),
    WOKWI("Wokwi Simulator"),
    REAL_HARDWARE("سخت‌افزار واقعی (برد آزمایشگاهی)")
}

enum class AiAgentPersona(
    val titleFa: String,
    val subtitleFa: String,
    val useDeepThinking: Boolean
) {
    CO_ANALYST(
        titleFa = "ایجنت ترکیبی سخت‌افزار و نرم‌افزار",
        subtitleFa = "بررسی همزمان کد، رجیسترها، ولتاژ، پین‌ها و خطاهای بحرانی",
        useDeepThinking = true
    ),
    CODE_ARCHITECT(
        titleFa = "معمار کد میکروکنترلر",
        subtitleFa = "تولید کد C/C++ ماژولار، ISR ایمن و آماده شبیه‌سازی",
        useDeepThinking = true
    ),
    LOW_POWER_TUNER(
        titleFa = "متخصص بهینه‌سازی مصرف انرژی",
        subtitleFa = "حالت‌های Sleep/Stop، کلاک‌گیتینگ و کاهش جریان استندبای",
        useDeepThinking = true
    ),
    CIRCUIT_DOCTOR(
        titleFa = "عیب‌یاب مدار و پیشنهاد قطعات",
        subtitleFa = "تحلیل خطای باس، انتخاب قطعات جایگزین و حفاظت ESD",
        useDeepThinking = false
    )
}

enum class ProjectCategory(val value: String, val labelFa: String) {
    CODE("CODE", "کد میکروکنترلر"),
    PROMPT("PROMPT", "پرامپت خودکار"),
    CO_ANALYSIS("CO_ANALYSIS", "تحلیل همزمان HW/SW"),
    OPTIMIZATION("OPTIMIZATION", "بهینه‌سازی انرژی/عملکرد"),
    HMI("HMI", "اسکریپت HMI و پایتون");

    companion object {
        fun fromRaw(raw: String): ProjectCategory =
            entries.firstOrNull { it.value == raw } ?: CODE
    }
}

enum class DiagnosticType(val value: String, val labelFa: String) {
    SCHEMATIC_VISION("SCHEMATIC_VISION", "تحلیل تصویری شماتیک"),
    STANDARDS_SEARCH("STANDARDS_SEARCH", "جستجوی استاندارد و دیتاشیت"),
    COMPONENT_BOM("COMPONENT_BOM", "پیشنهاد قطعات و BOM");

    companion object {
        fun fromRaw(raw: String): DiagnosticType =
            entries.firstOrNull { it.value == raw } ?: SCHEMATIC_VISION
    }
}

@Keep
@IgnoreExtraProperties
data class McuProjectRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mcuFamily: String = McuFamily.AVR.name,
    val mcuModel: String = "ATmega328P",
    val category: String = ProjectCategory.CODE.value,
    val promptText: String = "",
    val codeOrResult: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CircuitDiagnosticRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val mcuFamily: String = "GENERAL",
    val diagnosticType: String = DiagnosticType.SCHEMATIC_VISION.value,
    val queryText: String = "",
    val analysisResult: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ChatMessageItem(
    val id: String,
    val isUser: Boolean,
    val senderLabel: String,
    val content: String,
    val isStreaming: Boolean = false
)

data class TechnicalStandardItem(
    val id: String,
    val title: String,
    val category: String,
    val mcuOrStandard: String,
    val summaryFa: String,
    val keySpecs: List<String>,
    val deepQueryTemplate: String
)

data class AtmelStudioStep(
    val stepNumber: Int,
    val titleFa: String,
    val detailFa: String,
    val configSnippet: String
)
