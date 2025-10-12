package app.plugins.analytics

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream

interface ExcelWizard {
    fun excelify(data: List<MemberWithMark>): ByteArray
}

class PoiExcelWizard : ExcelWizard {

    override fun excelify(data: List<MemberWithMark>): ByteArray {
        val workbook = XSSFWorkbook()
        val outputStream = ByteArrayOutputStream()

        try {
            val sheet = workbook.createSheet("Student Marks")

            // Создание стилей
            val headerStyle = createHeaderStyle(workbook)
            val normalStyle = createNormalStyle(workbook)
            val boldStyle = createBoldStyle(workbook)

            // Создание заголовков
            createHeaders(sheet, headerStyle)

            // Заполнение данными
            fillData(sheet, data, normalStyle, boldStyle)

            // Авто-размер колонок
            autoSizeColumns(sheet)

            // Запись в поток
            workbook.write(outputStream)
            return outputStream.toByteArray()

        } finally {
            workbook.close()
            outputStream.close()
        }
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        return workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN

            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)

            alignment = HorizontalAlignment.CENTER
        }
    }

    private fun createNormalStyle(workbook: Workbook): CellStyle {
        return workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            verticalAlignment = VerticalAlignment.CENTER
        }
    }

    private fun createBoldStyle(workbook: Workbook): CellStyle {
        return workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN

            val font = workbook.createFont().apply {
                bold = true
            }
            setFont(font)

            verticalAlignment = VerticalAlignment.CENTER
        }
    }

    private fun createHeaders(sheet: Sheet, headerStyle: CellStyle) {
        val headerRow = sheet.createRow(0)
        val headers = listOf(
            "Фамилия",
            "Имя",
            "Отчество",
            "Группа",
            "Оценка",
            "Статус"
        )

        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
    }

    private fun fillData(
        sheet: Sheet,
        data: List<MemberWithMark>,
        normalStyle: CellStyle,
        boldStyle: CellStyle
    ) {
        val sortedData = data.sortedByDescending { it.mark ?: -1 }

        sortedData.forEachIndexed { rowIndex, member ->
            val row = sheet.createRow(rowIndex + 1)

            // Фамилия
            val lastNameCell = row.createCell(0)
            lastNameCell.setCellValue(member.lastName)
            lastNameCell.cellStyle = normalStyle

            // Имя
            val firstNameCell = row.createCell(1)
            firstNameCell.setCellValue(member.firstName)
            firstNameCell.cellStyle = normalStyle

            // Отчество
            val secondNameCell = row.createCell(2)
            secondNameCell.setCellValue(member.secondName)
            secondNameCell.cellStyle = normalStyle

            // Группа
            val groupCell = row.createCell(3)
            groupCell.setCellValue(member.group)
            groupCell.cellStyle = normalStyle

            // Оценка
            val markCell = row.createCell(4)
            if (member.mark != null) {
                markCell.setCellValue(member.mark.toDouble())
            } else {
                markCell.setCellValue("Н/А")
            }

            // Статус (выделяем жирным если есть оценка)
            val statusCell = row.createCell(5)
            val statusText = if (member.mark != null) "Оценен" else "Не оценен"
            statusCell.setCellValue(statusText)
            statusCell.cellStyle = if (member.mark != null) boldStyle else normalStyle

            // Выделяем ячейку с оценкой жирным если оценка есть
            if (member.mark != null) {
                markCell.cellStyle = boldStyle
            } else {
                markCell.cellStyle = normalStyle
            }
        }
    }

    private fun autoSizeColumns(sheet: Sheet) {
        for (columnIndex in 0..5) {
            sheet.autoSizeColumn(columnIndex)
        }
    }
}