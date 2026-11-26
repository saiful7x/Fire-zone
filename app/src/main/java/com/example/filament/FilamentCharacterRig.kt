package com.example.filament

import com.example.engine.DropState
import com.example.model.CharacterId
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Scene
import kotlin.math.sin

class FilamentCharacterRig(
    private val engine: Engine,
    private val scene: Scene,
    private val meshes: FilamentMeshBank,
    private val materials: FilamentMaterialBank
) {
    val rootEntity: Int = com.google.android.filament.EntityManager.get().create()
    private val hipsEntity: Int
    private val torsoEntity: Int
    private val backpackEntity: Int
    private val headEntity: Int
    private val visorEntity: Int
    private val leftArmEntity: Int
    private val rightArmEntity: Int
    private val gunBodyEntity: Int
    private val gunBarrelEntity: Int
    private val leftLegEntity: Int
    private val rightLegEntity: Int
    private val parachuteEntity: Int

    private val torsoMat: MaterialInstance
    private val pantsMat: MaterialInstance
    private val visorMat: MaterialInstance
    private val skinMat: MaterialInstance
    private val gunMat: MaterialInstance
    private val chuteMat: MaterialInstance

    private val allEntities = mutableListOf<Int>()
    private var inScene = false
    private var chuteVisible = false

    init {
        val tm = engine.transformManager
        tm.create(rootEntity)

        skinMat = materials.lit(0xFFDAB393, metallic = 0.0f, roughness = 0.72f)
        torsoMat = materials.lit(0xFF212936, metallic = 0.2f, roughness = 0.55f)
        pantsMat = materials.lit(0xFF2A323D, metallic = 0.05f, roughness = 0.78f)
        visorMat = materials.lit(0xFFFFB300, metallic = 0.6f, roughness = 0.25f, emissiveR = 0.5f, emissiveG = 0.35f, emissiveB = 0f)
        gunMat = materials.lit(0xFF1A1E24, metallic = 0.8f, roughness = 0.35f)
        chuteMat = materials.lit(0xFFFF6D00, metallic = 0.05f, roughness = 0.65f)
        val packMat = materials.lit(0xFF3E4A3D, metallic = 0.05f, roughness = 0.8f)

        hipsEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, pantsMat)
        torsoEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, torsoMat)
        backpackEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, packMat)
        headEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, skinMat)
        visorEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, visorMat)
        leftArmEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, skinMat)
        rightArmEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, skinMat)
        gunBodyEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunMat)
        gunBarrelEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, gunMat)
        leftLegEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, pantsMat)
        rightLegEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, pantsMat)
        parachuteEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.dome, chuteMat, castShadows = false)

        allEntities.addAll(
            listOf(
                hipsEntity, torsoEntity, backpackEntity, headEntity, visorEntity,
                leftArmEntity, rightArmEntity, gunBodyEntity, gunBarrelEntity,
                leftLegEntity, rightLegEntity
            )
        )

        val rootTi = tm.getInstance(rootEntity)
        val hipsTi = tm.getInstance(hipsEntity)
        val torsoTi = tm.getInstance(torsoEntity)
        val headTi = tm.getInstance(headEntity)
        val rightArmTi = tm.getInstance(rightArmEntity)

        tm.setParent(hipsTi, rootTi)
        tm.setParent(torsoTi, hipsTi)
        tm.setParent(tm.getInstance(backpackEntity), torsoTi)
        tm.setParent(headTi, torsoTi)
        tm.setParent(tm.getInstance(visorEntity), headTi)
        tm.setParent(tm.getInstance(leftArmEntity), torsoTi)
        tm.setParent(rightArmTi, torsoTi)
        tm.setParent(tm.getInstance(gunBodyEntity), rightArmTi)
        tm.setParent(tm.getInstance(gunBarrelEntity), tm.getInstance(gunBodyEntity))
        tm.setParent(tm.getInstance(leftLegEntity), hipsTi)
        tm.setParent(tm.getInstance(rightLegEntity), hipsTi)
        tm.setParent(tm.getInstance(parachuteEntity), rootTi)
    }

    fun applyCharacterAppearance(characterId: CharacterId, isEnemyBot: Boolean = false) {
        val (torsoColor, pantsColor, visorColor, emit) = when {
            isEnemyBot -> QuadColor(0xFF7B1F29, 0xFF23252C, 0xFFFF2E4C, Triple(0.55f, 0.05f, 0.08f))
            characterId == CharacterId.ALOK -> QuadColor(0xFF1A1E29, 0xFF151820, 0xFFFFB300, Triple(0.6f, 0.42f, 0f))
            characterId == CharacterId.CHRONO -> QuadColor(0xFF182A3E, 0xFF141B26, 0xFF00E5FF, Triple(0f, 0.55f, 0.7f))
            characterId == CharacterId.SKYLER -> QuadColor(0xFFE8ECF2, 0xFF232732, 0xFFFF2E4C, Triple(0.55f, 0.05f, 0.1f))
            characterId == CharacterId.WUKONG -> QuadColor(0xFF23402A, 0xFF2A261E, 0xFF00E676, Triple(0f, 0.55f, 0.2f))
            characterId == CharacterId.KELLY -> QuadColor(0xFFFF6D00, 0xFF1A1E26, 0xFFFFEA00, Triple(0.5f, 0.35f, 0f))
            else -> QuadColor(0xFF1E2D4D, 0xFF161D2C, 0xFF448AFF, Triple(0.1f, 0.3f, 0.7f))
        }
        setLitColor(torsoMat, torsoColor)
        setLitColor(pantsMat, pantsColor)
        setLitColor(visorMat, visorColor, emit.first, emit.second, emit.third)
    }

    private data class QuadColor(
        val torso: Long,
        val pants: Long,
        val visor: Long,
        val emit: Triple<Float, Float, Float>
    )

    private fun setLitColor(mat: MaterialInstance, argb: Long, er: Float = 0f, eg: Float = 0f, eb: Float = 0f) {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        mat.setParameter("baseColor", r, g, b, 1f)
        mat.setParameter("emissive", er, eg, eb)
    }

    fun setVisible(visible: Boolean, showParachute: Boolean = false) {
        if (visible && !inScene) {
            allEntities.forEach { scene.addEntity(it) }
            inScene = true
        } else if (!visible && inScene) {
            allEntities.forEach { scene.removeEntity(it) }
            inScene = false
        }
        val wantChute = visible && showParachute
        if (wantChute && !chuteVisible) {
            scene.addEntity(parachuteEntity)
            chuteVisible = true
        } else if (!wantChute && chuteVisible) {
            scene.removeEntity(parachuteEntity)
            chuteVisible = false
        }
    }

    fun updatePose(
        wx: Float,
        wy: Float,
        wz: Float,
        yawDeg: Float,
        timeSec: Float,
        isMoving: Boolean,
        isFiring: Boolean,
        dropState: DropState = DropState.GROUNDED,
        scale: Float = 1f
    ) {
        val tm = engine.transformManager
        val divePitch = if (dropState == DropState.SKYDIVING) 65f else 0f
        tm.setTransform(
            tm.getInstance(rootEntity),
            FilamentTransformHelper.buildTRS(wx, wy, wz, rotYDeg = yawDeg, rotXDeg = divePitch, sx = scale, sy = scale, sz = scale)
        )

        val breath = sin(timeSec * 2.4f) * 0.015f
        val stride = if (isMoving && dropState == DropState.GROUNDED) sin(timeSec * 11f) * 34f else 0f
        val armSwing = if (isMoving && dropState == DropState.GROUNDED) -stride * 0.55f else 0f
        val recoil = if (isFiring) -10f else 0f

        // Hips at pelvis height ~0.88m
        tm.setTransform(tm.getInstance(hipsEntity), FilamentTransformHelper.buildTRS(0f, 0.88f + breath, 0f, sx = 0.42f, sy = 0.2f, sz = 0.26f))
        // Torso above hips (note inverse parent scale cancels? Wait: Filament child inherits parent scale! Let's keep parent scales 1.0 or adjust!)
        // IMPORTANT: Since hipsEntity has scale (0.42, 0.2, 0.26), children of hipsEntity would inherit non-uniform scale!
        // To keep clean articulated joints without skew, let's attach limbs to rootEntity or use unscaled bone transforms!
        applyCleanHierarchyTransforms(tm, breath, stride, armSwing, recoil, dropState)
    }

    private fun applyCleanHierarchyTransforms(
        tm: com.google.android.filament.TransformManager,
        breath: Float,
        stride: Float,
        armSwing: Float,
        recoil: Float,
        dropState: DropState
    ) {
        // Re-parent directly under rootEntity? No, keep torso/hips unscaled? Wait: hipsEntity IS a renderable box, so its transform has sx/sy/sz!
        // Instead, detach child inheritance or parent all parts directly to rootEntity so every part has exact meter dimensions!
        val rootTi = tm.getInstance(rootEntity)
        allEntities.forEach { tm.setParent(tm.getInstance(it), rootTi) }

        tm.setTransform(tm.getInstance(hipsEntity), FilamentTransformHelper.buildTRS(0f, 0.85f + breath, 0f, sx = 0.40f, sy = 0.18f, sz = 0.24f))
        tm.setTransform(tm.getInstance(torsoEntity), FilamentTransformHelper.buildTRS(0f, 1.20f + breath, 0f, sx = 0.46f, sy = 0.52f, sz = 0.26f))
        tm.setTransform(tm.getInstance(backpackEntity), FilamentTransformHelper.buildTRS(0f, 1.22f + breath, -0.18f, sx = 0.30f, sy = 0.38f, sz = 0.14f))
        tm.setTransform(tm.getInstance(headEntity), FilamentTransformHelper.buildTRS(0f, 1.62f + breath, 0f, sx = 0.28f, sy = 0.30f, sz = 0.28f))
        tm.setTransform(tm.getInstance(visorEntity), FilamentTransformHelper.buildTRS(0f, 1.64f + breath, 0.12f, sx = 0.24f, sy = 0.08f, sz = 0.08f))

        val armPitch = if (dropState == DropState.PARACHUTE) -145f else -78f + recoil
        tm.setTransform(
            tm.getInstance(leftArmEntity),
            FilamentTransformHelper.buildTRS(-0.31f, 1.24f + breath, 0.08f, rotXDeg = armSwing - 25f, sx = 0.14f, sy = 0.46f, sz = 0.14f)
        )
        tm.setTransform(
            tm.getInstance(rightArmEntity),
            FilamentTransformHelper.buildTRS(0.31f, 1.28f + breath, 0.20f, rotXDeg = armPitch, sx = 0.14f, sy = 0.48f, sz = 0.14f)
        )

        // 3D Rifle/Weapon in front of right hand
        tm.setTransform(
            tm.getInstance(gunBodyEntity),
            FilamentTransformHelper.buildTRS(0.24f, 1.22f + breath, 0.48f, rotXDeg = recoil * 0.5f, sx = 0.08f, sy = 0.14f, sz = 0.42f)
        )
        tm.setTransform(
            tm.getInstance(gunBarrelEntity),
            FilamentTransformHelper.buildTRS(0.24f, 1.24f + breath, 0.78f, rotXDeg = 90f + recoil * 0.5f, sx = 0.045f, sy = 0.32f, sz = 0.045f)
        )

        // Legs
        tm.setTransform(
            tm.getInstance(leftLegEntity),
            FilamentTransformHelper.buildTRS(-0.12f, 0.42f, stride * 0.004f, rotXDeg = stride, sx = 0.16f, sy = 0.78f, sz = 0.16f)
        )
        tm.setTransform(
            tm.getInstance(rightLegEntity),
            FilamentTransformHelper.buildTRS(0.12f, 0.42f, -stride * 0.004f, rotXDeg = -stride, sx = 0.16f, sy = 0.78f, sz = 0.16f)
        )

        // 3D Parachute Canopy overhead
        if (chuteVisible) {
            tm.setTransform(
                tm.getInstance(parachuteEntity),
                FilamentTransformHelper.buildTRS(0f, 2.75f, 0f, sx = 2.6f, sy = 1.4f, sz = 2.6f)
            )
        }
    }

    fun destroy() {
        setVisible(false, showParachute = false)
        (allEntities + parachuteEntity + rootEntity).forEach { e ->
            scene.removeEntity(e)
            engine.destroyEntity(e)
        }
    }
}
