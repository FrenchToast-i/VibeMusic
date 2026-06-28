package com.maxrave.simpmusic.ui.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.ui.PlatformWebView
import com.maxrave.simpmusic.expect.ui.rememberWebViewState
import com.maxrave.simpmusic.ui.component.RippleIconButton
import kotlinx.coroutines.launch
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.baseline_arrow_back_ios_new_24

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun LastFmLoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val hazeState = rememberHazeState()
    
    val lastFmSessionKey by viewModel.lastFmSessionKey.collectAsState()
    val lastFmUsername by viewModel.lastFmUsername.collectAsState()
    val lastFmApiKey by viewModel.lastFmApiKey.collectAsState()
    val lastFmApiSecret by viewModel.lastFmApiSecret.collectAsState()
    
    var authUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }
    var shouldRetry by rememberSaveable { mutableStateOf(false) }
    var showCredentialInput by rememberSaveable { mutableStateOf(false) }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var apiSecretInput by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val viewModelScope = rememberCoroutineScope()
    
    // Hide bottom navigation when entering this screen
    LaunchedEffect(Unit) {
        hideBottomNavigation()
    }
    
    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }
    
    // Check if credentials are set
    LaunchedEffect(Unit) {
        if (lastFmApiKey.isEmpty() || lastFmApiSecret.isEmpty()) {
            showCredentialInput = true
            isLoading = false
        } else {
            viewModel.authenticateLastFm()
        }
    }
    
    // Open auth URL in external browser when available
    LaunchedEffect(authUrl) {
        val url = authUrl
        if (url != null) {
            Logger.d("LastFmLoginScreen", "Opening auth URL: $url")
            uriHandler.openUri(url)
            // Start polling for session completion
            Logger.d("LastFmLoginScreen", "Starting completeLastFmLogin")
            viewModel.completeLastFmLogin()
        }
    }
    
    // Handle login success
    LaunchedEffect(lastFmSessionKey) {
        if (lastFmSessionKey.isNotEmpty()) {
            viewModel.makeToast("Login successful")
            navController.navigateUp()
        }
    }
    
    // Get auth token and URL when credentials are set
    LaunchedEffect(Unit) {
        viewModel.lastFmAuthUrl.collect { url ->
            if (url.isNotEmpty()) {
                authUrl = url
                isLoading = false
            }
        }
    }
    
    // Save credentials and start auth
    fun saveCredentialsAndAuth() {
        viewModelScope.launch {
            viewModel.setLastFmCredentials(apiKeyInput, apiSecretInput)
            showCredentialInput = false
            isLoading = true
            viewModel.authenticateLastFm()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column {
            Spacer(
                Modifier.size(
                    innerPadding.calculateTopPadding() + 64.dp,
                )
            )
            
            if (showCredentialInput) {
                // Credential input form
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    Text(
                        text = "Enter Last.fm API Credentials",
                        style = typo().titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Get your API key from https://www.last.fm/api/account/create",
                        style = typo().bodySmall,
                        color = Color(0xC4FFFFFF),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    OutlinedTextField(
                        value = apiSecretInput,
                        onValueChange = { apiSecretInput = it },
                        label = { Text("API Secret", color = Color.White) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.size(24.dp))
                    Button(
                        onClick = { saveCredentialsAndAuth() },
                        enabled = apiKeyInput.isNotEmpty() && apiSecretInput.isNotEmpty()
                    ) {
                        Text("Save and Login")
                    }
                }
            } else if (isLoading) {
                // Loading state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Authenticating with Last.fm...",
                        style = typo().bodyLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (loginError != null) {
                // Error state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    Text(
                        text = "Login failed",
                        style = typo().titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = loginError ?: "Unknown error",
                        style = typo().bodyMedium,
                        color = Color(0xC4FFFFFF),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(32.dp))
                    Button(
                        onClick = {
                            loginError = null
                            isLoading = true
                            authUrl = null
                            shouldRetry = true
                        }
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                // Instructions for user
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                ) {
                    Text(
                        text = "Authorization page opened in your browser",
                        style = typo().titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "1. Log in to Last.fm in your browser\n2. Authorize this app\n3. The app will automatically complete login",
                        style = typo().bodyMedium,
                        color = Color(0xC4FFFFFF),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(32.dp))
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
        
        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = "Last.fm Login",
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        Res.drawable.baseline_arrow_back_ios_new_24,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}
