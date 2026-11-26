package com.example.filament

import android.opengl.Matrix
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

object FilamentInit {
    @Volatile
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                Filament.init()
                Utils.init()
                initialized = true
            }
        }
    }
}

data class FilamentMesh(
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer,
    val indexCount: Int,
    val bounds: Box
) {
    fun destroy(engine: Engine) {
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyIndexBuffer(indexBuffer)
    }
}

class FilamentMaterialBank(private val engine: Engine) {
    val litMaterial: Material
    val transparentMaterial: Material
    private val instances = mutableListOf<MaterialInstance>()

    init {
        MaterialBuilder.init()
        try {
            val litPkg = MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("FireZoneLit")
                .shading(MaterialBuilder.Shading.LIT)
                .blending(MaterialBuilder.BlendingMode.OPAQUE)
                .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "baseColor")
                .uniformParameter(MaterialBuilder.UniformType.FLOAT, "metallic")
                .uniformParameter(MaterialBuilder.UniformType.FLOAT, "roughness")
                .uniformParameter(MaterialBuilder.UniformType.FLOAT3, "emissive")
                .material(
                    """
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor = materialParams.baseColor;
                        material.metallic = materialParams.metallic;
                        material.roughness = materialParams.roughness;
                        material.emissive = vec4(materialParams.emissive, 0.0);
                    }
                    """.trimIndent()
                )
                .optimization(MaterialBuilder.Optimization.NONE)
                .build()
            require(litPkg.isValid) { "Failed to compile FireZoneLit material" }
            litMaterial = Material.Builder()
                .payload(litPkg.buffer, litPkg.buffer.remaining())
                .build(engine)

            val alphaPkg = MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("FireZoneAlpha")
                .shading(MaterialBuilder.Shading.UNLIT)
                .blending(MaterialBuilder.BlendingMode.TRANSPARENT)
                .doubleSided(true)
                .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "baseColor")
                .material(
                    """
                    void material(inout MaterialInputs material) {
                        prepareMaterial(material);
                        material.baseColor = materialParams.baseColor;
                    }
                    """.trimIndent()
                )
                .optimization(MaterialBuilder.Optimization.NONE)
                .build()
            require(alphaPkg.isValid) { "Failed to compile FireZoneAlpha material" }
            transparentMaterial = Material.Builder()
                .payload(alphaPkg.buffer, alphaPkg.buffer.remaining())
                .build(engine)
        } finally {
            MaterialBuilder.shutdown()
        }
    }

    fun lit(
        argb: Long,
        metallic: Float = 0.12f,
        roughness: Float = 0.62f,
        emissiveR: Float = 0f,
        emissiveG: Float = 0f,
        emissiveB: Float = 0f
    ): MaterialInstance {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return litMaterial.createInstance().apply {
            setParameter("baseColor", r, g, b, 1f)
            setParameter("metallic", metallic)
            setParameter("roughness", roughness)
            setParameter("emissive", emissiveR, emissiveG, emissiveB)
            instances.add(this)
        }
    }

    fun transparent(r: Float, g: Float, b: Float, a: Float): MaterialInstance {
        return transparentMaterial.createInstance().apply {
            setParameter("baseColor", r, g, b, a)
            instances.add(this)
        }
    }

    fun destroy() {
        instances.forEach { engine.destroyMaterialInstance(it) }
        instances.clear()
        engine.destroyMaterial(litMaterial)
        engine.destroyMaterial(transparentMaterial)
    }
}

class FilamentMeshBank(private val engine: Engine) {
    val box: FilamentMesh = buildUnitBox()
    val cylinder: FilamentMesh = buildUnitCylinder(14)
    val sphere: FilamentMesh = buildUnitSphere(14, 10)
    val dome: FilamentMesh = buildHemisphereDome(14, 7)

    fun destroy() {
        box.destroy(engine)
        cylinder.destroy(engine)
        sphere.destroy(engine)
        dome.destroy(engine)
    }

