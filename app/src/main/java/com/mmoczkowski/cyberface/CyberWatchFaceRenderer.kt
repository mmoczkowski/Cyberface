package com.mmoczkowski.cyberface

import android.content.res.Resources
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.view.SurfaceHolder
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val ANIM_DURATION: Long = 1000L
private const val CYBER_PLANE_SPACING: Float = 0.24f
private const val VERTICAL_LINE_COUNT: Int = 6
private const val HORIZONTAL_LINE_COUNT: Int = 6
private const val SCROLLING_PLANE_SPEED: Float = 0.2f

class CyberWatchFaceRenderer(
  resources: Resources,
  surfaceHolder: SurfaceHolder,
  currentUserStyleRepository: CurrentUserStyleRepository,
  watchState: WatchState,
  canvasType: Int,
  interactiveDrawModeUpdateDelayMillis: Long
) : Renderer.CanvasRenderer(
  surfaceHolder,
  currentUserStyleRepository,
  watchState,
  canvasType,
  interactiveDrawModeUpdateDelayMillis
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private val gridPaint = Paint().apply {
    color = Color.GREEN
    style = Paint.Style.STROKE
    strokeWidth = 2f
    maskFilter = BlurMaskFilter(1f, BlurMaskFilter.Blur.NORMAL)
  }

  private val textPaint = Paint().apply {
    color = Color.GREEN
    style = Paint.Style.FILL
    strokeWidth = 2f
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    textSize = 65f
    typeface = resources.getFont(R.font.braciola)
    maskFilter = BlurMaskFilter(1f, BlurMaskFilter.Blur.NORMAL)
  }

  private val gPaint = Paint().apply {
    color = Color.GREEN
    style = Paint.Style.FILL_AND_STROKE
    textAlign = Paint.Align.CENTER
    isAntiAlias = true
    textSize = 60f
    xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
  }

  private var lastVisibleTime: Long = System.currentTimeMillis()

  private var dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH mm ss")

  init {
    scope.launch {
      watchState.isVisible.collectLatest {
        lastVisibleTime = System.currentTimeMillis()
      }
    }
  }

  override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
    with(canvas) {
      // Clear background
      drawColor(Color.BLACK)

      // Calculate animation progress
      val animationTime: Long = System.currentTimeMillis() - lastVisibleTime
      val rawProgress: Float =
        if (animationTime >= ANIM_DURATION) 1f else (animationTime % ANIM_DURATION) / ANIM_DURATION.toFloat()
      val progress: Float = rawProgress.easeInOutCircle()

      drawScrollingPlanes(progress)
      drawTime(progress, zonedDateTime)
      drawOverlay(progress)
    }
  }

  override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
    /* no-op */
  }

  override fun onDestroy() {
    scope.cancel()
    super.onDestroy()
  }

  private fun Float.easeInOutCircle(): Float = -(cos(PI * this).toFloat() - 1f) / 2f

  private fun Canvas.drawScrollingPlanes(progress: Float) {
    drawScrollingPlane(progress)
    save()
    rotate(180f, centerX, centerY)
    drawScrollingPlane(progress)
    restore()
  }

  private fun Canvas.drawScrollingPlane(progress: Float) {
    val top = height * (0.5f + CYBER_PLANE_SPACING / 2f * progress)

    drawLine(0f, top, width.toFloat(), top, gridPaint)

    // Draw vertical lines
    save()
    clipRect(0f, top, width.toFloat(), height.toFloat())
    (-VERTICAL_LINE_COUNT / 2..VERTICAL_LINE_COUNT / 2).flatMap { step ->
      val t: Float = step / (VERTICAL_LINE_COUNT / 2f)
      listOf(width / 2f, -height.toFloat() / 2f, width / 2 + (width / 2f) * t, height.toFloat())
    }.let { lines ->
      drawLines(lines.toFloatArray(), gridPaint)
    }
    restore()

    // Draw horizontal lines
    val planeHeight: Float = height - top
    val duration: Float = ANIM_DURATION / SCROLLING_PLANE_SPEED
    val planeAnimationProgress = (System.currentTimeMillis() % duration.toInt()) / duration

    (0 until HORIZONTAL_LINE_COUNT).map { index ->
      top + (planeHeight * planeAnimationProgress + (index / HORIZONTAL_LINE_COUNT.toFloat()) * planeHeight) % planeHeight
    }.flatMap { yOffset ->
      listOf(0f, yOffset, width.toFloat(), yOffset)
    }.let { lines ->
      drawLines(lines.toFloatArray(), gridPaint)
    }
  }

  private fun Canvas.drawTime(progress: Float, zonedDateTime: ZonedDateTime) {
    val chars: List<Char> = zonedDateTime.format(dateTimeFormatter).toList()
    chars.forEachIndexed { index, char ->
      val coefficient: Float = (lastVisibleTime / 10f.pow(index)) % 10
      val scale: Float = progress.pow(coefficient)
      save()
      scale(scale, scale, width / 2f, height / 2f)
      drawText(
        "$char",
        width * (index + 1) / (chars.size + 1f),
        height / 2f - ((textPaint.descent() + textPaint.ascent()) / 2),
        textPaint
      )
      restore()
    }
  }

  private fun Canvas.drawOverlay(progress: Float) {
    gPaint.shader = RadialGradient(
      width / 2f,
      height / 2f,
      width / 2f,
      intArrayOf(Color.WHITE, Color.DKGRAY, Color.BLACK),
      floatArrayOf(0f, progress * 0.95f, progress * 1f),
      Shader.TileMode.MIRROR
    )
    drawCircle(width / 2f, height / 2f, width / 2f, gPaint)
  }
}
