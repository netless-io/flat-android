package io.agora.flat.ui.activity.cloud.preview

sealed class PreviewAction {
    object OnClose : PreviewAction()
    object OnLoadFinished : PreviewAction()
}