    private fun buildUnitBox(): FilamentMesh {
        val positions = ArrayList<Float>(24 * 3)
        val tangents = ArrayList<Float>(24 * 4)
        val indices = ArrayList<Short>(36)

        fun addFace(
            p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray,
            nx: Float, ny: Float, nz: Float
        ) {
            val base = (positions.size / 3).toShort()
            val q = normalToQuaternion(nx, ny, nz)
            listOf(p0, p1, p2, p3).forEach { p ->
                positions.add(p[0]); positions.add(p[1]); positions.add(p[2])
                tangents.add(q[0]); tangents.add(q[1]); tangents.add(q[2]); tangents.add(q[3])
            }
            indices.add(base); indices.add((base + 1).toShort()); indices.add((base + 2).toShort())
            indices.add(base); indices.add((base + 2).toShort()); indices.add((base + 3).toShort())
        }

        val h = 0.5f
        // +Z
        addFace(floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h), 0f, 0f, 1f)
        // -Z
        addFace(floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h), 0f, 0f, -1f)
        // +X
        addFace(floatArrayOf(h, -h, h), floatArrayOf(h, -h, -h), floatArrayOf(h, h, -h), floatArrayOf(h, h, h), 1f, 0f, 0f)
        // -X
        addFace(floatArrayOf(-h, -h, -h), floatArrayOf(-h, -h, h), floatArrayOf(-h, h, h), floatArrayOf(-h, h, -h), -1f, 0f, 0f)
        // +Y
        addFace(floatArrayOf(-h, h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), floatArrayOf(-h, h, -h), 0f, 1f, 0f)
        // -Y
        addFace(floatArrayOf(-h, -h, -h), floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(-h, -h, h), 0f, -1f, 0f)

        return createMesh(positions, tangents, indices, Box(0f, 0f, 0f, 0.5f, 0.5f, 0.5f))
    }

    private fun buildUnitCylinder(segments: Int): FilamentMesh {
        val positions = mutableListOf<Float>()
        val tangents = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        val r = 0.5f
        val h = 0.5f

        // Side wall vertices
        for (i in 0..segments) {
            val a = (i.toFloat() / segments) * (2f * PI.toFloat())
            val nx = cos(a)
            val nz = sin(a)
            val q = normalToQuaternion(nx, 0f, nz)
            // Bottom
            positions.add(nx * r); positions.add(-h); positions.add(nz * r)
            tangents.add(q[0]); tangents.add(q[1]); tangents.add(q[2]); tangents.add(q[3])
            // Top
            positions.add(nx * r); positions.add(h); positions.add(nz * r)
            tangents.add(q[0]); tangents.add(q[1]); tangents.add(q[2]); tangents.add(q[3])
        }
        for (i in 0 until segments) {
            val i0 = (i * 2).toShort()
            val i1 = (i * 2 + 1).toShort()
            val i2 = (i * 2 + 2).toShort()
            val i3 = (i * 2 + 3).toShort()
            indices.add(i0); indices.add(i2); indices.add(i1)
            indices.add(i1); indices.add(i2); indices.add(i3)
        }

        // Top cap
        val topCenter = (positions.size / 3).toShort()
        val qUp = normalToQuaternion(0f, 1f, 0f)
        positions.add(0f); positions.add(h); positions.add(0f)
        tangents.add(qUp[0]); tangents.add(qUp[1]); tangents.add(qUp[2]); tangents.add(qUp[3])
        val topStart = (positions.size / 3)
        for (i in 0..segments) {
            val a = (i.toFloat() / segments) * (2f * PI.toFloat())
            positions.add(cos(a) * r); positions.add(h); positions.add(sin(a) * r)
            tangents.add(qUp[0]); tangents.add(qUp[1]); tangents.add(qUp[2]); tangents.add(qUp[3])
        }
        for (i in 0 until segments) {
            indices.add(topCenter)
            indices.add((topStart + i + 1).toShort())
            indices.add((topStart + i).toShort())
        }

        // Bottom cap
        val botCenter = (positions.size / 3).toShort()
        val qDown = normalToQuaternion(0f, -1f, 0f)
        positions.add(0f); positions.add(-h); positions.add(0f)
        tangents.add(qDown[0]); tangents.add(qDown[1]); tangents.add(qDown[2]); tangents.add(qDown[3])
        val botStart = (positions.size / 3)
        for (i in 0..segments) {
            val a = (i.toFloat() / segments) * (2f * PI.toFloat())
            positions.add(cos(a) * r); positions.add(-h); positions.add(sin(a) * r)
            tangents.add(qDown[0]); tangents.add(qDown[1]); tangents.add(qDown[2]); tangents.add(qDown[3])
        }
        for (i in 0 until segments) {
            indices.add(botCenter)
            indices.add((botStart + i).toShort())
            indices.add((botStart + i + 1).toShort())
        }

        return createMesh(positions, tangents, indices, Box(0f, 0f, 0f, 0.5f, 0.5f, 0.5f))
    }

    private fun buildUnitSphere(slices: Int, stacks: Int): FilamentMesh {
        val positions = mutableListOf<Float>()
        val tangents = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        val r = 0.5f

        for (st in 0..stacks) {
            val v = st.toFloat() / stacks
            val phi = v * PI.toFloat()
            val y = cos(phi)
            val sinPhi = sin(phi)
            for (sl in 0..slices) {
                val u = sl.toFloat() / slices
                val theta = u * 2f * PI.toFloat()
                val nx = sinPhi * cos(theta)
                val nz = sinPhi * sin(theta)
                positions.add(nx * r); positions.add(y * r); positions.add(nz * r)
                val q = normalToQuaternion(nx, y, nz)
                tangents.add(q[0]); tangents.add(q[1]); tangents.add(q[2]); tangents.add(q[3])
            }
        }
        val stride = slices + 1
        for (st in 0 until stacks) {
            for (sl in 0 until slices) {
                val i0 = (st * stride + sl).toShort()
                val i1 = ((st + 1) * stride + sl).toShort()
                val i2 = (st * stride + sl + 1).toShort()
                val i3 = ((st + 1) * stride + sl + 1).toShort()
                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i2); indices.add(i1); indices.add(i3)
            }
        }
        return createMesh(positions, tangents, indices, Box(0f, 0f, 0f, 0.5f, 0.5f, 0.5f))
    }

    private fun buildHemisphereDome(slices: Int, stacks: Int): FilamentMesh {
        val positions = mutableListOf<Float>()
        val tangents = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        val r = 0.5f

        for (st in 0..stacks) {
            val v = st.toFloat() / stacks
            val phi = v * (PI.toFloat() * 0.5f)
            val y = cos(phi)
            val sinPhi = sin(phi)
            for (sl in 0..slices) {
                val u = sl.toFloat() / slices
                val theta = u * 2f * PI.toFloat()
                val nx = sinPhi * cos(theta)
                val nz = sinPhi * sin(theta)
                positions.add(nx * r); positions.add(y * r); positions.add(nz * r)
                val q = normalToQuaternion(nx, y, nz)
                tangents.add(q[0]); tangents.add(q[1]); tangents.add(q[2]); tangents.add(q[3])
            }
        }
        val stride = slices + 1
        for (st in 0 until stacks) {
            for (sl in 0 until slices) {
                val i0 = (st * stride + sl).toShort()
                val i1 = ((st + 1) * stride + sl).toShort()
                val i2 = (st * stride + sl + 1).toShort()
                val i3 = ((st + 1) * stride + sl + 1).toShort()
                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i2); indices.add(i1); indices.add(i3)
            }
        }
        return createMesh(positions, tangents, indices, Box(0f, 0.25f, 0f, 0.5f, 0.25f, 0.5f))
    }

    private fun createMesh(
        positions: List<Float>,
        tangents: List<Float>,
        indices: List<Short>,
        bounds: Box
    ): FilamentMesh {
        val vertexCount = positions.size / 3
        val posBuf = ByteBuffer.allocateDirect(positions.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                positions.forEach { put(it) }
                rewind()
            }
        val tanBuf = ByteBuffer.allocateDirect(tangents.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                tangents.forEach { put(it) }
                rewind()
            }
        val idxBuf = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                indices.forEach { put(it) }
                rewind()
            }

        val vb = VertexBuffer.Builder()
            .bufferCount(2)
            .vertexCount(vertexCount)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .attribute(VertexBuffer.VertexAttribute.TANGENTS, 1, VertexBuffer.AttributeType.FLOAT4, 0, 16)
            .build(engine)
        vb.setBufferAt(engine, 0, posBuf)
        vb.setBufferAt(engine, 1, tanBuf)

        val ib = IndexBuffer.Builder()
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .indexCount(indices.size)
            .build(engine)
        ib.setBuffer(engine, idxBuf)

        return FilamentMesh(vb, ib, indices.size, bounds)
    }

    private fun normalToQuaternion(nx: Float, ny: Float, nz: Float): FloatArray {
        val len = hypot(hypot(nx, ny), nz).coerceAtLeast(1e-5f)
        val n0 = nx / len
        val n1 = ny / len
        val n2 = nz / len
        // Pick reference vector not parallel to N
        val rx = if (kotlin.math.abs(n1) < 0.9f) 0f else 1f
        val ry = if (kotlin.math.abs(n1) < 0.9f) 1f else 0f
        val rz = 0f
        // T = normalize(R x N)
        var tx = ry * n2 - rz * n1
        var ty = rz * n0 - rx * n2
        var tz = rx * n1 - ry * n0
        val tLen = hypot(hypot(tx, ty), tz).coerceAtLeast(1e-5f)
        tx /= tLen; ty /= tLen; tz /= tLen
        // B = N x T
        val bx = n1 * tz - n2 * ty
        val by = n2 * tx - n0 * tz
        val bz = n0 * ty - n1 * tx

        // Rotation matrix [T, B, N] columns -> quaternion
        val m00 = tx; val m01 = bx; val m02 = n0
        val m10 = ty; val m11 = by; val m12 = n1
        val m20 = tz; val m21 = bz; val m22 = n2
        val trace = m00 + m11 + m22
        val qw: Float
        val qx: Float
        val qy: Float
        val qz: Float
        if (trace > 0f) {
            val s = 0.5f / sqrt(trace + 1f)
            qw = 0.25f / s
            qx = (m21 - m12) * s
            qy = (m02 - m20) * s
            qz = (m10 - m01) * s
        } else if (m00 > m11 && m00 > m22) {
            val s = 2f * sqrt(1f + m00 - m11 - m22)
            qw = (m21 - m12) / s
            qx = 0.25f * s
            qy = (m01 + m10) / s
            qz = (m02 + m20) / s
        } else if (m11 > m22) {
            val s = 2f * sqrt(1f + m11 - m00 - m22)
            qw = (m02 - m20) / s
            qx = (m01 + m10) / s
            qy = 0.25f * s
            qz = (m12 + m21) / s
        } else {
            val s = 2f * sqrt(1f + m22 - m00 - m11)
            qw = (m10 - m01) / s
            qx = (m02 + m20) / s
            qy = (m12 + m21) / s
            qz = 0.25f * s
        }
        return floatArrayOf(qx, qy, qz, qw)
    }
}

