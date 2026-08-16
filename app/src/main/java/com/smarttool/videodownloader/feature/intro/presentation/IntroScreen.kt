package com.smarttool.videodownloader.feature.intro.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.TextSub

private val DotInactive = Border
private val DotActive = Pri
private val TitleColor = TextColor

@Composable
fun IntroScreen(
    pages: List<IntroPage>,
    pagerState: PagerState,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            Image(
                painter = painterResource(pages[pageIndex].imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val currentPage = pages.getOrNull(pagerState.currentPage)
        if (currentPage != null) {
            Column(modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Surface)),
                        ),
                )

                Column(
                    modifier = Modifier.fillMaxWidth().background(Surface).padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(currentPage.titleRes),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = TitleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    Text(
                        text = stringResource(currentPage.descriptionRes),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = TextSub,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        pages.indices.forEach { index ->
                            val active = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(if (active) 20.dp else 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) DotActive else DotInactive),
                            )
                        }
                    }

                    Text(
                        text = stringResource(currentPage.ctaRes).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = PriInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                            .clip(ShapePill)
                            .background(Pri)
                            .clickable {
                                if (pagerState.currentPage == pages.lastIndex) onFinish() else onNext()
                            }
                            .padding(vertical = 11.dp),
                    )
                }
            }
        }
    }
}
