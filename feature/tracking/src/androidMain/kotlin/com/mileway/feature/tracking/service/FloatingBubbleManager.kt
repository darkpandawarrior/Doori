package com.mileway.feature.tracking.service

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import io.github.aakira.napier.Napier

class FloatingBubbleManager(private val context: Context) {
    companion object {
        private const val TAG = "FloatingBubbleManager"
        private const val BUBBLE_SIZE_DP = 56
        private const val ANIM_ENTER_MS = 400L
        private const val ANIM_EXIT_MS = 250L
        private const val ANIM_PULSE_MS = 1200L
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density

    private var bubbleView: ImageView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var pulseAnimator: ValueAnimator? = null
    private var isAdded = false

    private val bubbleSizePx = (BUBBLE_SIZE_DP * density).toInt()

    fun showBubble(
        isTracking: Boolean,
        savedX: Int = 0,
        savedY: Int = 100,
        onTap: () -> Unit,
    ) {
        if (isAdded) {
            Napier.w("Bubble already shown: skipping", tag = TAG)
            return
        }

        val view =
            ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_mylocation)
                setColorFilter(if (isTracking) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800"))
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setOnClickListener { onTap() }
                alpha = 0f
                scaleX = 0.3f
                scaleY = 0.3f
            }

        val params =
            WindowManager.LayoutParams(
                bubbleSizePx,
                bubbleSizePx,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = savedX
                y = savedY
            }

        try {
            windowManager.addView(view, params)
            isAdded = true
            bubbleView = view
            bubbleParams = params

            // Entrance animation
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).setDuration(ANIM_ENTER_MS),
                    ObjectAnimator.ofFloat(view, "scaleX", 0.3f, 1f).apply {
                        interpolator = OvershootInterpolator()
                        duration = ANIM_ENTER_MS
                    },
                    ObjectAnimator.ofFloat(view, "scaleY", 0.3f, 1f).apply {
                        interpolator = OvershootInterpolator()
                        duration = ANIM_ENTER_MS
                    },
                )
                start()
            }

            if (isTracking) startPulse(view)
        } catch (e: Exception) {
            Napier.e("Failed to add bubble view", e, tag = TAG)
            isAdded = false
        }
    }

    fun updateTrackingState(isTracking: Boolean) {
        val view = bubbleView ?: return
        view.post {
            view.setColorFilter(if (isTracking) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800"))
            if (isTracking) startPulse(view) else stopPulse()
        }
    }

    fun dismiss(onDismissed: () -> Unit) {
        val view =
            bubbleView ?: run {
                onDismissed()
                return
            }
        stopPulse()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).setDuration(ANIM_EXIT_MS),
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.2f).apply {
                    interpolator = DecelerateInterpolator()
                    duration = ANIM_EXIT_MS
                },
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.2f).apply {
                    interpolator = DecelerateInterpolator()
                    duration = ANIM_EXIT_MS
                },
            )
            addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        removeBubble()
                        onDismissed()
                    }
                },
            )
            start()
        }
    }

    fun forceRemove() {
        stopPulse()
        removeBubble()
    }

    private fun removeBubble() {
        try {
            bubbleView?.let { if (isAdded) windowManager.removeView(it) }
        } catch (e: Exception) {
            Napier.w("Error removing bubble view", e, tag = TAG)
        } finally {
            bubbleView = null
            bubbleParams = null
            isAdded = false
        }
    }

    private fun startPulse(view: View) {
        stopPulse()
        pulseAnimator =
            ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
                duration = ANIM_PULSE_MS
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                addUpdateListener { anim ->
                    val scale = anim.animatedValue as Float
                    view.scaleX = scale
                    view.scaleY = scale
                }
                start()
            }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        bubbleView?.let {
            it.scaleX = 1f
            it.scaleY = 1f
        }
    }

    fun vibrate() {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    fun getPosition(): Pair<Int, Int> {
        val params = bubbleParams ?: return Pair(0, 100)
        return Pair(params.x, params.y)
    }

    fun updatePosition(
        x: Int,
        y: Int,
    ) {
        val params = bubbleParams ?: return
        val view = bubbleView ?: return
        params.x = x
        params.y = y
        try {
            if (isAdded) windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Napier.w("Failed to update bubble position", e, tag = TAG)
        }
    }
}
