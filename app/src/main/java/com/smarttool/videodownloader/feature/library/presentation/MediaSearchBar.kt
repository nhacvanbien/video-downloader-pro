package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor

/**
 * Persistent search field shown right below [MediaFilterChipRow] — never toggled, always
 * on screen. Shared by the Downloads tab and the library screens (Downloaded/Private/SelectVideo).
 */
@Composable
fun MediaSearchBar(
    search: String,
    onSearchChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(ShapePill)
            .background(Surface)
            .border(1.dp, Border, ShapePill)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (search.isEmpty()) {
                Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = Muted)
            }

            BasicTextField(
                value = search,
                onValueChange = onSearchChange,
                singleLine = true,
                cursorBrush = SolidColor(Pri),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextColor),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}
