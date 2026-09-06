package io.github.sor2171.superframevision.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.net.URI

@Composable
fun LinkCard(
    imageRes: DrawableResource,
    stringRes: StringResource,
    buttonRes: StringResource? = null,
    link: String,
    imageDescription: String? = null,
) {
    Card(
        modifier = Modifier
            .height(128.dp)
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 26.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp),
                painter = painterResource(imageRes),
                contentDescription = imageDescription
            )
            Text(
                text = stringResource(stringRes),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                modifier = Modifier
                    .height(48.dp)
                    .width(92.dp),
                onClick = {
                    Desktop.getDesktop().browse(
                        URI(link)
                    )
                }
            ) {
                Text(buttonRes?.let { stringResource(it) } ?: "Go")
            }
        }
    }
}
