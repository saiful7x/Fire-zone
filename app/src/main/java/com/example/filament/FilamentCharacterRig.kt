package com.example.filament

import com.example.engine.DropState
import com.example.model.CharacterId
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Scene
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class FilamentCharacterRig(
    private val engine: Engine,
    private val scene: Scene,
    private val meshes: FilamentMeshBank,
    private val materials: FilamentMaterialBank
) {
    val rootEntity: Int = EntityManager.get().create()

    // Articulated Human Skeleton Hierarchy
    private val pelvisEntity: Int
    private val torsoEntity: Int
    private val hoodCollarEntity: Int
    private val backpackEntity: Int
    private val headEntity: Int
    private val hairEntity: Int
    private val shadesEntity: Int

    private val leftUpperArmEntity: Int
    private val leftForearmEntity: Int
    private val leftHandEntity: Int
    private val rightUpperArmEntity: Int
    private val rightForearmEntity: Int
    private val rightHandEntity: Int

    // Detailed Rifle attached to right hand / weapon grip
    private val gunReceiverEntity: Int
    private val gunStockEntity: Int
    private val gunHandguardEntity: Int
    private val gunBarrelEntity: Int
    private val gunMagEntity: Int
    private val gunScopeEntity: Int

    private val leftThighEntity: Int
    private val leftShinEntity: Int
    private val leftBootEntity: Int
    private val rightThighEntity: Int
    private val rightShinEntity: Int
    private val rightBootEntity: Int

    private val parachuteEntity: Int

    // Textured PBR Material Instances per rig
    private val skinMat: MaterialInstance
    private val faceMat: MaterialInstance
    private val hairMat: MaterialInstance
    private val torsoMat: MaterialInstance
    private val hoodAccentMat: MaterialInstance
    private val sleeveMat: MaterialInstance
    private val forearmMat: MaterialInstance
    private val pantsMat: MaterialInstance
    private val bootsMat: MaterialInstance
    private val shadesMat: MaterialInstance
    private val packMat: MaterialInstance
    private val gunMetalMat: MaterialInstance
    private val gunAccentMat: MaterialInstance
    private val chuteMat: MaterialInstance

    private val allEntities = mutableListOf<Int>()
    private var inScene = false
    private var chuteVisible = false
    private var showShades = true

    init {
        val tm = engine.transformManager
        tm.create(rootEntity)

        val tex = materials.textures
        skinMat = materials.textured(tex.skinMap, tintArgb = 0xFFFFFFFF, metallic = 0.0f, roughness = 0.68f, sampler = tex.clampSampler)
        faceMat = materials.textured(tex.faceMap, tintArgb = 0xFFFFFFFF, metallic = 0.0f, roughness = 0.64f, sampler = tex.clampSampler)
        hairMat = materials.textured(tex.hairMap, tintArgb = 0xFF2B201A, metallic = 0.05f, roughness = 0.52f)
        torsoMat = materials.textured(tex.hoodieMap, tintArgb = 0xFFB8BEC7, metallic = 0.04f, roughness = 0.78f)
        hoodAccentMat = materials.textured(tex.hoodieMap, tintArgb = 0xFF9EA5B0, metallic = 0.08f, roughness = 0.72f)
        sleeveMat = materials.textured(tex.hoodieMap, tintArgb = 0xFFB8BEC7, metallic = 0.04f, roughness = 0.76f)
        forearmMat = materials.textured(tex.skinMap, tintArgb = 0xFFFFFFFF, metallic = 0.0f, roughness = 0.68f)
        pantsMat = materials.textured(tex.cargoPantsMap, tintArgb = 0xFF33362B, metallic = 0.04f, roughness = 0.84f)
        bootsMat = materials.textured(tex.bootsMap, tintArgb = 0xFF222226, metallic = 0.12f, roughness = 0.68f)
        shadesMat = materials.lit(0xFFFFB300, metallic = 0.78f, roughness = 0.18f, emissiveR = 0.25f, emissiveG = 0.16f, emissiveB = 0f)
        packMat = materials.textured(tex.woodCrateMap, tintArgb = 0xFF343B46, metallic = 0.18f, roughness = 0.65f)
        gunMetalMat = materials.lit(0xFF1F242C, metallic = 0.85f, roughness = 0.28f)
        gunAccentMat = materials.lit(0xFF424B57, metallic = 0.65f, roughness = 0.40f)
        chuteMat = materials.textured(tex.corrugatedMetalMap, tintArgb = 0xFFFF6D00, metallic = 0.05f, roughness = 0.65f)

        val h = meshes.human
        pelvisEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.pelvisMesh, pantsMat)
        torsoEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.torsoMesh, torsoMat)
        hoodCollarEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.hoodCollarMesh, hoodAccentMat)
        backpackEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, packMat)
        headEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.headMesh, faceMat)
        hairEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.hairMesh, hairMat)
        shadesEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, shadesMat)

        leftUpperArmEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.upperArmMesh, sleeveMat)
        leftForearmEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.forearmMesh, forearmMat)
        leftHandEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.handMesh, skinMat)
        rightUpperArmEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.upperArmMesh, sleeveMat)
        rightForearmEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.forearmMesh, forearmMat)
        rightHandEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.handMesh, skinMat)

        gunReceiverEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunMetalMat)
        gunStockEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunAccentMat)
        gunHandguardEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunAccentMat)
        gunBarrelEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.cylinder, gunMetalMat)
        gunMagEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunMetalMat)
        gunScopeEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.box, gunAccentMat)

        leftThighEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.thighMesh, pantsMat)
        leftShinEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.shinMesh, pantsMat)
        leftBootEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.bootMesh, bootsMat)
        rightThighEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.thighMesh, pantsMat)
        rightShinEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.shinMesh, pantsMat)
        rightBootEntity = FilamentTransformHelper.spawnMeshEntity(engine, h.bootMesh, bootsMat)

        parachuteEntity = FilamentTransformHelper.spawnMeshEntity(engine, meshes.dome, chuteMat, castShadows = false)

        allEntities.addAll(
            listOf(
                pelvisEntity, torsoEntity, hoodCollarEntity, backpackEntity,
                headEntity, hairEntity, shadesEntity,
                leftUpperArmEntity, leftForearmEntity, leftHandEntity,
                rightUpperArmEntity, rightForearmEntity, rightHandEntity,
                gunReceiverEntity, gunStockEntity, gunHandguardEntity, gunBarrelEntity, gunMagEntity, gunScopeEntity,
                leftThighEntity, leftShinEntity, leftBootEntity,
                rightThighEntity, rightShinEntity, rightBootEntity
            )
        )

        val rootTi = tm.getInstance(rootEntity)
        val pelvisTi = tm.getInstance(pelvisEntity)
        val torsoTi = tm.getInstance(torsoEntity)
        val headTi = tm.getInstance(headEntity)
        val lUpperTi = tm.getInstance(leftUpperArmEntity)
        val lForeTi = tm.getInstance(leftForearmEntity)
        val rUpperTi = tm.getInstance(rightUpperArmEntity)
        val rForeTi = tm.getInstance(rightForearmEntity)
        val rHandTi = tm.getInstance(rightHandEntity)
        val lThighTi = tm.getInstance(leftThighEntity)
        val lShinTi = tm.getInstance(leftShinEntity)
        val rThighTi = tm.getInstance(rightThighEntity)
        val rShinTi = tm.getInstance(rightShinEntity)

        tm.setParent(pelvisTi, rootTi)
        tm.setParent(tm.getInstance(torsoEntity), pelvisTi)
        tm.setParent(tm.getInstance(hoodCollarEntity), torsoTi)
        tm.setParent(tm.getInstance(backpackEntity), torsoTi)
        tm.setParent(headTi, torsoTi)
        tm.setParent(tm.getInstance(hairEntity), headTi)
        tm.setParent(tm.getInstance(shadesEntity), headTi)

        tm.setParent(lUpperTi, torsoTi)
        tm.setParent(lForeTi, lUpperTi)
        tm.setParent(tm.getInstance(leftHandEntity), lForeTi)

        tm.setParent(rUpperTi, torsoTi)
        tm.setParent(rForeTi, rUpperTi)
        tm.setParent(rHandTi, rForeTi)

        listOf(gunReceiverEntity, gunStockEntity, gunHandguardEntity, gunBarrelEntity, gunMagEntity, gunScopeEntity).forEach {
            tm.setParent(tm.getInstance(it), rHandTi)
        }

        tm.setParent(lThighTi, pelvisTi)
        tm.setParent(lShinTi, lThighTi)
        tm.setParent(tm.getInstance(leftBootEntity), lShinTi)

        tm.setParent(rThighTi, pelvisTi)
        tm.setParent(rShinTi, rThighTi)
        tm.setParent(tm.getInstance(rightBootEntity), rShinTi)

        tm.setParent(tm.getInstance(parachuteEntity), rootTi)
    }

    fun applyCharacterAppearance(characterId: CharacterId, isEnemyBot: Boolean = false) {
        val tex = materials.textures
        val style = if (isEnemyBot) {
            CharOutfit(
                torso = 0xFF7D242B, accent = 0xFFB71C1C, sleeve = 0xFF6B1E24,
                pants = 0xFF282428, boots = 0xFF1A1A1E, hair = 0xFF1E1918,
                skin = 0xFFF2D2BE, shades = 0xFFFF2E4C, sleeveless = false, hasShades = true
            )
        } else {
            when (characterId) {
                // Classic Adam/Alok style with sleeveless hoodie or signature Free Fire streetwear
                CharacterId.ALOK -> CharOutfit(
                    torso = 0xFF1E222B, accent = 0xFFFFB300, sleeve = 0xFF1E222B,
                    pants = 0xFF232830, boots = 0xFF222226, hair = 0xFF241A16,
                    skin = 0xFFF7DEC8, shades = 0xFFFFCA28, sleeveless = true, hasShades = true
                )
                CharacterId.CHRONO -> CharOutfit(
                    torso = 0xFF1B3048, accent = 0xFF00E5FF, sleeve = 0xFF19283D,
                    pants = 0xFF1E242E, boots = 0xFF1A222C, hair = 0xFF2B211B,
                    skin = 0xFFFFEBDC, shades = 0xFF00E5FF, sleeveless = false, hasShades = true
                )
                CharacterId.SKYLER -> CharOutfit(
                    torso = 0xFFEAEFF5, accent = 0xFFFF2E4C, sleeve = 0xFFE4E9F0,
                    pants = 0xFFECEFF4, boots = 0xFF24262B, hair = 0xFF18181E,
                    skin = 0xFFFFEBDC, shades = 0xFF212126, sleeveless = false, hasShades = true
                )
                CharacterId.WUKONG -> CharOutfit(
                    torso = 0xFF242428, accent = 0xFFD84315, sleeve = 0xFF242428,
                    pants = 0xFF2B2420, boots = 0xFF1E1B1A, hair = 0xFF3E2723,
                    skin = 0xFFF2D0B8, shades = 0xFFFF6D00, sleeveless = true, hasShades = false
                )
                CharacterId.KELLY -> CharOutfit(
                    torso = 0xFFFFB300, accent = 0xFFFFEA00, sleeve = 0xFFFFA000,
                    pants = 0xFF252A34, boots = 0xFFE8EAED, hair = 0xFF4A2E20,
                    skin = 0xFFFFECE0, shades = 0xFFFFEA00, sleeveless = false, hasShades = false
                )
                CharacterId.K_MASTER -> CharOutfit(
                    torso = 0xFF203459, accent = 0xFF448AFF, sleeve = 0xFF1B2B4A,
                    pants = 0xFF1C2330, boots = 0xFF181C24, hair = 0xFF3E2A20,
                    skin = 0xFFF5D8C2, shades = 0xFF448AFF, sleeveless = false, hasShades = true
                )
                CharacterId.MOCO -> CharOutfit(
                    torso = 0xFF311F52, accent = 0xFF00E5FF, sleeve = 0xFF291A45,
                    pants = 0xFF1D1A2B, boots = 0xFF191824, hair = 0xFF00B8D4,
                    skin = 0xFFFFEDE2, shades = 0xFFE040FB, sleeveless = true, hasShades = false
                )
                CharacterId.RAFAEL -> CharOutfit(
                    torso = 0xFF252A32, accent = 0xFFB0BEC5, sleeve = 0xFF21262E,
                    pants = 0xFF1A1E24, boots = 0xFF17191E, hair = 0xFF8D99AE,
                    skin = 0xFFF5D8C2, shades = 0xFFFF5252, sleeveless = false, hasShades = false
                )
                CharacterId.A124 -> CharOutfit(
                    torso = 0xFFECEFF4, accent = 0xFF00E5FF, sleeve = 0xFFE0E5EC,
                    pants = 0xFF263238, boots = 0xFFECEFF4, hair = 0xFF29B6F6,
                    skin = 0xFFFFF0E8, shades = 0xFF00E5FF, sleeveless = true, hasShades = false
                )
                CharacterId.ALVARO -> CharOutfit(
                    torso = 0xFF5D4037, accent = 0xFFFF6D00, sleeve = 0xFF4E342E,
                    pants = 0xFF363228, boots = 0xFF211C18, hair = 0xFFB7410E,
                    skin = 0xFFF4D4BC, shades = 0xFFFF9100, sleeveless = true, hasShades = false
                )
                CharacterId.NIKITA -> CharOutfit(
                    torso = 0xFF282E38, accent = 0xFFFFC107, sleeve = 0xFFECEFF4,
                    pants = 0xFF1E222A, boots = 0xFF1C1E22, hair = 0xFFD9A441,
                    skin = 0xFFFFECE0, shades = 0xFFFFCA28, sleeveless = true, hasShades = false
                )
                CharacterId.FORD -> CharOutfit(
                    torso = 0xFFCFD6E0, accent = 0xFF223C66, sleeve = 0xFFC5CCD6,
                    pants = 0xFF2D3326, boots = 0xFF1F1E1E, hair = 0xFF3E2A20,
                    skin = 0xFFF2CFB4, shades = 0xFF40C4FF, sleeveless = true, hasShades = false
                )
            }
        }

        setTint(torsoMat, style.torso)
        setTint(hoodAccentMat, style.accent)
        setTint(sleeveMat, if (style.sleeveless) style.torso else style.sleeve)
        forearmMat.setParameter("albedoMap", if (style.sleeveless) tex.skinMap else tex.hoodieMap, tex.repeatSampler)
        setTint(forearmMat, if (style.sleeveless) style.skin else style.sleeve)
        setTint(skinMat, style.skin)
        setTint(faceMat, style.skin)
        setTint(hairMat, style.hair)
        setTint(pantsMat, style.pants)
        setTint(bootsMat, style.boots)
        setLitColor(shadesMat, style.shades, 0.2f, 0.14f, 0.05f)
        showShades = style.hasShades
    }

    private data class CharOutfit(
        val torso: Long,
        val accent: Long,
        val sleeve: Long,
        val pants: Long,
        val boots: Long,
        val hair: Long,
        val skin: Long,
        val shades: Long,
        val sleeveless: Boolean,
        val hasShades: Boolean
    )

    private fun setTint(mat: MaterialInstance, argb: Long) {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        mat.setParameter("tint", r, g, b, 1f)
    }

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
        isLobbyShowcase: Boolean = false,
        scale: Float = 1f
    ) {
        val tm = engine.transformManager
        val divePitch = if (dropState == DropState.SKYDIVING) 66f else 0f
        tm.setTransform(
            tm.getInstance(rootEntity),
            FilamentTransformHelper.buildTRS(wx, wy, wz, rotYDeg = yawDeg, rotXDeg = divePitch, sx = scale, sy = scale, sz = scale)
        )

        val breath = sin(timeSec * 2.2f) * 0.010f
        val crouchDrop = if (isCrouching && dropState == DropState.GROUNDED) -0.28f else 0f
        val strideFreq = if (isSprinting) 13.5f else 9.5f
        val strideAmp = if (isSprinting) 42f else 30f
        val phase = if (isMoving && dropState == DropState.GROUNDED) timeSec * strideFreq else 0f
        val walkBob = if (isMoving && dropState == DropState.GROUNDED) kotlin.math.abs(sin(phase)) * 0.045f else 0f
        val recoil = if (isFiring) -11f else 0f

        // 1. Pelvis (origin height ~0.88m above ground)
        val pelvisY = 0.88f + crouchDrop + walkBob
        val torsoLean = when {
            isLobbyShowcase -> -2f
            isSprinting && isMoving -> 14f
            isCrouching -> 18f
            else -> 4f
        }
        tm.setTransform(
            tm.getInstance(pelvisEntity),
            FilamentTransformHelper.buildTRS(0f, pelvisY, 0f, rotYDeg = if (isLobbyShowcase) -8f else 0f)
        )

        // 2. Sculpted Torso, Hood Collar & Tactical Backpack
        tm.setTransform(
            tm.getInstance(torsoEntity),
            FilamentTransformHelper.buildTRS(0f, 0.04f + breath, 0f, rotXDeg = torsoLean, rotYDeg = if (isLobbyShowcase) 10f else 0f)
        )
        tm.setTransform(tm.getInstance(hoodCollarEntity), FilamentTransformHelper.buildTRS(0f, 0f, 0f))
        tm.setTransform(
            tm.getInstance(backpackEntity),
            FilamentTransformHelper.buildTRS(0f, 0.30f, -0.17f, sx = 0.28f, sy = 0.36f, sz = 0.13f)
        )

        // 3. Sculpted Head, Hair & Optional Sunglasses
        val headYaw = if (isLobbyShowcase) sin(timeSec * 0.8f) * 5f - 6f else 0f
        val headPitch = if (isLobbyShowcase) -3f else -torsoLean * 0.65f
        tm.setTransform(
            tm.getInstance(headEntity),
            FilamentTransformHelper.buildTRS(0f, 0.52f, 0f, rotYDeg = headYaw, rotXDeg = headPitch)
        )
        tm.setTransform(tm.getInstance(hairEntity), FilamentTransformHelper.buildTRS(0f, 0f, 0f))
        val shadeScale = if (showShades) 1f else 0.0001f
        tm.setTransform(
            tm.getInstance(shadesEntity),
            FilamentTransformHelper.buildTRS(0f, 0.21f, 0.105f, sx = 0.16f * shadeScale, sy = 0.042f * shadeScale, sz = 0.055f * shadeScale)
        )

        // 4. Articulated Left & Right Arms + Rifle
        if (isLobbyShowcase) {
            // Relaxed Free Fire Lobby / Profile Heroic Combat Pose
            val sway = sin(timeSec * 1.6f) * 2.5f
            tm.setTransform(
                tm.getInstance(leftUpperArmEntity),
                FilamentTransformHelper.buildTRS(-0.228f, 0.45f, 0f, rotXDeg = 10f + sway, rotZDeg = -14f)
            )
            tm.setTransform(
                tm.getInstance(leftForearmEntity),
                FilamentTransformHelper.buildTRS(0f, -0.27f, 0f, rotXDeg = -22f, rotZDeg = 8f)
            )
            tm.setTransform(tm.getInstance(leftHandEntity), FilamentTransformHelper.buildTRS(0f, -0.25f, 0f))

            // Right arm holding detailed rifle casually downward-forward
            tm.setTransform(
                tm.getInstance(rightUpperArmEntity),
                FilamentTransformHelper.buildTRS(0.228f, 0.45f, 0f, rotXDeg = -18f + sway, rotZDeg = 13f)
            )
            tm.setTransform(
                tm.getInstance(rightForearmEntity),
                FilamentTransformHelper.buildTRS(0f, -0.27f, 0f, rotXDeg = -34f, rotYDeg = -16f)
            )
            tm.setTransform(
                tm.getInstance(rightHandEntity),
                FilamentTransformHelper.buildTRS(0f, -0.25f, 0f, rotXDeg = 18f, rotZDeg = 12f)
            )
        } else if (dropState == DropState.PARACHUTE) {
            tm.setTransform(tm.getInstance(leftUpperArmEntity), FilamentTransformHelper.buildTRS(-0.228f, 0.45f, 0f, rotXDeg = -130f, rotZDeg = -18f))
            tm.setTransform(tm.getInstance(leftForearmEntity), FilamentTransformHelper.buildTRS(0f, -0.27f, 0f, rotXDeg = -25f))
            tm.setTransform(tm.getInstance(leftHandEntity), FilamentTransformHelper.buildTRS(0f, -0.25f, 0f))
            tm.setTransform(tm.getInstance(rightUpperArmEntity), FilamentTransformHelper.buildTRS(0.228f, 0.45f, 0f, rotXDeg = -130f, rotZDeg = 18f))
            tm.setTransform(tm.getInstance(rightForearmEntity), FilamentTransformHelper.buildTRS(0f, -0.27f, 0f, rotXDeg = -25f))
            tm.setTransform(tm.getInstance(rightHandEntity), FilamentTransformHelper.buildTRS(0f, -0.25f, 0f))
        } else {
            // Combat rifle aiming pose
            tm.setTransform(
                tm.getInstance(leftUpperArmEntity),
                FilamentTransformHelper.buildTRS(-0.228f, 0.45f, 0f, rotXDeg = -52f + recoil * 0.4f, rotYDeg = 28f, rotZDeg = -10f)
            )
            tm.setTransform(
                tm.getInstance(leftForearmEntity),
                FilamentTransformHelper.buildTRS(0f, -0.27f, 0f, rotXDeg = -46f, rotYDeg = 16f)
            )
            tm.setTransform(tm.getInstance(leftHandEntity), FilamentTransformHelper.buildTRS(0f, -0.25f, 0f))

            tm.setTransform(
                tm.getInstance(rightUpperArmEntity),
                FilamentTransformHelper.buildTRS(0.228f, 0.45f, 0f, rotXDeg = -46f + recoil, rotYDeg = -12f, rotZDeg = 8f)
            )
            tm.setTransform(
                tm.getInstance(rightForearmEntity),
                FilamentTransformHelper.buildTRS(0f, -0.27f, 0f, rotXDeg = -48f, rotYDeg = -14f)
            )
            tm.setTransform(
                tm.getInstance(rightHandEntity),
                FilamentTransformHelper.buildTRS(0f, -0.25f, 0f, rotXDeg = 86f + recoil * 0.4f, rotYDeg = 14f)
            )
        }

        // Detailed Assault Rifle (attached cleanly to rightHandEntity)
        val gunScale = if (dropState == DropState.PARACHUTE || dropState == DropState.SKYDIVING) 0.0001f else 1f
        tm.setTransform(tm.getInstance(gunReceiverEntity), FilamentTransformHelper.buildTRS(0f, -0.05f, 0.08f, sx = 0.055f * gunScale, sy = 0.095f * gunScale, sz = 0.30f * gunScale))
        tm.setTransform(tm.getInstance(gunStockEntity), FilamentTransformHelper.buildTRS(0f, -0.06f, -0.14f, sx = 0.045f * gunScale, sy = 0.11f * gunScale, sz = 0.18f * gunScale))
        tm.setTransform(tm.getInstance(gunHandguardEntity), FilamentTransformHelper.buildTRS(0f, -0.04f, 0.30f, sx = 0.048f * gunScale, sy = 0.070f * gunScale, sz = 0.22f * gunScale))
        tm.setTransform(tm.getInstance(gunBarrelEntity), FilamentTransformHelper.buildTRS(0f, -0.03f, 0.49f, rotXDeg = 90f, sx = 0.025f * gunScale, sy = 0.24f * gunScale, sz = 0.025f * gunScale))
        tm.setTransform(tm.getInstance(gunMagEntity), FilamentTransformHelper.buildTRS(0f, -0.15f, 0.14f, rotXDeg = 16f, sx = 0.036f * gunScale, sy = 0.15f * gunScale, sz = 0.065f * gunScale))
        tm.setTransform(tm.getInstance(gunScopeEntity), FilamentTransformHelper.buildTRS(0f, 0.02f, 0.09f, sx = 0.038f * gunScale, sy = 0.045f * gunScale, sz = 0.09f * gunScale))

        // 5. Articulated Legs (Thigh -> Knee -> Shin -> Combat Boot)
        val lHipX: Float
        val lKneeX: Float
        val rHipX: Float
        val rKneeX: Float
        val stanceSpreadZ = if (isLobbyShowcase) 5.5f else 3f

        if (isLobbyShowcase) {
            lHipX = -6f
            lKneeX = 8f
            rHipX = 6f
            rKneeX = 4f
        } else if (isCrouching && dropState == DropState.GROUNDED) {
            val s = if (isMoving) sin(phase) * 18f else 0f
            lHipX = -58f + s
            lKneeX = 94f
            rHipX = -52f - s
            rKneeX = 88f
        } else if (isMoving && dropState == DropState.GROUNDED) {
            val swing = sin(phase) * strideAmp
            lHipX = swing
            lKneeX = max(4f, -swing * 1.15f + 18f)
            rHipX = -swing
            rKneeX = max(4f, swing * 1.15f + 18f)
        } else {
            lHipX = -4f
            lKneeX = 6f
            rHipX = 4f
            rKneeX = 4f
        }

        tm.setTransform(tm.getInstance(leftThighEntity), FilamentTransformHelper.buildTRS(-0.098f, -0.02f, 0f, rotXDeg = lHipX, rotZDeg = -stanceSpreadZ))
        tm.setTransform(tm.getInstance(leftShinEntity), FilamentTransformHelper.buildTRS(0f, -0.42f, 0f, rotXDeg = lKneeX))
        tm.setTransform(tm.getInstance(leftBootEntity), FilamentTransformHelper.buildTRS(0f, -0.39f, 0f, rotXDeg = -(lHipX + lKneeX) * 0.45f))

        tm.setTransform(tm.getInstance(rightThighEntity), FilamentTransformHelper.buildTRS(0.098f, -0.02f, 0f, rotXDeg = rHipX, rotZDeg = stanceSpreadZ))
        tm.setTransform(tm.getInstance(rightShinEntity), FilamentTransformHelper.buildTRS(0f, -0.42f, 0f, rotXDeg = rKneeX))
        tm.setTransform(tm.getInstance(rightBootEntity), FilamentTransformHelper.buildTRS(0f, -0.39f, 0f, rotXDeg = -(rHipX + rKneeX) * 0.45f))

        // 6. 3D Parachute Canopy
        if (chuteVisible) {
            tm.setTransform(
                tm.getInstance(parachuteEntity),
                FilamentTransformHelper.buildTRS(0f, 2.85f, 0f, sx = 2.8f, sy = 1.45f, sz = 2.8f)
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
