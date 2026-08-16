package com.smarttool.videodownloader.feature.settings.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSheet(
    selected: SearchEngine,
    onSelect: (SearchEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        SearchEngine.GOOGLE to (R.string.string_google to R.drawable.ic_google),
        SearchEngine.BING to (R.string.string_bing to R.drawable.ic_bing),
        SearchEngine.YAHOO to (R.string.string_yahoo to R.drawable.ic_yahoo),
        SearchEngine.DUCK_DUCK_GO to (R.string.string_duckduckgo to R.drawable.ic_duckduckgo),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Surface,
    ) {
        // The sheet composes in its own window, whose AbstractComposeView re-provides
        // LocalContext from the Activity — discarding the override installed around the
        // main composition. Without this the sheet keeps the launch-time language.
        AppLocaleProvider {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Text(
                    text = stringResource(R.string.string_search_engine),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextColor,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                options.forEach { (engine, labels) ->
                    val (labelRes, iconRes) = labels
                    val isSelected = engine == selected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(ShapeMd)
                            .border(1.dp, if (isSelected) Pri else Border, ShapeMd)
                            .clickable { onSelect(engine) }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                        )

                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            ),
                            color = if (isSelected) TextColor else Muted,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )

                        if (isSelected) {
                            Row(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clip(ShapePill)
                                    .background(Pri),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = PriInk,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
