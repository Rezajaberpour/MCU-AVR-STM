package com.example.data

import com.example.data.model.AtmelStudioStep
import com.example.data.model.TechnicalStandardItem

object EngineeringReferenceData {

    val atmelStudioSteps: List<AtmelStudioStep> = listOf(
        AtmelStudioStep(
            stepNumber = 1,
            titleFa = "ایجاد پروژه GCC C Executable در Atmel Studio 7",
            detailFa = "از منوی File > New > Project گزینه GCC C Executable Project را انتخاب کنید. در مرحله Device Selection میکروکنترلر دقیق (مانند ATmega328P یا ATmega32A) را برگزینید تا هدر <avr/io.h> و فایل Linker به صورت خودکار پیکربندی شوند.",
            configSnippet = "Template: GCC C Executable Project\nDevice: ATmega328P (8-bit AVR)\nCompiler: avr-gcc (AVR_8_bit_GNU_Toolchain)"
        ),
        AtmelStudioStep(
            stepNumber = 2,
            titleFa = "تعریف فرکانس کلاک (F_CPU) و سطح بهینه‌سازی (-Os)",
            detailFa = "در Project Properties > Toolchain > AVR/GNU C Compiler > Symbols مقدار F_CPU=16000000UL را اضافه کنید تا توابع _delay_ms() و نرخ باود UART دقیق محاسبه شوند. در بخش Optimization سطح Optimize for size (-Os) را انتخاب کنید.",
            configSnippet = "Defined Symbols (-D): F_CPU=16000000UL\nOptimization Level: -Os (Mandatory for <util/delay.h>)"
        ),
        AtmelStudioStep(
            stepNumber = 3,
            titleFa = "تنظیم فیوزبیت‌ها (Fuse Bits) برای کلاک کریستال و BOD",
            detailFa = "در پنجره Tools > Device Programming قبل از پروگرام کردن، فیوزبیت کلاک (SUT_CKSEL) و Brown-out Detector (BODLEVEL = 2.7V/4.3V) را تنظیم کنید. مراقب فیوز SPIEN و RSTDISBL باشید تا پروگرامر قفل نشود.",
            configSnippet = "ATmega328P 16MHz Ext Crystal + BOD 4.3V:\nLFUSE = 0xFF | HFUSE = 0xDE | EFUSE = 0xFC"
        ),
        AtmelStudioStep(
            stepNumber = 4,
            titleFa = "تولید فایل HEX / ELF برای پروتئوس و پروگرام با USBasp",
            detailFa = "در مسیر Output Files هر دو فایل .hex (برای پروگرامر) و .elf (برای دیباگ سورس‌لول در Proteus ISIS) تولید می‌شوند. برای پروگرام با USBasp می‌توانید از ابزار External Tools و دستور avrdude استفاده کنید.",
            configSnippet = "avrdude -c usbasp -p m328p -U flash:w:\"\$(TargetDir)\$(TargetName).hex\":i"
        ),
        AtmelStudioStep(
            stepNumber = 5,
            titleFa = "تست اولیه ساختار مینیمال و مصرف انرژی کم در AVR",
            detailFa = "در ابتدای main() رجیسترهای PRR (Power Reduction Register) را برای ماژول‌های بلااستفاده ست کنید و پین‌های آزاد را با Pull-up داخلی پایدار سازید تا نویز و جریان استندبای به حداقل برسد.",
            configSnippet = """#define F_CPU 16000000UL
#include <avr/io.h>
#include <avr/sleep.h>
#include <avr/power.h>

int main(void) {
    // Disable unused peripherals (TWI, SPI, ADC) to save power
    PRR = (1 << PRTWI) | (1 << PRSPI) | (1 << PRADC);
    DDRB |= (1 << PB5);
    while (1) {
        PORTB ^= (1 << PB5);
        set_sleep_mode(SLEEP_MODE_IDLE);
        sleep_mode();
    }
}"""
        )
    )

