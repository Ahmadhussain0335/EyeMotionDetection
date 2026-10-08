package com.app.eyesmotiondetection.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.eyesmotiondetection.domain.model.EyeGesture

private data class OnboardingPage(val emoji: String, val title: String, val desc: String)

private val pages = listOf(
    OnboardingPage("👁", "Eye Control", "Control your entire phone using just your eyes. No hands required."),
    OnboardingPage("🎯", "${EyeGesture.supported.size} Gestures","Map blinks, winks, gaze direction, and more to any phone action."),
    OnboardingPage("♿", "Always Accessible", "Designed for hands-free interaction. Runs as a background service."),
    OnboardingPage("🔒", "Private & Secure", "All ML processing is on-device. Your camera data never leaves your phone.")
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var page by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        AnimatedContent(targetState = page, label = "page") { idx ->
            val p = pages[idx]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(p.emoji, fontSize = 72.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = p.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = p.desc,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Page dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(if (i == page) 10.dp else 8.dp)
                        .background(
                            if (i == page) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                viewModel.completeOnboarding()
                onComplete()
            }) { Text("Skip") }

            Button(
                onClick = {
                    if (page < pages.lastIndex) page++
                    else { viewModel.completeOnboarding(); onComplete() }
                }
            ) {
                Text(if (page < pages.lastIndex) "Next" else "Get Started")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
