package app.prafullkumar.stats.data

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Clock

/** Small date and number helpers so common code never needs java.time or String.format. */

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun nowLocal(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun today(): LocalDate = nowLocal().date

fun LocalDate.plusDays(n: Int): LocalDate = plus(DatePeriod(days = n))
fun LocalDate.minusDays(n: Int): LocalDate = minus(DatePeriod(days = n))

/** Days from [this] to [other]; negative when [other] is earlier. */
fun LocalDate.daysTo(other: LocalDate): Int = daysUntil(other)

fun parseDate(s: String?): LocalDate? =
    s?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/** Inclusive list of days from [from] to [to]; empty when [to] is before [from]. */
fun daysBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
    val n = from.daysTo(to)
    if (n < 0) return emptyList()
    return (0..n).map { from.plusDays(it) }
}

/** Monday of the week [date] is in. */
fun weekStart(date: LocalDate): LocalDate = date.minusDays(date.dayOfWeek.isoDayNumber - 1)

private val shortDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val longDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
private val shortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun DayOfWeek.short(): String = shortDays[isoDayNumber - 1]
fun DayOfWeek.long(): String = longDays[isoDayNumber - 1]
fun DayOfWeek.letter(): String = short().take(1)

/** "2 Oct" */
fun LocalDate.dayMonth(): String = "$day ${shortMonths[month.number - 1]}"

/** "Friday, 2 Oct 2026" */
fun LocalDate.fullLabel(): String = "${dayOfWeek.long()}, ${dayMonth()} $year"

/** "Today", "Yesterday", "Tomorrow" or "2 Oct". */
fun LocalDate.friendly(today: LocalDate): String = when (today.daysTo(this)) {
    0 -> "Today"
    -1 -> "Yesterday"
    1 -> "Tomorrow"
    else -> dayMonth()
}

/** "in 5 days", "3 days late", "due today". */
fun dueLabel(due: LocalDate, today: LocalDate): String {
    val d = today.daysTo(due)
    return when {
        d == 0 -> "due today"
        d == 1 -> "due tomorrow"
        d > 1 -> "in $d days"
        d == -1 -> "1 day late"
        else -> "${-d} days late"
    }
}

fun pad2(n: Int): String = if (n < 10) "0$n" else "$n"
fun pad2(n: Long): String = if (n < 10) "0$n" else "$n"

/** Seconds as "mm:ss" (or "h:mm:ss" past an hour). */
fun clock(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "$h:${pad2(m)}:${pad2(sec)}" else "${pad2(m)}:${pad2(sec)}"
}

/** Minutes as "1h 25m" / "40m". */
fun minutesLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** Whole numbers stay whole, others get one decimal; 100+ rounds. */
fun formatNumber(v: Double): String {
    if (v >= 100 || v == v.toLong().toDouble()) return v.roundToLong().toString()
    val tenths = (abs(v) * 10).roundToLong()
    val sign = if (v < 0) "-" else ""
    return if (tenths % 10 == 0L) "$sign${tenths / 10}" else "$sign${tenths / 10}.${tenths % 10}"
}

fun plural(n: Int, word: String): String = "$n $word${if (n == 1) "" else "s"}"
