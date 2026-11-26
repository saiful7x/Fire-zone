package com.example.filament

import com.example.engine.DropState
import com.example.model.CharacterId
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Scene
import kotlin.math.sin

class FilamentCharacterRig(
    private val engine: Engine,
    private val scene: Scene,
    private val meshes: FilamentMeshBank,
    private val materials: FilamentMaterialBank
) {
    val rootEntity: Int = EntityManager.get().create()
    private val hipsEntity: Int
    private val torsoEntity: Int
    private val chestPlateEntity: Int
    private val coatTailEntity: Int
    private val backpackEntity: Int
    private val headEntity: Int
    private val hairCrownEntity: Int
    private val visorEntity: Int
    private val leftShoulderEntity: Int
    private val rightShoulderEntity: Int
    private val leftArmEntity: Int
    private val rightArmEntity: Int
    private val leftGauntletEntity: Int
    private val rightGauntletEntity: Int
    private val gunBodyEntity: Int
    private val gunBarrelEntity: Int
    private val leftLegEntity: Int
    private val rightLegEntity: Int
    private val leftBootEntity: Int
    private val rightBootEntity: Int
    private val parachuteEntity: Int

    private val torsoMat: MaterialInstance
    private val accentArmorMat: MaterialInstance
    private val pantsMat: MaterialInstance
    private val visorMat: MaterialInstance
    private val gauntletMat: MaterialInstance
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
        torsoMat = materials.lit(0xFF212936, metallic = 0.25f, roughness = 0.50f)
        accentArmorMat = materials.lit(0xFFFF6D00, metallic = 0.45f, roughness = 0.35f)
        pantsMat = materials.lit(0xFF1D222B, metallic = 0.08f, roughness = 0.75f)
        visorMat = materials.lit(0xFFFFB300, metallic = 0.65f, roughness = 0.25f, emissiveR = 0.55f, emissiveG = 0.38f, emissiveB = 0f)
        gauntletMat = materials.lit(0xFF9C27B0, metallic = 0.4f, roughness = 0.3f, emissiveR = 0.55f, emissiveG = 0.12f, emissiveB = 0.75f)
        gunMat = materials.lit(0xFF1A1E24, metallic = 0.8f, roughness = 0.32f)
        chuteMat = materials.lit(0xFFFF6D00, metallic = 0.05f, roughness = 0.65f)
        val packMat = materials.lit(0xFF2F3742, metallic = 0.2f, roughness = 0.65f)

        hipsEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, pantsMat)
        torsoEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, torsoMat)
        chestPlateEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, accentArmorMat)
        coatTailEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, torsoMat)
        backpackEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, packMat)
        headEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, skinMat)
        hairCrownEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, accentArmorMat)
        visorEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, visorMat)
        leftShoulderEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, accentArmorMat)
        rightShoulderEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, accentArmorMat)
        leftArmEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, torsoMat)
        rightArmEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, torsoMat)
        leftGauntletEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, gauntletMat)
        rightGauntletEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.sphere, gauntletMat)
        gunBodyEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunMat)
        gunBarrelEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, gunMat)
        leftLegEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, pantsMat)
        rightLegEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, pantsMat)
        leftBootEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, accentArmorMat)
        rightBootEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, accentArmorMat)
        parachuteEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.dome, chuteMat, castShadows = false)

        allEntities.addAll(
            listOf(
                hipsEntity, torsoEntity, chestPlateEntity, coatTailEntity, backpackEntity,
                headEntity, hairCrownEntity, visorEntity,
                leftShoulderEntity, rightShoulderEntity,
                leftArmEntity, rightArmEntity, leftGauntletEntity, rightGauntletEntity,
                gunBodyEntity, gunBarrelEntity,
                leftLegEntity, rightLegEntity, leftBootEntity, rightBootEntity
            )
        )

        val rootTi = tm.getInstance(rootEntity)
        allEntities.forEach { tm.setParent(tm.getInstance(it), rootTi) }
        tm.setParent(tm.getInstance(parachuteEntity), rootTi)
    }

    fun applyCharacterAppearance(characterId: CharacterId, isEnemyBot: Boolean = false) {
        val style = if (isEnemyBot) {
            CharStyle(0xFF5D1720, 0xFFB71C1C, 0xFF1D1F25, 0xFFFF2E4C, Triple(0.6f, 0.06f, 0.08f), 0xFFFF3D00, Triple(0.55f, 0.12f, 0.04f))
        } else {
            when (characterId) {
                CharacterId.ALOK -> CharStyle(0xFF171A23, 0xFFFFB300, 0xFF14171F, 0xFFFFCA28, Triple(0.75f, 0.50f, 0f), 0xFFFFB300, Triple(0.65f, 0.40f, 0f))
                CharacterId.CHRONO -> CharStyle(0xFF17263A, 0xFF00E5FF, 0xFF121822, 0xFF00E5FF, Triple(0f, 0.65f, 0.85f), 0xFF00E5FF, Triple(0f, 0.6f, 0.8f))
                CharacterId.SKYLER -> CharStyle(0xFFE3E8F0, 0xFFFF2E4C, 0xFF202530, 0xFFFF2E4C, Triple(0.65f, 0.06f, 0.15f), 0xFFFF5252, Triple(0.55f, 0.08f, 0.1f))
                CharacterId.WUKONG -> CharStyle(0xFF1B1F24, 0xFFD84315, 0xFF191C20, 0xFFFF6D00, Triple(0.75f, 0.25f, 0f), 0xFFAB47BC, Triple(0.60f, 0.12f, 0.80f))
                CharacterId.KELLY -> CharStyle(0xFFFF6D00, 0xFFFFEA00, 0xFF1A1E26, 0xFFFFEA00, Triple(0.65f, 0.55f, 0f), 0xFFFFAB00, Triple(0.5f, 0.35f, 0f))
                CharacterId.K_MASTER -> CharStyle(0xFF1D2D4E, 0xFF448AFF, 0xFF151C2B, 0xFF448AFF, Triple(0.15f, 0.35f, 0.85f), 0xFF2979FF, Triple(0.15f, 0.35f, 0.85f))
                CharacterId.MOCO -> CharStyle(0xFF281A45, 0xFF00E5FF, 0xFF181428, 0xFFE040FB, Triple(0.65f, 0.15f, 0.80f), 0xFFE040FB, Triple(0.65f, 0.15f, 0.80f))
                CharacterId.RAFAEL -> CharStyle(0xFF23272F, 0xFFB0BEC5, 0xFF15181F, 0xFFFF5252, Triple(0.6f, 0.08f, 0.08f), 0xFF90A4AE, Triple(0.2f, 0.25f, 0.3f))
                CharacterId.A124 -> CharStyle(0xFFECEFF1, 0xFF00E5FF, 0xFF263238, 0xFF00E5FF, Triple(0f, 0.65f, 0.85f), 0xFF00E5FF, Triple(0f, 0.65f, 0.85f))
                CharacterId.ALVARO -> CharStyle(0xFF3E2723, 0xFFFF6D00, 0xFF26211F, 0xFFFF9100, Triple(0.7f, 0.35f, 0f), 0xFFFF6D00, Triple(0.7f, 0.3f, 0f))
                CharacterId.NIKITA -> CharStyle(0xFF1F242D, 0xFFFFC107, 0xFF181B22, 0xFFFFCA28, Triple(0.65f, 0.45f, 0f), 0xFFFFC107, Triple(0.55f, 0.4f, 0f))
                CharacterId.FORD -> CharStyle(0xFF1A3254, 0xFFD4AF37, 0xFF142138, 0xFF40C4FF, Triple(0.15f, 0.5f, 0.75f), 0xFFD4AF37, Triple(0.5f, 0.4f, 0.1f))
            }
        }
        setLitColor(torsoMat, style.torso)
        setLitColor(accentArmorMat, style.accent)
        setLitColor(pantsMat, style.pants)
        setLitColor(visorMat, style.visor, style.visorEmit.first, style.visorEmit.second, style.visorEmit.third)
        setLitColor(gauntletMat, style.gauntlet, style.gauntletEmit.first, style.gauntletEmit.second, style.gauntletEmit.third)
    }

    private data class CharStyle(
        val torso: Long,
        val accent: Long,
        val pants: Long,
        val visor: Long,
        val visorEmit: Triple<Float, Float, Float>,
        val gauntlet: Long,
        val gauntletEmit: Triple<Float, Float, Float>
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
        isCrouching: Boolean = false,
        isSprinting: Boolean = false,
        scale: Float = 1f
    ) {
        val tm = engine.transformManager
        val divePitch = if (dropState == DropState.SKYDIVING) 65f else 0f
        tm.setTransform(
            tm.getInstance(rootEntity),
            FilamentTransformHelper.buildTRS(wx, wy, wz, rotYDeg = yawDeg, rotXDeg = divePitch, sx = scale, sy = scale, sz = scale)
        )

        val crouchDrop = if (isCrouching && dropState == DropState.GROUNDED) -0.34f else 0f
        val breath = sin(timeSec * 2.4f) * 0.015f
        val strideFreq = if (isSprinting) 15f else 11f
        val strideAmp = if (isSprinting) 44f else 34f
        val stride = if (isMoving && dropState == DropState.GROUNDED) sin(timeSec * strideFreq) * strideAmp else 0f
        val armSwing = if (isMoving && dropState == DropState.GROUNDED) -stride * 0.55f else 0f
        val recoil = if (isFiring) -10f else 0f
        val by = breath + crouchDrop

        // Core Torso & Tactical Coat
        tm.setTransform(tm.getInstance(hipsEntity), FilamentTransformHelper.buildTRS(0f, 0.85f + by, 0f, sx = 0.40f, sy = 0.18f, sz = 0.24f))
        tm.setTransform(tm.getInstance(torsoEntity), FilamentTransformHelper.buildTRS(0f, 1.20f + by, 0f, sx = 0.46f, sy = 0.52f, sz = 0.26f))
        tm.setTransform(tm.getInstance(chestPlateEntity), FilamentTransformHelper.buildTRS(0f, 1.24f + by, 0.12f, sx = 0.38f, sy = 0.34f, sz = 0.08f))
        tm.setTransform(tm.getInstance(coatTailEntity), FilamentTransformHelper.buildTRS(0f, 0.72f + by, -0.14f, rotXDeg = 10f, sx = 0.44f, sy = 0.46f, sz = 0.08f))
        tm.setTransform(tm.getInstance(backpackEntity), FilamentTransformHelper.buildTRS(0f, 1.22f + by, -0.19f, sx = 0.32f, sy = 0.40f, sz = 0.14f))

        // Head, Crown/Mask Crest & Glowing Visor
        tm.setTransform(tm.getInstance(headEntity), FilamentTransformHelper.buildTRS(0f, 1.63f + by, 0f, sx = 0.28f, sy = 0.30f, sz = 0.28f))
        tm.setTransform(tm.getInstance(hairCrownEntity), FilamentTransformHelper.buildTRS(0f, 1.79f + by, -0.02f, sx = 0.30f, sy = 0.12f, sz = 0.28f))
        tm.setTransform(tm.getInstance(visorEntity), FilamentTransformHelper.buildTRS(0f, 1.64f + by, 0.125f, sx = 0.24f, sy = 0.08f, sz = 0.08f))

        // Shoulder Armor Pads
        tm.setTransform(tm.getInstance(leftShoulderEntity), FilamentTransformHelper.buildTRS(-0.30f, 1.43f + by, 0f, rotZDeg = 12f, sx = 0.18f, sy = 0.12f, sz = 0.22f))
        tm.setTransform(tm.getInstance(rightShoulderEntity), FilamentTransformHelper.buildTRS(0.30f, 1.43f + by, 0f, rotZDeg = -12f, sx = 0.18f, sy = 0.12f, sz = 0.22f))

        val armPitch = if (dropState == DropState.PARACHUTE) -145f else -78f + recoil
        tm.setTransform(
            tm.getInstance(leftArmEntity),
            FilamentTransformHelper.buildTRS(-0.31f, 1.22f + by, 0.08f, rotXDeg = armSwing - 22f, sx = 0.14f, sy = 0.46f, sz = 0.14f)
        )
        tm.setTransform(
            tm.getInstance(rightArmEntity),
            FilamentTransformHelper.buildTRS(0.31f, 1.26f + by, 0.20f, rotXDeg = armPitch, sx = 0.14f, sy = 0.48f, sz = 0.14f)
        )
        // Glowing Hand Gauntlets (inspired by Screenshot #1)
        tm.setTransform(
            tm.getInstance(leftGauntletEntity),
            FilamentTransformHelper.buildTRS(-0.32f, 1.00f + by, 0.18f, sx = 0.14f, sy = 0.14f, sz = 0.14f)
        )
        tm.setTransform(
            tm.getInstance(rightGauntletEntity),
            FilamentTransformHelper.buildTRS(0.28f, 1.18f + by, 0.38f, sx = 0.14f, sy = 0.14f, sz = 0.14f)
        )

        // 3D Weapon
        tm.setTransform(
            tm.getInstance(gunBodyEntity),
            FilamentTransformHelper.buildTRS(0.24f, 1.22f + by, 0.48f, rotXDeg = recoil * 0.5f, sx = 0.08f, sy = 0.14f, sz = 0.42f)
        )
        tm.setTransform(
            tm.getInstance(gunBarrelEntity),
            FilamentTransformHelper.buildTRS(0.24f, 1.24f + by, 0.78f, rotXDeg = 90f + recoil * 0.5f, sx = 0.045f, sy = 0.34f, sz = 0.045f)
        )

        // Legs & Tactical Boots
        val legHeightScale = if (isCrouching && dropState == DropState.GROUNDED) 0.52f else 0.78f
        val legY = if (isCrouching && dropState == DropState.GROUNDED) 0.27f else 0.42f
        tm.setTransform(
            tm.getInstance(leftLegEntity),
            FilamentTransformHelper.buildTRS(-0.12f, legY, stride * 0.004f, rotXDeg = stride, sx = 0.16f, sy = legHeightScale, sz = 0.16f)
        )
        tm.setTransform(
            tm.getInstance(rightLegEntity),
            FilamentTransformHelper.buildTRS(0.12f, legY, -stride * 0.004f, rotXDeg = -stride, sx = 0.16f, sy = legHeightScale, sz = 0.16f)
        )
        tm.setTransform(
            tm.getInstance(leftBootEntity),
            FilamentTransformHelper.buildTRS(-0.12f, 0.08f, stride * 0.004f + 0.04f, sx = 0.17f, sy = 0.14f, sz = 0.26f)
        )
        tm.setTransform(
            tm.getInstance(rightBootEntity),
            FilamentTransformHelper.buildTRS(0.12f, 0.08f, -stride * 0.004f + 0.04f, sx = 0.17f, sy = 0.14f, sz = 0.26f)
        )

        // 3D Parachute Canopy
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
