package com.example.filament

import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.ArenaState
import com.example.engine.DropState
import com.example.engine.ObstacleType
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val WORLD_SCALE = 1f / 14f

@Composable
fun FilamentArenaViewport(
    arenaState: ArenaState,
    modifier: Modifier = Modifier
) {
    val controller = remember {
        FilamentInit.ensureInitialized()
        Arena3DController()
    }

    controller.latestState = arenaState

    DisposableEffect(Unit) {
        controller.startLoop()
        onDispose {
            controller.stopAndDestroy()
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                controller.attach(tv)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

private class Arena3DController {
    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity: Int = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val sunEntity: Int = EntityManager.get().create()
    private val fillLightEntity: Int = EntityManager.get().create()
    private val skybox: Skybox
    private val indirectLight: IndirectLight
    private val colorGrading: ColorGrading

    private val materials = FilamentMaterialBank(engine)
    private val meshes = FilamentMeshBank(engine)

    private val staticEntities = mutableListOf<Int>()
    private var builtMapKey: String = ""

    private val characterRigs = mutableMapOf<Int, FilamentCharacterRig>()
    private val glooPool = mutableListOf<Int>()
    private val bulletPool = mutableListOf<Int>()
    private val lootPool = mutableListOf<Int>()
    private val domePool = mutableListOf<Int>()
    private val auraPool = mutableListOf<Int>()
    private val zoneRingSegments = mutableListOf<Int>()

    private val planeRoot: Int = EntityManager.get().create()
    private val planeParts = mutableListOf<Int>()
    private var planeVisible = false

    private val glooWallMat: MaterialInstance
    private val bulletMat: MaterialInstance
    private val weaponLootMat: MaterialInstance
    private val chronoMat: MaterialInstance
    private val alokMat: MaterialInstance
    private val zoneMat: MaterialInstance

    private var swapChain: SwapChain? = null
    private var uiHelper: UiHelper? = null
    private var running = false
    private var startNs = System.nanoTime()

    @Volatile var latestState: ArenaState? = null

    init {
        view.scene = scene
        view.camera = camera
        colorGrading = FilamentPostProcess.configureView(engine, view, enableFog = true)

        skybox = Skybox.Builder()
            .color(0.36f, 0.56f, 0.80f, 1.0f)
            .build(engine)
        scene.skybox = skybox

        val shBands = floatArrayOf(
            0.78f, 0.82f, 0.88f,
            0.05f, 0.14f, 0.20f,
            0.0f, 0.0f, 0.0f
        )
        indirectLight = IndirectLight.Builder()
            .irradiance(1, shBands)
            .intensity(38000f)
            .build(engine)
        scene.indirectLight = indirectLight

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.94f, 0.84f)
            .intensity(125000f)
            .direction(-0.42f, -0.82f, -0.38f)
            .castShadows(true)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.55f, 0.78f, 1.0f)
            .intensity(34000f)
            .direction(0.52f, -0.35f, 0.65f)
            .castShadows(false)
            .build(engine, fillLightEntity)
        scene.addEntity(fillLightEntity)

        val tex = materials.textures
        glooWallMat = materials.texturedTransparent(tex.glooHexMap, r = 0.25f, g = 0.92f, b = 1.0f, a = 0.72f, uvScaleX = 2.5f, uvScaleY = 2f)
        bulletMat = materials.lit(0xFFFFCA28, metallic = 0.2f, roughness = 0.3f, emissiveR = 1.0f, emissiveG = 0.8f, emissiveB = 0.1f)
        weaponLootMat = materials.textured(tex.woodCrateMap, tintArgb = 0xFFFFD54F, metallic = 0.35f, roughness = 0.4f, emissiveR = 0.55f, emissiveG = 0.35f, emissiveB = 0f)
        chronoMat = materials.transparent(0.0f, 0.85f, 1.0f, 0.30f)
        alokMat = materials.transparent(1.0f, 0.72f, 0.0f, 0.32f)
        zoneMat = materials.transparent(0.18f, 0.56f, 1.0f, 0.32f)

        buildC130Airplane()
    }

    private fun buildC130Airplane() {
        val tm = engine.transformManager
        tm.create(planeRoot)
        val rootTi = tm.getInstance(planeRoot)
        val tex = materials.textures

        val bodyMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF546E7A, metallic = 0.65f, roughness = 0.38f, uvScaleX = 2f, uvScaleY = 4f)
        val wingMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF78909C, metallic = 0.6f, roughness = 0.42f, uvScaleX = 3f, uvScaleY = 1f)
        val accentMat = materials.lit(0xFFFF6D00, metallic = 0.4f, roughness = 0.4f, emissiveR = 0.6f, emissiveG = 0.2f, emissiveB = 0f)
        val glassMat = materials.lit(0xFF1A3A4A, metallic = 0.9f, roughness = 0.12f)

        fun part(mesh: FilamentMesh, mat: MaterialInstance, tx: Float, ty: Float, tz: Float, sx: Float, sy: Float, sz: Float, rx: Float = 0f): Int {
            val e = FilamentTransformHelper.spawnMeshEntity(engine, mesh, mat, castShadows = true)
            tm.setParent(tm.getInstance(e), rootTi)
            tm.setTransform(tm.getInstance(e), FilamentTransformHelper.buildTRS(tx, ty, tz, rotXDeg = rx, sx = sx, sy = sy, sz = sz))
            planeParts.add(e)
            return e
        }

        part(meshes.cylinder, bodyMat, 0f, 0f, 0f, 2.3f, 13.5f, 2.3f, rx = 90f)
        part(meshes.sphere, accentMat, 0f, 0f, 6.6f, 2.0f, 1.8f, 2.4f)
        part(meshes.box, glassMat, 0f, 0.6f, 5.7f, 1.35f, 0.55f, 0.9f)
        part(meshes.box, wingMat, 0f, 0.55f, 0.8f, 17.0f, 0.28f, 3.1f)
        part(meshes.box, accentMat, 0f, 1.95f, -5.6f, 0.32f, 3.1f, 2.2f)
        part(meshes.box, wingMat, 0f, 0.45f, -5.6f, 6.4f, 0.22f, 1.5f)
        listOf(-5.2f, -2.8f, 2.8f, 5.2f).forEach { ex ->
            part(meshes.cylinder, accentMat, ex, 0.1f, 2.1f, 0.75f, 1.95f, 0.75f, rx = 90f)
        }
    }

    private fun ensureMapBuilt(state: ArenaState) {
        val key = "${state.mode.name}_${state.obstacles.size}"
        if (builtMapKey == key) return
        builtMapKey = key

        staticEntities.forEach { e ->
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
        staticEntities.clear()

        val tm = engine.transformManager
        val tex = materials.textures
        val mapW3D = state.mapWidth * WORLD_SCALE
        val mapH3D = state.mapHeight * WORLD_SCALE
        val isWarehouse = state.mode.name == "LONE_WOLF" || state.mode.name == "CLASH_SQUAD"

        // Adjust atmospheric fog depending on indoor warehouse vs outdoor Bermuda island
        view.setFogOptions(
            View.FogOptions().apply {
                enabled = !isWarehouse
                distance = 24f
                cutOffDistance = 320f
                maximumOpacity = 0.62f
                density = 0.12f
                height = 18f
                heightFalloff = 0.45f
                color = floatArrayOf(0.56f, 0.73f, 0.90f)
            }
        )

        // Ocean surround
        val oceanMat = materials.lit(0xFF144B6E, metallic = 0.35f, roughness = 0.20f)
        val ocean = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, oceanMat, castShadows = false)
        tm.setTransform(tm.getInstance(ocean), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, -1.25f, mapH3D * 0.5f, sx = mapW3D + 260f, sy = 1f, sz = mapH3D + 260f))
        scene.addEntity(ocean); staticEntities.add(ocean)

        // Textured Ground (Warehouse Concrete Slabs vs Bermuda Tiled Grass)
        val groundMat = if (isWarehouse) {
            materials.textured(tex.warehouseFloorMap, tintArgb = 0xFFFFFFFF, metallic = 0.08f, roughness = 0.78f, uvScaleX = 16f, uvScaleY = 12f)
        } else {
            materials.textured(tex.bermudaGrassMap, tintArgb = 0xFFFFFFFF, metallic = 0.0f, roughness = 0.92f, uvScaleX = 28f, uvScaleY = 28f)
        }
        val ground = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, groundMat, castShadows = false)
        tm.setTransform(tm.getInstance(ground), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, -0.5f, mapH3D * 0.5f, sx = mapW3D, sy = 1f, sz = mapH3D))
        scene.addEntity(ground); staticEntities.add(ground)

        if (isWarehouse) {
            // Corrugated steel warehouse perimeter walls (Screenshot #2)
            val hangarWallMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF8C949E, metallic = 0.48f, roughness = 0.54f, uvScaleX = 12f, uvScaleY = 2f)
            val pillarMat = materials.lit(0xFF4A372B, metallic = 0.38f, roughness = 0.62f)
            val wallH = 11.5f
            listOf(
                FilamentTransformHelper.buildTRS(mapW3D * 0.5f, wallH * 0.5f, -0.8f, sx = mapW3D + 2f, sy = wallH, sz = 1.6f),
                FilamentTransformHelper.buildTRS(mapW3D * 0.5f, wallH * 0.5f, mapH3D + 0.8f, sx = mapW3D + 2f, sy = wallH, sz = 1.6f),
                FilamentTransformHelper.buildTRS(-0.8f, wallH * 0.5f, mapH3D * 0.5f, sx = 1.6f, sy = wallH, sz = mapH3D + 2f),
                FilamentTransformHelper.buildTRS(mapW3D + 0.8f, wallH * 0.5f, mapH3D * 0.5f, sx = 1.6f, sy = wallH, sz = mapH3D + 2f)
            ).forEach { m ->
                val w = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, hangarWallMat, castShadows = false)
                tm.setTransform(tm.getInstance(w), m)
                scene.addEntity(w); staticEntities.add(w)
            }

            // Textured Caution Hazard Pads & Center Lane (Screenshot #2)
            val hazardMat = materials.textured(tex.hazardPadMap, tintArgb = 0xFFFFFFFF, metallic = 0.1f, roughness = 0.72f)
            val laneMat = materials.textured(tex.hazardPadMap, tintArgb = 0xFFFFFFFF, metallic = 0.1f, roughness = 0.72f, uvScaleX = 18f, uvScaleY = 1f)
            val lane = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, laneMat, castShadows = false)
            tm.setTransform(tm.getInstance(lane), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, 0.015f, mapH3D * 0.5f, sx = mapW3D * 0.88f, sy = 0.03f, sz = 0.62f))
            scene.addEntity(lane); staticEntities.add(lane)

            listOf(
                6.5f to mapH3D * 0.25f,
                6.5f to mapH3D * 0.75f,
                mapW3D - 6.5f to mapH3D * 0.25f,
                mapW3D - 6.5f to mapH3D * 0.75f
            ).forEach { (px, pz) ->
                val pad = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, hazardMat, castShadows = false)
                tm.setTransform(tm.getInstance(pad), FilamentTransformHelper.buildTRS(px, 0.02f, pz, sx = 6.5f, sy = 0.04f, sz = 5.5f))
                scene.addEntity(pad); staticEntities.add(pad)
            }

            // Glowing Red & Cyan Spawn Portal Gates
            val redGateMat = materials.lit(0xFFFF1744, metallic = 0.2f, roughness = 0.2f, emissiveR = 1.0f, emissiveG = 0.12f, emissiveB = 0.16f)
            val cyanGateMat = materials.lit(0xFF00E5FF, metallic = 0.2f, roughness = 0.2f, emissiveR = 0.05f, emissiveG = 0.85f, emissiveB = 1.0f)
            val gateAlly = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, cyanGateMat, castShadows = false)
            tm.setTransform(tm.getInstance(gateAlly), FilamentTransformHelper.buildTRS(2.2f, 2.2f, mapH3D * 0.5f, sx = 0.45f, sy = 4.4f, sz = 6.2f))
            scene.addEntity(gateAlly); staticEntities.add(gateAlly)

            val gateEnemy = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, redGateMat, castShadows = false)
            tm.setTransform(tm.getInstance(gateEnemy), FilamentTransformHelper.buildTRS(mapW3D - 2.2f, 2.2f, mapH3D * 0.5f, sx = 0.45f, sy = 4.4f, sz = 6.2f))
            scene.addEntity(gateEnemy); staticEntities.add(gateEnemy)

            for (k in 1..4) {
                val px = mapW3D * (k / 5f)
                listOf(0.8f, mapH3D - 0.8f).forEach { pz ->
                    val pil = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, pillarMat, castShadows = false)
                    tm.setTransform(tm.getInstance(pil), FilamentTransformHelper.buildTRS(px, wallH * 0.5f, pz, sx = 0.9f, sy = wallH, sz = 0.9f))
                    scene.addEntity(pil); staticEntities.add(pil)
                }
            }
        } else {
            // Bermuda Asphalt Highway Roads + Distant Mountain Ridges (Screenshot #4)
            val roadMatH = materials.textured(tex.asphaltRoadMap, tintArgb = 0xFFFFFFFF, metallic = 0.04f, roughness = 0.86f, uvScaleX = 18f, uvScaleY = 1f)
            val roadMatV = materials.textured(tex.asphaltRoadMap, tintArgb = 0xFFFFFFFF, metallic = 0.04f, roughness = 0.86f, uvScaleX = 1f, uvScaleY = 18f)
            val road1 = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, roadMatH, castShadows = false)
            tm.setTransform(tm.getInstance(road1), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, 0.015f, mapH3D * 0.5f, sx = mapW3D * 0.94f, sy = 0.04f, sz = 4.6f))
            scene.addEntity(road1); staticEntities.add(road1)

            val road2 = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, roadMatV, castShadows = false)
            tm.setTransform(tm.getInstance(road2), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, 0.020f, mapH3D * 0.5f, sx = 4.6f, sy = 0.04f, sz = mapH3D * 0.94f))
            scene.addEntity(road2); staticEntities.add(road2)

            val ridgeMat = materials.textured(tex.bermudaGrassMap, tintArgb = 0xFF6B8A72, metallic = 0.0f, roughness = 0.95f, uvScaleX = 4f, uvScaleY = 4f)
            listOf(
                Triple(mapW3D * 0.22f, -22f, 58f),
                Triple(mapW3D * 0.74f, -24f, 66f),
                Triple(mapW3D + 22f, mapH3D * 0.42f, 62f),
                Triple(-22f, mapH3D * 0.58f, 54f)
            ).forEach { (mx, mz, scale) ->
                val peak = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, ridgeMat, castShadows = false)
                tm.setTransform(tm.getInstance(peak), FilamentTransformHelper.buildTRS(mx, scale * 0.16f, mz, sx = scale, sy = scale * 0.52f, sz = scale))
                scene.addEntity(peak); staticEntities.add(peak)
            }
        }

        // Textured World Obstacles
        val buildingMat = materials.textured(tex.buildingFacadeMap, tintArgb = 0xFFFFFFFF, metallic = 0.05f, roughness = 0.78f, uvScaleX = 2f, uvScaleY = 2f)
        val roofMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF9E4432, metallic = 0.15f, roughness = 0.72f, uvScaleX = 3f, uvScaleY = 3f)
        val crateMat = materials.textured(tex.woodCrateMap, tintArgb = 0xFFFFFFFF, metallic = 0.05f, roughness = 0.80f)
        val rockMat = materials.textured(tex.warehouseFloorMap, tintArgb = 0xFF7C848A, metallic = 0.02f, roughness = 0.92f)
        val trunkMat = materials.textured(tex.woodCrateMap, tintArgb = 0xFF5D4037, metallic = 0.0f, roughness = 0.95f)
        val leafMat = materials.textured(tex.bermudaGrassMap, tintArgb = 0xFF689F38, metallic = 0.0f, roughness = 0.85f, uvScaleX = 2f, uvScaleY = 2f)
        val blueContainerMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF2D6CA2, metallic = 0.42f, roughness = 0.48f, uvScaleX = 2f, uvScaleY = 1f)
        val redContainerMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFFC85130, metallic = 0.40f, roughness = 0.50f, uvScaleX = 2f, uvScaleY = 1f)
        val vanBodyMat = materials.textured(tex.vanBodyMap, tintArgb = 0xFFFFFFFF, metallic = 0.30f, roughness = 0.42f)
        val vanRoofMat = materials.lit(0xFFEBE8DA, metallic = 0.18f, roughness = 0.45f)
        val wheelMat = materials.lit(0xFF1A1D22, metallic = 0.1f, roughness = 0.85f)

        state.obstacles.forEachIndexed { idx, obs ->
            val cx = (obs.x + obs.width * 0.5f) * WORLD_SCALE
            val cz = (obs.y + obs.height * 0.5f) * WORLD_SCALE
            val w = obs.width * WORLD_SCALE
            val d = obs.height * WORLD_SCALE

            when (obs.type) {
                ObstacleType.BUILDING -> {
                    val h = 5.6f
                    val wall = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, buildingMat)
                    tm.setTransform(tm.getInstance(wall), FilamentTransformHelper.buildTRS(cx, h * 0.5f, cz, sx = w, sy = h, sz = d))
                    scene.addEntity(wall); staticEntities.add(wall)

                    val roof = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, roofMat)
                    tm.setTransform(tm.getInstance(roof), FilamentTransformHelper.buildTRS(cx, h + 0.25f, cz, sx = w + 0.55f, sy = 0.5f, sz = d + 0.55f))
                    scene.addEntity(roof); staticEntities.add(roof)
                }
                ObstacleType.CONTAINER_BLUE, ObstacleType.CONTAINER_RED -> {
                    val cMat = if (obs.type == ObstacleType.CONTAINER_BLUE) blueContainerMat else redContainerMat
                    val h = 2.65f
                    val box = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, cMat)
                    tm.setTransform(tm.getInstance(box), FilamentTransformHelper.buildTRS(cx, h * 0.5f, cz, sx = w, sy = h, sz = d))
                    scene.addEntity(box); staticEntities.add(box)
                    if (idx % 2 == 0) {
                        val topBox = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, cMat)
                        tm.setTransform(tm.getInstance(topBox), FilamentTransformHelper.buildTRS(cx, h * 1.5f + 0.05f, cz, sx = w * 0.96f, sy = h, sz = d * 0.96f))
                        scene.addEntity(topBox); staticEntities.add(topBox)
                    }
                }
                ObstacleType.VAN -> {
                    val shell = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, vanBodyMat)
                    tm.setTransform(tm.getInstance(shell), FilamentTransformHelper.buildTRS(cx, 1.25f, cz, sx = w, sy = 1.90f, sz = d))
                    scene.addEntity(shell); staticEntities.add(shell)

                    val roof = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, vanRoofMat)
                    tm.setTransform(tm.getInstance(roof), FilamentTransformHelper.buildTRS(cx, 2.24f, cz, sx = w * 0.92f, sy = 0.14f, sz = d * 0.92f))
                    scene.addEntity(roof); staticEntities.add(roof)

                    listOf(-w * 0.34f to -d * 0.48f, w * 0.34f to -d * 0.48f, -w * 0.34f to d * 0.48f, w * 0.34f to d * 0.48f).forEach { (ox, oz) ->
                        val wheel = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, wheelMat)
                        tm.setTransform(tm.getInstance(wheel), FilamentTransformHelper.buildTRS(cx + ox, 0.36f, cz + oz, rotXDeg = 90f, sx = 0.72f, sy = 0.26f, sz = 0.72f))
                        scene.addEntity(wheel); staticEntities.add(wheel)
                    }
                }
                ObstacleType.CRATE -> {
                    val h = 2.1f
                    val crate = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, crateMat)
                    tm.setTransform(tm.getInstance(crate), FilamentTransformHelper.buildTRS(cx, h * 0.5f, cz, sx = w, sy = h, sz = d))
                    scene.addEntity(crate); staticEntities.add(crate)
                }
                ObstacleType.ROCK -> {
                    val r = (w + d) * 0.5f
                    val rock = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, rockMat)
                    tm.setTransform(tm.getInstance(rock), FilamentTransformHelper.buildTRS(cx, r * 0.35f, cz, sx = r, sy = r * 0.72f, sz = r))
                    scene.addEntity(rock); staticEntities.add(rock)
                }
                ObstacleType.TREE -> {
                    val trunk = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, trunkMat)
                    tm.setTransform(tm.getInstance(trunk), FilamentTransformHelper.buildTRS(cx, 1.8f, cz, sx = 0.52f, sy = 3.6f, sz = 0.52f))
                    scene.addEntity(trunk); staticEntities.add(trunk)

                    val crown1 = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, leafMat)
                    tm.setTransform(tm.getInstance(crown1), FilamentTransformHelper.buildTRS(cx, 3.6f, cz, sx = 3.8f, sy = 2.2f, sz = 3.8f))
                    scene.addEntity(crown1); staticEntities.add(crown1)

                    val crown2 = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, leafMat)
                    tm.setTransform(tm.getInstance(crown2), FilamentTransformHelper.buildTRS(cx, 5.1f, cz, sx = 2.6f, sy = 2.1f, sz = 2.6f))
                    scene.addEntity(crown2); staticEntities.add(crown2)
                }
            }
        }
    }

    fun attach(textureView: TextureView) {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    swapChain?.let { engine.destroySwapChain(it) }
                    swapChain = engine.createSwapChain(surface)
                }

                override fun onDetachedFromSurface() {
                    swapChain?.let {
                        engine.destroySwapChain(it)
                        swapChain = null
                    }
                }

                override fun onResized(width: Int, height: Int) {
                    view.viewport = Viewport(0, 0, width, height)
                    val aspect = width.toDouble() / height.coerceAtLeast(1).toDouble()
                    camera.setProjection(52.0, aspect, 0.2, 420.0, Camera.Fov.VERTICAL)
                }
            }
            attachTo(textureView)
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            Choreographer.getInstance().postFrameCallback(this)
            renderFrame(frameTimeNanos)
        }
    }

    fun startLoop() {
        if (running) return
        running = true
        startNs = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun renderFrame(frameTimeNanos: Long) {
        val sc = swapChain ?: return
        val state = latestState ?: return
        val t = (frameTimeNanos - startNs) / 1_000_000_000f
        val tm = engine.transformManager

        ensureMapBuilt(state)

        // 1. 3D C-130 Cargo Airplane
        val wantPlane = state.plane.active
        if (wantPlane && !planeVisible) {
            planeParts.forEach { scene.addEntity(it) }
            planeVisible = true
        } else if (!wantPlane && planeVisible) {
            planeParts.forEach { scene.removeEntity(it) }
            planeVisible = false
        }
        if (wantPlane) {
            val px = state.plane.x * WORLD_SCALE
            val py = state.plane.altitudeMeters
            val pz = state.plane.y * WORLD_SCALE
            val planeYaw = 90f - Math.toDegrees(state.plane.headingRad.toDouble()).toFloat()
            tm.setTransform(tm.getInstance(planeRoot), FilamentTransformHelper.buildTRS(px, py, pz, rotYDeg = planeYaw))
        }

        // 2. 3D Combatants
        val activeIds = HashSet<Int>(state.combatants.size)
        state.combatants.forEach { c ->
            activeIds.add(c.id)
            val rig = characterRigs.getOrPut(c.id) {
                FilamentCharacterRig(engine, scene, meshes, materials).also {
                    it.applyCharacterAppearance(c.characterId, isEnemyBot = c.team != 0)
                }
            }
            val visible = c.isAlive && c.dropState != DropState.IN_PLANE
            rig.setVisible(visible, showParachute = c.dropState == DropState.PARACHUTE)
            if (visible) {
                val yawDeg = 90f - Math.toDegrees(c.aimAngle.toDouble()).toFloat()
                rig.updatePose(
                    wx = c.x * WORLD_SCALE,
                    wy = c.altitudeMeters + c.jumpHeightMeters,
                    wz = c.y * WORLD_SCALE,
                    yawDeg = yawDeg,
                    timeSec = t,
                    isMoving = c.isMoving,
                    isFiring = c.muzzleFlashSec > 0f,
                    dropState = c.dropState,
                    isCrouching = c.isCrouching,
                    isSprinting = c.isSprinting,
                    isLobbyShowcase = false,
                    scale = 1.0f
                )
            }
        }
        characterRigs.forEach { (id, rig) ->
            if (id !in activeIds) rig.setVisible(false, false)
        }

        // 3. Textured Hex Gloo Walls
        syncEntityPool(glooPool, state.glooWalls.size, meshes.box, glooWallMat) { idx, entity ->
            val gw = state.glooWalls[idx]
            val yawDeg = 90f - Math.toDegrees(gw.angleRad.toDouble()).toFloat()
            val w3 = gw.width * WORLD_SCALE
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(
                    tx = gw.centerX * WORLD_SCALE,
                    ty = 1.15f,
                    tz = gw.centerY * WORLD_SCALE,
                    rotYDeg = yawDeg,
                    sx = w3,
                    sy = 2.3f,
                    sz = 0.45f
                )
            )
        }

        // 4. Bullets / Tracers
        syncEntityPool(bulletPool, state.bullets.size, meshes.sphere, bulletMat) { idx, entity ->
            val b = state.bullets[idx]
            val s = if (b.isSonicWave) 1.5f else 0.28f
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(b.x * WORLD_SCALE, 1.25f, b.y * WORLD_SCALE, sx = s, sy = s, sz = s)
            )
        }

        // 5. Loot Crates
        syncEntityPool(lootPool, state.lootDrops.size, meshes.box, weaponLootMat) { idx, entity ->
            val l = state.lootDrops[idx]
            val bob = 0.45f + sin(t * 3.5f + idx) * 0.14f
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(
                    l.x * WORLD_SCALE,
                    bob,
                    l.y * WORLD_SCALE,
                    rotYDeg = t * 65f,
                    sx = 0.72f,
                    sy = 0.45f,
                    sz = 0.72f
                )
            )
        }

        // 6. Chrono Domes & Alok Auras
        syncEntityPool(domePool, state.chronoDomes.size, meshes.sphere, chronoMat) { idx, entity ->
            val d = state.chronoDomes[idx]
            val dia = d.radius * WORLD_SCALE * 2f
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(d.x * WORLD_SCALE, 1.0f, d.y * WORLD_SCALE, sx = dia, sy = dia * 0.65f, sz = dia)
            )
        }
        syncEntityPool(auraPool, state.alokAuras.size, meshes.cylinder, alokMat) { idx, entity ->
            val a = state.alokAuras[idx]
            val dia = a.radius * WORLD_SCALE * 2f
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(a.x * WORLD_SCALE, 0.08f, a.y * WORLD_SCALE, sx = dia, sy = 0.12f, sz = dia)
            )
        }

        // 7. Blue Zone Boundary Segments
        val segCount = 16
        syncEntityPool(zoneRingSegments, segCount, meshes.box, zoneMat) { idx, entity ->
            val sz = state.safeZone
            val a = (idx.toFloat() / segCount) * (2f * PI.toFloat())
            val r3 = sz.currentRadius * WORLD_SCALE
            val wx = sz.centerX * WORLD_SCALE + cos(a) * r3
            val wz = sz.centerY * WORLD_SCALE + sin(a) * r3
            val chordW = (2f * PI.toFloat() * r3 / segCount) * 1.05f
            val yaw = 90f - Math.toDegrees(a.toDouble()).toFloat()
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(wx, 11f, wz, rotYDeg = yaw, sx = chordW, sy = 22f, sz = 0.35f)
            )
        }

        updateCamera(state)

        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun updateCamera(state: ArenaState) {
        val player = state.player ?: return
        val px = player.x * WORLD_SCALE
        val py = player.altitudeMeters
        val pz = player.y * WORLD_SCALE

        when (player.dropState) {
            DropState.IN_PLANE -> {
                val h = state.plane.headingRad
                val fx = cos(h)
                val fz = sin(h)
                val eyeX = state.plane.x * WORLD_SCALE - fx * 22.0
                val eyeY = state.plane.altitudeMeters + 10.5
                val eyeZ = state.plane.y * WORLD_SCALE - fz * 22.0
                camera.lookAt(
                    eyeX, eyeY, eyeZ,
                    state.plane.x * WORLD_SCALE + fx * 10.0, state.plane.altitudeMeters - 8.0, state.plane.y * WORLD_SCALE + fz * 10.0,
                    0.0, 1.0, 0.0
                )
            }
            DropState.SKYDIVING, DropState.PARACHUTE -> {
                val fx = cos(player.aimAngle)
                val fz = sin(player.aimAngle)
                val distBack = if (player.dropState == DropState.PARACHUTE) 9.0 else 7.2
                val heightUp = if (player.dropState == DropState.PARACHUTE) 4.2 else 5.5
                camera.lookAt(
                    px - fx * distBack, py + heightUp, pz - fz * distBack,
                    px + fx * 6.0, py - 2.5, pz + fz * 6.0,
                    0.0, 1.0, 0.0
                )
            }
            DropState.GROUNDED -> {
                val fx = cos(player.aimAngle)
                val fz = sin(player.aimAngle)
                val rx = -fz
                val rz = fx
                val zoomBack = if (player.isScoped) 2.85 else 5.15
                val crouchOffset = if (player.isCrouching) -0.45 else 0.0
                val camHeight = (if (player.isScoped) 1.90 else 2.48) + player.jumpHeightMeters + crouchOffset
                val shoulderOffset = if (player.isScoped) 0.52 else 0.72
                val eyeX = px - fx * zoomBack + rx * shoulderOffset
                val eyeY = camHeight
                val eyeZ = pz - fz * zoomBack + rz * shoulderOffset
                val lookDist = if (player.isScoped) 16.0 else 10.0
                camera.lookAt(
                    eyeX, eyeY, eyeZ,
                    px + fx * lookDist + rx * 0.25,
                    1.38 + player.jumpHeightMeters + crouchOffset * 0.6,
                    pz + fz * lookDist + rz * 0.25,
                    0.0, 1.0, 0.0
                )
            }
        }
    }

    private inline fun syncEntityPool(
        pool: MutableList<Int>,
        needed: Int,
        mesh: FilamentMesh,
        mat: MaterialInstance,
        onConfigure: (Int, Int) -> Unit
    ) {
        while (pool.size < needed) {
            val e = FilamentTransformHelper.spawnMeshEntity(engine, mesh, mat, castShadows = false)
            scene.addEntity(e)
            pool.add(e)
        }
        while (pool.size > needed) {
            val e = pool.removeAt(pool.lastIndex)
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
        for (i in 0 until needed) {
            onConfigure(i, pool[i])
        }
    }

    fun stopAndDestroy() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        uiHelper?.detach()
        swapChain?.let { engine.destroySwapChain(it) }
        characterRigs.values.forEach { it.destroy() }
        characterRigs.clear()
        (staticEntities + glooPool + bulletPool + lootPool + domePool + auraPool + zoneRingSegments + planeParts + planeRoot + sunEntity + fillLightEntity + cameraEntity).forEach { e ->
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
        engine.destroyColorGrading(colorGrading)
        engine.destroySkybox(skybox)
        engine.destroyIndirectLight(indirectLight)
        materials.destroy()
        meshes.destroy()
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroy()
    }
}
