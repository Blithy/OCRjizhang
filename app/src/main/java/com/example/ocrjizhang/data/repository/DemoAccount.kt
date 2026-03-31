package com.example.ocrjizhang.data.repository

object DemoAccount {
    const val USERNAME = "demo"
    const val PASSWORD = "123456"
    const val USER_ID = 9_001_001L
    const val NICKNAME = "本机演示用户"
    const val TOKEN = "local-demo-token"

    fun matches(username: String, password: String): Boolean =
        username.trim() == USERNAME && password == PASSWORD
}
