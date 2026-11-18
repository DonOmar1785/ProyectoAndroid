package com.example.proyectoandroid.data.repository

import com.example.proyectoandroid.data.model.AssemblyModel
import com.example.proyectoandroid.data.model.AssemblyStep
import com.example.proyectoandroid.data.model.NodeHighlightInfo

class ModelRepository {

    private val sampleModels = listOf(
        AssemblyModel(
            id = "model_taburete",
            title = "Taburete de Madera Roble (Multicolor)",
            subtitle = "5 pasos • Base, 4 Patas, Asiento y 4 Pernos",
            description = "Taburete de taller en madera roble. Cada pieza tiene su propio color: base oscura, patas medias, asiento claro y pernos azules.",
            assetPath = "models/stool.glb",
            nodeCount = 5,
            steps = listOf(
                AssemblyStep(
                    stepNumber = 1,
                    title = "Paso 1: Colocar la Base",
                    description = "Desliza el marco de madera oscura sobre la superficie. Tiene 4 casquillos en las esquinas para las patas.",
                    targetNodeName = "node_base",
                    offsetX = -0.45f, offsetY = 0.00f, offsetZ = 0.0f,
                    detailedInstructions = "Apoya la base plana y centrada. Los 4 casquillos oscuros quedan hacia arriba."
                ),
                AssemblyStep(
                    stepNumber = 2,
                    title = "Paso 2: Patas Delanteras",
                    description = "Baja las 2 patas delanteras (las más cercanas a ti) hasta los casquillos marcados con el punto amarillo.",
                    targetNodeName = "node_pata_1",
                    offsetX = 0.0f, offsetY = 0.14f, offsetZ = 0.0f,
                    detailedInstructions = "Sigue los puntos azules hacia abajo. Deben quedar verticales dentro del casquillo."
                ),
                AssemblyStep(
                    stepNumber = 3,
                    title = "Paso 3: Patas Traseras",
                    description = "Baja las 2 patas traseras hasta los casquillos del fondo.",
                    targetNodeName = "node_pata_2",
                    offsetX = 0.0f, offsetY = 0.14f, offsetZ = 0.0f,
                    detailedInstructions = "Verifica que las 4 patas queden a la misma altura."
                ),
                AssemblyStep(
                    stepNumber = 4,
                    title = "Paso 4: Asiento",
                    description = "Baja el asiento de madera clara y apóyalo sobre las 4 patas.",
                    targetNodeName = "node_asiento",
                    offsetX = 0.0f, offsetY = 0.30f, offsetZ = 0.0f,
                    detailedInstructions = "El asiento debe quedar centrado, con la misma saliente en los 4 lados."
                ),
                AssemblyStep(
                    stepNumber = 5,
                    title = "Paso 5: Pernos Azules",
                    description = "Coloca los 4 pernos azules en las esquinas del asiento, justo sobre cada pata.",
                    targetNodeName = "node_tornillos",
                    offsetX = 0.0f, offsetY = 0.42f, offsetZ = 0.0f,
                    detailedInstructions = "Aprieta en cruz y verifica la estabilidad del taburete terminado."
                )
            )
        ),
        AssemblyModel(
            id = "model_motor",
            title = "Motor Industrial Mecánico",
            subtitle = "4 pasos • Bloque, 2 Pistones y Culata dorada",
            description = "Motor de 2 cilindros: bloque gris azulado, pistones plateados y culata dorada con bujías.",
            assetPath = "models/engine.glb",
            nodeCount = 4,
            steps = listOf(
                AssemblyStep(
                    stepNumber = 1,
                    title = "Paso 1: Bloque del Motor",
                    description = "Ubica el bloque gris azulado en la mesa de trabajo. Ya trae los 4 espárragos guía y los 2 cilindros.",
                    targetNodeName = "node_bloque",
                    offsetX = -0.45f, offsetY = 0.00f, offsetZ = 0.0f
                ),
                AssemblyStep(
                    stepNumber = 2,
                    title = "Paso 2: Pistón Izquierdo",
                    description = "Baja el pistón plateado dentro del cilindro izquierdo (punto amarillo).",
                    targetNodeName = "node_piston_1",
                    offsetX = 0.0f, offsetY = 0.14f, offsetZ = 0.0f
                ),
                AssemblyStep(
                    stepNumber = 3,
                    title = "Paso 3: Pistón Derecho",
                    description = "Baja el segundo pistón dentro del cilindro derecho.",
                    targetNodeName = "node_piston_2",
                    offsetX = 0.0f, offsetY = 0.14f, offsetZ = 0.0f
                ),
                AssemblyStep(
                    stepNumber = 4,
                    title = "Paso 4: Culata Dorada",
                    description = "Baja la culata dorada sobre los 4 espárragos para sellar los cilindros.",
                    targetNodeName = "node_tapa",
                    offsetX = 0.0f, offsetY = 0.28f, offsetZ = 0.0f
                )
            )
        )
    )

    private val knownNodeDescriptions = mapOf(
        "node_base" to NodeHighlightInfo("node_base", "Base Principal de Madera Roble", "Estructura portante de madera roble macizo con acoples inferiores."),
        "node_pata_1" to NodeHighlightInfo("node_pata_1", "Patas Delanteras (2)", "Par de patas verticales en madera que se encajan en los casquillos delanteros de la base."),
        "node_pata_2" to NodeHighlightInfo("node_pata_2", "Patas Traseras (2)", "Par de patas verticales que completa el soporte del asiento."),
        "node_asiento" to NodeHighlightInfo("node_asiento", "Tablero de Asiento Pulido", "Superficie de apoyo ergonómica en acabado barnizado natural."),
        "node_tornillos" to NodeHighlightInfo("node_tornillos", "Pernos de Sujeción Metálicos", "Juego de 4 tornillos de acero galvanizado en azul eléctrico."),
        "node_bloque" to NodeHighlightInfo("node_bloque", "Bloque de Hierro Fundido", "Cuerpo central del motor de alta resistencia térmica."),
        "node_piston_1" to NodeHighlightInfo("node_piston_1", "Pistón Metálico Cromo 1", "Pistón de aleación ligera pulido espejo."),
        "node_piston_2" to NodeHighlightInfo("node_piston_2", "Pistón Metálico Cromo 2", "Pistón rectificado de alta compresión."),
        "node_tapa" to NodeHighlightInfo("node_tapa", "Tapa de Culata en Bronce", "Cubierta superior de sellado hermético en aleación dorada.")
    )

    fun getAvailableModels(): List<AssemblyModel> = sampleModels

    fun getModelById(id: String): AssemblyModel? {
        return sampleModels.find { it.id == id } ?: sampleModels.firstOrNull()
    }

    fun getNodeInfo(nodeName: String): NodeHighlightInfo {
        return knownNodeDescriptions[nodeName] ?: NodeHighlightInfo(
            nodeName = nodeName,
            displayName = formatNodeName(nodeName),
            description = "Componente técnico interactivo detectado por Raycast."
        )
    }

    private fun formatNodeName(rawName: String): String {
        return rawName.replace("node_", "").replace("_", " ").capitalizeWords()
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
