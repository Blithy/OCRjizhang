package com.example.ocrjizhang

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用级入口。
 * 这个文件的作用是让 Hilt 在应用启动时完成全局依赖注入初始化。
 */
@HiltAndroidApp
class App : Application()
