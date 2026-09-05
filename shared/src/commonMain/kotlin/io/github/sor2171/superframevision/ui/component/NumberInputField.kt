package io.github.sor2171.superframevision.ui.component

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "请输入数字",
    allowDecimal: Boolean = false,
    maxValue: Double? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = if (allowDecimal) {
                input.filter { it.isDigit() || it == '.' }
                    .let { text ->
                        val firstDotIndex = text.indexOf('.')
                        if (firstDotIndex != -1) {
                            text.substring(0, firstDotIndex + 1) +
                                    text.substring(firstDotIndex + 1).replace(".", "")
                        } else text
                    }
            } else {
                input.filter { it.isDigit() }
            }

            val doubleVal = filtered.toDoubleOrNull()
            if (filtered.isEmpty() || maxValue == null || (doubleVal != null && doubleVal <= maxValue)) {
                onValueChange(filtered)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}