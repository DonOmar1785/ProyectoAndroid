package com.example.proyectoandroid.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.proyectoandroid.theme.DarkGrayBackground
import com.example.proyectoandroid.theme.DarkGrayCardHeader
import com.example.proyectoandroid.theme.DarkGraySurface
import com.example.proyectoandroid.theme.ElectricBlue
import com.example.proyectoandroid.theme.StatusSuccess
import com.example.proyectoandroid.theme.StatusWarning
import com.example.proyectoandroid.theme.TextPrimary
import com.example.proyectoandroid.theme.TextSecondary
import com.example.proyectoandroid.theme.WoodOak
import com.example.proyectoandroid.util.Assembly3DNodeFactory
import com.example.proyectoandroid.util.AssemblyRig
import com.example.proyectoandroid.viewmodel.ARWorkspaceViewModel
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation

@Composable
fun ARWorkspaceScreen(
    modelId: String,
    viewModel: ARWorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var arSceneViewRef by remember { mutableStateOf<ARSceneView?>(null) }
    var assemblyRig by remember { mutableStateOf<AssemblyRig?>(null) }

    LaunchedEffect(modelId) {
        viewModel.loadModel(modelId)
    }

    // Manejo del ciclo de vida para Android 15.0+ (API 35+)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> arSceneViewRef?.planeRenderer?.isEnabled = true
                Lifecycle.Event.ON_PAUSE -> arSceneViewRef?.planeRenderer?.isEnabled = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Anima el paso actual: la pieza activa entra y se acopla; las demás quedan hechas o pendientes.
    // Se relanza solo al cambiar de paso o de vista explosionada (y cancela la animación anterior).
    LaunchedEffect(assemblyRig, uiState.currentStepIndex, uiState.isExplodedView) {
        assemblyRig?.playStep(uiState.currentStepIndex, uiState.isExplodedView)
    }

    // Construye el modelo UNA sola vez y lo coloca delante de la cámara
    fun placeModelInScene(sceneView: ARSceneView) {
        val currentModel = uiState.currentModel ?: return
        try {
            assemblyRig?.let {
                sceneView.removeChildNode(it.root)
                it.destroy()
            }
            assemblyRig = null

            sceneView.planeRenderer.isEnabled = true
            // Iluminación fija (las luces vienen dentro del modelo): evita modelos negros
            // cuando ARCore todavía no estimó la luz del ambiente.
            sceneView.lightEstimator?.isEnabled = false

            val rig = Assembly3DNodeFactory.createRig(
                engine = sceneView.engine,
                materialLoader = sceneView.materialLoader,
                modelId = currentModel.id
            )

            if (rig != null) {
                rig.root.position = Position(0f, -0.50f, -0.90f)
                rig.root.rotation = Rotation(0f, 25f, 0f)
                sceneView.addChildNode(rig.root)
                assemblyRig = rig
            }
            viewModel.onModelAnchored(anchored = true)
        } catch (_: Exception) {
            viewModel.onModelAnchored(anchored = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Visor 3D / ARSceneView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ARSceneView(ctx).apply {
                    planeRenderer.isEnabled = true
                    lightEstimator?.isEnabled = false
                    viewModel.onPlaneDetected(detected = true)
                    arSceneViewRef = this

                    setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            view.performClick()
                            if (!uiState.isModelAnchored) {
                                placeModelInScene(this)
                            } else {
                                uiState.currentStep?.targetNodeName?.let { target ->
                                    viewModel.onNodeTouched(target)
                                }
                            }
                        }
                        true
                    }
                }
            },
            update = { sceneView ->
                arSceneViewRef = sceneView
            }
        )

        // Overlay de simulación de plano / indicador de anclaje si no está anclado
        if (!uiState.isModelAnchored) {
            PlaneDetectionOverlay(
                isPlaneDetected = uiState.isPlaneDetected,
                onSimulateTapToAnchor = {
                    arSceneViewRef?.let { placeModelInScene(it) } ?: viewModel.onModelAnchored(anchored = true)
                }
            )
        }

        // Header Superior HUD
        ARWorkspaceHeader(
            modelTitle = uiState.currentModel?.title ?: "Cargando...",
            isAnchored = uiState.isModelAnchored,
            isExplodedView = uiState.isExplodedView,
            onBackClicked = onNavigateBack,
            onResetAnchorClicked = {
                arSceneViewRef?.let { scene ->
                    assemblyRig?.let { scene.removeChildNode(it.root) }
                }
                assemblyRig?.destroy()
                assemblyRig = null
                viewModel.resetAnchor()
            },
            onToggleExplodedView = {
                viewModel.toggleExplodedView()
            }
        )

        // HUD Central Inferior (Tarjeta de Pasos y Raycast Info)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tarjeta de información de nodo tocado (Raycast result)
            uiState.highlightedNodeInfo?.let { nodeInfo ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.5.dp, ElectricBlue, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkGrayCardHeader.copy(alpha = 0.92f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(ElectricBlue.copy(alpha = 0.2f), CircleShape)
                                        .border(1.dp, ElectricBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Pieza Seleccionada",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Pieza: ${nodeInfo.displayName}",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = nodeInfo.description,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.clearHighlight() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Tarjeta Principal de Paso Actual
            uiState.currentStep?.let { step ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, WoodOak, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkGrayBackground.copy(alpha = 0.88f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASO ${step.stepNumber} DE ${uiState.totalSteps}",
                                color = ElectricBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (uiState.currentStepIndex == uiState.totalSteps - 1) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completado",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ensamble Completo",
                                        color = ElectricBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.title,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.description,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )

                        if (step.detailedInstructions.isNotEmpty()) {
                            Text(
                                text = "💡 ${step.detailedInstructions}",
                                color = WoodOak,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Barra de progreso de ensamblaje
                        LinearProgressIndicator(
                            progress = { uiState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricBlue,
                            trackColor = DarkGraySurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controles de Navegación de Pasos (Anterior / Siguiente / Finalizar)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botón Anterior
                            FloatingActionButton(
                                onClick = { viewModel.previousStep() },
                                containerColor = if (uiState.currentStepIndex > 0) DarkGraySurface else DarkGraySurface.copy(alpha = 0.5f),
                                contentColor = if (uiState.currentStepIndex > 0) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                elevation = FloatingActionButtonDefaults.elevation(2.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Anterior"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Anterior", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                            }

                            // Botón Siguiente / Finalizar
                            FloatingActionButton(
                                onClick = {
                                    if (uiState.currentStepIndex < uiState.totalSteps - 1) {
                                        viewModel.nextStep()
                                    } else {
                                        onNavigateBack() // Devuelve al catálogo de todos los modelos
                                    }
                                },
                                containerColor = ElectricBlue,
                                contentColor = DarkGrayBackground,
                                shape = RoundedCornerShape(12.dp),
                                elevation = FloatingActionButtonDefaults.elevation(4.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (uiState.currentStepIndex < uiState.totalSteps - 1) "Siguiente" else "Finalizar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = if (uiState.currentStepIndex < uiState.totalSteps - 1) "Siguiente" else "Finalizar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snackbar de Pérdida de Tracking o Advertencia ARCore
        uiState.trackingFailureMessage?.let { failureMsg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                containerColor = DarkGraySurface,
                contentColor = StatusWarning,
                action = {
                    TextButton(onClick = { viewModel.setTrackingFailureMessage(null) }) {
                        Text("OK", color = ElectricBlue)
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tracking Warning",
                        tint = StatusWarning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = failureMsg, fontSize = 12.sp)
                }
            }
        }

        // Diálogo de error al cargar modelo
        uiState.errorMessage?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissError() },
                containerColor = DarkGraySurface,
                title = {
                    Text("Error de Modelo 3D", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(errorMsg, color = TextSecondary)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissError()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Volver al Catálogo", color = DarkGrayBackground, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Indicador de Carga
        if (uiState.isLoadingModel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkGrayBackground.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ElectricBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cargando modelo 3D y submallas...",
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ARWorkspaceHeader(
    modelTitle: String,
    isAnchored: Boolean,
    isExplodedView: Boolean,
    onBackClicked: () -> Unit,
    onResetAnchorClicked: () -> Unit,
    onToggleExplodedView: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Volver
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier
                .size(42.dp)
                .background(DarkGraySurface.copy(alpha = 0.85f), CircleShape)
                .border(1.dp, WoodOak, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = TextPrimary
            )
        }

        // Título del Modelo en AR con Badge de Estado de Anclaje
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .background(DarkGraySurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .border(1.dp, WoodOak, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = modelTitle,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isAnchored) StatusSuccess else StatusWarning, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAnchored) "3D Anclado" else "Buscando plano",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Row {
            // Toggle Vista Explotada
            IconButton(
                onClick = onToggleExplodedView,
                modifier = Modifier
                    .size(42.dp)
                    .background(if (isExplodedView) ElectricBlue else DarkGraySurface.copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, WoodOak, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Vista Explotada",
                    tint = if (isExplodedView) DarkGrayBackground else ElectricBlue
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón Resetear Ancla
            IconButton(
                onClick = onResetAnchorClicked,
                modifier = Modifier
                    .size(42.dp)
                    .background(DarkGraySurface.copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, WoodOak, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resetear Ancla",
                    tint = ElectricBlue
                )
            }
        }
    }
}

@Composable
fun PlaneDetectionOverlay(
    isPlaneDetected: Boolean,
    onSimulateTapToAnchor: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onSimulateTapToAnchor() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(DarkGraySurface.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                .border(1.5.dp, ElectricBlue, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(ElectricBlue.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, ElectricBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Indicador AR",
                    tint = ElectricBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isPlaneDetected) "Plano Detectado" else "Buscando Superficie...",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = if (isPlaneDetected) "Toca en la pantalla para anclar el objeto 3D sobre la mesa" else "Mueve lentamente el teléfono para detectar el suelo o mesa",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Button(
                onClick = onSimulateTapToAnchor,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Anclar Modelo Aquí",
                    color = DarkGrayBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
