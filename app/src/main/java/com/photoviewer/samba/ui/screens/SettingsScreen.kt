package com.photoviewer.samba.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.photoviewer.samba.data.model.SambaConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: SambaConfig,
    onSave: (SambaConfig) -> Unit,
    onClose: () -> Unit
) {
    var serverIp  by remember { mutableStateOf(currentConfig.serverIp) }
    var shareName by remember { mutableStateOf(currentConfig.shareName) }
    var username  by remember { mutableStateOf(currentConfig.username) }
    var password  by remember { mutableStateOf(currentConfig.password) }
    var showPass  by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres réseau") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Fermer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Serveur Samba", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = serverIp, onValueChange = { serverIp = it.trim() },
                label = { Text("Adresse IP du serveur") }, placeholder = { Text("192.168.1.100") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = shareName, onValueChange = { shareName = it.trim() },
                label = { Text("Nom du partage") }, placeholder = { Text("photos") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Identifiants (optionnel)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("Nom d'utilisateur") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Mot de passe") },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSave(SambaConfig(serverIp, shareName, username, password)) },
                enabled = serverIp.isNotBlank() && shareName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Enregistrer et connecter") }
            Spacer(Modifier.height(16.dp))
        }
    }
}
