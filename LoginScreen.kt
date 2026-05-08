package com.example.myai

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    chatViewModel: ChatViewModel,
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    // Load saved credentials when the composable is first launched
    LaunchedEffect(Unit) {
        val savedCredentials = SessionManager.getSavedCredentials(context)
        email = savedCredentials?.first ?: ""
        password = savedCredentials?.second ?: ""
        rememberMe = SessionManager.shouldRememberMe()
    }

    // Color scheme
    val primaryColor = Color(0xFF4361EE)
    val surfaceColor = Color(0xFFF8FAFF)
    val errorColor = Color(0xFFE63946)
    val textColor = Color(0xFF2B2D42)
    val buttonColor = Color(0xFF4361EE)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo/Header
            Image(
                painter = painterResource(id = R.drawable.chatbot),
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "You are Welcome",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                "Login in to continue",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = "" },
                label = { Text("Email Address") },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(

                    containerColor = Color.White,
                    cursorColor = primaryColor,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email
                ),
                isError = error.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = "" },
                label = { Text("Password") },
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password"
                            else "Show password",
                            tint = primaryColor.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(

                    containerColor = Color.White,
                    cursorColor = primaryColor,
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = primaryColor,
                    unfocusedLabelColor = Color.Gray
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                isError = error.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Remember Me Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = primaryColor,
                            uncheckedColor = Color.Gray
                        )
                    )
                    Text(
                        "Remember me",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                TextButton(
                    onClick = { /* Handle forgot password */ },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Forgot Password?",
                        color = primaryColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

// Error message (keep below if needed)
            if (error.isNotEmpty()) {
                Text(
                    error,
                    color = errorColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 1.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button
            Button(
                onClick = {
                    when {
                        email.isEmpty() -> error = "Please enter your email"
                        password.isEmpty() -> error = "Please enter your password"
                        !UserStore.login(email.trim(), password.trim()) -> {
                            error = "Invalid email or password"
                        }
                        else -> {
                            if (rememberMe) {
                                SessionManager.saveCredentials(
                                    context,
                                    email.trim(),
                                    password.trim(),
                                    rememberMe
                                )
                            } else {
                                // Only save email if not remembering
                                SessionManager.saveCredentials(
                                    context,
                                    email.trim(),
                                    "",
                                    false
                                )
                            }
                            chatViewModel.clearMessages()
                            onLoginSuccess()
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    "Login",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // Sign up prompt
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor.copy(alpha = 0.7f)
                    )
                )
                TextButton(
                    onClick = onSignUpClick,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        "Sign Up",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}