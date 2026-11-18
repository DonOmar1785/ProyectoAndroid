package com.example.proyectoandroid.data.model

data class AssemblyModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val assetPath: String,
    val nodeCount: Int,
    val steps: List<AssemblyStep>
)
