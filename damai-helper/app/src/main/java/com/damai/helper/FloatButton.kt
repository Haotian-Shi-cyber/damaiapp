package com.damai.helper


import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import kotlin.math.abs


@SuppressLint("AppCompatCustomView")
class FloatButton(context: Context) : Button(context) {
    private var downX: Int = 0
    private var downY: Int = 0
    private var lastX: Int = 0
    private var lastY: Int = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val rawX = event?.rawX?.toInt() ?: 0
        val rawY = event?.rawY?.toInt() ?: 0
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = rawX
                downY = rawY
                lastX = rawX
                lastY = rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val params = layoutParams as WindowManager.LayoutParams
                params.x += rawX - lastX
                params.y += rawY - lastY
                windowManager.updateViewLayout(this, params)
                lastX = rawX
                lastY = rawY
            }
            MotionEvent.ACTION_UP -> {
                val offset = 10
                if (abs(rawX - downX) < offset && abs(rawY - downY) < offset) performClick()
            }
        }
        return true
    }
}