object FilamentTransformHelper {
    fun buildTRS(
        tx: Float, ty: Float, tz: Float,
        rotYDeg: Float = 0f,
        rotXDeg: Float = 0f,
        rotZDeg: Float = 0f,
        sx: Float = 1f, sy: Float = 1f, sz: Float = 1f
    ): FloatArray {
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        Matrix.translateM(m, 0, tx, ty, tz)
        if (rotYDeg != 0f) Matrix.rotateM(m, 0, rotYDeg, 0f, 1f, 0f)
        if (rotXDeg != 0f) Matrix.rotateM(m, 0, rotXDeg, 1f, 0f, 0f)
        if (rotZDeg != 0f) Matrix.rotateM(m, 0, rotZDeg, 0f, 0f, 1f)
        Matrix.scaleM(m, 0, sx, sy, sz)
        return m
    }

    fun spawnMeshEntity(
        engine: Engine,
        mesh: FilamentMesh,
        materialInstance: MaterialInstance,
        castShadows: Boolean = true,
        receiveShadows: Boolean = true
    ): Int {
        val entity = com.google.android.filament.EntityManager.get().create()
        engine.transformManager.create(entity)
        RenderableManager.Builder(1)
            .boundingBox(mesh.bounds)
            .material(0, materialInstance)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, mesh.vertexBuffer, mesh.indexBuffer, 0, mesh.indexCount)
            .castShadows(castShadows)
            .receiveShadows(receiveShadows)
            .build(engine, entity)
        return entity
    }
}
