package data

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date())
}