package com.example.filament

import com.google.android.filament.Box
import com.google.android.filament.Engine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class FilamentHumanMeshes(private val engine: Engine) {
    val headMesh: FilamentMesh = buildSculptedHead()
    val hairMesh: FilamentMesh = buildSculptedHair()
    val torsoMesh: FilamentMesh = buildSculptedTorso()
    val hoodCollarMesh: FilamentMesh = buildSculptedHoodCollar()
    val pelvisMesh: FilamentMesh = buildSculptedPelvis()
    val upperArmMesh: FilamentMesh = buildTaperedLimb(
        length = 0.28f,
        rings = listOf(
            RingSpec(0.00f, 0.068f, 0.068f),
            RingSpec(-0.07f, 0.064f, 0.066f),
            RingSpec(-0.17f, 0.056f, 0.058f),
            RingSpec(-0.28f, 0.044f, 0.044f)
        )
    )
    val forearmMesh: FilamentMesh = buildTaperedLimb(
        length = 0.26f,
        rings = listOf(
            RingSpec(0.00f, 0.046f, 0.046f),
            RingSpec(-0.08f, 0.050f, 0.050f),
            RingSpec(-0.18f, 0.040f, 0.038f),
            RingSpec(-0.26f, 0.032f, 0.030f)
        )
    )
    val handMesh: FilamentMesh = buildSculptedHand()
    val thighMesh: FilamentMesh = buildTaperedLimb(
        length = 0.43f,
        rings = listOf(
            RingSpec(0.00f, 0.095f, 0.102f),
            RingSpec(-0.14f, 0.092f, 0.098f),
            RingSpec(-0.29f, 0.082f, 0.086f),
            RingSpec(-0.43f, 0.068f, 0.070f)
        )
    )
    val shinMesh: FilamentMesh = buildTaperedLimb(
        length = 0.41f,
        rings = listOf(
            RingSpec(0.00f, 0.068f, 0.070f),
            RingSpec(-0.12f, 0.073f, 0.078f),
            RingSpec(-0.28f, 0.060f, 0.064f),
            RingSpec(-0.41f, 0.052f, 0.055f)
        )
    )
    val bootMesh: FilamentMesh = buildSculptedBoot()

    fun destroy() {
        headMesh.destroy(engine)
        hairMesh.destroy(engine)
        torsoMesh.destroy(engine)
        hoodCollarMesh.destroy(engine)
        pelvisMesh.destroy(engine)
        upperArmMesh.destroy(engine)
        forearmMesh.destroy(engine)
        handMesh.destroy(engine)
        thighMesh.destroy(engine)
        shinMesh.destroy(engine)
        bootMesh.destroy(engine)
    }

    private data class RingSpec(
        val y: Float,
        val rx: Float,
        val rz: Float,
        val offsetZ: Float = 0f,
        val frontBulge: Float = 0f,
        val noseSpike: Float = 0f
    )

    private fun buildSculptedHead(): FilamentMesh {
        // y=0 is base of neck, y=0.33 is crown top; u=0.5 faces +Z
        val rings = listOf(
            RingSpec(-0.04f, 0.058f, 0.060f, offsetZ = -0.008f), // Neck base
            RingSpec(0.03f, 0.056f, 0.058f, offsetZ = -0.004f),  // Upper neck
            RingSpec(0.075f, 0.072f, 0.086f, offsetZ = 0.018f, frontBulge = 0.012f), // Chin / jaw
            RingSpec(0.135f, 0.088f, 0.098f, offsetZ = 0.010f, frontBulge = 0.010f), // Mouth / cheeks
            RingSpec(0.180f, 0.094f, 0.104f, offsetZ = 0.006f, noseSpike = 0.022f),  // Nose tip & cheekbones
            RingSpec(0.225f, 0.095f, 0.106f, offsetZ = 0.004f, frontBulge = 0.006f), // Eye sockets & brow
            RingSpec(0.275f, 0.090f, 0.102f, offsetZ = 0.0f),                        // Forehead
            RingSpec(0.320f, 0.066f, 0.076f, offsetZ = -0.006f),                     // Upper cranium
            RingSpec(0.345f, 0.014f, 0.014f, offsetZ = -0.008f)                      // Top cap
        )
        return buildLoftedRings(rings, slices = 20, vReversed = true)
    }

    private fun buildSculptedHair(): FilamentMesh {
        // Swept-back pompadour volume sitting on cranium
        val rings = listOf(
            RingSpec(0.185f, 0.096f, 0.104f, offsetZ = -0.012f),
            RingSpec(0.235f, 0.102f, 0.110f, offsetZ = -0.008f),
            RingSpec(0.285f, 0.100f, 0.114f, offsetZ = 0.006f, frontBulge = 0.024f),
            RingSpec(0.335f, 0.082f, 0.104f, offsetZ = 0.012f, frontBulge = 0.028f),
            RingSpec(0.372f, 0.036f, 0.056f, offsetZ = 0.016f),
            RingSpec(0.388f, 0.006f, 0.010f, offsetZ = 0.014f)
        )
        return buildLoftedRings(rings, slices = 18, vReversed = true)
    }

    private fun buildSculptedTorso(): FilamentMesh {
        // y=0 at waist, y=0.52 at collar; u=0.5 faces +Z chest
        val rings = listOf(
            RingSpec(0.00f, 0.160f, 0.104f),                                  // Waist hem
            RingSpec(0.12f, 0.156f, 0.106f, frontBulge = 0.006f),             // Abs / lower ribs
            RingSpec(0.25f, 0.182f, 0.122f, frontBulge = 0.014f),             // Ribcage / lower chest
            RingSpec(0.38f, 0.214f, 0.132f, frontBulge = 0.022f),             // Upper pecs / lats
            RingSpec(0.47f, 0.198f, 0.114f, offsetZ = -0.004f),               // Shoulder slope
            RingSpec(0.52f, 0.086f, 0.082f, offsetZ = -0.004f)                // Collar opening
        )
        return buildLoftedRings(rings, slices = 22, vReversed = true)
    }

    private fun buildSculptedHoodCollar(): FilamentMesh {
        // Draped hood behind shoulders & collar
        val rings = listOf(
            RingSpec(0.41f, 0.145f, 0.115f, offsetZ = -0.065f),
            RingSpec(0.48f, 0.135f, 0.110f, offsetZ = -0.055f),
            RingSpec(0.54f, 0.105f, 0.095f, offsetZ = -0.032f)
        )
        return buildLoftedRings(rings, slices = 16, vReversed = true)
    }

    private fun buildSculptedPelvis(): FilamentMesh {
        val rings = listOf(
            RingSpec(-0.08f, 0.168f, 0.114f),
            RingSpec(0.00f, 0.172f, 0.118f),
            RingSpec(0.08f, 0.166f, 0.110f)
        )
        return buildLoftedRings(rings, slices = 18, vReversed = true)
    }

    private fun buildTaperedLimb(length: Float, rings: List<RingSpec>): FilamentMesh {
        // Note: rings go downward from y=0 to -length
        return buildLoftedRings(rings, slices = 14, vReversed = false)
    }

    private fun buildSculptedHand(): FilamentMesh {
        val rings = listOf(
            RingSpec(0.00f, 0.032f, 0.024f),
            RingSpec(-0.035f, 0.038f, 0.026f, frontBulge = 0.006f),
            RingSpec(-0.075f, 0.034f, 0.020f),
            RingSpec(-0.102f, 0.018f, 0.012f)
        )
        return buildLoftedRings(rings, slices = 10, vReversed = false)
    }

    private fun buildSculptedBoot(): FilamentMesh {
        // Boot extending downward from ankle (y=0.06) to sole (y=-0.06), protruding forward along +Z
        val rings = listOf(
            RingSpec(0.08f, 0.055f, 0.058f, offsetZ = 0.00f),
            RingSpec(0.02f, 0.058f, 0.078f, offsetZ = 0.028f),
            RingSpec(-0.025f, 0.062f, 0.105f, offsetZ = 0.048f),
            RingSpec(-0.055f, 0.064f, 0.112f, offsetZ = 0.050f),
            RingSpec(-0.065f, 0.012f, 0.020f, offsetZ = 0.050f)
        )
        return buildLoftedRings(rings, slices = 14, vReversed = false)
    }

    private fun buildLoftedRings(
        rings: List<RingSpec>,
        slices: Int,
        vReversed: Boolean
    ): FilamentMesh {
        val ringCount = rings.size
        val stride = slices + 1
        val positions = ArrayList<Float>(ringCount * stride * 3)
        val uvs = ArrayList<Float>(ringCount * stride * 2)

        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

        for (rIdx in 0 until ringCount) {
            val spec = rings[rIdx]
            val vRaw = rIdx.toFloat() / (ringCount - 1).coerceAtLeast(1)
            val v = if (vReversed) 1f - vRaw else vRaw

            for (s in 0..slices) {
                val u = s.toFloat() / slices
                // u = 0.5 -> theta = 0 -> front (+Z)! u = 0 or 1 -> theta = -PI / +PI -> back (-Z)
                val theta = (u - 0.5f) * (2f * PI.toFloat())
                val sinT = sin(theta)
                val cosT = cos(theta)

                val frontFactor = max(0f, cosT)
                val noseFactor = max(0f, (cosT - 0.92f) / 0.08f)
                val x = sinT * spec.rx
                val z = cosT * spec.rz + spec.offsetZ + frontFactor * spec.frontBulge + noseFactor * spec.noseSpike
                val y = spec.y

                positions.add(x); positions.add(y); positions.add(z)
                uvs.add(u); uvs.add(v)

                minX = min(minX, x); maxX = max(maxX, x)
                minY = min(minY, y); maxY = max(maxY, y)
                minZ = min(minZ, z); maxZ = max(maxZ, z)
            }
        }

        val indices = ArrayList<Short>((ringCount - 1) * slices * 6)
        for (r in 0 until ringCount - 1) {
            for (s in 0 until slices) {
                val i0 = (r * stride + s).toShort()
                val i1 = ((r + 1) * stride + s).toShort()
                val i2 = (r * stride + s + 1).toShort()
                val i3 = ((r + 1) * stride + s + 1).toShort()
                indices.add(i0); indices.add(i2); indices.add(i1)
                indices.add(i2); indices.add(i3); indices.add(i1)
            }
        }

        // Compute smooth area-weighted vertex normals
        val vertexCount = positions.size / 3
        val normals = FloatArray(vertexCount * 3)
        for (i in 0 until indices.size step 3) {
            val ia = indices[i].toInt()
            val ib = indices[i + 1].toInt()
            val ic = indices[i + 2].toInt()
            val ax = positions[ia * 3]; val ay = positions[ia * 3 + 1]; val az = positions[ia * 3 + 2]
            val bx = positions[ib * 3]; val by = positions[ib * 3 + 1]; val bz = positions[ib * 3 + 2]
            val cx = positions[ic * 3]; val cy = positions[ic * 3 + 1]; val cz = positions[ic * 3 + 2]

            val ux = bx - ax; val uy = by - ay; val uz = bz - az
            val vx = cx - ax; val vy = cy - ay; val vz = cz - az
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx

            normals[ia * 3] += nx; normals[ia * 3 + 1] += ny; normals[ia * 3 + 2] += nz
            normals[ib * 3] += nx; normals[ib * 3 + 1] += ny; normals[ib * 3 + 2] += nz
            normals[ic * 3] += nx; normals[ic * 3 + 1] += ny; normals[ic * 3 + 2] += nz
        }

        // Seam-weld normals at u=0 and u=1 so back of character has seamless lighting
        for (r in 0 until ringCount) {
            val first = (r * stride) * 3
            val last = (r * stride + slices) * 3
            val sx = normals[first] + normals[last]
            val sy = normals[first + 1] + normals[last + 1]
            val sz = normals[first + 2] + normals[last + 2]
            normals[first] = sx; normals[first + 1] = sy; normals[first + 2] = sz
            normals[last] = sx; normals[last + 1] = sy; normals[last + 2] = sz
        }

        val tangents = ArrayList<Float>(vertexCount * 4)
        for (vIdx in 0 until vertexCount) {
            val nx = normals[vIdx * 3]
            val ny = normals[vIdx * 3 + 1]
            val nz = normals[vIdx * 3 + 2]
            val len = hypot(hypot(nx, ny), nz)
            val q = if (len > 1e-5f) {
                FilamentMeshUtil.normalToQuaternion(nx / len, ny / len, nz / len)
            } else {
                FilamentMeshUtil.normalToQuaternion(0f, 1f, 0f)
            }
            tangents.add(q[0]); tangents.add(q[1]); tangents.add(q[2]); tangents.add(q[3])
        }

        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val hx = max(0.05f, (maxX - minX) * 0.5f)
        val hy = max(0.05f, (maxY - minY) * 0.5f)
        val hz = max(0.05f, (maxZ - minZ) * 0.5f)
        return FilamentMeshUtil.createMesh(engine, positions, tangents, uvs, indices, Box(cx, cy, cz, hx, hy, hz))
    }
}
