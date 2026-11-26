package com.example.filament

import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CharacterId
import com.google.android.filament.Camera
import com.google.android.filament.ColorGrading
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.UiHelper

@Composable
fun FilamentLobbyViewport(
    characterId: CharacterId,
    offsetCharacterLeft: Boolean,
    modifier: Modifier = Modifier
) {
    var yawDeg by remember { mutableFloatStateOf(16f) }

    val holder = remember {
        FilamentInit.ensureInitialized()
        LobbySceneController()
    }

    LaunchedEffect(characterId) {
        holder.setCharacter(characterId)
    }

    DisposableEffect(Unit) {
        holder.startLoop()
        onDispose {
            holder.stopAndDestroy()
        }
    }

    holder.targetYawDeg = yawDeg
    holder.characterOffsetX = if (offsetCharacterLeft) -1.05f else 0.0f

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                holder.attach(tv)
            }
        },
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yawDeg += dragAmount.x * 0.42f
                }
            }
    )
}

private class LobbySceneController {
    private val engine: Engine = Engine.create()
    private val renderer: Renderer = engine.createRenderer()
    private val scene: Scene = engine.createScene()
    private val view: View = engine.createView()
    private val cameraEntity: Int = EntityManager.get().create()
    private val camera: Camera = engine.createCamera(cameraEntity)
    private val keyLightEntity: Int = EntityManager.get().create()
    private val rimLightEntity: Int = EntityManager.get().create()
    private val fillLightEntity: Int = EntityManager.get().create()
    private val skybox: Skybox
    private val indirectLight: IndirectLight
    private val colorGrading: ColorGrading

    private val materials = FilamentMaterialBank(engine)
    private val meshes = FilamentMeshBank(engine)
    private val characterRig = FilamentCharacterRig(engine, scene, meshes, materials)
    private val propEntities = mutableListOf<Int>()

    private var swapChain: SwapChain? = null
    private var uiHelper: UiHelper? = null
    private var running = false
    private var startNs = System.nanoTime()

    @Volatile var targetYawDeg: Float = 16f
    @Volatile var characterOffsetX: Float = 0f

    private var pedestalRingEntity: Int = 0

