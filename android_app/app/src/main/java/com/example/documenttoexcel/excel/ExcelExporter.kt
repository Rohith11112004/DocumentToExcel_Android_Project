package com.example.documenttoexcel.excel

import android.content.Context
import com.example.documenttoexcel.model.ExtractedRecord
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

object ExcelExporter {
    fun exportToExcel(context: Context, records: List<ExtractedRecord>, fileName: String = "Processed_Records.xlsx"): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Extracted Data")

        // Header row
        val headerRow = sheet.createRow(0)
        val headers = listOf("File Reference", "Date", "Particulars", "Amount")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
        }

        // Data rows
        records.forEachIndexed { rowIndex, record ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(record.fileReference)
            row.createCell(1).setCellValue(record.date)
            row.createCell(2).setCellValue(record.particulars)
            row.createCell(3).setCellValue(record.amount)
        }

        // Auto-fit columns
        for (i in headers.indices) {
            sheet.autoSizeColumn(i)
        }

        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { out ->
            workbook.write(out)
        }
        workbook.close()

        return outputFile
    }
}
