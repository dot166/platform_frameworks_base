/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settingslib.spa.widget.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsShape
import com.android.settingslib.spa.framework.theme.isSpaExpressiveEnabled
import com.android.settingslib.spa.widget.ui.SettingsTitle
import kotlin.let

@Composable
fun SettingsDialog(
    title: String,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        SettingsDialogCard(title, content)
    }
}

/**
 * Card for dialog, suitable for independent dialog in the [NavHost].
 */
@Composable
fun SettingsDialogCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = SettingsShape.CornerExtraLarge1,
        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
    ) {
        Column(modifier = Modifier.padding(vertical = SettingsDimension.itemPaddingAround)) {
            Box(modifier = Modifier.padding(SettingsDimension.dialogItemPadding)) {
                SettingsTitle(title = title, useMediumWeight = true)
            }
            content()
        }
    }
}

@Composable
fun rememberDialogPresenter(
    confirmButton: AlertDialogButton? = null,
    dismissButton: AlertDialogButton? = null,
    title: String,
    content: @Composable () -> Unit,
): AlertDialogPresenter {
    var openDialog by rememberSaveable { mutableStateOf(false) }
    val alertDialogPresenter = remember {
        object : AlertDialogPresenter {
            override fun open() {
                openDialog = true
            }

            override fun close() {
                openDialog = false
            }
        }
    }
    if (openDialog) {
        alertDialogPresenter.SettingsDialog(confirmButton, dismissButton, title, content)
    }
    return alertDialogPresenter
}

@Composable
private fun AlertDialogPresenter.SettingsDialog(
    confirmButton: AlertDialogButton?,
    dismissButton: AlertDialogButton?,
    title: String,
    content: @Composable () -> Unit,
) {
    SettingsDialog(
        onDismissRequest = ::close,
        title = title,
    ) {
        Column {
            content()
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                dismissButton?.let { if (isSpaExpressiveEnabled) DismissButton(it) else Button(it) }
                confirmButton?.let { if (isSpaExpressiveEnabled) ConfirmButton(it) else Button(it) }
            }
        }
    }
}

@Composable
private fun AlertDialogPresenter.Button(button: AlertDialogButton) {
    TextButton(
        onClick = {
            close()
            button.onClick()
        },
        enabled = button.enabled,
    ) {
        Text(button.text)
    }
}

@Composable
private fun AlertDialogPresenter.DismissButton(button: AlertDialogButton) {
    OutlinedButton(
        onClick = {
            close()
            button.onClick()
        },
        enabled = button.enabled,
    ) {
        Text(button.text)
    }
}

@Composable
private fun AlertDialogPresenter.ConfirmButton(button: AlertDialogButton) {
    Button(
        onClick = {
            close()
            button.onClick()
        },
        enabled = button.enabled,
    ) {
        Text(button.text)
    }
}

@Preview
@Composable
private fun DialogPreview() {
    val dialogPresenter = remember {
        object : AlertDialogPresenter {
            override fun open() {}

            override fun close() {}
        }
    }
    dialogPresenter.SettingsDialog(
        confirmButton = AlertDialogButton("Ok"),
        dismissButton = AlertDialogButton("Cancel"),
        title = "Title",
    ) {
        Text("Text")
        Button({}) { Text("Button") }
    }
}
