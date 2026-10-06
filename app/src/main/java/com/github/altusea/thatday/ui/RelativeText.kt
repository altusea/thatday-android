package com.github.altusea.thatday.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.github.altusea.thatday.R
import com.github.altusea.thatday.time.Relative
import com.github.altusea.thatday.time.RelativeUnit

/** Localised, plural-aware label for a [Relative] value, e.g. "3 days ago". */
@Composable
@ReadOnlyComposable
fun relativeText(relative: Relative): String = when (relative) {
    Relative.Today -> stringResource(R.string.relative_today)
    is Relative.Away -> {
        val id = when (relative.unit) {
            RelativeUnit.DAY ->
                if (relative.future) R.plurals.relative_future_days else R.plurals.relative_past_days
            RelativeUnit.WEEK ->
                if (relative.future) R.plurals.relative_future_weeks else R.plurals.relative_past_weeks
            RelativeUnit.MONTH ->
                if (relative.future) R.plurals.relative_future_months else R.plurals.relative_past_months
            RelativeUnit.YEAR ->
                if (relative.future) R.plurals.relative_future_years else R.plurals.relative_past_years
        }
        pluralStringResource(id, relative.amount.toInt(), relative.amount)
    }
}
