package com.android.settingslib.spa.widget.preference

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.util.wrapOnSetItemIdWithLog
import com.android.settingslib.spa.widget.ui.Spinner
import com.android.settingslib.spa.widget.ui.SpinnerOption

/**
 * The widget model for [SpinnerPreference] widget.
 */
interface SpinnerPreferenceModel {
    val onSetItemId: ((Int) -> Unit)?
        get() = null
    val selectedItem: Int?
        get() = null
    val list: List<SpinnerOption>

    /**
     * The title of this [SpinnerPreference].
     */
    val title: String

    /**
     * The summary of this [SpinnerPreference].
     */
    val summary: () -> CharSequence
        get() = { "" }

    /**
     * The icon of this [Preference].
     *
     * Default is `null` which means no icon.
     */
    val icon: (@Composable () -> Unit)?
        get() = null

    /**
     * Indicates whether this [SpinnerPreference] is enabled.
     *
     * Disabled [SpinnerPreference] will be displayed in disabled style.
     */
    val enabled: () -> Boolean
        get() = { true }
}

/**
 * SpinnerPreference widget.
 *
 * Data is provided through [SpinnerPreferenceModel].
 */
@Composable
fun SpinnerPreference(model: SpinnerPreferenceModel) {
    val onSetItemIdWithLog = wrapOnSetItemIdWithLog(model.onSetItemId)
    BasePreference(
        title = model.title,
        summary = model.summary,
        enabled = model.enabled,
        icon = model.icon,
    ) {
        Spacer(Modifier.width(SettingsDimension.itemPaddingEnd))
        Spinner(model.list, model.selectedItem, onSetItemIdWithLog)
    }
}
