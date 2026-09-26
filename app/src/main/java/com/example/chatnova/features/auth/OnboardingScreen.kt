package com.example.chatnova.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatnova.core.localization.AppStrings
import com.example.chatnova.core.theme.NovaCyan
import com.example.chatnova.core.theme.NovaEmerald
import com.example.chatnova.core.theme.NovaPrimary
import com.example.chatnova.domain.model.AppLanguage
import com.example.chatnova.features.shared.NovaButton
import com.example.chatnova.features.shared.NovaOutlinedButton

data class OnboardingPageData(
    val titleKey: String,
    val descKey: String,
    val icon: ImageVector,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            titleKey = "onboarding_title_1",
            descKey = "onboarding_desc_1",
            icon = Icons.Filled.ChatBubble,
            gradientColors = listOf(NovaPrimary, NovaCyan)
        ),
        OnboardingPageData(
            titleKey = "onboarding_title_2",
            descKey = "onboarding_desc_2",
            icon = Icons.Filled.SportsEsports,
            gradientColors = listOf(NovaCyan, NovaEmerald)
        ),
        OnboardingPageData(
            titleKey = "onboarding_title_3",
            descKey = "onboarding_desc_3",
            icon = Icons.Filled.Group,
            gradientColors = listOf(NovaPrimary, Color(0xFFF43F5E))
        )
    )

    val currentData = pages[currentPage]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Skip Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppStrings.get("app_name", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NovaPrimary
            )

            if (currentPage < pages.size - 1) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.testTag("skip_onboarding_button")
                ) {
                    Text(
                        text = AppStrings.get("skip", language),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Center Content with Artwork and text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Illustrated Circular Card
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Brush.linearGradient(currentData.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentData.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(92.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = AppStrings.get(currentData.titleKey, language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = AppStrings.get(currentData.descKey, language),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) NovaPrimary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }

        // Bottom Navigation Buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            if (currentPage < pages.size - 1) {
                NovaButton(
                    text = AppStrings.get("next", language),
                    onClick = { currentPage++ },
                    testTag = "onboarding_next_button"
                )
            } else {
                NovaButton(
                    text = AppStrings.get("get_started", language),
                    onClick = onFinish,
                    testTag = "onboarding_start_button"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
