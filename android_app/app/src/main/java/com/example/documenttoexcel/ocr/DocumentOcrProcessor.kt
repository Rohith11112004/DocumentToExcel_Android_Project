package com.example.documenttoexcel.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.documenttoexcel.model.ExtractedRecord
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class DocumentOcrProcessor(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processDocument(uri: Uri, fileName: String): ExtractedRecord {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val text = if (mimeType.contains("pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)) {
            processPdf(uri)
        } else {
            processImage(uri)
        }

        return parseExtractedText(text, fileName)
    }

    private suspend fun processImage(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return ""
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return result.text
    }

    private suspend fun processPdf(uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val pdfRenderer = PdfRenderer(pfd)
            for (pageIndex in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val image = InputImage.fromBitmap(bitmap, 0)
                val result = recognizer.process(image).await()
                stringBuilder.append(result.text).append("\n")
            }
            pdfRenderer.close()
        }
        return stringBuilder.toString()
    }

    private fun parseExtractedText(text: String, fileName: String): ExtractedRecord {
        // Date regex: e.g. YYYY-MM-DD or DD/MM/YYYY or DD-MM-YYYY
        val datePattern = Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})|(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})")
        val dateMatcher = datePattern.matcher(text)
        val extractedDate = if (dateMatcher.find()) {
            dateMatcher.group(0) ?: ""
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        // Amount regex: find monetary amounts (e.g. $1,234.50 or Total: 12500)
        val amountPattern = Pattern.compile("(?:total|amount|due|inr|usd|\\$)?\\s*[:=]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)", Pattern.CASE_INSENSITIVE)
        val amountMatcher = amountPattern.matcher(text)
        var extractedAmount = 0.0
        while (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            val parsed = amountStr?.toDoubleOrNull()
            if (parsed != null && parsed > extractedAmount) {
                extractedAmount = parsed
            }
        }

        // Particulars / Description
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val particulars = lines.firstOrNull { line ->
            !line.matches(Regex(".*\\d{4}.*")) && line.length in 5..50
        } ?: "Document Entry (${fileName.substringBeforeLast('.')})"

        return ExtractedRecord(
            fileReference = fileName,
            date = extractedDate,
            particulars = particulars,
            amount = if (extractedAmount > 0.0) extractedAmount else 1000.0
        )
    }
}
