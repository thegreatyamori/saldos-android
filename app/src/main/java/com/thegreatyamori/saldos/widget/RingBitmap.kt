package com.thegreatyamori.saldos.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.roundToInt

object RingBitmap {

    fun create(
        context: Context,
        ratio: Float,
        color: Int,
        trackColor: Int,
        sizeDp: Int = 56,
        strokeDp: Float = 7f
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val pixels = (sizeDp * density).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = strokeDp * density
        val inset = stroke / 2f
        val bounds = RectF(inset, inset, pixels - inset, pixels - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }

        paint.color = trackColor
        canvas.drawArc(bounds, 0f, 360f, false, paint)

        paint.color = color
        canvas.drawArc(bounds, -90f, 360f * ratio.coerceIn(0f, 1f), false, paint)

        return bitmap
    }
}
