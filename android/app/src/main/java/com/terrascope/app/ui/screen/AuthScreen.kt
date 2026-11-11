package com.terrascope.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.R
import com.terrascope.app.ui.theme.*
import com.terrascope.app.ui.viewmodel.AuthState
import com.terrascope.app.ui.viewmodel.AuthViewModel

private val Night = Color(0xFF082A21)
private val Good = Color(0xFF2E8B57)
private val Warn = Color(0xFFC98A1B)
private val Bad = Color(0xFFC4473A)

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()

    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { appeared = true }

    val errorMsg = if (state is AuthState.Error) (state as AuthState.Error).message else null

    // Reglas: solo se exigen al registrarse (el login no bloquea cuentas ya existentes).
    val emailErr = if (isRegister) AuthRules.emailError(email) else null
    val checks = AuthRules.passwordChecks(password)
    val registerOk = AuthRules.emailValid(email) && checks.ok && displayName.isNotBlank()
    val canSubmit = !loading && email.isNotBlank() && password.isNotBlank() && (if (isRegister) registerOk else true)

    val zoom by rememberInfiniteTransition(label = "bg").animateFloat(
        initialValue = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse), label = "zoom"
    )

    BoxWithConstraints(Modifier.fillMaxSize().background(Night)) {
        // Con el teclado abierto la foto se encoge y el formulario sube; al cerrarlo todo vuelve a su lugar.
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val heroHeight by animateDpAsState(
            targetValue = if (imeVisible) 150.dp else maxHeight * 0.46f,
            animationSpec = tween(380, easing = FastOutSlowInEasing), label = "hero"
        )
        val titleSize by animateFloatAsState(if (imeVisible) 28f else 38f, tween(380), label = "title")
        val fullHeroHeight = maxHeight * 0.46f

        Column(Modifier.fillMaxSize().imePadding()) {
            Box(Modifier.fillMaxWidth().height(heroHeight).clipToBounds()) {
                Image(
                    painter = painterResource(R.drawable.somerton_sign),
                    contentDescription = "Letrero de bienvenida de Somerton, Arizona",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(fullHeroHeight + 40.dp).align(Alignment.Center).scale(zoom)
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0f to Night.copy(alpha = 0.35f),
                            0.4f to Color.Transparent,
                            1f to Night
                        )
                    )
                )
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(visible = appeared, enter = fadeIn(tween(900)) + slideInVertically(tween(900)) { it / 3 }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TerraScope", fontSize = titleSize.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
                            Text(
                                "Inteligencia territorial · Somerton, AZ",
                                fontSize = 13.sp, color = BrandAccent, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 14.dp, bottom = 24.dp)
            ) {
                AnimatedVisibility(visible = appeared, enter = fadeIn(tween(1100)) + slideInVertically(tween(1100)) { it / 2 }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White.copy(alpha = 0.96f),
                        shadowElevation = 12.dp
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            ModeSwitch(isRegister) {
                                isRegister = it
                                viewModel.clearError()
                                email = ""; password = ""; displayName = ""
                            }
                            AnimatedVisibility(
                                visible = !imeVisible,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        if (isRegister) "Crea tu cuenta" else "Bienvenido de vuelta",
                                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandText
                                    )
                                    Text(
                                        if (isRegister) "Analiza zonas e inversiones de Somerton." else "Entra para ver tus zonas y propiedades.",
                                        fontSize = 12.sp, color = BrandTextMuted, modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))

                            AnimatedVisibility(visible = isRegister) {
                                Column {
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it.take(AuthRules.NAME_MAX); if (state is AuthState.Error) viewModel.clearError() },
                                        label = { Text("Nombre") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                        colors = authFieldColors()
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it.take(AuthRules.EMAIL_MAX); if (state is AuthState.Error) viewModel.clearError() },
                                label = { Text("Correo electrónico") },
                                singleLine = true,
                                isError = emailErr != null,
                                supportingText = { FieldFooter(emailErr, email.length, AuthRules.EMAIL_MAX) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                colors = authFieldColors()
                            )
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it.take(AuthRules.PASSWORD_MAX); if (state is AuthState.Error) viewModel.clearError() },
                                label = { Text("Contraseña") },
                                singleLine = true,
                                supportingText = { FieldFooter(null, password.length, AuthRules.PASSWORD_MAX) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (canSubmit) doAuth(viewModel, isRegister, email, password, displayName)
                                }),
                                trailingIcon = {
                                    TextButton(onClick = { showPassword = !showPassword }) {
                                        Text(if (showPassword) "Ocultar" else "Ver", fontSize = 12.sp, color = BrandPrimary)
                                    }
                                },
                                colors = authFieldColors()
                            )

                            // Requisitos de la contraseña (solo al registrarse): se ponen en verde al cumplirse.
                            AnimatedVisibility(visible = isRegister) {
                                Column(Modifier.padding(top = 6.dp)) {
                                    Requirement("Entre ${AuthRules.PASSWORD_MIN} y ${AuthRules.PASSWORD_MAX} caracteres", checks.length)
                                    Requirement("Al menos una mayúscula", checks.upper)
                                    Requirement("Al menos un número", checks.digit)
                                }
                            }

                            AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut()) {
                                errorMsg?.let {
                                    Text(it, color = Bad, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                                }
                            }

                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    doAuth(viewModel, isRegister, email, password, displayName)
                                },
                                enabled = canSubmit,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary,
                                    disabledContainerColor = BrandPrimary.copy(alpha = 0.4f)
                                )
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                } else {
                                    Text(if (isRegister) "Crear cuenta" else "Entrar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pie de un campo: mensaje de error a la izquierda y contador "n/máx" a la derecha. */
@Composable
private fun FieldFooter(error: String?, count: Int, max: Int) {
    val counterColor by animateColorAsState(
        when {
            count >= max -> Bad
            count >= max * 0.9 -> Warn
            else -> BrandTextMuted
        }, label = "counter"
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(error ?: "", color = Bad, fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.weight(1f))
        Text("$count/$max", color = counterColor, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Requirement(text: String, met: Boolean) {
    val color by animateColorAsState(if (met) Good else BrandTextMuted, label = "req")
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(if (met) Good else BrandLine), contentAlignment = Alignment.Center) {
            if (met) Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = color)
    }
}

@Composable
private fun ModeSwitch(isRegister: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(BrandBackground).padding(4.dp)
    ) {
        listOf(false to "Entrar", true to "Crear cuenta").forEach { (reg, label) ->
            val selected = isRegister == reg
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                    .background(if (selected) BrandPrimary else Color.Transparent)
                    .clickable { if (!selected) onChange(reg) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (selected) Color.White else BrandTextMuted)
            }
        }
    }
}

private fun doAuth(viewModel: AuthViewModel, isRegister: Boolean, email: String, password: String, displayName: String) {
    if (isRegister) viewModel.register(email.trim(), password, displayName.trim())
    else viewModel.login(email.trim(), password)
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandPrimary,
    focusedLabelColor = BrandPrimary,
    cursorColor = BrandPrimary,
    unfocusedBorderColor = BrandLine,
    unfocusedLabelColor = BrandTextMuted
)
