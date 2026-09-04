package io.github.sor2171.superframevision.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sor2171.superframevision.core.utils.Const
import io.github.sor2171.superframevision.ui.component.LinkCard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import superframevision.shared.generated.resources.Res
import superframevision.shared.generated.resources.bilibili
import superframevision.shared.generated.resources.github
import superframevision.shared.generated.resources.donor
import superframevision.shared.generated.resources.info_bilibili
import superframevision.shared.generated.resources.info_donor
import superframevision.shared.generated.resources.info_github
import superframevision.shared.generated.resources.info_qq_group
import superframevision.shared.generated.resources.qq

@Composable
fun InfoScreen() {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val texts = Const.SOFTWARE_INFO.split("|")
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = texts[0],
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = texts[1],
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        text = texts[2],
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LinkCard(
                            imageRes = Res.drawable.github,
                            stringRes = Res.string.info_github,
                            link = Const.GITHUB_LINK,
                            imageDescription = "GitHub"
                        )

                        LinkCard(
                            imageRes = Res.drawable.bilibili,
                            stringRes = Res.string.info_bilibili,
                            link = Const.BILIBILI_LINK,
                            imageDescription = "BiliBili"
                        )

                        LinkCard(
                            imageRes = Res.drawable.qq,
                            stringRes = Res.string.info_qq_group,
                            link = Const.QQ_GROUP_LINK,
                            imageDescription = "QQ"
                        )
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.donor),
                                contentDescription = null
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier.padding(16.dp),
                                    text = stringResource(Res.string.info_donor),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun InfoScreenPreview() {
    InfoScreen()
}