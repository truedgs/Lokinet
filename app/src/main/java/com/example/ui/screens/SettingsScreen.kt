package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text("Loki VPN", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
            Text("Version 1.0.0", color = TextSecondary)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Architecture & Components", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "• Designed for V2Ray/Xray Core integration\n" +
                "• Supports VLESS, VMess, Trojan, Shadowsocks\n" +
                "• Remote GitHub JSON configuration\n" +
                "• Client-side HWID premium check",
                color = TextSecondary,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Security Notice", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The current Premium authorization model relies on a public GitHub file and client-side validation. For production, it is highly recommended to migrate the HWID verification to a backend API to prevent bypassing.",
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
