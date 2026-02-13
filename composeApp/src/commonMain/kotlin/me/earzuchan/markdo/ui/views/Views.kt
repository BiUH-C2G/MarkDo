package me.earzuchan.markdo.ui.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.earzuchan.markdo.duties.*
import me.earzuchan.markdo.resources.Res
import me.earzuchan.markdo.resources.cancel
import me.earzuchan.markdo.resources.confirm
import me.earzuchan.markdo.resources.dashboard
import me.earzuchan.markdo.ui.models.DialogActionItem
import me.earzuchan.markdo.utils.ComposeUtils.only
import me.earzuchan.markdo.utils.ResUtils.t
import me.earzuchan.markdo.utils.ResUtils.v
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun <T> MOptionsDialog(title: String, options: List<Pair<String, T>>, selectedValue: T, onDismiss: () -> Unit, onConfirm: (T) -> Unit) {
    var tempSelectedValue by remember { mutableStateOf(selectedValue) }

    MAlertDialog(
        title, actionsInColumn = false, onDismissRequest = onDismiss,
        actions = listOf(DialogActionItem(Res.string.cancel.t), DialogActionItem(Res.string.confirm.t) { onConfirm(tempSelectedValue) })
    ) {
        Column(Modifier.selectableGroup()) {
            options.forEach { (optionTitle, optionValue) ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp).selectable(
                        (tempSelectedValue == optionValue), role = Role.RadioButton
                    ) { tempSelectedValue = optionValue }.padding(horizontal = 24.dp),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Text(optionTitle, style = MaterialTheme.typography.bodyLarge)
                    RadioButton((tempSelectedValue == optionValue), null)
                }
            }
        }
    }
}

@Composable
fun MAlertDialog(
    title: String, icon: DrawableResource? = null, description: String? = null, actions: List<DialogActionItem>,
    actionsInColumn: Boolean = true, onDismissRequest: () -> Unit = {}, content: (@Composable () -> Unit)? = null
) = MDialogBase(onDismissRequest) {
    Column(Modifier.width(IntrinsicSize.Min)) {
        val tFdp = 24.dp
        val eSdp = PaddingValues(tFdp, 20.dp)

        val midUpper = icon != null
        val hasContent = content != null

        Column(
            Modifier.padding(tFdp, tFdp, tFdp, if (hasContent) tFdp else 0.dp).fillMaxWidth(),
            Arrangement.spacedBy(16.dp), if (midUpper) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (midUpper) Icon(icon.v, title, tint = MaterialTheme.colorScheme.secondary)

            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            description?.let {
                Text(
                    it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasContent) content()

        @Composable
        fun listActions() = actions.forEach { actionItem ->
            TextButton({
                actionItem.action()
                if (actionItem.finalDismiss) onDismissRequest()
            }, Modifier.only(actionsInColumn) { fillMaxWidth() }.height(40.dp)) {
                Text(actionItem.text, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (actionsInColumn) Column(
            Modifier.padding(eSdp).fillMaxWidth(),
            Arrangement.spacedBy(8.dp), Alignment.CenterHorizontally
        ) { listActions() } else Row(
            Modifier.padding(eSdp).fillMaxWidth(),
            Arrangement.spacedBy(8.dp, Alignment.End), Alignment.CenterVertically
        ) { listActions() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MDialogBase(onDismissRequest: () -> Unit = {}, content: @Composable () -> Unit) = BasicAlertDialog(onDismissRequest) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        content()
    }
}

@Composable
fun MToast(message: String) = Box(Modifier.fillMaxSize()) {
    Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp), MaterialTheme.shapes.large, MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 4.dp) {
        Text(message, Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}