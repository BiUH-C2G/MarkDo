package me.earzuchan.markdo.ui.widgets

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.earzuchan.markdo.utils.ResUtils.v
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun MIcon(res: DrawableResource, tint: Color = LocalContentColor.current) = Icon(res.v, null, tint = tint)