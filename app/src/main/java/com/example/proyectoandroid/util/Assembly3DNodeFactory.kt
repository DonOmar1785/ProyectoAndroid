package com.example.proyectoandroid.util

import com.google.android.filament.Engine
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Color
import io.github.sceneview.math.Position
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import kotlinx.coroutines.delay
import kotlin.math.pow

/**
 * Modelos 3D ensamblables construidos por código.
 *
 * Ajustes de iluminación y gamma:
 *  - COLOR_GAMMA reducido a 1.6f para mayor luminosidad en pantallas Android.
 *  - Luces direccionales e intensidades incrementadas para eliminar sombras oscuras.
 *  - Colores base más brillantes para Roble, Cromo, Bronce y Pernos.
 */
private const val COLOR_GAMMA = 1.6f

private val ZERO = Position(0f, 0f, 0f)

internal enum class PartState { DONE, ACTIVE, PENDING, EXPLODED }

// ---------------------------------------------------------------------------------------------
// Colores
// ---------------------------------------------------------------------------------------------

private fun lin(r: Float, g: Float, b: Float) =
    Color(r.pow(COLOR_GAMMA), g.pow(COLOR_GAMMA), b.pow(COLOR_GAMMA), 1.0f)

/** Color en sRGB 0..255 (el mismo hex que ves en Color.kt / diseño). */
private data class Tint(val r: Int, val g: Int, val b: Int) {
    fun solid(): Color = lin(r / 255f, g / 255f, b / 255f)

    /** Versión apagada (grisácea brillante) para las piezas que todavía no toca instalar. */
    fun ghost(): Color {
        fun m(c: Int) = (c / 255f) * 0.45f + 0.55f * 0.60f
        return lin(m(r), m(g), m(b))
    }
}

private val GUIDE_BLUE = Tint(0, 191, 255)
private val GUIDE_YELLOW = Tint(255, 214, 10)

// ---------------------------------------------------------------------------------------------
// Descripción de piezas
// ---------------------------------------------------------------------------------------------

private enum class Shape { BOX, CYL, BALL }

/** size: BOX=(ancho, alto, fondo) · CYL=(radio, alto, -) · BALL=(radio, -, -) */
private class Piece(
    val shape: Shape,
    val size: Position,
    val pos: Position,
    val tint: Tint,
    val rough: Float
)

private fun box(w: Float, h: Float, d: Float, x: Float, y: Float, z: Float, tint: Tint, rough: Float = 0.55f) =
    Piece(Shape.BOX, Position(w, h, d), Position(x, y, z), tint, rough)

private fun cyl(r: Float, h: Float, x: Float, y: Float, z: Float, tint: Tint, rough: Float = 0.55f) =
    Piece(Shape.CYL, Position(r, h, 0f), Position(x, y, z), tint, rough)

private class PartSpec(
    val nodeName: String,
    val pieces: List<Piece>,
    val hover: Position,
    val approach: Position,
    val targets: List<Position>
)

private class MatCache(private val loader: MaterialLoader) {
    private val map = HashMap<String, MaterialInstance>()
    fun get(key: String, color: Color, rough: Float): MaterialInstance =
        map.getOrPut(key) {
            loader.createColorInstance(
                color = color,
                metallic = 0.0f,
                roughness = rough
            )
        }
}

// ---------------------------------------------------------------------------------------------
// Pieza en runtime
// ---------------------------------------------------------------------------------------------

