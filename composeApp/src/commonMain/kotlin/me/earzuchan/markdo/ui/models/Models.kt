package me.earzuchan.markdo.ui.models

class DialogActionItem(val text: String, val finalDismiss: Boolean = true, val action: () -> Unit = {})

class AppToastModel(val text: String, val duration: Long)