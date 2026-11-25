package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.EngineeringReferenceData
import com.example.data.model.CircuitDiagnosticRecord
import com.example.data.model.DiagnosticType
import com.example.data.model.McuProjectRecord
import com.example.data.model.ProjectCategory
import com.example.ui.components.AiResultCard
import com.example.ui.components.copyToClipboard
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.EngineeringAiViewModel
import com.example.ui.viewmodel.EngineeringStudioUiState
import com.example.ui.viewmodel.WorkspaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardsAndLibraryScreen(
    uiState: EngineeringStudioUiState,
    projectsState: CloudUiState<List<McuProjectRecord>>,
    diagnosticsState: CloudUiState<List<CircuitDiagnosticRecord>>,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("دیتابیس استانداردها", "آرشیو ابری من")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.testTag("standards_tab_$index")
                    )
                }
            }

            if (selectedTab == 0) {
                StandardsDeepSearchTab(uiState, aiViewModel, workspaceViewModel)
            } else {
                CloudArchiveTab(projectsState, diagnosticsState, workspaceViewModel)
            }
        }
    }
}

@Composable
private fun StandardsDeepSearchTab(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "جستجوی عمیق در مستندات دیتاشیت و استانداردها",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.standardsQuery,
                onValueChange = aiViewModel::updateStandardsQuery,
                label = { Text("نام رجیستر، استاندارد IPC/IEC یا شماره قطعه (مثلاً STM32F103 DMA)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("standards_search_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { aiViewModel.runDeepStandardSearch() },
                enabled = uiState.standardsQuery.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("standards_search_button")
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("جستجوی عمیق با جمنای Deep Think")
            }
        }

        item {
            AiResultCard(
                state = uiState.standardsAiState,
                title = "مستندات فنی استخراج‌شده",
                modelBadge = "gemini-4-carbon • HIGH",
                onSaveToCloud = { result ->
                    workspaceViewModel.saveDiagnostic(
                        title = uiState.standardsQuery.take(80).ifBlank { "گزارش استاندارد فنی" },
                        mcuFamily = uiState.selectedFamily.name,
                        diagnosticType = DiagnosticType.STANDARDS_SEARCH.value,
                        queryText = uiState.standardsQuery,
                        analysisResult = result
                    )
                }
            )
        }

        item {
            Text(
                text = "مراجع سریع مهندسی (دیتاشیت و استانداردها)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(EngineeringReferenceData.technicalStandards, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.mcuOrStandard,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.summaryFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.keySpecs.forEach { spec ->
                        Text(
                            text = "• $spec",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedButton(
                        onClick = { aiViewModel.runDeepStandardSearch(item.deepQueryTemplate) },
                        modifier = Modifier.fillMaxWidth().testTag("deep_query_${item.id}")
                    ) {
                        Text("استخراج کامل دیتاشیت و جداول با جمنای")
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudArchiveTab(
    projectsState: CloudUiState<List<McuProjectRecord>>,
    diagnosticsState: CloudUiState<List<CircuitDiagnosticRecord>>,
    workspaceViewModel: WorkspaceViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "پروژه‌ها، کدها و پرامپت‌های ذخیره‌شده",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (projectsState) {
            CloudUiState.Loading -> item {
                Text("در حال بارگذاری پروژه‌ها...", style = MaterialTheme.typography.bodySmall)
            }
            is CloudUiState.Error -> item {
                Text(projectsState.message, color = MaterialTheme.colorScheme.error)
            }
            is CloudUiState.Success -> {
                if (projectsState.data.isEmpty()) {
                    item {
                        Text(
                            text = "موردی در آرشیو پروژه‌ها ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(projectsState.data, key = { it.id }) { proj ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = proj.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${proj.mcuFamily} (${proj.mcuModel}) • ${ProjectCategory.fromRaw(proj.category).labelFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { copyToClipboard(context, proj.title, proj.codeOrResult) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_copy_code),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { workspaceViewModel.deleteProject(proj.id) },
                                            modifier = Modifier.testTag("delete_project_${proj.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = stringResource(R.string.cd_delete_item),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = proj.codeOrResult.take(260) + if (proj.codeOrResult.length > 260) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "گزارش‌های تحلیل شماتیک و استانداردهای فنی",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        when (diagnosticsState) {
            CloudUiState.Loading -> item {
                Text("در حال بارگذاری گزارش‌ها...", style = MaterialTheme.typography.bodySmall)
            }
            is CloudUiState.Error -> item {
                Text(diagnosticsState.message, color = MaterialTheme.colorScheme.error)
            }
            is CloudUiState.Success -> {
                if (diagnosticsState.data.isEmpty()) {
                    item {
                        Text(
                            text = "هنوز گزارش تحلیل مدار یا استانداردی ذخیره نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(diagnosticsState.data, key = { it.id }) { diag ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = diag.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${diag.mcuFamily} • ${DiagnosticType.fromRaw(diag.diagnosticType).labelFa}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { copyToClipboard(context, diag.title, diag.analysisResult) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_copy_code),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { workspaceViewModel.deleteDiagnostic(diag.id) },
                                            modifier = Modifier.testTag("delete_diagnostic_${diag.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = stringResource(R.string.cd_delete_item),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = diag.analysisResult.take(260) + if (diag.analysisResult.length > 260) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