class AssemblyPart internal constructor(
    val nodeName: String,
    private val group: Node,
    private val solid: List<Node>,
    private val ghost: List<Node>,
    private val guides: List<Node>,
    private val hover: Position,
    internal val approach: Position
) {
    internal fun setState(state: PartState) {
        when (state) {
            PartState.DONE -> { group.position = ZERO; show(solid = true, ghost = false, guides = false) }
            PartState.ACTIVE -> { group.position = approach; show(solid = true, ghost = false, guides = true) }
            PartState.PENDING -> { group.position = hover; show(solid = false, ghost = true, guides = false) }
            PartState.EXPLODED -> { group.position = hover; show(solid = true, ghost = false, guides = false) }
        }
    }

    private fun show(solid: Boolean, ghost: Boolean, guides: Boolean) {
        this.solid.forEach { it.isVisible = solid }
        this.ghost.forEach { it.isVisible = ghost }
        this.guides.forEach { it.isVisible = guides }
    }

    internal fun placeAtApproach() {
        group.position = approach
    }

    /** Desliza la pieza desde su punto de entrada hasta su lugar final, con suavizado. */
    internal suspend fun slideIn(durationMs: Long) {
        val steps = 30
        for (i in 1..steps) {
            val f = i / steps.toFloat()
            val s = f * f * (3f - 2f * f)
            group.position = Position(
                approach.x * (1f - s),
                approach.y * (1f - s),
                approach.z * (1f - s)
            )
            delay(durationMs / steps)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Conjunto completo (un modelo)
// ---------------------------------------------------------------------------------------------

class AssemblyRig internal constructor(
    val root: Node,
    private val parts: List<AssemblyPart>
) {
    /** Coloca todas las piezas según el paso (sin animación). */
    fun showStep(stepIndex: Int, exploded: Boolean) {
        parts.forEachIndexed { i, part ->
            val state = when {
                exploded -> PartState.EXPLODED
                i < stepIndex -> PartState.DONE
                i == stepIndex -> PartState.ACTIVE
                else -> PartState.PENDING
            }
            part.setState(state)
        }
    }

    suspend fun playStep(stepIndex: Int, exploded: Boolean) {
        showStep(stepIndex, exploded)
        if (exploded) return
        val part = parts.getOrNull(stepIndex) ?: return
        val isLast = stepIndex == parts.lastIndex
        var plays = 0
        while (true) {
            part.placeAtApproach()
            delay(600)
            part.slideIn(1100)
            delay(1300)
            plays++
            if (isLast && plays >= 2) {
                part.setState(PartState.DONE)
                return
            }
        }
    }

    fun destroy() {
        try {
            root.destroy()
        } catch (_: Exception) {
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Fábrica
// ---------------------------------------------------------------------------------------------

object Assembly3DNodeFactory {

    fun createRig(
        engine: Engine,
        materialLoader: MaterialLoader,
        modelId: String
    ): AssemblyRig? {
        val (rootName, specs) = when (modelId) {
            "model_taburete" -> "model_taburete_root" to stoolSpecs()
            "model_motor" -> "model_motor_root" to engineSpecs()
            else -> return null
        }
        return build(engine, materialLoader, rootName, specs)
    }

    // ---------------- Taburete ----------------

    private fun stoolSpecs(): List<PartSpec> {
        val baseWood = Tint(140, 90, 45)    // #8C5A2D Madera Roble Vistosa
        val socket = Tint(75, 48, 24)
        val legWood = Tint(210, 150, 90)   // #D2965A Roble Claro
        val seatWood = Tint(235, 175, 120)  // #EBAF78 Madera Barnizada
        val bolt = Tint(0, 191, 255)       // #00BFFF Azul Eléctrico

        val c = 0.15f

        val base = PartSpec(
            nodeName = "node_base",
            pieces = listOf(
                box(0.34f, 0.030f, 0.04f, 0f, 0.015f, c, baseWood),
                box(0.34f, 0.030f, 0.04f, 0f, 0.015f, -c, baseWood),
                box(0.04f, 0.030f, 0.26f, c, 0.015f, 0f, baseWood),
                box(0.04f, 0.030f, 0.26f, -c, 0.015f, 0f, baseWood),
                box(0.07f, 0.050f, 0.07f, c, 0.025f, c, socket),
                box(0.07f, 0.050f, 0.07f, -c, 0.025f, c, socket),
                box(0.07f, 0.050f, 0.07f, c, 0.025f, -c, socket),
                box(0.07f, 0.050f, 0.07f, -c, 0.025f, -c, socket)
            ),
            hover = ZERO,
            approach = Position(-0.45f, 0f, 0f),
            targets = listOf(Position(0f, 0.02f, 0f))
        )

        fun legs(name: String, z: Float) = PartSpec(
            nodeName = name,
            pieces = listOf(
                box(0.05f, 0.36f, 0.05f, -c, 0.23f, z, legWood),
                box(0.05f, 0.36f, 0.05f, c, 0.23f, z, legWood)
            ),
            hover = Position(0f, 0.14f, 0f),
            approach = Position(0f, 0.14f, 0f),
            targets = listOf(Position(-c, 0.05f, z), Position(c, 0.05f, z))
        )

        val seat = PartSpec(
            nodeName = "node_asiento",
            pieces = listOf(box(0.40f, 0.05f, 0.40f, 0f, 0.435f, 0f, seatWood, 0.4f)),
            hover = Position(0f, 0.30f, 0f),
            approach = Position(0f, 0.30f, 0f),
            targets = listOf(Position(0f, 0.41f, 0f))
        )

        val bolts = PartSpec(
            nodeName = "node_tornillos",
            pieces = listOf(
                cyl(0.02f, 0.014f, c, 0.467f, c, bolt, 0.3f),
                cyl(0.02f, 0.014f, -c, 0.467f, c, bolt, 0.3f),
                cyl(0.02f, 0.014f, c, 0.467f, -c, bolt, 0.3f),
                cyl(0.02f, 0.014f, -c, 0.467f, -c, bolt, 0.3f)
            ),
            hover = Position(0f, 0.42f, 0f),
            approach = Position(0f, 0.42f, 0f),
            targets = listOf(
                Position(c, 0.46f, c), Position(-c, 0.46f, c),
                Position(c, 0.46f, -c), Position(-c, 0.46f, -c)
            )
        )

        return listOf(base, legs("node_pata_1", c), legs("node_pata_2", -c), seat, bolts)
    }

    // ---------------- Motor ----------------

    private fun engineSpecs(): List<PartSpec> {
        val pan = Tint(65, 75, 90)
        val block = Tint(110, 130, 160)     // #6E82A0 Gris Claro
        val bore = Tint(25, 28, 35)
        val stud = Tint(180, 190, 200)
        val piston = Tint(220, 228, 240)    // Cromo Brillante
        val ring = Tint(45, 48, 55)
        val head = Tint(245, 185, 60)       // #F5B93C Dorado Claro
        val headBolt = Tint(90, 80, 50)
        val plug = Tint(240, 235, 220)

        val blockSpec = PartSpec(
            nodeName = "node_bloque",
            pieces = listOf(
                box(0.30f, 0.05f, 0.20f, 0f, 0.025f, 0f, pan),
                box(0.34f, 0.14f, 0.22f, 0f, 0.12f, 0f, block, 0.45f),
                cyl(0.043f, 0.004f, -0.08f, 0.192f, 0f, bore, 0.9f),
                cyl(0.043f, 0.004f, 0.08f, 0.192f, 0f, bore, 0.9f),
                cyl(0.011f, 0.10f, 0.15f, 0.24f, 0.085f, stud, 0.4f),
                cyl(0.011f, 0.10f, -0.15f, 0.24f, 0.085f, stud, 0.4f),
                cyl(0.011f, 0.10f, 0.15f, 0.24f, -0.085f, stud, 0.4f),
                cyl(0.011f, 0.10f, -0.15f, 0.24f, -0.085f, stud, 0.4f)
            ),
            hover = ZERO,
            approach = Position(-0.45f, 0f, 0f),
            targets = listOf(Position(0f, 0.12f, 0f))
        )

        fun pistonSpec(name: String, x: Float) = PartSpec(
            nodeName = name,
            pieces = listOf(
                cyl(0.038f, 0.09f, x, 0.175f, 0f, piston, 0.35f),
                cyl(0.040f, 0.007f, x, 0.198f, 0f, ring),
                cyl(0.040f, 0.007f, x, 0.212f, 0f, ring)
            ),
            hover = Position(0f, 0.14f, 0f),
            approach = Position(0f, 0.14f, 0f),
            targets = listOf(Position(x, 0.19f, 0f))
        )

        val headSpec = PartSpec(
            nodeName = "node_tapa",
            pieces = listOf(
                box(0.36f, 0.06f, 0.24f, 0f, 0.26f, 0f, head, 0.4f),
                cyl(0.012f, 0.05f, -0.08f, 0.315f, 0f, plug),
                cyl(0.012f, 0.05f, 0.08f, 0.315f, 0f, plug),
                cyl(0.012f, 0.010f, 0.15f, 0.295f, 0.085f, headBolt),
                cyl(0.012f, 0.010f, -0.15f, 0.295f, 0.085f, headBolt),
                cyl(0.012f, 0.010f, 0.15f, 0.295f, -0.085f, headBolt),
                cyl(0.012f, 0.010f, -0.15f, 0.295f, -0.085f, headBolt)
            ),
            hover = Position(0f, 0.28f, 0f),
            approach = Position(0f, 0.28f, 0f),
            targets = listOf(Position(0f, 0.23f, 0f))
        )

        return listOf(blockSpec, pistonSpec("node_piston_1", -0.08f), pistonSpec("node_piston_2", 0.08f), headSpec)
    }

    // ---------------- Construcción ----------------

    private fun build(
        engine: Engine,
        materialLoader: MaterialLoader,
        rootName: String,
        specs: List<PartSpec>
    ): AssemblyRig {
        val cache = MatCache(materialLoader)
        val root = Node(engine).apply { name = rootName }

        addLights(engine, root)

        // Plataforma (mesa de trabajo) clara para alto contraste
        val floorTint = Tint(50, 56, 68)
        root.addChildNode(
            CylinderNode(
                engine,
                radius = 0.32f,
                height = 0.004f,
                materialInstance = cache.get("floor", floorTint.solid(), 0.9f)
            ).apply { position = Position(0f, -0.002f, 0f); name = "node_floor" }
        )

        val blueMat = cache.get("guide_blue", GUIDE_BLUE.solid(), 0.4f)
        val yellowMat = cache.get("guide_yellow", GUIDE_YELLOW.solid(), 0.4f)

        val parts = specs.map { spec ->
            val group = Node(engine).apply { name = spec.nodeName }
            val solid = ArrayList<Node>()
            val ghost = ArrayList<Node>()

            spec.pieces.forEachIndexed { i, piece ->
                val solidMat = cache.get("${spec.nodeName}#$i", piece.tint.solid(), piece.rough)
                val ghostMat = cache.get("${spec.nodeName}#$i-ghost", piece.tint.ghost(), 0.9f)
                val a = makeLeaf(engine, piece, solidMat)
                val b = makeLeaf(engine, piece, ghostMat)
                group.addChildNode(a)
                group.addChildNode(b)
                solid += a
                ghost += b
            }
            root.addChildNode(group)

            val guides = ArrayList<Node>()
            spec.targets.forEach { t ->
                for (k in 1..5) {
                    val f = k / 5f
                    val dot = SphereNode(engine, radius = 0.005f + 0.004f * f, materialInstance = blueMat).apply {
                        position = Position(
                            t.x + spec.approach.x * f,
                            t.y + spec.approach.y * f,
                            t.z + spec.approach.z * f
                        )
                    }
                    root.addChildNode(dot)
                    guides += dot
                }
                val dest = SphereNode(engine, radius = 0.014f, materialInstance = yellowMat).apply {
                    position = t
                }
                root.addChildNode(dest)
                guides += dest
            }

            AssemblyPart(
                nodeName = spec.nodeName,
                group = group,
                solid = solid,
                ghost = ghost,
                guides = guides,
                hover = spec.hover,
                approach = spec.approach
            )
        }

        return AssemblyRig(root, parts).also { it.showStep(0, exploded = false) }
    }

    private fun makeLeaf(engine: Engine, p: Piece, mat: MaterialInstance): Node {
        val node: Node = when (p.shape) {
            Shape.BOX -> CubeNode(engine, size = p.size, materialInstance = mat)
            Shape.CYL -> CylinderNode(engine, radius = p.size.x, height = p.size.y, materialInstance = mat)
            Shape.BALL -> SphereNode(engine, radius = p.size.x, materialInstance = mat)
        }
        node.position = p.pos
        return node
    }

    /**
     * Luces ambientales direccionales de alta intensidad (180.000 lux frontal + 100.000 lux laterales/rebote)
     * para garantizar máxima claridad y brillo en cualquier dispositivo o nivel de luz.
     */
    private fun addLights(engine: Engine, root: Node) {
        fun dirLight(lux: Float, dx: Float, dy: Float, dz: Float): LightNode =
            LightNode(engine, LightManager.Type.DIRECTIONAL) {
                color(1.0f, 0.98f, 0.95f)
                intensity(lux)
                direction(dx, dy, dz)
                castShadows(false)
            }
        root.addChildNode(dirLight(180_000f, -0.4f, -1.0f, -0.5f)) // Luz principal frontal superior
        root.addChildNode(dirLight(100_000f, 0.7f, -0.5f, 0.8f))   // Luz de relleno lateral derecha
        root.addChildNode(dirLight(80_000f, 0.0f, 0.8f, -0.8f))    // Luz frontal directa
        root.addChildNode(dirLight(45_000f, 0.0f, 1.0f, 0.2f))     // Luz de rebote inferior
    }
}