    val technicalStandards: List<TechnicalStandardItem> = listOf(
        TechnicalStandardItem(
            id = "avr_regs",
            title = "رجیسترهای کلیدی و فیوزبیت‌های AVR (ATmega328P / ATmega32)",
            category = "دیتاشیت میکروکنترلر",
            mcuOrStandard = "AVR",
            summaryFa = "مرجع سریع DDRx/PORTx/PINx، تایمر ۱ (TCCR1A/B، OCR1A)، USART (UBRR0) و رجیستر کاهش توان PRR برای پروژههای کم‌مصرف.",
            keySpecs = listOf(
                "UBRR = (F_CPU / (16 * BAUD)) - 1 (Asynchronous Normal)",
                "ADC Clock Optimal: 50 kHz تا 200 kHz با Prescaler 128 در 16MHz",
                "PRR & SMCR: کاهش جریان Sleep تا < 1 µA در Power-Down"
            ),
            deepQueryTemplate = "تمام رجیسترهای تایمر۱، وقفه مقایسه، تنظیمات دقیق UBRR برای 115200bps در 16MHz (با U2X0=1 برای کاهش خطا) و پیکربندی Power-Down Sleep در ATmega328P را همراه جدول بیت‌ها توضیح بده."
        ),
        TechnicalStandardItem(
            id = "stm32_clock",
            title = "معماری کلاک RCC، باس DMA و پین‌های SWD در STM32",
            category = "دیتاشیت میکروکنترلر",
            mcuOrStandard = "STM32",
            summaryFa = "نکات حیاتی راه‌اندازی HSE/PLL، تاخیر Flash Latency، کانال‌های DMA بدون درگیری CPU و پین‌های دیباگ SWDIO/SWCLK/NRST.",
            keySpecs = listOf(
                "ولتاژ تغذیه: 2.0V تا 3.6V (خازن دکوپلاژ 100nF روی هر جفت VDD/VSS)",
                "SWD Interface: PA13 (SWDIO)، PA14 (SWCLK)، NRST + 100nF به زمین",
                "ADC Sample Time + DMA Circular Buffer برای نمونهبرداری پایدار"
            ),
            deepQueryTemplate = "نحوه پیکربندی کامل Clock Tree (HSE 8MHz به SYSCLK 72MHz در STM32F103 یا 168MHz در STM32F407)، تنظیم Flash Wait States، و راه‌اندازی ADC چندکاناله با DMA بدون خطا را شرح بده."
        ),
        TechnicalStandardItem(
            id = "stm8_swim",
            title = "ساختار حافظه، Option Bytes و رابط SWIM در STM8",
            category = "دیتاشیت میکروکنترلر",
            mcuOrStandard = "STM8",
            summaryFa = "پیکربندی Alternate Function (AFR) از طریق Option Bytes، وقفه سطح اولویت ITC و پروتکل یک‌سیمه SWIM برای ST-Link.",
            keySpecs = listOf(
                "VCAP Capacitor: خازن 470nF تا 1µF X7R روی پایه VCAP الزامی است",
                "SWIM Pin: پایه PD1 با مقاومت Pull-up 10k و خازن نزدیک پایه تغذیه",
                "IWDG / WWDG: نگهبان سخت‌افزاری برای پایداری صنعتی"
            ),
            deepQueryTemplate = "نحوه تنظیم Option Bytes (AFR0..AFR7)، مقداردهی کلاک HSI 16MHz، جلوگیری از خطای خازن VCAP و پیاده‌سازی وقفه تایمر4 در STM8S003F3 با کامپایلر SDCC/Cosmic را توضیح بده."
        ),
        TechnicalStandardItem(
            id = "holtek_cfg",
            title = "تنظیمات Configuration Options و ضدنویز در Holtek",
            category = "دیتاشیت میکروکنترلر",
            mcuOrStandard = "HOLTEK",
            summaryFa = "تنظیمات LVR (Low Voltage Reset)، تایمر Watchdog (WDT)، نوسان‌ساز داخلی HIRC/LIRC و راه‌اندازی ADC در سری HT66F.",
            keySpecs = listOf(
                "HIRC Accuracy: ±1% تا ±2% بدون نیاز به کریستال خارجی",
                "LVR Threshold: انتخاب 2.1V / 2.55V / 3.15V / 3.8V بر اساس تغذیه",
                "Touch Key / EEPROM داخلی برای ذخیره‌سازی پارامترهای دستگاه"
            ),
            deepQueryTemplate = "معماری حافظه بانک‌بندی‌شده، تنظیمات Configuration Options (HIRC, WDT, LVR) و راه‌اندازی PWM و ADC با HT-IDE3000 روی میکروکنترلر Holtek HT66F018 را کامل شرح بده."
        ),
        TechnicalStandardItem(
            id = "ipc_2221",
            title = "استاندارد IPC-2221 طراحی PCB و عرض مسیر جریان",
            category = "استاندارد صنعتی",
            mcuOrStandard = "IPC-2221 / IEC",
            summaryFa = "محاسبه عرض ترک مس (1oz / 2oz) بر اساس جریان و افزایش دما، فاصله ایمنی ولتاژ بالا (Clearance/Creepage) و قوانین مسیرهای فرکانس بالا.",
            keySpecs = listOf(
                "1oz Copper, 1A, ΔT=10°C: عرض مسیر خارجی حدود 0.30mm (12 mil)",
                "3A, ΔT=10°C: عرض مسیر خارجی حدود 1.40mm (55 mil)",
                "فاصله ایمنی ولتاژ 220VAC شبکه: حداقل 2.5mm تا 3.2mm (یا شیار ایزوله)"
            ),
            deepQueryTemplate = "بر اساس استاندارد IPC-2221 و IEC-61000-4-2 قوانین دقیق طراحی برد میکروکنترلر صنعتی (عرض ترک تغذیه، Ground Plane، چیدمان کریستال، حفاظت ESD و ایزولاسیون رله/ترایاک) را ارائه بده."
        ),
        TechnicalStandardItem(
            id = "bus_standards",
            title = "استانداردهای باس I2C، SPI، RS-485 و CAN در محیط صنعتی",
            category = "پروتکل و استاندارد",
            mcuOrStandard = "RS-485 / I2C / CAN",
            summaryFa = "انتخاب دقیق مقاومت Pull-up در I2C، مقاومت ترمینال 120 اهم و بایاس Failsafe در RS-485/Modbus و دیودهای حفاظت TVS.",
            keySpecs = listOf(
                "I2C Pull-up (100kHz/400kHz): معمولاً 4.7kΩ یا 2.2kΩ بسته به خازن خط",
                "RS-485 Termination: مقاومت 120Ω در دو سر خط + Failsafe 560Ω",
                "TVS Protection: استفاده از SMBJ5.0CA یا PESD2CAN روی خطوط بیرونی"
            ),
            deepQueryTemplate = "مقادیر دقیق سخت‌افزاری (مقاومت Pull-up، ترمینال، دیود TVS، Common-Mode Choke) و تایمینگ نرم‌افزاری برای باس‌های I2C، RS-485 Modbus RTU و CAN Bus را مقایسه و استانداردسازی کن."
        )
    )
}
