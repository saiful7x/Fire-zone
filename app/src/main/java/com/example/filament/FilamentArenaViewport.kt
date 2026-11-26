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
import com.example.engine.LootType
import com.example.engine.ObstacleType
import com.google.android.filament.Camera
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
    private val skybox: Skybox
    private val indirectLight: IndirectLight

    private val materials = FilamentMaterialBank(engine)
    private val meshes = FilamentMeshBank(engine)

    private val staticEntities = mutableListOf<Int>()
    private var builtMapKey: String = ""

    // Dynamic entity pools
    private val characterRigs = mutableMapOf<Int, FilamentCharacterRig>()
    private val glooPool = mutableListOf<Int>()
    private val bulletPool = mutableListOf<Int>()
    private val lootPool = mutableListOf<Int>()
    private val domePool = mutableListOf<Int>()
    private val auraPool = mutableListOf<Int>()
    private val zoneRingSegments = mutableListOf<Int>()

    // 3D C-130 Cargo Plane parts
    private val planeRoot: Int = EntityManager.get().create()
    private val planeParts = mutableListOf<Int>()
    private var planeVisible = false

    private val glooWallMat: MaterialInstance
    private val bulletMat: MaterialInstance
    private val weaponLootMat: MaterialInstance
    private val glooLootMat: MaterialInstance
    private val medLootMat: MaterialInstance
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

        skybox = Skybox.Builder()
            .color(0.28f, 0.45f, 0.68f, 1.0f)
            .build(engine)
        scene.skybox = skybox

        val shBands = floatArrayOf(
            0.65f, 0.68f, 0.72f,
            0.0f, 0.1f, 0.1f,
            0.0f, 0.0f, 0.0f
        )
        indirectLight = IndirectLight.Builder()
            .irradiance(1, shBands)
            .intensity(32000f)
            .build(engine)
        scene.indirectLight = indirectLight

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.94f, 0.84f)
            .intensity(120000f)
            .direction(-0.45f, -0.82f, -0.35f)
            .castShadows(true)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)

        glooWallMat = materials.transparent(0.0f, 0.88f, 1.0f, 0.62f)
        bulletMat = materials.lit(0xFFFFCA28, metallic = 0.2f, roughness = 0.3f, emissiveR = 1.0f, emissiveG = 0.8f, emissiveB = 0.1f)
        weaponLootMat = materials.lit(0xFFFFB300, metallic = 0.5f, roughness = 0.3f, emissiveR = 0.8f, emissiveG = 0.5f, emissiveB = 0f)
        glooLootMat = materials.lit(0xFF00E5FF, metallic = 0.4f, roughness = 0.3f, emissiveR = 0f, emissiveG = 0.7f, emissiveB = 0.9f)
        medLootMat = materials.lit(0xFF00E676, metallic = 0.2f, roughness = 0.4f, emissiveR = 0f, emissiveG = 0.8f, emissiveB = 0.3f)
        chronoMat = materials.transparent(0.0f, 0.85f, 1.0f, 0.32f)
        alokMat = materials.transparent(1.0f, 0.72f, 0.0f, 0.34f)
        zoneMat = materials.transparent(0.18f, 0.56f, 1.0f, 0.32f)

        buildC130Airplane()
    }

    private fun buildC130Airplane() {
        val tm = engine.transformManager
        tm.create(planeRoot)
        val rootTi = tm.getInstance(planeRoot)

        val bodyMat = materials.lit(0xFF455A64, metallic = 0.65f, roughness = 0.38f)
        val wingMat = materials.lit(0xFF607D8B, metallic = 0.6f, roughness = 0.42f)
        val accentMat = materials.lit(0xFFFF6D00, metallic = 0.4f, roughness = 0.4f, emissiveR = 0.6f, emissiveG = 0.2f, emissiveB = 0f)

        fun part(mesh: FilamentMesh, mat: MaterialInstance, tx: Float, ty: Float, tz: Float, sx: Float, sy: Float, sz: Float, rx: Float = 0f): Int {
            val e = FilamentTransformHelper.spawnMeshEntity(engine, mesh, mat, castShadows = true)
            tm.setParent(tm.getInstance(e), rootTi)
            tm.setTransform(tm.getInstance(e), FilamentTransformHelper.buildTRS(tx, ty, tz, rotXDeg = rx, sx = sx, sy = sy, sz = sz))
            planeParts.add(e)
            return e
        }

        // Fuselage along +Z
        part(meshes.cylinder, bodyMat, 0f, 0f, 0f, 2.2f, 13f, 2.2f, rx = 90f)
        // Nose cone
        part(meshes.sphere, accentMat, 0f, 0f, 6.4f, 2.0f, 1.8f, 2.4f)
        // Main swept wings
        part(meshes.box, wingMat, 0f, 0.55f, 0.8f, 16.5f, 0.28f, 3.0f)
        // Tail vertical stabilizer & horizontal stabilizers
        part(meshes.box, accentMat, 0f, 1.9f, -5.4f, 0.32f, 3.0f, 2.2f)
        part(meshes.box, wingMat, 0f, 0.45f, -5.5f, 6.2f, 0.22f, 1.5f)
        // 4 Turbofan engines
        listOf(-5.2f, -2.8f, 2.8f, 5.2f).forEach { ex ->
            part(meshes.cylinder, accentMat, ex, 0.1f, 2.1f, 0.72f, 1.9f, 0.72f, rx = 90f)
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
        val mapW3D = state.mapWidth * WORLD_SCALE
        val mapH3D = state.mapHeight * WORLD_SCALE

        // Ocean surround
        val oceanMat = materials.lit(0xFF0E3C5C, metallic = 0.3f, roughness = 0.22f)
        val ocean = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, oceanMat, castShadows = false)
        tm.setTransform(tm.getInstance(ocean), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, -1.2f, mapH3D * 0.5f, sx = mapW3D + 220f, sy = 1f, sz = mapH3D + 220f))
        scene.addEntity(ocean)
        staticEntities.add(ocean)

        // Island ground terrain
        val groundColor = when (state.mode.name) {
            "LONE_WOLF" -> 0xFF333842
            "CLASH_SQUAD" -> 0xFF3B434E
            else -> 0xFF3D5A3C
        }
        val groundMat = materials.lit(groundColor, metallic = 0.02f, roughness = 0.9f)
        val ground = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, groundMat, castShadows = false)
        tm.setTransform(tm.getInstance(ground), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, -0.5f, mapH3D * 0.5f, sx = mapW3D, sy = 1f, sz = mapH3D))
        scene.addEntity(ground)
        staticEntities.add(ground)

        // Crossroad asphalt strips on Bermuda / CS
        val roadMat = materials.lit(0xFF2B313B, metallic = 0.05f, roughness = 0.85f)
        val road1 = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, roadMat, castShadows = false)
        tm.setTransform(tm.getInstance(road1), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, 0.01f, mapH3D * 0.5f, sx = mapW3D * 0.92f, sy = 0.04f, sz = 4.5f))
        scene.addEntity(road1); staticEntities.add(road1)

        val road2 = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, roadMat, castShadows = false)
        tm.setTransform(tm.getInstance(road2), FilamentTransformHelper.buildTRS(mapW3D * 0.5f, 0.015f, mapH3D * 0.5f, sx = 4.5f, sy = 0.04f, sz = mapH3D * 0.92f))
        scene.addEntity(road2); staticEntities.add(road2)

        // Obstacles in 3D
        val buildingMat = materials.lit(0xFF566376, metallic = 0.12f, roughness = 0.78f)
        val roofMat = materials.lit(0xFF8C3B2B, metallic = 0.1f, roughness = 0.7f)
        val crateMat = materials.lit(0xFF8D623F, metallic = 0.05f, roughness = 0.82f)
        val rockMat = materials.lit(0xFF606E78, metallic = 0.05f, roughness = 0.92f)
        val trunkMat = materials.lit(0xFF5D4037, metallic = 0.0f, roughness = 0.95f)
        val leafMat = materials.lit(0xFF2E7D32, metallic = 0.0f, roughness = 0.85f)

        state.obstacles.forEach { obs ->
            val cx = (obs.x + obs.width * 0.5f) * WORLD_SCALE
            val cz = (obs.y + obs.height * 0.5f) * WORLD_SCALE
            val w = obs.width * WORLD_SCALE
            val d = obs.height * WORLD_SCALE

            when (obs.type) {
                ObstacleType.BUILDING -> {
                    val h = 5.4f
                    val wall = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, buildingMat)
                    tm.setTransform(tm.getInstance(wall), FilamentTransformHelper.buildTRS(cx, h * 0.5f, cz, sx = w, sy = h, sz = d))
                    scene.addEntity(wall); staticEntities.add(wall)

                    val roof = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, roofMat)
                    tm.setTransform(tm.getInstance(roof), FilamentTransformHelper.buildTRS(cx, h + 0.25f, cz, sx = w + 0.5f, sy = 0.5f, sz = d + 0.5f))
                    scene.addEntity(roof); staticEntities.add(roof)
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
                    tm.setTransform(tm.getInstance(trunk), FilamentTransformHelper.buildTRS(cx, 1.5f, cz, sx = 0.55f, sy = 3.0f, sz = 0.55f))
                    scene.addEntity(trunk); staticEntities.add(trunk)

                    val crown = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, leafMat)
                    tm.setTransform(tm.getInstance(crown), FilamentTransformHelper.buildTRS(cx, 3.8f, cz, sx = 3.4f, sy = 2.8f, sz = 3.4f))
                    scene.addEntity(crown); staticEntities.add(crown)
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

        // 1. Update 3D C-130 Cargo Airplane
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

        // 2. Update 3D Combatants
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
                    wy = c.altitudeMeters,
                    wz = c.y * WORLD_SCALE,
                    yawDeg = yawDeg,
                    timeSec = t,
                    isMoving = c.isMoving,
                    isFiring = c.muzzleFlashSec > 0f,
                    dropState = c.dropState,
                    scale = 1.0f
                )
            }
        }
        // Hide rigs no longer in state
        characterRigs.forEach { (id, rig) ->
            if (id !in activeIds) rig.setVisible(false, false)
        }

        // 3. Update 3D Gloo Walls 🧱
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

        // 4. Update 3D Bullets / Tracers
        syncEntityPool(bulletPool, state.bullets.size, meshes.sphere, bulletMat) { idx, entity ->
            val b = state.bullets[idx]
            val s = if (b.isSonicWave) 1.5f else 0.28f
            tm.setTransform(
                tm.getInstance(entity),
                FilamentTransformHelper.buildTRS(b.x * WORLD_SCALE, 1.25f, b.y * WORLD_SCALE, sx = s, sy = s, sz = s)
            )
        }

        // 5. Update 3D Loot Pickups
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

        // 6. Update 3D Chrono Domes & Alok Auras
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

        // 7. Update 3D Shrinking Blue Zone Boundary Segments (16 wall panels around safe zone)
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

        // 8. Position 3D Third-Person / Flight / Parachute Camera
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
                    state.plane.x * WORLD_SCALE + fx * 10.0, (state.plane.altitudeMeters - 8.0), state.plane.y * WORLD_SCALE + fz * 10.0,
                    0.0, 1.0, 0.0
                )
            }
            DropState.SKYDIVING, DropState.PARACHUTE -> {
                val fx = cos(player.aimAngle)
                val fz = sin(player.aimAngle)
                val distBack = if (player.dropState == DropState.PARACHUTE) 9.0 else 7.2
                val heightUp = if (player.dropState == DropState.PARACHUTE) 4.2 else 5.5
                camera.lookAt(
                    (px - fx * distBack).toDouble(), (py + heightUp).toDouble(), (pz - fz * distBack).toDouble(),
                    (px + fx * 6.0).toDouble(), (py - 2.5).toDouble(), (pz + fz * 6.0).toDouble(),
                    0.0, 1.0, 0.0
                )
            }
            DropState.GROUNDED -> {
                val fx = cos(player.aimAngle)
                val fz = sin(player.aimAngle)
                // Right shoulder offset
                val rx = -fz
                val rz = fx
                val zoomBack = if (player.isScoped) 2.9 else 5.4
                val camHeight = if (player.isScoped) 1.95 else 2.65
                val shoulderOffset = if (player.isScoped) 0.55 else 0.72
                val eyeX = px - fx * zoomBack + rx * shoulderOffset
                val eyeY = camHeight
                val eyeZ = pz - fz * zoomBack + rz * shoulderOffset
                val lookDist = if (player.isScoped) 16.0 else 10.0
                camera.lookAt(
                    eyeX, eyeY, eyeZ,
                    (px + fx * lookDist + rx * 0.25).toDouble(), 1.38, (pz + fz * lookDist + rz * 0.25).toDouble(),
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
        (staticEntities + glooPool + bulletPool + lootPool + domePool + auraPool + zoneRingSegments + planeParts + planeRoot + sunEntity + cameraEntity).forEach { e ->
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
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
