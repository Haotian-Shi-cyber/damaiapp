package com.damai.helper

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var editTime: EditText
    private lateinit var editTicket: EditText
    private lateinit var buttonStart: Button
    private lateinit var dialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    /**
     * 初始化应用
     */
    private fun init() {
        // 查找控件
        sharedPrefs = getPreferences(MODE_PRIVATE)
        editTime = findViewById(R.id.editTextTime)
        editTicket = findViewById(R.id.editTextTicket)
        buttonStart = findViewById(R.id.buttonStart)
        // 初始化输入框内容
        editTime.setText(sharedPrefs.getString("time", ""))
        editTicket.setText(sharedPrefs.getString("ticket", ""))
        // 设置监听
        editTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 文本将要发生改变时调用，可以在这里处理相关逻辑
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本发生变化时调用，可以在这里处理相关逻辑
            }
            override fun afterTextChanged(s: Editable?) {
                // 文本已经发生改变时调用，可以在这里处理相关逻辑
                DamaiHelperService.time = s.toString()
                sharedPrefs.edit().apply {
                    putString("time", s.toString())
                    apply()
                }  // 保存场次
            }
        })
        editTicket.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 文本将要发生改变时调用，可以在这里处理相关逻辑
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本发生变化时调用，可以在这里处理相关逻辑
            }
            override fun afterTextChanged(s: Editable?) {
                // 文本已经发生改变时调用，可以在这里处理相关逻辑
                DamaiHelperService.ticket = s.toString()
                sharedPrefs.edit().apply {
                    putString("ticket", s.toString())
                    apply()
                }  // 保存票档
            }
        })
        buttonStart.setOnClickListener {
            if (DamaiHelperService.isStarted()) {
                stop()
            } else {
                start()
            }
        }
    }

    /**
     * 启动抢票服务
     */
    private fun start() {
        if (!isCanDrawOverlay()) {
            showOverlayDialog()
            return
        }
        if (!isAccessibilitySettingsOn(DamaiHelperService::class.java)) {
            showAccessDialog()
            return
        }
        val time = editTime.text.toString()
        val ticket = editTicket.text.toString()
        if (TextUtils.isEmpty(ticket)) {
            showDialog(getString(R.string.edit_ticket_hint))
            editTicket.requestFocus()
            return
        }
        // 启动大麦APP和抢票服务
        if (startDamaiApp()) {
            DamaiHelperService.time = time
            DamaiHelperService.ticket = ticket
            DamaiHelperService.start()
            buttonStart.setText(R.string.stop_server)
        }
    }

    /**
     * 停止抢票服务
     */
    private fun stop() {
        DamaiHelperService.stop()
        buttonStart.setText(R.string.start_server)
    }

    /**
     * 跳转到悬浮窗权限设置界面
     */
    private fun setOverlayPermission() {
        val uri = Uri.parse("package:$packageName")
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
    }

    /**
     * 跳转到无障碍服务设置界面
     */
    private fun setAccessibilityServer() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /**
     * 启动大麦APP
     */
    private fun startDamaiApp(): Boolean {
        return startApp(
            getString(R.string.damai_app_package_name),
            DamaiHelperService.ME_UI,
            getString(R.string.damai_app_not_installed)
        )
    }

    /**
     * 悬浮窗权限未开启弹窗
     */
    private fun showOverlayDialog() {
        dialog = AlertDialog.Builder(this)
            .setTitle("请开启【${getString(R.string.app_name)}】的悬浮窗权限")
            .setPositiveButton(getString(R.string.open_setting)) { dialog, _ ->
                dialog.dismiss()
                setOverlayPermission()
            }
            .create()
        dialog.show()
    }

    /**
     * 无障碍服务未开启弹窗
     */
    private fun showAccessDialog() {
        dialog = AlertDialog.Builder(this)
            .setTitle("请开启【${getString(R.string.accessibility_service_label)}】的无障碍服务")
            .setPositiveButton(getString(R.string.open_setting)) { dialog, _ ->
                dialog.dismiss()
                setAccessibilityServer()
            }
            .create()
        dialog.show()
    }

    /**
     * 消息弹窗
     */
    private fun showDialog(text: String) {
        dialog = AlertDialog.Builder(this)
            .setTitle(text)
            .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }

    override fun onResume() {
        if (!DamaiHelperService.isStarted()) buttonStart.setText(R.string.start_server)
        super.onResume()
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }
}