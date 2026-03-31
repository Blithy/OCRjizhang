package com.example.ocrjizhang.ui.auth

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.ocrjizhang.MainNavGraphDirections

fun NavController.navigateToHomeClearingAuth() {
    val options = NavOptions.Builder()
        .setPopUpTo(graph.id, true)
        .setLaunchSingleTop(true)
        .build()
    navigate(MainNavGraphDirections.actionGlobalHomeFragment(), options)
}

fun NavController.navigateToLoginClearingBackStack() {
    val options = NavOptions.Builder()
        .setPopUpTo(graph.id, true)
        .setLaunchSingleTop(true)
        .build()
    navigate(MainNavGraphDirections.actionGlobalLoginFragment(), options)
}
