package com.example.filltracking2.util

import android.content.Context
import android.net.Uri
import com.example.filltracking2.data.FileRecord
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

    fun exportToExcel(
        context: Context,
        uri: Uri,
        records: List<FileRecord>
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("الأرشيف")
                sheet.setRightToLeft(true) // Arabic RTL support

                // Colors
                val headerBg = XSSFColor(byteArrayOf(0x00.toByte(), 0x80.toByte(), 0x80.toByte()), null) // Teal
                val titleBg = XSSFColor(byteArrayOf(0x1A.toByte(), 0x33.toByte(), 0x52.toByte()), null) // Navy
                val altRowBg = XSSFColor(byteArrayOf(0xF0.toByte(), 0xF8.toByte(), 0xFF.toByte()), null) // Alice Blue
                
                // Fonts
                val titleFont = workbook.createFont().apply {
                    bold = true
                    color = IndexedColors.WHITE.index
                    fontHeightInPoints = 16
                }
                
                val headerFont = workbook.createFont().apply {
                    bold = true
                    color = IndexedColors.WHITE.index
                }

                // Styles
                val titleStyle = workbook.createCellStyle().apply {
                    (this as XSSFCellStyle).setFillForegroundColor(titleBg)
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    setFont(titleFont)
                }

                val headerStyle = workbook.createCellStyle().apply {
                    (this as XSSFCellStyle).setFillForegroundColor(headerBg)
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    borderBottom = BorderStyle.THIN
                    setFont(headerFont)
                }

                val normalStyle = workbook.createCellStyle().apply {
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    wrapText = true
                }

                val altStyle = workbook.createCellStyle().apply {
                    (this as XSSFCellStyle).setFillForegroundColor(altRowBg)
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    alignment = HorizontalAlignment.CENTER
                    verticalAlignment = VerticalAlignment.CENTER
                    wrapText = true
                }

                // Header Row 1: Title
                val row0 = sheet.createRow(0)
                row0.heightInPoints = 40f
                val titleCell = row0.createCell(0)
                titleCell.setCellValue("أرشيف تتبع الوثائق - نسخة احتياطية")
                titleCell.setCellStyle(titleStyle)
                sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 10))

                // Header Row 2: Info
                val row1 = sheet.createRow(1)
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                row1.createCell(0).setCellValue("تاريخ التصدير: $timestamp | إجمالي السجلات: ${records.size}")

                // Header Row 4: Column Headers
                val headers = arrayOf(
                    "الحالة", "الرقم الأصلي", "تاريخ الاستلام", "الرقم الداخلي", 
                    "تاريخ التسليم", "تاريخ التسجيل", "المستلم", "المصدر", 
                    "الموضوع", "المصالح", "ملاحظات المدير"
                )
                val row3 = sheet.createRow(3)
                headers.forEachIndexed { index, title ->
                    val cell = row3.createCell(index)
                    cell.setCellValue(title)
                    cell.setCellStyle(headerStyle)
                    sheet.setColumnWidth(index, 15 * 256)
                }
                sheet.setColumnWidth(8, 30 * 256) // Subject width
                sheet.setColumnWidth(9, 25 * 256) // Sectors width
                sheet.setColumnWidth(10, 30 * 256) // Notes width

                // Freeze Pane: Headers stay visible
                sheet.createFreezePane(0, 4)

                // Data Rows
                records.forEachIndexed { rowIndex, record ->
                    val row = sheet.createRow(rowIndex + 4)
                    val currentStyle = if (rowIndex % 2 == 1) altStyle else normalStyle
                    
                    // Column 0: Urgency (with color)
                    val urgencyCell = row.createCell(0)
                    urgencyCell.setCellValue(if (record.urgency == "Urgent") "عاجل" else "عادي")
                    
                    val urgencyStyle = workbook.createCellStyle().apply {
                        cloneStyleFrom(currentStyle)
                        val color = if (record.urgency == "Urgent") {
                            XSSFColor(byteArrayOf(0xFF.toByte(), 0xCD.toByte(), 0xD2.toByte()), null) // Light Red
                        } else {
                            XSSFColor(byteArrayOf(0xC8.toByte(), 0xE6.toByte(), 0xC9.toByte()), null) // Light Green
                        }
                        (this as XSSFCellStyle).setFillForegroundColor(color)
                        fillPattern = FillPatternType.SOLID_FOREGROUND
                    }
                    urgencyCell.setCellStyle(urgencyStyle)

                    row.createCell(1).apply { setCellValue(record.originalSerial); setCellStyle(currentStyle) }
                    row.createCell(2).apply { setCellValue(record.dateReceivedGov); setCellStyle(currentStyle) }
                    row.createCell(3).apply { setCellValue(record.internalSerial); setCellStyle(currentStyle) }
                    row.createCell(4).apply { setCellValue(record.dateDeliveredToDomain); setCellStyle(currentStyle) }
                    row.createCell(5).apply { setCellValue(record.dateRegistered); setCellStyle(currentStyle) }
                    row.createCell(6).apply { setCellValue(record.recipientName); setCellStyle(currentStyle) }
                    row.createCell(7).apply { setCellValue(record.source); setCellStyle(currentStyle) }
                    row.createCell(8).apply { setCellValue(record.subject); setCellStyle(currentStyle) }
                    row.createCell(9).apply { setCellValue(record.sectors.joinToString(", ")); setCellStyle(currentStyle) }
                    row.createCell(10).apply { setCellValue(record.notes); setCellStyle(currentStyle) }
                }

                workbook.write(outputStream)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
