package com.smarttool.videodownloader.feature.main.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.core.ui.theme.ElevationFloatingBar
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.softShadow

private val InactiveTabColor = Muted
private val BadgeBg = Pri
private val BadgeInk = PriInk

/**
 * @param badgeCounts how many items each tab wants to advertise; a tab absent from the map, or
 * mapped to zero, shows no badge.
 */
@Composable
fun MainBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    badgeCounts: Map<MainTab, Int> = emptyMap(),
) {
    Column(
        modifier = modifier.fillMaxWidth().softShadow(ElevationFloatingBar, RectangleShape),
    ) {
        NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = isSelected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(tab) },
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val badgeCount = badgeCounts[tab] ?: 0

                    Box(contentAlignment = Alignment.TopEnd) {
                        Image(
                            painter = painterResource(
                                if (isSelected) tab.selectedIconRes else tab.normalIconRes,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )

                        if (badgeCount > 0) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = BadgeInk,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .offset(x = 7.dp, y = (-4).dp)
                                    .defaultMinSize(minWidth = 16.dp)
                                    .clip(CircleShape)
                                    .background(BadgeBg)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else InactiveTabColor,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
