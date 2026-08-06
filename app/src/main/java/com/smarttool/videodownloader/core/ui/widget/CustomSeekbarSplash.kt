package com.smarttool.videodownloader.core.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CustomSeekbarSplash : View {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var w = 0f
    private var rectF = RectF()
    private var path = Path()
    private var paintBg: Paint
    private var paintProgress: Paint
    var progress = 0
    private var max = 100
    private var sizeThumb = 0f
    private var sizeBg = 0f
    private var sizePos = 0f
    private var isFirstShader = false
    private var radius = 0f
    private var isCreate = true

    private var colorBg = intArrayOf(Color.WHITE,  Color.parseColor("#D9D9D9"))
    private var colorPr = intArrayOf(
        Color.parseColor("#F46621"),
        Color.parseColor("#F46621"),
    )

    var onProgress: ProgressCallback? = null

    // StateFlow to track progress
    private val _progressFlow = MutableStateFlow(0)
    val progressFlow: StateFlow<Int> = _progressFlow.asStateFlow()

    // Timing variables for custom progress logic
    // First 4 seconds: progress goes from 0% to 70%
    // Next 5 seconds: progress goes from 70% to 90%
    private var startTime = 0L
    private val firstPhaseTarget = 70 // 70% in 4 seconds
    private val secondPhaseTarget = 90 // 90% in next 5 seconds
    private val firstPhaseDuration = 4000L // 4 seconds
    private val secondPhaseDuration = 5000L // 5 seconds

    init {
        w = resources.displayMetrics.widthPixels / 100f
        sizeBg = 3.33f * w
        sizePos = 2.22f * w
        radius = 3.33f * w

        paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (isCreate) {
            isCreate = false
            rectF.set(radius * 2f, (height - radius) / 2f, width - radius * 2f, (height + radius) / 2f)
            path.addRoundRect(rectF, radius / 2f, radius / 2f, Path.Direction.CW)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Initialize start time on first draw
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
        }

        paintBg.apply {
            color = colorBg[0]
            strokeWidth = sizeBg
        }
        canvas.drawLine(sizeThumb / 2 + radius, height / 2f, width - sizeThumb / 2 - radius, height / 2f, paintBg)
        paintBg.apply {
            color = colorBg[1]
            strokeWidth = 2 * sizeBg / 3
        }
        canvas.drawLine(sizeThumb / 2 + radius, height / 2f, width - sizeThumb / 2 - radius, height / 2f, paintBg)

        if (!isFirstShader) {
            paintProgress.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colorPr, null, Shader.TileMode.CLAMP)
            isFirstShader = true
        }
        paintProgress.strokeWidth = sizePos
        val p = (width - sizeThumb - 2 * radius) * progress / max + sizeThumb / 2f + radius
        canvas.drawLine(sizeThumb / 2f + radius, height / 2f, p, height / 2f, paintProgress)

        // Calculate progress based on elapsed time
        val elapsedTime = System.currentTimeMillis() - startTime

        val targetProgress = when {
            elapsedTime < firstPhaseDuration -> {
                // Phase 1: 0% to 70% in 4 seconds
                (firstPhaseTarget * elapsedTime / firstPhaseDuration).toInt()
            }
            elapsedTime < firstPhaseDuration + secondPhaseDuration -> {
                // Phase 2: 70% to 90% in 5 seconds
                val phaseElapsed = elapsedTime - firstPhaseDuration
                firstPhaseTarget + ((secondPhaseTarget - firstPhaseTarget) * phaseElapsed / secondPhaseDuration).toInt()
            }
            else -> {
                // After 9 seconds, stay at 90%
                secondPhaseTarget
            }
        }

        if (progress < targetProgress) {
            progress = targetProgress
            _progressFlow.value = progress
            onProgress?.onProgress(progress)
        }

        // Continue invalidating if progress hasn't reached the target
        if (progress < secondPhaseTarget) {
            postInvalidateDelayed(50) // Update every 50ms for smooth animation
        }
    }




}