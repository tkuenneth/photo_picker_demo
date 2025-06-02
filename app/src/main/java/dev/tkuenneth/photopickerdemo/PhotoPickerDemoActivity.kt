package dev.tkuenneth.photopickerdemo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest


class PhotoPickerDemoActivity : ComponentActivity() {

    private lateinit var uri: MutableState<Uri?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val chooseImageLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { result ->
            uri.value = result
        }
        setContent {
            MaterialTheme {
                uri = remember { mutableStateOf(null) }
                Content(
                    uri = uri.value,
                    showEmbeddedPickerButton = Build.VERSION.SDK_INT >= 36,
                    chooseImage = {
                        chooseImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    showEmbeddedPicker = {
                        startActivity(Intent(this, EmbeddedPhotoPickerActivity::class.java))
                    })
            }
        }
    }
}

@Composable
fun Content(
    uri: Uri?,
    showEmbeddedPickerButton: Boolean,
    chooseImage: () -> Unit,
    showEmbeddedPicker: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = chooseImage
            ) {
                Text(text = stringResource(id = R.string.choose_image))
            }
            if (showEmbeddedPickerButton) {
                Button(
                    onClick = showEmbeddedPicker
                ) {
                    Text(text = stringResource(id = R.string.show_embedded_picker))
                }
            }
        }
        Box(
            contentAlignment = Center, modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (uri == null) Text(
                text = stringResource(id = R.string.no_image),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            else AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(uri).crossfade(true)
                    .build(), contentDescription = null, modifier = Modifier.fillMaxSize()
            )
        }
    }
}
