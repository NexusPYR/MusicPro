package com.example.musicpro

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RuntimeShader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity

/**
 * BaseActivity that provides a stable, hardware-accelerated fluid background.
 * The background is set via window.setBackgroundDrawable to ensure it stays at Z=0.
 */
abstract class BaseActivity : AppCompatActivity() {

    private var fluidShader: RuntimeShader? = null
    private var shaderAnimator: ValueAnimator? = null
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    protected var useFluidBackground: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (useFluidBackground) {
            setupFluidBackground()
        }
    }

    private fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    private fun setupFluidBackground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // High-stability, low-complexity AGSL shader for fluid motion
            val shaderCode = """
                uniform float2 iResolution;
                uniform float iTime;
                uniform float3 color1;
                uniform float3 color2;

                half4 main(float2 fragCoord) {
                    float2 uv = fragCoord / iResolution.xy;
                    float t = iTime * 0.05;
                    
                    float x = uv.x * 2.0 - 1.0;
                    float y = uv.y * 2.0 - 1.0;
                    x *= iResolution.x / iResolution.y;
                    
                    // Stable wave function
                    float wave = sin(x * 1.2 + t) * cos(y * 1.5 - t * 0.8);
                    float3 col = mix(color1, color2, wave * 0.5 + 0.5);
                    
                    return half4(col, 1.0);
                }
            """
            fluidShader = RuntimeShader(shaderCode)
            
            val c1 = getThemeColor(androidx.appcompat.R.attr.colorPrimary)
            val c2 = getThemeColor(android.R.attr.colorBackground)
            
            // Log colors for debug (will show in Logcat if we could see it, but good for stability)
            fluidShader?.setFloatUniform("color1", Color.red(c1)/255f, Color.green(c1)/255f, Color.blue(c1)/255f)
            fluidShader?.setFloatUniform("color2", Color.red(c2)/255f, Color.green(c2)/255f, Color.blue(c2)/255f)
            
            val displayMetrics = resources.displayMetrics
            fluidShader?.setFloatUniform("iResolution", displayMetrics.widthPixels.toFloat(), displayMetrics.heightPixels.toFloat())

            shaderAnimator = ValueAnimator.ofFloat(0f, 1000f).apply {
                duration = 300000 // Very slow, stable animation
                interpolator = LinearInterpolator()
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animator ->
                    fluidShader?.setFloatUniform("iTime", animator.animatedValue as Float)
                    window.decorView.invalidate()
                }
            }

            val shaderDrawable = object : Drawable() {
                override fun draw(canvas: Canvas) {
                    backgroundPaint.shader = fluidShader
                    canvas.drawRect(bounds, backgroundPaint)
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: ColorFilter?) {}
                @Deprecated("Deprecated in Java")
                override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            }

            window.setBackgroundDrawable(shaderDrawable)
            shaderAnimator?.start()
        } else {
            val bgColor = getThemeColor(android.R.attr.colorBackground)
            window.decorView.setBackgroundColor(bgColor)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shaderAnimator?.cancel()
    }
}
