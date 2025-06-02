package dev.tkuenneth.photopickerdemo

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.SurfaceControlViewHost.SurfacePackage
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class EmbeddedPhotoPickerActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
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

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@Composable
fun EmbeddedPhotoPicker(provider: EmbeddedPhotoPickerProvider) {
    var photoPickerSession by remember { mutableStateOf<EmbeddedPhotoPickerSession?>(null) }
    var selectedUris by rememberSaveable { mutableStateOf<List<Uri>>(emptyList()) }
    var viewSize by remember { mutableStateOf<IntSize>(IntSize.Zero) }
    var surfacePackage by remember { mutableStateOf<SurfacePackage?>(null) }
    val context = LocalContext.current
    val view = remember {
        SurfaceView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            setZOrderOnTop(true)
        }
    }
    val photoPickerClient = remember {
        object : EmbeddedPhotoPickerClient {
            override fun onSelectionComplete() {}

            override fun onSessionError(cause: Throwable) {}

            override fun onSessionOpened(session: EmbeddedPhotoPickerSession) {
                photoPickerSession = session
            }

            override fun onUriPermissionGranted(uris: List<Uri>) {
                selectedUris = (selectedUris + uris.toSet()).distinct()
            }

            override fun onUriPermissionRevoked(uris: List<Uri>) {
                selectedUris = selectedUris - uris.toSet()
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AndroidView(
            factory = { view },
            update = { view ->
                photoPickerSession?.surfacePackage?.let {
                    surfacePackage = it
                    view.setChildSurfacePackage(
                        it
                    )
                }
            },
            modifier = Modifier
                .weight(1F)
                .border(width = 1.dp, color = MaterialTheme.colorScheme.primary)
                .onGloballyPositioned { coordinates ->
                    viewSize = coordinates.size
                })
        key(viewSize) {
            if (viewSize != IntSize.Zero && photoPickerSession == null) {
                provider.openSession(
                    view.windowToken,
                    view.display.displayId,
                    viewSize.width,
                    viewSize.height,
                    EmbeddedPhotoPickerFeatureInfo.Builder()
                        .setMaxSelectionLimit(3)
                        .setPreSelectedUris(selectedUris)
                        .build(),
                    context.mainExecutor,
                    photoPickerClient
                )
            }
        }
        val text = selectedUris.mapNotNull {
            it.lastPathSegment
        }.joinToString(separator = ", ")
        Text(
            text = text.takeIf { it.isNotEmpty() } ?: stringResource(R.string.no_image),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            photoPickerSession?.close()
            photoPickerSession = null
            surfacePackage?.release()
            surfacePackage = null
            view.clearChildSurfacePackage()
        }
    }
}
