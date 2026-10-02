package app.prafullkumar.stats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val DAYS_BACK = 120

/**
 * Horizontal calendar for the top of the log screen: scroll back to fill in
 * a day that was missed, tap any day to edit it.
 */
@Composable
fun DateStrip(
    selected: LocalDate,
    today: LocalDate,
    earliest: LocalDate,
    onSelect: (LocalDate) -> Unit,
    dayState: (LocalDate) -> DayMark,
    modifier: Modifier = Modifier
) {
    // No point showing days before tracking started.
    val days = remember(today, earliest) {
        val back = java.time.temporal.ChronoUnit.DAYS.between(earliest, today)
            .coerceIn(0L, DAYS_BACK.toLong())
        (0..back).map { today.minusDays(it) }.reversed()
    }
    val listState = rememberLazyListState()

    LaunchedEffect(selected, days.size) {
        val index = days.indexOf(selected)
        if (index >= 0) listState.scrollToItem(maxOf(0, index - 4))
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onSelect(selected.minusDays(1)) },
                enabled = selected.isAfter(earliest)
            ) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    friendlyLabel(selected, today),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    selected.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onSelect(selected.plusDays(1)) },
                enabled = selected.isBefore(today)
            ) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next day")
            }
        }

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { day ->
                DayChip(
                    day = day,
                    isSelected = day == selected,
                    isToday = day == today,
                    mark = dayState(day),
                    onClick = { onSelect(day) }
                )
            }
        }
    }
}

enum class DayMark { EMPTY, PARTIAL, DONE }

@Composable
private fun DayChip(
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    mark: DayMark,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bg = when {
        isSelected -> scheme.primary
        mark == DayMark.DONE -> scheme.primaryContainer
        else -> scheme.surface
    }
    val fg = when {
        isSelected -> scheme.onPrimary
        mark == DayMark.DONE -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    Column(
        Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(
                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                color = if (isToday && !isSelected) scheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            day.format(DateTimeFormatter.ofPattern("EEE")),
            style = MaterialTheme.typography.labelMedium,
            color = fg.copy(alpha = 0.75f)
        )
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when (mark) {
                        DayMark.DONE -> if (isSelected) scheme.onPrimary else scheme.primary
                        DayMark.PARTIAL -> scheme.secondary
                        DayMark.EMPTY -> androidx.compose.ui.graphics.Color.Transparent
                    }
                )
        )
    }
}

private fun friendlyLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
}
