package com.example.recordapp.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Enhanced haptic feedback utility for providing more nuanced, mobile-like haptic sensations.
 * Provides a richer touch experience for drag-and-drop operations.
 */
class HapticUtil(private val context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    /**
     * Perform haptic feedback for drag start - longer, more noticeable feedback
     */
    fun performDragStart(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ has richer haptic effects
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(60)
        }
    }
    
    /**
     * Perform haptic feedback for item swap - medium intensity, quick feedback
     */
    fun performItemSwap(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    }
    
    /**
     * Perform haptic feedback for drag end/drop - satisfying completion
     */
    fun performDragEnd(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(35)
        }
    }
    
    /**
     * Perform a rich compound haptic pattern for completing a successful drag and drop
     * Creates a more satisfying, iOS-like "snap" feeling when an item is placed
     */
    fun performSuccessfulDrop(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create a custom pattern that feels like a successful snap
            val timings = longArrayOf(0, 30, 30, 20)
            val amplitudes = intArrayOf(0, 80, 0, 255) // varied intensity
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
    
    /**
     * Gentle feedback for hovering over potential drop target
     */
    fun performHoverFeedback(view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use EFFECT_TICK instead of EFFECT_TEXTURE_TICK which may not be available
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(10, 50) // very gentle
            vibrator.vibrate(effect)
        }
        // Skip on older devices as it would be too intrusive
    }
    
    /**
     * Cancel any ongoing vibration
     */
    fun cancel() {
        vibrator.cancel()
    }
}

/**
 * Remember a HapticUtil instance
 */
@Composable
fun rememberHapticUtil(): HapticUtil {
    val context = LocalContext.current
    return remember { HapticUtil(context) }
}

/**
 * Composable that provides both View-based and Vibrator-based haptic feedback
 */
@Composable
fun rememberEnhancedHaptic(): EnhancedHapticFeedback {
    val context = LocalContext.current
    val view = LocalView.current
    val hapticUtil = remember { HapticUtil(context) }
    
    return remember(hapticUtil, view) {
        EnhancedHapticFeedback(hapticUtil, view)
    }
}

/**
 * Combined haptic feedback that uses both View and Vibrator methods
 * for the richest possible haptic experience
 */
class EnhancedHapticFeedback(
    private val hapticUtil: HapticUtil,
    private val view: View
) {
    /**
     * Perform enhanced drag start feedback
     */
    fun dragStart() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        hapticUtil.performDragStart(view)
    }
    
    /**
     * Perform enhanced item swap/reorder feedback
     */
    fun itemSwap() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        hapticUtil.performItemSwap(view)
    }
    
    /**
     * Perform enhanced drop feedback
     */
    fun dragEnd(successful: Boolean = true) {
        if (successful) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            hapticUtil.performSuccessfulDrop(view)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            hapticUtil.performDragEnd(view)
        }
    }
    
    /**
     * Light hover feedback (use sparingly)
     */
    fun hover() {
        hapticUtil.performHoverFeedback(view)
    }
} 