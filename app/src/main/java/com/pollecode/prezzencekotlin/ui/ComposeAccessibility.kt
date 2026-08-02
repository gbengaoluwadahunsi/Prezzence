package com.pollecode.prezzencekotlin.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

fun Modifier.a11yIconButton(label: String): Modifier = semantics {
    contentDescription = label
    role = Role.Button
}
