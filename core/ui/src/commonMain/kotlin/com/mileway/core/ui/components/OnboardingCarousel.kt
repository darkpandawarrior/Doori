package com.mileway.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_action_next
import com.mileway.core.ui.resources.core_action_skip
import com.mileway.core.ui.resources.core_get_started
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** One page of an [OnboardingCarousel], a hero icon, title and supporting copy. */
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

/**
 * Reusable onboarding / what's-new carousel (UX.5): a [HorizontalPager] of [OnboardingPage]s with an
 * animated dot indicator, a Skip shortcut, a Next button that advances, and a Get-started button on the last
 * page. Stateless about persistence, the caller decides when to show it (first launch / post-update) and
 * what [onFinish] does (set a "seen" flag, navigate on).
 */
@Composable
fun OnboardingCarousel(
    pages: List<OnboardingPage>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = onFinish,
    finishLabel: String = stringResource(Res.string.core_get_started),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSkip, enabled = !isLast, shape = DesignTokens.Shape.button) {
                Text(if (isLast) "" else stringResource(Res.string.core_action_skip))
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            OnboardingPageContent(pages[page])
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dotWidth")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "dotColor",
                )
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .size(width = width, height = 8.dp)
                            .background(color, DesignTokens.Shape.button),
                )
            }
        }

        Button(
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = DesignTokens.Shape.button,
        ) {
            Text(if (isLast) finishLabel else stringResource(Res.string.core_action_next), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, DesignTokens.Shape.button),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
