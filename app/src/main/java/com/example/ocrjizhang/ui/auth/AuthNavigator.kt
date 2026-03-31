package com.example.ocrjizhang.ui.auth

import androidx.navigation.NavController
import com.example.ocrjizhang.R

fun NavController.navigateToHomeClearingAuth() {
    navigate(R.id.homeFragment) {
        popUpTo(graph.id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

fun NavController.navigateToLoginClearingBackStack() {
    navigate(R.id.loginFragment) {
        popUpTo(graph.id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
