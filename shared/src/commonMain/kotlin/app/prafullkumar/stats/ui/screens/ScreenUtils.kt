package app.prafullkumar.stats.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.prafullkumar.stats.data.StatsRepo
import app.prafullkumar.stats.ui.components.DayMark
import kotlinx.datetime.LocalDate

/** Date strip dot: full = beast day, half = something logged. */
fun dayMark(day: LocalDate): DayMark {
    val score = StatsRepo.dayScore(day).score
    return when {
        score >= StatsRepo.targets.beastScore -> DayMark.DONE
        score > 0 -> DayMark.PARTIAL
        else -> DayMark.EMPTY
    }
}

/** The scrolling column every screen is built on. */
@Composable
fun ScreenColumn(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 10.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}
