package com.example.proyectoandroid.data.model

data class AssemblyStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val targetNodeName: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val detailedInstructions: String = ""
)
