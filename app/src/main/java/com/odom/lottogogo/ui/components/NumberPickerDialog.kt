package com.odom.lottogogo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.odom.lottogogo.R
import com.odom.lottogogo.domain.LottoRules
import com.odom.lottogogo.ui.theme.BallText

/** 회차 + 번호 6개 입력. 수동 입력과 보관함 수정에 함께 쓴다. */
@Composable
fun NumberPickerDialog(
    title: String,
    initialRound: Int,
    initialNumbers: List<Int>,
    onConfirm: (round: Int, numbers: List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var roundText by rememberSaveable { mutableStateOf(initialRound.toString()) }
    var selected by rememberSaveable { mutableStateOf(initialNumbers.sorted()) }
    val round = roundText.toIntOrNull()?.takeIf { it > 0 }
    val canConfirm = round != null && LottoRules.isValidGame(selected)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = roundText,
                    onValueChange = { text -> roundText = text.filter(Char::isDigit).take(ROUND_MAX_DIGITS) },
                    label = { Text(stringResource(R.string.picker_round_label)) },
                    isError = round == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.picker_selected_count, selected.size, LottoRules.NUMBERS_PER_GAME),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LottoRules.NUMBER_RANGE.forEach { number ->
                        val isSelected = number in selected
                        PickableNumber(
                            number = number,
                            selected = isSelected,
                            enabled = isSelected || selected.size < LottoRules.NUMBERS_PER_GAME,
                            onToggle = {
                                selected = if (isSelected) selected - number else (selected + number).sorted()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(requireNotNull(round), selected) }, enabled = canConfirm) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PickableNumber(number: Int, selected: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val description = stringResource(R.string.ball_description, number)
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(
                if (selected) {
                    Modifier.background(ballColor(number), CircleShape)
                } else {
                    Modifier.border(BorderStroke(1.dp, outline), CircleShape)
                }
            )
            .toggleable(value = selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            color = if (selected) BallText else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private const val ROUND_MAX_DIGITS = 4
