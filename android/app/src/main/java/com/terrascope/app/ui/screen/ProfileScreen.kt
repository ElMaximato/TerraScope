package com.terrascope.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.data.model.AuthUser
import com.terrascope.app.ui.theme.*

@Composable
fun ProfileScreen(
    user: AuthUser,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
    ) {
        // Encabezado
        Surface(
            color = BrandSurface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Perfil",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = BrandText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar + nombre
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar inicial
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(BrandPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user.displayName?.firstOrNull() ?: user.email.firstOrNull())
                                ?.uppercaseChar()?.toString() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = user.displayName ?: "Usuario TerraScope",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = BrandText
                        )
                        Text(
                            text = user.email,
                            fontSize = 13.sp,
                            color = BrandTextMuted
                        )
                    }
                }
            }

            // Información del proyecto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Versión", "1.0.0 (Etapa 5)")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BrandLine
                    )
                    InfoRow("Backend", "http://127.0.0.1:3000")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BrandLine
                    )
                    InfoRow("Área de estudio", "Somerton, Arizona")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BrandLine
                    )
                    InfoRow("Fuente de datos", "U.S. Census ACS 5-Year")
                }
            }

            // Botón cerrar sesión
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC4473A)),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Diálogo de confirmación
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) },
            text = { Text("¿Deseas cerrar tu sesión en TerraScope?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Cerrar sesión", color = Color(0xFFC4473A))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = BrandPrimary)
                }
            },
            containerColor = BrandSurface
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = BrandTextMuted)
        Text(text = value, fontSize = 13.sp, color = BrandText, fontWeight = FontWeight.Medium)
    }
}

