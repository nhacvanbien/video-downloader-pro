package com.smarttool.videodownloader.feature.main.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.core.ui.theme.AppGray
import com.smarttool.videodownloader.core.ui.theme.AppWhite

private val InactiveTabColor = Color(0xFFBFBFBF)

@Composable
fun MainBottomBar(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Matches the old custom bar's hairline separator; NavigationBar has no border of its own.
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppGray))

        NavigationBar(containerColor = AppWhite, tonalElevation = 0.dp) {
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
                    Image(
                        painter = painterResource(if (isSelected) tab.selectedIconRes else tab.normalIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
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
