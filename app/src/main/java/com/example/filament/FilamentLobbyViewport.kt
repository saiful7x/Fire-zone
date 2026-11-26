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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CharacterId
import com.google.android.filament.Camera
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
    val context = LocalContext.current
    var yawDeg by remember { mutableFloatStateOf(16f) }

    val holder = remember {
        FilamentInit.ensureInitialized()
        LobbySceneController(context)
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

private class LobbySceneController(context: android.content.Context) {
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

        skybox = Skybox.Builder()
            .color(0.08f, 0.09f, 0.14f, 1.0f)
            .build(engine)
        scene.skybox = skybox

        val shBands = floatArrayOf(
            0.55f, 0.52f, 0.60f,
            0.0f, 0.1f, 0.1f,
            0.0f, 0.0f, 0.0f
        )
        indirectLight = IndirectLight.Builder()
            .irradiance(1, shBands)
            .intensity(28000f)
            .build(engine)
        scene.indirectLight = indirectLight

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.88f, 0.72f)
            .intensity(115000f)
            .direction(-0.42f, -0.78f, -0.45f)
            .castShadows(true)
            .build(engine, sunEntity)
        scene.addEntity(sunEntity)

        buildLobbyEnvironment()
        characterRig.setVisible(true, showParachute = false)
        characterRig.applyCharacterAppearance(CharacterId.ALOK)
    }

    private fun buildLobbyEnvironment() {
        val tm = engine.transformManager
        val floorMat = materials.lit(0xFF141A24, metallic = 0.2f, roughness = 0.75f)
        val pedestalMat = materials.lit(0xFF1F2735, metallic = 0.65f, roughness = 0.35f)
        val glowAmberMat = materials.lit(0xFFFFB300, metallic = 0.4f, roughness = 0.3f, emissiveR = 0.9f, emissiveG = 0.58f, emissiveB = 0.05f)
        val glowCyanMat = materials.lit(0xFF00E5FF, metallic = 0.3f, roughness = 0.3f, emissiveR = 0.0f, emissiveG = 0.75f, emissiveB = 0.95f)
        val crateMat = materials.lit(0xFF5D4635, metallic = 0.1f, roughness = 0.85f)
        val pillarMat = materials.lit(0xFF1D2432, metallic = 0.45f, roughness = 0.55f)

        // Courtyard floor
        val floor = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, floorMat, castShadows = false)
        tm.setTransform(tm.getInstance(floor), FilamentTransformHelper.buildTRS(0f, -0.25f, 0f, sx = 28f, sy = 0.5f, sz = 24f))
        scene.addEntity(floor)
        propEntities.add(floor)

        // Central 3D Pedestal
        val ped = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, pedestalMat)
        tm.setTransform(tm.getInstance(ped), FilamentTransformHelper.buildTRS(0f, -0.06f, 0f, sx = 2.4f, sy = 0.14f, sz = 2.4f))
        scene.addEntity(ped)
        propEntities.add(ped)

        pedestalRingEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, glowAmberMat, castShadows = false)
        tm.setTransform(tm.getInstance(pedestalRingEntity), FilamentTransformHelper.buildTRS(0f, 0.005f, 0f, sx = 2.55f, sy = 0.03f, sz = 2.55f))
        scene.addEntity(pedestalRingEntity)
        propEntities.add(pedestalRingEntity)

        // Tactical Crates & Arena Pillars behind character
        val crateCoords = listOf(
            Triple(-3.4f, 0.55f, -3.2f),
            Triple(3.6f, 0.45f, -2.8f),
            Triple(-4.2f, 0.38f, -1.8f)
        )
        crateCoords.forEach { (cx, cy, cz) ->
            val crate = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, crateMat)
            tm.setTransform(tm.getInstance(crate), FilamentTransformHelper.buildTRS(cx, cy, cz, rotYDeg = 22f, sx = 1.1f, sy = cy * 2f, sz = 1.1f))
            scene.addEntity(crate)
            propEntities.add(crate)
        }

        // Translucent Cyan Gloo Wall showcase shield in lobby background
        val glooMat = materials.transparent(0.0f, 0.85f, 1.0f, 0.48f)
        val lobbyGloo = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, glooMat, castShadows = false)
        tm.setTransform(tm.getInstance(lobbyGloo), FilamentTransformHelper.buildTRS(3.2f, 1.05f, -3.9f, rotYDeg = -28f, sx = 2.4f, sy = 2.1f, sz = 0.18f))
        scene.addEntity(lobbyGloo)
        propEntities.add(lobbyGloo)

        // Arena backdrop pillars + glowing strips
        listOf(-5.5f, -2.2f, 2.2f, 5.5f).forEach { px ->
            val pillar = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, pillarMat)
            tm.setTransform(tm.getInstance(pillar), FilamentTransformHelper.buildTRS(px, 2.8f, -6.5f, sx = 0.9f, sy = 6.0f, sz = 0.9f))
            scene.addEntity(pillar)
            propEntities.add(pillar)

            val strip = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, glowCyanMat, castShadows = false)
            tm.setTransform(tm.getInstance(strip), FilamentTransformHelper.buildTRS(px, 2.8f, -6.02f, sx = 0.12f, sy = 4.6f, sz = 0.08f))
            scene.addEntity(strip)
            propEntities.add(strip)
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
                    camera.setProjection(42.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
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
            scale = 1.05f
        )

        val tm = engine.transformManager
        tm.setTransform(
            tm.getInstance(pedestalRingEntity),
            FilamentTransformHelper.buildTRS(cx, 0.01f, 0f, rotYDeg = t * 28f, sx = 2.45f, sy = 0.03f, sz = 2.45f)
        )

        camera.lookAt(
            0.0, 1.48, 4.15,
            0.0, 1.05, 0.0,
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
        scene.removeEntity(sunEntity)
        engine.destroyEntity(sunEntity)
        engine.destroyEntity(cameraEntity)
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
