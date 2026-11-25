package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DiagnosticType
import com.example.ui.components.AiResultCard
import com.example.ui.components.McuFamilySelectorRow
import com.example.ui.viewmodel.EngineeringAiViewModel
import com.example.ui.viewmodel.EngineeringStudioUiState
import com.example.ui.viewmodel.WorkspaceViewModel

@Composable
fun SchematicVisionScreen(
    uiState: EngineeringStudioUiState,
    aiViewModel: EngineeringAiViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var circuitNotes by rememberSaveable { mutableStateOf("") }
    var selectedMode by rememberSaveable { mutableStateOf("عیب‌یابی مدار و بررسی ولتاژ") }

    val analysisModes = listOf(
        "عیب‌یابی مدار و بررسی ولتاژ",
        "پیشنهاد قطعات جایگزین و لیست BOM",
        "بهینه‌سازی چیدمان PCB و کاهش نویز"
    )

    // Zero-Permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedBitmap = decodeUriToBitmap(context, uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "تحلیلگر تصویری شماتیک و برد PCB",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "تصویر نقشه شماتیک، برد لحیم‌کاری یا مدار پروتئوس را انتخاب کنید تا جمنای با مدل تصویری قطعات، خطاهای اتصال و قطعات جایگزین را پیشنهاد دهد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            McuFamilySelectorRow(
                selectedFamily = uiState.selectedFamily,
                onSelectFamily = aiViewModel::selectFamily
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bmp = selectedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = stringResource(R.string.cd_user_uploaded_schematic),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_schematic_scan),
                            contentDescription = stringResource(R.string.cd_schematic_banner),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pick_schematic_photo_button")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = if (selectedUri == null) "انتخاب تصویر شماتیک یا برد از گالری" else "تغییر تصویر انتخاب‌شده"
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                analysisModes.forEach { mode ->
                    FilterChip(
                        selected = mode == selectedMode,
                        onClick = { selectedMode = mode },
                        label = { Text(mode, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            OutlinedTextField(
                value = circuitNotes,
                onValueChange = { circuitNotes = it },
                label = { Text("توضیح مشکل مدار یا شماره آی‌سی‌ها (اختیاری)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("schematic_notes_input")
            )

            Button(
                onClick = {
                    val bmp = selectedBitmap
                    if (bmp != null) {
                        aiViewModel.analyzeSchematic(
                            bitmap = bmp,
                            mcuFamily = "${uiState.selectedFamily.displayName} (${uiState.selectedModel})",
                            analysisMode = selectedMode,
                            circuitNotes = circuitNotes
                        )
                    }
                },
                enabled = selectedBitmap != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("analyze_schematic_button")
            ) {
                Icon(Icons.Filled.Biotech, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("تحلیل هوشمند شماتیک با جمنای Vision")
            }

            AiResultCard(
                state = uiState.visionAnalysisState,
                title = "گزارش عیب‌یابی مدار و پیشنهاد قطعات",
                modelBadge = "gemini-4-neon",
                onSaveToCloud = { report ->
                    workspaceViewModel.saveDiagnostic(
                        title = "تحلیل شماتیک ${uiState.selectedModel}",
                        mcuFamily = uiState.selectedFamily.name,
                        diagnosticType = DiagnosticType.SCHEMATIC_VISION.value,
                        queryText = circuitNotes.ifBlank { selectedMode },
                        analysisResult = report
                    )
                }
            )
        }
    }
}

private fun decodeUriToBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (_: Exception) {
        null
    }
}
