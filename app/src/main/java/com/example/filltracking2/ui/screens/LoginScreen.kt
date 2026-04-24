package com.example.filltracking2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.filltracking2.ui.theme.ThemeManager

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = ThemeManager.getString("login_title"),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = ThemeManager.getString("login_subtitle"),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text(ThemeManager.getString("email")) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text(ThemeManager.getString("password")) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage != null
        )

        if (errorMessage != null) {
            Text(
                text = ThemeManager.getString("login_error"),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email == "admin" && password == "admin") {
                    onLoginSuccess(email)
                } else {
                    errorMessage = ThemeManager.getString("login_error")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(ThemeManager.getString("login_button"), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
