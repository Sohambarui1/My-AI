package com.example.myai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myai.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.max
import kotlin.math.min


class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SessionManager.initialize(this)
        UserStore.initialize(this)

        setContent {
            MyAITheme {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    UserStore.initialize(context)
                }

                val navController = rememberNavController()
                val isLoggedIn = SessionManager.isLoggedIn(context)

                // Determine the start destination based on login state
                val startDestination = if (isLoggedIn) {
                    "chat"
                } else {
                    "onboarding"
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable("onboarding") {
                        OnboardingScreen {
                            navController.navigate("login") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }

                    composable("login") {
                        LoginScreen(
                            chatViewModel = chatViewModel,
                            onLoginSuccess = {
                                navController.navigate("chat") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onSignUpClick = {
                                navController.navigate("signup")
                            }
                        )
                    }

                    composable("signup") {
                        val context = LocalContext.current

                        SignUpScreen(
                            onSignUpSuccess = { email, password ->
                                // Save credentials
                                SessionManager.saveCredentials(
                                    context,
                                    email,
                                    password,
                                    rememberMe = true
                                )
                                // Navigate to chat
                                navController.navigate("chat") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("chat") {
                        ChatScreen(viewModel = chatViewModel, navController = navController)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    val email = remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    // Soft color palette
    val primaryColor = Color(0xFF6C63FF)  // Soft purple
    val userBubbleColor = Color(0xFFEDE7FF)  // Very light purple
    val aiBubbleColor = Color.White
    val backgroundColor = Color(0xFFF9F9FF)  // Off-white with purple tint
    val surfaceColor = Color.White
    val outlineColor = Color(0xFFE0E0E0)

    LaunchedEffect(Unit) {
        email.value = SessionManager.getSavedCredentials(context).first ?: ""
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    var showVoiceError by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf("") }
    val voiceRecognitionHelper = rememberVoiceRecognitionHelper(
        onResult = { input = it },
        onError = { error ->
            voiceError = error
            showVoiceError = true
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceRecognitionHelper.startListening()
        } else {
            voiceError = "Microphone permission required for voice input"
            showVoiceError = true
        }
    }

    if (showVoiceError) {
        AlertDialog(
            onDismissRequest = { showVoiceError = false },
            title = { Text("Voice Input") },
            text = { Text(voiceError) },
            confirmButton = {
                TextButton(
                    onClick = { showVoiceError = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = primaryColor
                    )
                ) {
                    Text("OK")
                }
            },
            containerColor = surfaceColor
        )
    }

    Box(modifier = Modifier.background(backgroundColor)) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    if (isTyping) {
                        TypingIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                ambientColor = primaryColor.copy(alpha = 0.1f),
                                spotColor = primaryColor.copy(alpha = 0.1f)
                            ),
                        color = surfaceColor,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .navigationBarsPadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Voice input button
                            FloatingMicButton(
                                isListening = voiceRecognitionHelper.isListening,
                                onClick = {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                },
                                modifier = Modifier.size(48.dp)
                            )

                            // Message input field
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp, max = 120.dp),
                                placeholder = {
                                    Text(
                                        "Type a message...",
                                        color = Color.Gray.copy(alpha = 0.6f)
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = surfaceColor,
                                    unfocusedContainerColor = surfaceColor,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color(0xFF333333),
                                    unfocusedTextColor = Color(0xFF333333)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (input.isNotBlank()) {
                                            viewModel.sendMessage(input)
                                            input = ""
                                        }
                                    }
                                ),
                                trailingIcon = {
                                    AnimatedVisibility(
                                        visible = input.isNotBlank(),
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (input.isNotBlank()) {
                                                    viewModel.sendMessage(input)
                                                    input = ""
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Send",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize()) {
                // Navigation bar that scrolls with messages
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                            bottom = 2.dp
                        ),
                    color = Color.Transparent,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.chatbot),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(32.dp)
                        )

                        Text(
                            "AI Assistant",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor
                            )
                        )

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = primaryColor
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.width(240.dp)
                            ) {
                                if (email.value.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Signed in as ${email.value.take(20)}${if (email.value.length > 20) "..." else ""}",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium.copy( // Changed from bodySmall
                                                    fontWeight = FontWeight.Bold, // Added bold
                                                    color = Color(0xFF333333) // Optional: Darker text for better readability
                                                )

                                            )
                                        },
                                        onClick = { showMenu = false },
                                        enabled = false
                                    )
                                    Divider(color = outlineColor)
                                }
                                DropdownMenuItem(
                                    text = { Text("Clear conversation" ,  color = Color(0xFFB388FF) ) },
                                    onClick = {
                                        viewModel.clearMessages()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            tint = Color(0xFFB388FF)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Logout" ,  color = Color(0xFFB388FF) ) },
                                    onClick = {
                                        coroutineScope.launch {
                                            SessionManager.clearSession(context)
                                            viewModel.clearMessages()
                                            navController.navigate("login") {
                                                popUpTo("chat") { inclusive = true }
                                            }
                                        }
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.ExitToApp,
                                            null,
                                            tint = Color(0xFFB388FF)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(
                        top = 2.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    items(
                        items = messages.reversed(),
                        key = { it.id }
                    ) { message ->
                        ChatBubble(
                            message = message,
                            isUser = message.isUser,
                            userBubbleColor = userBubbleColor,
                            aiBubbleColor = aiBubbleColor,
                            viewModel = viewModel,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    isUser: Boolean,
    userBubbleColor: Color,
    aiBubbleColor: Color,
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isUser) userBubbleColor else aiBubbleColor
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val context = LocalContext.current
    var showCopyButton by remember { mutableStateOf(false) }
    var showCopyToast by remember { mutableStateOf(false) }
    var isTextSelected by remember { mutableStateOf(false) }

    // Text animation state
    var animatedText by remember { mutableStateOf("") }
    val textToAnimate = message.text

    // Animation effect (only for AI messages)
    if (!isUser && !message.isAnimated) {
        LaunchedEffect(textToAnimate) {
            animatedText = ""
            textToAnimate.forEachIndexed { index, _ ->
                delay(30) // Adjust typing speed
                animatedText = textToAnimate.take(index + 1)
            }
            viewModel.markMessageAsAnimated(message.id)
        }
    } else {
        animatedText = textToAnimate
    }

    val textSelectionColors = TextSelectionColors(
        handleColor = Color(0xFFC3B4FD),
        backgroundColor = Color(0xFF9574FC).copy(alpha = 0.4f)
    )

    // Clear both copy button and text selection when clicking outside
    val focusManager = LocalFocusManager.current

    LaunchedEffect(showCopyToast) {
        if (showCopyToast) {
            delay(2000)
            showCopyToast = false
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showCopyButton = false
                        isTextSelected = false
                        focusManager.clearFocus()
                    },
                    onLongPress = {
                        showCopyButton = true
                    }
                )
            }
    ) {
        Column(
            horizontalAlignment = alignment,
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
                Surface(
                    color = bubbleColor,
                    shape = if (isUser)
                        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                    else
                        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                    shadowElevation = 1.dp,
                    tonalElevation = 1.dp,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Box {
                        SelectionContainer(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = animatedText, // Use animated text here
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (isUser) Color(0xFF333333) else Color(0xFF444444)
                                ),
                                modifier = Modifier.onFocusChanged { focusState ->
                                    isTextSelected = focusState.isFocused
                                }
                            )
                        }
                    }
                }
            }

            // Copy Button
            AnimatedVisibility(
                visible = showCopyButton,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Chat message", message.text)
                        clipboard.setPrimaryClip(clip)
                        showCopyToast = true
                        showCopyButton = false
                        isTextSelected = false
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF6C63FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
            }

            // Toast Message
            AnimatedVisibility(showCopyToast) {
                Text(
                    text = "Copied to clipboard",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6C63FF),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                )
            }

            Text(
                text = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    .format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}


@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 160.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), // Tighter padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced dot animation
                DotTypingAnimation(
                    modifier = Modifier.size(24.dp) // Better dot sizing
                )
                Spacer(modifier = Modifier.width(6.dp)) // Reduced spacing
                Text(
                    text = "AI is thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray.copy(alpha = 0.8f) // Slightly darker for better readability
                )
            }
        }
    }
}

