package com.example.documenttoexcel.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.documenttoexcel.excel.ExcelExporter
import com.example.documenttoexcel.model.ExtractedRecord
import com.example.documenttoexcel.ocr.DocumentOcrProcessor
import kotlinx.coroutines.launch
import java.io.File

data class SelectedFile(val uri: Uri, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentToExcelScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ocrProcessor = remember { DocumentOcrProcessor(context) }

    var selectedFiles by remember { mutableStateOf<List<SelectedFile>>(emptyList()) }
    var extractedData by remember { mutableStateOf<List<ExtractedRecord>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var generatedExcelFile by remember { mutableStateOf<File?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val files = uris.map { uri ->
            SelectedFile(uri = uri, name = getFileName(context, uri))
        }
        selectedFiles = files
        generatedExcelFile = null
        extractedData = emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automated Document to Excel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📄 Ingestion & Extraction Pipeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload scanned PDFs or images to extract text, amounts, and dates automatically into an Excel (.xlsx) workbook.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Step 1: Upload / Select Documents
            item {
                Button(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose PDF or Image Files")
                }
            }

            if (selectedFiles.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Successfully loaded ${selectedFiles.size} document(s).",
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Process Button
                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                val results = mutableListOf<ExtractedRecord>()
                                for (file in selectedFiles) {
                                    val record = ocrProcessor.processDocument(file.uri, file.name)
                                    results.add(record)
                                }
                                extractedData = results
                                val excelFile = ExcelExporter.exportToExcel(context, results)
                                generatedExcelFile = excelFile
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracting parameters and compiling data...")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Process Documents to Excel")
                        }
                    }
                }
            }

            // Step 2 & 3: Extracted Data Table Preview
            if (extractedData.isNotEmpty()) {
                item {
                    Text(
                        text = "Extracted Data Preview:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(8.dp)
                        ) {
                            Text("File", Modifier.weight(1.2f), fontWeight = FontWeight.Bold)
                            Text("Date", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text("Particulars", Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                            Text("Amount", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        // Rows
                        extractedData.forEach { record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(record.fileReference, Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall)
                                Text(record.date, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                Text(record.particulars, Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.2f", record.amount), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            }
                            Divider()
                        }
                    }
                }

                // Download/Share Button
                item {
                    Button(
                        onClick = {
                            generatedExcelFile?.let { file ->
                                shareExcelFile(context, file)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)) // Excel green
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📥 Download / Share Master Excel Sheet")
                    }
                }
            }
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "Document"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name
}

fun shareExcelFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Master Excel File"))
}
