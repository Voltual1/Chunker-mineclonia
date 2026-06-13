package me.voltual.vb.core.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn

// 动画效果来源自 https://github.com/10miaomiao/bilimiao
private const val DefaultFadeEndThresholdEnter = 0.3f

private val Int.ForFade: Int
  get() = (this * DefaultFadeEndThresholdEnter).toInt()

fun materialFadeIn(durationMillis: Int = DefaultFadeInDuration): EnterTransition =
  fadeIn(animationSpec = tween(durationMillis = durationMillis.ForFade, easing = LinearEasing)) +
    scaleIn(
      animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
      initialScale = 0.8f,
    )

fun materialFadeOut(durationMillis: Int = DefaultFadeOutDuration): ExitTransition =
  fadeOut(animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing))