    init {
        view.scene = scene
        view.camera = camera
        colorGrading = FilamentPostProcess.configureView(engine, view, enableFog = false)

        skybox = Skybox.Builder()
            .color(0.10f, 0.13f, 0.21f, 1.0f)
            .build(engine)
        scene.skybox = skybox

        val shBands = floatArrayOf(
            0.78f, 0.76f, 0.84f,
            0.05f, 0.12f, 0.18f,
            0.0f, 0.0f, 0.0f
        )
        indirectLight = IndirectLight.Builder()
            .irradiance(1, shBands)
            .intensity(34000f)
            .build(engine)
        scene.indirectLight = indirectLight

        // 3-Point Studio Lighting: Warm Key Light + Cool Rim Light + Soft Fill Light
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.90f, 0.76f)
            .intensity(120000f)
            .direction(-0.42f, -0.75f, -0.50f)
            .castShadows(true)
            .build(engine, keyLightEntity)
        scene.addEntity(keyLightEntity)

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.25f, 0.78f, 1.0f)
            .intensity(55000f)
            .direction(0.55f, -0.25f, 0.78f)
            .castShadows(false)
            .build(engine, rimLightEntity)
        scene.addEntity(rimLightEntity)

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.78f, 0.52f)
            .intensity(32000f)
            .direction(0.65f, -0.35f, -0.60f)
            .castShadows(false)
            .build(engine, fillLightEntity)
        scene.addEntity(fillLightEntity)

        buildLobbyEnvironment()
        characterRig.setVisible(true, showParachute = false)
        characterRig.applyCharacterAppearance(CharacterId.ALOK)
    }

    private fun buildLobbyEnvironment() {
        val tm = engine.transformManager
        val tex = materials.textures
        val floorMat = materials.textured(tex.warehouseFloorMap, tintArgb = 0xFF424B59, metallic = 0.25f, roughness = 0.62f, uvScaleX = 8f, uvScaleY = 8f)
        val pedestalMat = materials.textured(tex.pedestalMetalMap, tintArgb = 0xFFFFFFFF, metallic = 0.75f, roughness = 0.30f)
        val glowAmberMat = materials.lit(0xFFFFB300, metallic = 0.4f, roughness = 0.25f, emissiveR = 0.95f, emissiveG = 0.62f, emissiveB = 0.08f)
        val glowCyanMat = materials.lit(0xFF00E5FF, metallic = 0.3f, roughness = 0.25f, emissiveR = 0.0f, emissiveG = 0.85f, emissiveB = 1.0f)
        val crateMat = materials.textured(tex.woodCrateMap, tintArgb = 0xFFFFFFFF, metallic = 0.05f, roughness = 0.80f)
        val blueContainerMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF2B6090, metallic = 0.45f, roughness = 0.46f)
        val orangeContainerMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFFC7512E, metallic = 0.42f, roughness = 0.48f)
        val pillarMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFF283244, metallic = 0.55f, roughness = 0.48f, uvScaleX = 1f, uvScaleY = 4f)

        // Textured tactical courtyard floor
        val floor = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, floorMat, castShadows = false)
        tm.setTransform(tm.getInstance(floor), FilamentTransformHelper.buildTRS(0f, -0.25f, 0f, sx = 32f, sy = 0.5f, sz = 28f))
        scene.addEntity(floor)
        propEntities.add(floor)

        // Central 3D Pedestal
        val ped = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, pedestalMat)
        tm.setTransform(tm.getInstance(ped), FilamentTransformHelper.buildTRS(0f, -0.06f, 0f, sx = 2.45f, sy = 0.14f, sz = 2.45f))
        scene.addEntity(ped)
        propEntities.add(ped)

        pedestalRingEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, glowAmberMat, castShadows = false)
        tm.setTransform(tm.getInstance(pedestalRingEntity), FilamentTransformHelper.buildTRS(0f, 0.006f, 0f, sx = 2.60f, sy = 0.025f, sz = 2.60f))
        scene.addEntity(pedestalRingEntity)
        propEntities.add(pedestalRingEntity)

        // Background Corrugated Containers & Wooden Crates
        val containerL = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, blueContainerMat)
        tm.setTransform(tm.getInstance(containerL), FilamentTransformHelper.buildTRS(-4.6f, 1.35f, -5.2f, rotYDeg = 24f, sx = 2.6f, sy = 2.7f, sz = 5.4f))
        scene.addEntity(containerL); propEntities.add(containerL)

        val containerR = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, orangeContainerMat)
        tm.setTransform(tm.getInstance(containerR), FilamentTransformHelper.buildTRS(4.8f, 1.35f, -5.4f, rotYDeg = -22f, sx = 2.6f, sy = 2.7f, sz = 5.4f))
        scene.addEntity(containerR); propEntities.add(containerR)

        listOf(
            Triple(-3.1f, 0.55f, -2.8f),
            Triple(3.3f, 0.45f, -2.5f),
            Triple(-2.6f, 0.40f, -1.7f)
        ).forEach { (cx, cy, cz) ->
            val crate = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, crateMat)
            tm.setTransform(tm.getInstance(crate), FilamentTransformHelper.buildTRS(cx, cy, cz, rotYDeg = 22f, sx = 1.05f, sy = cy * 2f, sz = 1.05f))
            scene.addEntity(crate); propEntities.add(crate)
        }

        // Translucent Hex-Pattern Cyan Gloo Wall showcase shield
        val glooMat = materials.texturedTransparent(tex.glooHexMap, r = 0.25f, g = 0.92f, b = 1f, a = 0.68f, uvScaleX = 2f, uvScaleY = 2f)
        val lobbyGloo = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, glooMat, castShadows = false)
        tm.setTransform(tm.getInstance(lobbyGloo), FilamentTransformHelper.buildTRS(3.2f, 1.05f, -3.9f, rotYDeg = -28f, sx = 2.4f, sy = 2.1f, sz = 0.16f))
        scene.addEntity(lobbyGloo); propEntities.add(lobbyGloo)

        // Arena backdrop pillars + glowing energy strips
        listOf(-5.6f, -2.2f, 2.2f, 5.6f).forEach { px ->
            val pillar = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, pillarMat)
            tm.setTransform(tm.getInstance(pillar), FilamentTransformHelper.buildTRS(px, 2.9f, -7.2f, sx = 0.95f, sy = 6.2f, sz = 0.95f))
            scene.addEntity(pillar); propEntities.add(pillar)

            val strip = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, glowCyanMat, castShadows = false)
            tm.setTransform(tm.getInstance(strip), FilamentTransformHelper.buildTRS(px, 2.9f, -6.68f, sx = 0.12f, sy = 4.8f, sz = 0.08f))
            scene.addEntity(strip); propEntities.add(strip)
        }
    }

    fun setCharacter(characterId: CharacterId) {
        characterRig.applyCharacterAppearance(characterId)
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
                    camera.setProjection(40.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
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
        val t = (frameTimeNanos - startNs) / 1_000_000_000f

        val cx = characterOffsetX
        characterRig.updatePose(
            wx = cx,
            wy = 0.01f,
            wz = 0f,
            yawDeg = targetYawDeg,
            timeSec = t,
            isMoving = false,
            isFiring = false,
            isLobbyShowcase = true,
            scale = 1.06f
        )

        val tm = engine.transformManager
        tm.setTransform(
            tm.getInstance(pedestalRingEntity),
            FilamentTransformHelper.buildTRS(cx, 0.01f, 0f, rotYDeg = t * 28f, sx = 2.52f, sy = 0.025f, sz = 2.52f)
        )

        camera.lookAt(
            0.0, 1.44, 3.85,
            0.0, 1.02, 0.0,
            0.0, 1.0, 0.0
        )

        if (renderer.beginFrame(sc, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun stopAndDestroy() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        uiHelper?.detach()
        swapChain?.let { engine.destroySwapChain(it) }
        characterRig.destroy()
        propEntities.forEach { e ->
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
        listOf(keyLightEntity, rimLightEntity, fillLightEntity, cameraEntity).forEach { e ->
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
