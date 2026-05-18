package com.example.musicpro

import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

object GlassUtils {
    /**
     * Applies a glassmorphism effect ONLY to the background of a container.
     * The children of the container remain sharp and unaffected by the blur.
     */
    fun applyGlassEffect(container: ViewGroup, blurRadius: Float = 30f, cornerRadius: Float = 40f) {
        val context = container.context
        
        // 1. Check if we already have a glass background
        var glassBg = container.findViewById<View>(R.id.glass_bg)
        if (glassBg == null) {
            glassBg = View(context).apply {
                id = R.id.glass_bg
                // CRITICAL FIX: Do not use MATCH_PARENT for height in a wrap_content container.
                // We use a dummy size and then use a listener to match the parent's actual size.
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0 // Height will be set dynamically
                )
            }
            container.addView(glassBg, 0)
            
            container.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                val height = bottom - top
                if (glassBg.height != height) {
                    glassBg.layoutParams.height = height
                    glassBg.requestLayout()
                }
            }
        }

        // 3. Define the translucent surface with a stroke
        val shape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            this.cornerRadius = cornerRadius
            // Higher opacity color for readability against animated background
            setColor(ContextCompat.getColor(context, R.color.glass_fill))
            setStroke(2, ContextCompat.getColor(context, R.color.glass_stroke))
        }
        glassBg.background = shape
        glassBg.clipToOutline = true
        
        // 🌟 Ensure parent is also transparent to avoid blocking the glass effect
        container.background = null
        if (container.parent is View) {
            (container.parent as View).background = null
        }

        // 4. Apply RenderEffect blur ONLY to this background view (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // This blurs the glass_fill color and stroke, creating the frosted glass texture.
            // Since it's a separate view at the bottom, children are NOT blurred.
            glassBg.setRenderEffect(
                RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
            )
        }
        
        // Ensure the container itself doesn't have a background that blocks the glass
        container.background = null
    }
}
