package com.example.soulspeak

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soulspeak.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    isGirlsTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val themeColors = LocalSoulspeakColors.current

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnCompleteListener { if (it.isSuccessful) onLoginSuccess() else isLoading = false }
        } catch (e: Exception) { isLoading = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(themeColors.background)) {
        // Background Glows
        Box(modifier = Modifier.size(400.dp).align(Alignment.TopEnd).background(Brush.radialGradient(listOf(themeColors.primary.copy(alpha = 0.15f), Color.Transparent))))
        Box(modifier = Modifier.size(400.dp).align(Alignment.BottomStart).background(Brush.radialGradient(listOf(themeColors.secondary.copy(alpha = 0.1f), Color.Transparent))))

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // --- Theme Selection Panel ---
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                color = themeColors.primary.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Choose Your Vibe", color = themeColors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ThemeOption(
                            label = "Midnight (Boys)", 
                            isSelected = !isGirlsTheme, 
                            color = PrimaryPurple, 
                            onClick = { onThemeChange(false) }
                        )
                        ThemeOption(
                            label = "Blossom (Girls)", 
                            isSelected = isGirlsTheme, 
                            color = GirlsPrimary, 
                            onClick = { onThemeChange(true) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(30.dp)),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Image(painter = painterResource(id = R.drawable.logo), "Logo", modifier = Modifier.padding(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Soulspeak", color = themeColors.text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Speak Freely. Stay Anonymous.", color = themeColors.primary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(48.dp))

            // Glass Inputs
            LoginInput(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            LoginInput(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)
            
            //Forgot Password Option
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isNotEmpty()) {
                            isLoading = true
                            auth.sendPasswordResetEmail(trimmedEmail).addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Password reset email sent to $trimmedEmail", Toast.LENGTH_LONG).show()
                                } else {
                                    val exception = task.exception
                                    val errorMsg = when {
                                        exception is com.google.firebase.FirebaseNetworkException ||
                                        exception?.message?.contains("network error", ignoreCase = true) == true ->
                                            "Network Error: Please check your internet connection."
                                        else -> exception?.localizedMessage ?: "Failed to send reset email"
                                    }
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter your Email address first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("Forgot Password?", color = themeColors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                CircularProgressIndicator(color = themeColors.primary)
            } else {
                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            auth.signInWithEmailAndPassword(trimmedEmail, password).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onLoginSuccess() 
                                } else {
                                    isLoading = false
                                    val exception = task.exception
                                    val errorMsg = when {
                                        exception is com.google.firebase.FirebaseNetworkException ||
                                        exception?.message?.contains("network error", ignoreCase = true) == true ->
                                            "Network Error: Please check your internet connection and try again."
                                        else -> exception?.localizedMessage ?: "Login failed"
                                    }
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter both Email and Password", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primary),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("806222339778-gqpothrias63s512ihqsfhdc94ao1kkj.apps.googleusercontent.com").requestEmail().build()
                        googleSignInLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) { Text("Continue with Google", color = themeColors.text) }

                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = {
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.createUserWithEmailAndPassword(trimmedEmail, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                isLoading = false
                                val exception = task.exception
                                val errorMsg = when {
                                    exception is com.google.firebase.FirebaseNetworkException ||
                                    exception?.message?.contains("network error", ignoreCase = true) == true ->
                                        "Network Error: Please check your internet connection and try again."
                                    else -> exception?.localizedMessage ?: "Account creation failed"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please enter both Email and Password first", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("New here? Create Account", color = themeColors.primary) }
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = label, 
            color = if (isSelected) Color.White else color, 
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LoginInput(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isPassword: Boolean = false) {
    val themeColors = LocalSoulspeakColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(icon, null, tint = themeColors.text.copy(alpha = 0.5f)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = themeColors.text, unfocusedTextColor = themeColors.text,
            focusedBorderColor = themeColors.primary, unfocusedBorderColor = themeColors.text.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.03f), unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
            focusedLabelColor = themeColors.primary, unfocusedLabelColor = themeColors.text.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}
