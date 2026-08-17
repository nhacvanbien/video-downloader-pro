package com.smarttool.videodownloader.feature.rating.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.AppDialogCard
import com.smarttool.videodownloader.core.ui.dialogs.DialogBody
import com.smarttool.videodownloader.core.ui.dialogs.DialogPrimaryButton
import com.smarttool.videodownloader.core.ui.dialogs.DialogTitle
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.WarnInk

private const val STAR_COUNT = 5

@Composable
fun RatingPromptDialogContent(
    onDismissRequest: () -> Unit,
    onRatingSelected: (rating: Int) -> Unit,
) {
    var selectedRating by remember { mutableIntStateOf(0) }

    AppDialogCard(onDismissRequest = onDismissRequest) {
        Image(
            painter = painterResource(faceDrawableFor(selectedRating)),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp)
                .size(96.dp),
        )
        DialogTitle(
            text = stringResource(titleFor(selectedRating)),
            modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
        )
        DialogBody(
            text = stringResource(subtitleFor(selectedRating)),
            modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 18.dp),
        ) {
            for (star in 1..STAR_COUNT) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_round),
                    contentDescription = null,
                    tint = if (star <= selectedRating) Pri else Border,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { selectedRating = star },
                )
            }
        }
        if (selectedRating == 0) {
            Text(
                text = stringResource(R.string.string_rating_best_hint),
                color = WarnInk,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 6.dp),
            )
        }
        DialogPrimaryButton(
            text = stringResource(R.string.string_rate),
            onClick = { onRatingSelected(selectedRating) },
            enabled = selectedRating > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 20.dp),
        )
    }
}

private fun faceDrawableFor(rating: Int): Int = when (rating) {
    1 -> R.drawable.ic_face_1
    2 -> R.drawable.ic_face_2
    3 -> R.drawable.ic_face_3
    4 -> R.drawable.ic_face_4
    5 -> R.drawable.ic_face_5
    else -> R.drawable.ic_face_start
}

private fun titleFor(rating: Int): Int = when (rating) {
    1, 2, 3 -> R.string.string_rating_sorry_title
    4, 5 -> R.string.string_rating_appreciated_title
    else -> R.string.string_rating_thank_you_title
}

private fun subtitleFor(rating: Int): Int = when (rating) {
    1, 2, 3 -> R.string.string_rating_sorry_subtitle
    4, 5 -> R.string.string_rating_appreciated_subtitle
    else -> R.string.string_rating_thank_you_subtitle
}
