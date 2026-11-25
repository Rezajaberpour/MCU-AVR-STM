package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectCategory
import com.example.ui.components.AiResultCard
import com.example.ui.components.McuFamilySelectorRow
import com.example.ui.viewmodel.EngineeringAiViewModel
import com.example.ui.viewmodel.EngineeringStudioUiState
import com.example.ui.viewmodel.WorkspaceViewModel

@Composable
fun HmiPythonStudioScreen(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    val latest = uiState.telemetryHistory.lastOrNull()
    val baudOptions = listOf(9600, 115200, 921600)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "کنسول بلادرنگ HMI، پایتون و فلوکد",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "شبیه‌ساز پروتکل UART و کنترل پارامترهای دستگاه متصل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = aiViewModel::toggleHmiRunning,
                    modifier = Modifier.size(48.dp).testTag("hmi_toggle_stream_button")
                ) {
                    Icon(
                        imageVector = if (uiState.hmiRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (uiState.hmiRunning) "توقف نمونه‌برداری" else "شروع نمونه‌برداری",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            McuFamilySelectorRow(
                selectedFamily = uiState.selectedFamily,
                onSelectFamily = aiViewModel::selectFamily
            )

            // Live Gauges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricCard(
                    label = "ولتاژ تغذیه",
                    value = latest?.let { "%.2f V".format(it.voltageV) } ?: "--",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    label = "جریان مصرفی",
                    value = latest?.let { "%.1f mA".format(it.currentMa) } ?: "--",
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    label = "دمای هسته",
                    value = latest?.let { "%.1f °C".format(it.tempC) } ?: "--",
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricCard(
                    label = "ADC 12-bit",
                    value = latest?.adcRaw?.toString() ?: "--",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Live Waveform Canvas
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "نوسان‌نگار بلادرنگ جریان (mA) و ADC",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.selectedModel} • ${uiState.baudRate} bps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val gridColor = MaterialTheme.colorScheme.outlineVariant
                    val points = uiState.telemetryHistory

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        for (i in 1..3) {
                            val y = h * (i / 4f)
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        }
                        if (points.size >= 2) {
                            val adcPath = Path()
                            val currentPath = Path()
                            points.forEachIndexed { idx, pt ->
                                val x = w * (idx.toFloat() / (points.size - 1).coerceAtLeast(1))
                                val yAdc = h - (pt.adcRaw / 4095f).coerceIn(0f, 1f) * (h - 12f) - 6f
                                val yCur = h - (pt.currentMa / 260f).coerceIn(0f, 1f) * (h - 12f) - 6f
                                if (idx == 0) {
                                    adcPath.moveTo(x, yAdc)
                                    currentPath.moveTo(x, yCur)
                                } else {
                                    adcPath.lineTo(x, yAdc)
                                    currentPath.lineTo(x, yCur)
                                }
                            }
                            drawPath(adcPath, color = primaryColor, style = Stroke(width = 4f))
                            drawPath(currentPath, color = secondaryColor, style = Stroke(width = 4f))
                        }
                    }
                }
            }

            // Interactive HMI Control Parameters
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "کنترل پارامترهای سخت‌افزاری HMI",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "دیوتی سایکل PWM: ${uiState.pwmDutyPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = uiState.pwmDutyPercent.toFloat(),
                        onValueChange = { aiViewModel.setPwmDuty(it.toInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.testTag("hmi_pwm_slider")
                    )

                    Text(
                        text = "بار مقاومتی شبیه‌سازی‌شده: ${uiState.loadResistanceOhms} Ω",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Slider(
                        value = uiState.loadResistanceOhms.toFloat(),
                        onValueChange = { aiViewModel.setLoadResistance(it.toInt()) },
                        valueRange = 20f..500f,
                        modifier = Modifier.testTag("hmi_load_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فعال‌سازی حالت کم‌مصرف (Sleep Mode)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = uiState.lowPowerSleepActive,
                            onCheckedChange = aiViewModel::toggleLowPowerSleep,
                            modifier = Modifier.testTag("hmi_sleep_switch")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        baudOptions.forEach { baud ->
                            FilterChip(
                                selected = baud == uiState.baudRate,
                                onClick = { aiViewModel.setBaudRate(baud) },
                                label = { Text("$baud bps", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // UART Serial Monitor Log
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "مانیتور فریم‌های سریال UART (Flowcode / Python)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            uiState.telemetryHistory.takeLast(4).forEach { pt ->
                                Text(
                                    text = "[${pt.timestampLabel}] ${pt.uartFrame}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = aiViewModel::generateHmiPythonBundle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_hmi_python_button")
            ) {
                Icon(Icons.Filled.Code, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("تولید کد ترکیبی C + Flowcode HMI + Python")
            }

            AiResultCard(
                state = uiState.hmiBundleAiState,
                title = "بسته کامل پایتون، فلوکد و میکروکنترلر",
                modelBadge = "gemini-3.8-flash",
                onSaveToCloud = { result ->
                    workspaceViewModel.saveProject(
                        title = "HMI Python ${uiState.selectedModel} (${uiState.baudRate})",
                        mcuFamily = uiState.selectedFamily.name,
                        mcuModel = uiState.selectedModel,
                        category = ProjectCategory.HMI.value,
                        promptText = "PWM=${uiState.pwmDutyPercent}%, Baud=${uiState.baudRate}",
                        codeOrResult = result,
                        notes = "Flowcode HMI + pyserial + MCU UART"
                    )
                }
            )
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1
            )
        }
    }
}
