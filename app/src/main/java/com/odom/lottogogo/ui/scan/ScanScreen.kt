package com.odom.lottogogo.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.odom.lottogogo.R
import com.odom.lottogogo.ui.isCompactWidth
import com.odom.lottogogo.ui.theme.Dimens
import com.odom.lottogogo.ui.theme.LottoGogoTheme

const val SCAN_RESULT_SHEET_TAG = "scan_result_sheet"
const val SCAN_RESULT_DIALOG_TAG = "scan_result_dialog"

@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val prompt = stringResource(R.string.scan_prompt)

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        viewModel.onScanned(result.contents)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (granted) scanLauncher.launch(scanOptions(prompt))
    }

    ScanContent(
        state = state,
        permissionDenied = permissionDenied,
        windowSizeClass = windowSizeClass,
        onScanClick = {
            if (context.hasCameraPermission()) {
                permissionDenied = false
                scanLauncher.launch(scanOptions(prompt))
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onOpenSettings = { context.openAppSettings() },
        onSave = viewModel::save,
        onDismiss = viewModel::dismiss,
    )
}

private fun scanOptions(prompt: String) = ScanOptions()
    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
    .setBeepEnabled(false)
    .setOrientationLocked(false)
    .setPrompt(prompt)

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanContent(
    state: ScanUiState,
    permissionDenied: Boolean,
    windowSizeClass: WindowSizeClass,
    onScanClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(Dimens.ScreenPadding), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = Dimens.SinglePaneMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.scan_description), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onScanClick) { Text(stringResource(R.string.action_scan)) }
            if (permissionDenied) {
                Text(stringResource(R.string.scan_permission_denied), color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onOpenSettings) { Text(stringResource(R.string.action_open_settings)) }
            }
            if (state.isInvalidQr) {
                Text(stringResource(R.string.scan_invalid_qr), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    val result = state.result ?: return
    if (windowSizeClass.isCompactWidth) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            ScanResultContent(result, state.isSaved, onSave, onDismiss, Modifier.testTag(SCAN_RESULT_SHEET_TAG))
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card {
                ScanResultContent(result, state.isSaved, onSave, onDismiss, Modifier.testTag(SCAN_RESULT_DIALOG_TAG))
            }
        }
    }
}

@PreviewScreenSizes
@Composable
private fun ScanContentPreview() {
    LottoGogoTheme {
        ScanContent(
            state = ScanUiState(),
            permissionDenied = true,
            windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
            onScanClick = {},
            onOpenSettings = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
