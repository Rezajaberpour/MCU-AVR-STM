package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.AiAgentPersona
import com.example.data.model.McuFamily
import com.example.data.model.ProjectCategory
import com.example.data.model.PromptTargetAi
import com.example.data.model.TargetSimulator
import com.example.ui.components.AiResultCard
import com.example.ui.components.CodeBlockSurface
import com.example.ui.components.McuFamilySelectorRow
import com.example.ui.viewmodel.EngineeringAiViewModel
import com.example.ui.viewmodel.EngineeringStudioUiState
import com.example.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeAndPromptLabScreen(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    val subTabs = listOf("پرامپت و کد", "بهینه‌ساز / شبیه‌ساز", "چت ایجنت‌ها")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
    ) {
        PrimaryTabRow(selectedTabIndex = selectedSubTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(text = title, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.testTag("code_lab_tab_$index")
                )
            }
        }

        when (selectedSubTab) {
            0 -> AutoPromptAndFirmwareTab(uiState, aiViewModel, workspaceViewModel)
            1 -> OptimizerAndCoAnalysisTab(uiState, aiViewModel, workspaceViewModel)
            else -> MultiAgentChatTab(uiState, aiViewModel)
        }
    }
}

@Composable
private fun AutoPromptAndFirmwareTab(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "تنظیمات میکروکنترلر و پرامپت‌نویسی خودکار",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        McuFamilySelectorRow(
            selectedFamily = uiState.selectedFamily,
            onSelectFamily = aiViewModel::selectFamily
        )

        // Chip model selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.selectedFamily.defaultModels.forEach { chip ->
                FilterChip(
                    selected = chip == uiState.selectedModel,
                    onClick = { aiViewModel.selectModel(chip) },
                    label = { Text(chip, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        // Clock & Simulator selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.selectedFamily.defaultClocks.forEach { clk ->
                FilterChip(
                    selected = clk == uiState.selectedClock,
                    onClick = { aiViewModel.selectClock(clk) },
                    label = { Text(clk, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Text(
            text = "هدف شبیه‌سازی و سبک هوش مصنوعی پرامپت",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TargetSimulator.entries.forEach { sim ->
                FilterChip(
                    selected = sim == uiState.selectedSimulator,
                    onClick = { aiViewModel.selectSimulator(sim) },
                    label = { Text(sim.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PromptTargetAi.entries.forEach { target ->
                FilterChip(
                    selected = target == uiState.selectedPromptTarget,
                    onClick = { aiViewModel.selectPromptTarget(target) },
                    label = { Text(target.label, style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.testTag("prompt_target_${target.badge.lowercase()}")
                )
            }
        }

        OutlinedTextField(
            value = uiState.selectedPeripheral,
            onValueChange = aiViewModel::updatePeripheral,
            label = { Text("پریفرال یا ماژول (مثلاً ADC+DMA، UART، I2C OLED)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("peripheral_input")
        )

        OutlinedTextField(
            value = uiState.taskSpecification,
            onValueChange = aiViewModel::updateTaskSpecification,
            label = { Text("شرح پروژه یا رفتار مورد انتظار مدار و کد") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth().testTag("task_spec_input")
        )

        CodeBlockSurface(
            title = "پرامپت خودکار تولیدشده (${uiState.selectedPromptTarget.badge})",
            codeText = uiState.generatedAutoPrompt
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = aiViewModel::runGeneratedPromptWithGemini,
                modifier = Modifier.weight(1f).height(50.dp).testTag("run_prompt_gemini_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("تولید کد با جمنای Deep Think")
            }
            OutlinedButton(
                onClick = {
                    workspaceViewModel.saveProject(
                        title = "پرامپت ${uiState.selectedModel} - ${uiState.selectedPeripheral}",
                        mcuFamily = uiState.selectedFamily.name,
                        mcuModel = uiState.selectedModel,
                        category = ProjectCategory.PROMPT.value,
                        promptText = uiState.taskSpecification,
                        codeOrResult = uiState.generatedAutoPrompt,
                        notes = "Target AI: ${uiState.selectedPromptTarget.badge} | ${uiState.selectedSimulator.label}"
                    )
                },
                modifier = Modifier.height(50.dp).testTag("save_prompt_cloud_button")
            ) {
                Text("ذخیره پرامپت")
            }
        }

        AiResultCard(
            state = uiState.codeLabAiState,
            title = "خروجی کد و معماری (${uiState.selectedModel})",
            modelBadge = "gemini-4-carbon • HIGH",
            onSaveToCloud = { resultText ->
                workspaceViewModel.saveProject(
                    title = "${uiState.selectedModel} - ${uiState.selectedPeripheral}",
                    mcuFamily = uiState.selectedFamily.name,
                    mcuModel = uiState.selectedModel,
                    category = ProjectCategory.CODE.value,
                    promptText = uiState.taskSpecification,
                    codeOrResult = resultText,
                    notes = "${uiState.selectedFamily.defaultToolchain} | ${uiState.selectedSimulator.label}"
                )
            }
        )
    }
}

@Composable
private fun OptimizerAndCoAnalysisTab(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "تحلیل همزمان سخت‌افزار و نرم‌افزار / کاهش مصرف انرژی",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "کد C/C++ میکروکنترلر خود را وارد کنید تا جمنای با استدلال عمیق باگ‌های سخت‌افزاری، تاخیرهای مسدودکننده و مصرف جریان را اصلاح کند.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            OutlinedTextField(
                value = uiState.codeInputForAnalysis,
                onValueChange = aiViewModel::updateCodeInput,
                label = { Text("MCU C / C++ Source Code") },
                textStyle = MaterialTheme.typography.labelSmall,
                minLines = 7,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth().testTag("optimizer_code_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { aiViewModel.runCodeOptimization("بهینه‌سازی مصرف انرژی (Low-Power Sleep & Clock Gating)") },
                modifier = Modifier.weight(1f).testTag("optimize_power_button")
            ) {
                Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("کاهش مصرف انرژی", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { aiViewModel.runCodeOptimization("تحلیل همزمان خطاهای نرم‌افزار و سخت‌افزار (HW+SW Co-Analysis)") },
                modifier = Modifier.weight(1f).testTag("co_analysis_button")
            ) {
                Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text("تحلیل ترکیبی HW/SW", style = MaterialTheme.typography.labelLarge)
            }
        }

        OutlinedButton(
            onClick = { aiViewModel.runCodeOptimization("تلفیق کد آماده شبیه‌سازی دقیق در ${uiState.selectedSimulator.label}") },
            modifier = Modifier.fillMaxWidth().testTag("sim_bundle_button")
        ) {
            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("تولید نسخه آماده شبیه‌سازی (${uiState.selectedSimulator.label})")
        }

        AiResultCard(
            state = uiState.codeLabAiState,
            title = "گزارش تحلیل و کد اصلاح‌شده",
            modelBadge = "gemini-4-carbon • HIGH",
            onSaveToCloud = { resultText ->
                workspaceViewModel.saveProject(
                    title = "بهینه‌سازی ${uiState.selectedModel}",
                    mcuFamily = uiState.selectedFamily.name,
                    mcuModel = uiState.selectedModel,
                    category = ProjectCategory.OPTIMIZATION.value,
                    promptText = uiState.codeInputForAnalysis.take(1500),
                    codeOrResult = resultText,
                    notes = "HW/SW Co-Analysis & Power Optimization"
                )
            }
        )
    }
}

@Composable
private fun MultiAgentChatTab(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel
) {
    var inputMessage by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Persona Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiAgentPersona.entries.forEach { persona ->
                FilterChip(
                    selected = persona == uiState.selectedPersona,
                    onClick = { aiViewModel.selectPersona(persona) },
                    label = { Text(persona.titleFa, style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (persona.useDeepThinking) Icons.Filled.Bolt else Icons.Filled.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("persona_${persona.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.chatMessages, key = { it.id }) { msg ->
                val isUser = msg.isUser
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 620.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = msg.senderLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        if (uiState.chatError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = uiState.chatError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("سوال درباره ${uiState.selectedModel}، کد یا مدار...") },
                maxLines = 3,
                modifier = Modifier.weight(1f).testTag("agent_chat_input")
            )
            IconButton(
                onClick = {
                    val text = inputMessage
                    inputMessage = ""
                    aiViewModel.sendChatMessage(text)
                },
                enabled = inputMessage.isNotBlank() && !uiState.isChatStreaming,
                modifier = Modifier.size(48.dp).testTag("agent_chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "ارسال پیام",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
