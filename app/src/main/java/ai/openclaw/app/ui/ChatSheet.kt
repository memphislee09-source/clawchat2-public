package ai.openclaw.app.ui

import androidx.compose.runtime.Composable
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.ui.chat.ChatSheetContent

@Composable
fun ChatSheet(viewModel: MainViewModel, onOpenVoice: () -> Unit) {
  ChatSheetContent(viewModel = viewModel, onOpenVoice = onOpenVoice)
}
