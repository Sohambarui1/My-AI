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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(onSignUpSuccess: (email: String, password: String) -> Unit,  // Now accepts credentials
                 onNavigateToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Color scheme
    val primaryColor = Color(0xFF4361EE)
    val surfaceColor = Color(0xFFF8FAFF)
    val errorColor = Color(0xFFE63946)
    val textColor = Color(0xFF2B2D42)

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
                painter = painterResource(id = R.drawable.chatbot), // Replace with your logo
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Create Your Account",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                "Join our community today",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.6f)
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = ""
                },
                label = {
                    Text(
                        "Email Address",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    // Text color
                    containerColor = Color.White,               // Background color
                    cursorColor = Color.Blue,                  // Cursor color
                    focusedBorderColor = Color.Blue,           // Border when focused
                    unfocusedBorderColor = Color.LightGray,    // Border when not focused
                    focusedLabelColor = Color.Blue,            // Label color when focused
                    unfocusedLabelColor = Color.Gray            // Label color when not focused
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = error.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = ""
                },
                label = {
                    Text(
                        "Password",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
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

                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    containerColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                isError = error.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    error = ""
                },
                label = {
                    Text(
                        "Confirm Password",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = RoundedCornerShape(12.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility
                            else Icons.Default.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password"
                            else "Show password",
                            tint = primaryColor.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(

                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color.LightGray,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    containerColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                isError = error.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password requirements
            Text(
                "• At least 8 characters\n• One uppercase letter\n• One number",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = textColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            if (error.isNotEmpty()) {
                Text(
                    error,
                    color = errorColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sign Up Button
            Button(
                onClick = {
                    when {
                        email.isEmpty() -> error = "Please enter your email"
                        password.isEmpty() -> error = "Please enter a password"
                        password.length < 8 -> error = "Password must be at least 8 characters"
                        password != confirmPassword -> error = "Passwords do not match"
                        !UserStore.signUp(email.trim(), password.trim()) ->
                            error = "This email is already registered"

                        else -> {
                            onSignUpSuccess(email.trim(), password.trim())  // Then trigger navigation
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 6.dp)
            ) {
                Text(
                    "Sign Up",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Already have an account?
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor.copy(alpha = 0.7f)
                    )
                )
                TextButton(
                    onClick = onNavigateToLogin,  // Use the new parameter
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        "Log In",
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