package com.damai.helper

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import java.lang.Thread.sleep


class DamaiHelperService : AccessibilityService() {

    companion object {
        // 首页-我的
        const val ME_UI = "cn.damai.mine.activity.MineMainActivity"
        // 演唱会详情页
        const val LIVE_DETAIL_UI = "cn.damai.trade.newtradeorder.ui.projectdetail.ui.activity.ProjectDetailActivity"
        // 票档选择页
        const val TICKET_PRICE_UI = "cn.damai.commonbusiness.seatbiz.sku.qilin.ui.NcovSkuActivity"
        // 订单提交页
        const val ORDER_SUBMIT_UI = "cn.damai.ultron.view.activity.DmOrderActivity"

        const val ID_LIVE_DETAIL_BUY = "tv_left_main_text"  // 详情页-开抢
        const val ID_PLUS_TICKET = "img_jia"  // 选择票数
        const val ID_CONFIRM_BUY = "btn_buy"  // 确定购买

        const val STEP_FIRST = 1
        const val STEP_SECOND = 2
        const val STEP_THIRD = 3

        const val SERVER_STOPPED = 0
        const val SERVER_STARTED = 1

        lateinit var time: String  // 场次
        lateinit var ticket: String  // 票档

        @SuppressLint("StaticFieldLeak")
        var instance: DamaiHelperService? = null

        fun start() {
            instance?.startServer()
        }

        fun stop() {
            instance?.stopServer()
        }

        fun isStarted(): Boolean {
            return instance?.status == SERVER_STARTED
        }
    }

    private var isStop = true
    private var step = STEP_FIRST
    private var status = SERVER_STOPPED

    private var windowManager: WindowManager? = null
    private var floatButton: FloatButton? = null

    private lateinit var aboutToStartStr: String
    private lateinit var submitOrderStr: String

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatButton = FloatButton(this)
        aboutToStartStr = getString(R.string.about_to_start)
        submitOrderStr = getString(R.string.submit_order)
    }

    /**
     * 监听窗口变化的回调
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (isStop || event == null) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            when (event.className.toString()) {
                LIVE_DETAIL_UI -> step = STEP_FIRST
                TICKET_PRICE_UI -> step = STEP_SECOND
                ORDER_SUBMIT_UI -> step = STEP_THIRD
            }
        }
        when (step) {
            STEP_FIRST -> placeOrder(event)
            STEP_SECOND -> confirmOrder(event)
            STEP_THIRD -> submitOrder(event)
        }
    }

    /**
     * 第一步：抢票下单
     */
    private fun placeOrder(event: AccessibilityEvent) {
        event.source?.let { source ->
            source.getNodeById(dmNodeId(ID_LIVE_DETAIL_BUY))?.let { node ->
                if (node.text() != aboutToStartStr) {
                    sleep(100)
                    node.click()
                }
            }
        }
    }

    /**
     * 第二步：确认订单
     */
    private fun confirmOrder(event: AccessibilityEvent) {
        event.source?.let { source ->
            source.getNodeByText(time)?.let { node ->
                sleep(100)
                node.click()
            }
            source.getNodeByText(ticket)?.let { node ->
                sleep(100)
                node.click()
            }
            source.getNodeById(dmNodeId(ID_CONFIRM_BUY))?.let { node ->
                sleep(100)
                node.click()
            }
        }
    }

    /**
     * 第三步：提交订单
     */
    private fun submitOrder(event: AccessibilityEvent) {
        event.source?.let { source ->
            source.getNodeByText(submitOrderStr, true)?.let { node ->
                sleep(100)
                node.click()
                stopServer()
            }
        }
    }

    /**
     * 创建通知
     */
    private fun createForegroundNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        // 创建通知渠道，一定要写在创建显示通知之前，创建通知渠道的代码只有在第一次执行才会创建
        // 以后每次执行创建代码检测到该渠道已存在，因此不会重复创建
        val channelId = "damai"
        notificationManager?.createNotificationChannel(NotificationChannel(
            channelId,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        ))
        return NotificationCompat.Builder(this, channelId)
            // 设置点击notification跳转，比如跳转到设置页
            .setContentIntent(PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                FLAG_IMMUTABLE
            ))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.desc))
            .build()
    }

    /**
     * 显示开抢/暂停按钮
     */
    @SuppressLint("InflateParams", "ResourceAsColor", "ClickableViewAccessibility")
    private fun showFloatButton() {
        // 设置悬浮按钮样式
        floatButton?.setText(R.string.start_check)
        floatButton?.setTextColor(Color.WHITE)
        floatButton?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24F)
        floatButton?.setBackgroundResource(R.drawable.float_start_button_background)
        floatButton?.stateListAnimator = null
        // 设置悬浮按钮参数
        val flag = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = WindowManager.LayoutParams(
            150,
            150,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flag,
            PixelFormat.TRANSPARENT
        ).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.START }
        // 将悬浮按钮添加到 WindowManager 中
        windowManager?.addView(floatButton, params)
        // 设置悬浮按钮监听
        floatButton?.setOnClickListener {
            if (isStop) {
                floatButton?.setText(R.string.stop_check)
                floatButton?.setBackgroundResource(R.drawable.float_stop_button_background)
            } else {
                floatButton?.setText(R.string.start_check)
                floatButton?.setBackgroundResource(R.drawable.float_start_button_background)
            }
            isStop = !isStop
        }
    }

    /**
     * 开启服务
     */
    private fun startServer() {
        if (status == SERVER_STOPPED) {
            // 显示浮动按钮
            showFloatButton()
            // 创建Notification渠道，并开启前台服务
            startForeground(1, createForegroundNotification())
            step = STEP_FIRST
            status = SERVER_STARTED
        }
    }

    /**
     * 停止服务
     */
    private fun stopServer() {
        if (status == SERVER_STARTED) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            windowManager?.removeView(floatButton)
            isStop = true
            step = STEP_FIRST
            status = SERVER_STOPPED
            stopSelf()
        }
    }

    override fun onInterrupt() {

    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}