@Composable
private fun DotTypingAnimation(modifier: Modifier = Modifier) {
    val dotSize = 8.dp
    val animationDelay = 200
    val delayUnit = 3

    @Composable
    fun Dot(alpha: Float) = Box(
        modifier = Modifier
            .size(dotSize)
            .background(
                color = Color.Gray.copy(alpha = alpha),
                shape = CircleShape
            )
    )

    val infiniteTransition = rememberInfiniteTransition()

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animationDelay * delayUnit
                0.7f at animationDelay with LinearEasing
                0.3f at animationDelay * 2
            },
            repeatMode = RepeatMode.Restart
        )
    )

    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animationDelay * delayUnit
                0.3f at animationDelay
                0.7f at animationDelay * 2 with LinearEasing
                0.3f at animationDelay * 3
            },
            repeatMode = RepeatMode.Restart
        )
    )

    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = animationDelay * delayUnit
                0.3f at animationDelay * 2
                0.7f at animationDelay * 3 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp) // Consistent dot spacing
    ) {
        Dot(alpha1)
        Dot(alpha2)
        Dot(alpha3)
    }
}

@Composable
fun FloatingMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isListening) {
                    Color(0xFF6C63FF).copy(alpha = glowAlpha)
                } else {
                    Color(0xFF6C63FF).copy(alpha = 0.1f)
                }
            )
            .border(
                width = if (isListening) 0.dp else 1.dp,
                color = Color(0xFF6C63FF).copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_mic),
            contentDescription = "Voice input",
            tint = if (isListening) Color.White else Color(0xFF6C63FF),
            modifier = Modifier.size(24.dp)
        )
    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to AI Assistant",
            description = "Your personal AI companion for smart conversations and assistance",
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.chatbot),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(100.dp)
                )
            }
        ),
        OnboardingPage(
            title = "Voice & Text Input",
            description = "Chat using voice commands or typing - whatever works best for you",
            icon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF6C63FF)
                )
            }
        ),
        OnboardingPage(
            title = "Smart Responses",
            description = "Get intelligent, contextual responses to your questions",
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Smart AI",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF6C63FF)
                )
            }
        )
    )


    val pagerState = rememberPagerState()
    var autoScrollEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState, autoScrollEnabled) {
        while (autoScrollEnabled) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % pages.size
            pagerState.animateScrollToPage(
                nextPage,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FF))
    ) {
        HorizontalPager(
            state = pagerState,
            count = pages.size,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { autoScrollEnabled = false }
                    }
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                pages[page].icon()

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = pages[page].title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = pages[page].description,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF666666)),
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) Color(0xFF6C63FF)
                                else Color(0xFFD0D0D0)
                            )
                            .padding(2.dp)
                    )
                    if (index < pages.size - 1) Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    autoScrollEnabled = false
                    coroutineScope.launch {
                        if (pagerState.currentPage < pages.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onGetStarted()
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}


data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)