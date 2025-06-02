package dev.tkuenneth.photopickerdemo

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.photopicker.EmbeddedPhotoPickerClient
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerProvider
import android.widget.photopicker.EmbeddedPhotoPickerProviderFactory
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class EmbeddedPhotoPickerActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val provider = EmbeddedPhotoPickerProviderFactory.create(this)
        setContent {
            MaterialTheme {
                EmbeddedPhotoPicker(provider = provider)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@Composable
fun EmbeddedPhotoPicker(provider: EmbeddedPhotoPickerProvider) {
    val mainExecutor = LocalContext.current.mainExecutor
    val windowToken = LocalView.current.windowToken
    val display = LocalView.current.display
    var photoPickerSession by remember { mutableStateOf<EmbeddedPhotoPickerSession?>(null) }
    var selectedUris by remember { mutableStateOf<List<Uri?>>(emptyList()) }
    var viewSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    isFocusable = true
                    isFocusableInTouchMode = true
                }
            },
            update = { view ->
                photoPickerSession?.surfacePackage?.let { surfacePackage ->
                    view.setChildSurfacePackage(
                        surfacePackage
                    )
                }
            },
            modifier = Modifier
                .weight(1F)
                .background(color = Color.Red)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.primary)
                .onGloballyPositioned { coordinates ->
                    viewSize = coordinates.size
                })
        key(viewSize) {
            if (viewSize != IntSize.Zero) {
                provider.openSession(
                    windowToken,
                    display.displayId,
                    viewSize.width,
                    viewSize.height,
                    EmbeddedPhotoPickerFeatureInfo.Builder().build(),
                    mainExecutor,
                    object : EmbeddedPhotoPickerClient {
                        override fun onSelectionComplete() {}

                        override fun onSessionError(cause: Throwable) {}

                        override fun onSessionOpened(session: EmbeddedPhotoPickerSession) {
                            photoPickerSession = session
                        }

                        override fun onUriPermissionGranted(uris: List<Uri?>) {
                            selectedUris = uris
                        }

                        override fun onUriPermissionRevoked(uris: List<Uri?>) {
                            selectedUris = selectedUris - uris.toSet()
                        }
                    })
            }
        }
        Text(
            text = if (selectedUris.isNotEmpty()) selectedUris.first()
                .toString() else stringResource(R.string.no_image),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            photoPickerSession?.close()
        }
    }
}
