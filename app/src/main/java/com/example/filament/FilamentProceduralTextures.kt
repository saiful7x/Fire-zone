package com.example.filament

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.android.TextureHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class FilamentTextureBank(private val engine: Engine) {
    val repeatSampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT
    ).apply { anisotropy = 4f }

    val clampSampler = TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.CLAMP_TO_EDGE
    ).apply { anisotropy = 4f }

    private val createdTextures = mutableListOf<Texture>()

    val faceMap: Texture = uploadBitmap(buildFaceBitmap(), srgb = true)
    val hairMap: Texture = uploadBitmap(buildHairBitmap(), srgb = true)
    val hoodieMap: Texture = uploadBitmap(buildHoodieBitmap(), srgb = true)
    val cargoPantsMap: Texture = uploadBitmap(buildCargoPantsBitmap(), srgb = true)
    val bootsMap: Texture = uploadBitmap(buildBootsBitmap(), srgb = true)
    val skinMap: Texture = uploadBitmap(buildSkinBitmap(), srgb = true)

    val bermudaGrassMap: Texture = uploadBitmap(buildBermudaGrassBitmap(), srgb = true)
    val asphaltRoadMap: Texture = uploadBitmap(buildAsphaltRoadBitmap(), srgb = true)
    val warehouseFloorMap: Texture = uploadBitmap(buildWarehouseFloorBitmap(), srgb = true)
    val hazardPadMap: Texture = uploadBitmap(buildHazardPadBitmap(), srgb = true)
    val corrugatedMetalMap: Texture = uploadBitmap(buildCorrugatedMetalBitmap(), srgb = true)
    val vanBodyMap: Texture = uploadBitmap(buildVanBodyBitmap(), srgb = true)
    val buildingFacadeMap: Texture = uploadBitmap(buildBuildingFacadeBitmap(), srgb = true)
    val woodCrateMap: Texture = uploadBitmap(buildWoodCrateBitmap(), srgb = true)
    val glooHexMap: Texture = uploadBitmap(buildGlooHexBitmap(), srgb = true)
    val pedestalMetalMap: Texture = uploadBitmap(buildPedestalMetalBitmap(), srgb = true)

    fun destroy() {
        createdTextures.forEach { engine.destroyTexture(it) }
        createdTextures.clear()
    }

    private fun uploadBitmap(bitmap: Bitmap, srgb: Boolean): Texture {
        val format = if (srgb) Texture.InternalFormat.SRGB8_A8 else Texture.InternalFormat.RGBA8
        val levels = 7
        val tex = Texture.Builder()
            .width(bitmap.width)
            .height(bitmap.height)
            .levels(levels)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(format)
            .build(engine)
        TextureHelper.setBitmap(engine, tex, 0, bitmap)
        tex.generateMipmaps(engine)
        bitmap.recycle()
        createdTextures.add(tex)
        return tex
    }

    // -------------------------------------------------------------------------
    // CHARACTER TEXTURES (Adam / Free Fire Style Sculpted Albedo Maps)
    // -------------------------------------------------------------------------

    private fun buildSkinBitmap(): Bitmap {
        val s = 128
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(220, 174, 142))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, s.toFloat(), Color.rgb(228, 184, 152), Color.rgb(200, 152, 120), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), p)
        // Subtle muscle/knuckle shading
        p.shader = null
        p.color = Color.argb(26, 130, 75, 50)
        for (i in 0..8) {
            c.drawCircle((i * 37 % s).toFloat(), (i * 53 % s).toFloat(), 18f, p)
        }
        return bmp
    }

    private fun buildFaceBitmap(): Bitmap {
        // Cylindrical UV: u=0.5 is front center of face (+Z), v=0 top of head, v=1 bottom of neck
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Base warm skin gradient (forehead brighter, lower neck slightly shaded)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(232, 188, 156), Color.rgb(198, 148, 118), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        val cx = w * 0.5f

        // Subtle jaw & neck shadow
        p.color = Color.argb(65, 110, 62, 42)
        c.drawRect(0f, h * 0.75f, w.toFloat(), h.toFloat(), p)

        // Hairline shadow across forehead & back of head
        p.color = Color.rgb(52, 38, 30)
        c.drawRect(0f, 0f, w.toFloat(), h * 0.25f, p)
        c.drawRect(0f, h * 0.22f, w * 0.26f, h * 0.72f, p)
        c.drawRect(w * 0.74f, h * 0.22f, w.toFloat(), h * 0.72f, p)

        // Sideburns
        c.drawRect(w * 0.28f, h * 0.34f, w * 0.32f, h * 0.54f, p)
        c.drawRect(w * 0.68f, h * 0.34f, w * 0.72f, h * 0.54f, p)

        // Warm cheek blush & nose bridge highlight
        p.shader = RadialGradient(cx - 26f, h * 0.53f, 22f, Color.argb(45, 210, 95, 75), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx - 26f, h * 0.53f, 22f, p)
        p.shader = RadialGradient(cx + 26f, h * 0.53f, 22f, Color.argb(45, 210, 95, 75), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx + 26f, h * 0.53f, 22f, p)
        p.shader = null

        // Sharp masculine eyebrows
        p.color = Color.rgb(38, 26, 20)
        p.strokeWidth = 4.5f
        p.strokeCap = Paint.Cap.ROUND
        c.drawLine(cx - 32f, h * 0.39f, cx - 10f, h * 0.405f, p)
        c.drawLine(cx + 32f, h * 0.39f, cx + 10f, h * 0.405f, p)

        // Eyes (sclera + dark iris + pupil + bright specular catchlight)
        val eyeY = h * 0.455f
        for (sign in listOf(-1f, 1f)) {
            val ex = cx + sign * 21f
            p.color = Color.argb(55, 90, 50, 35)
            c.drawOval(RectF(ex - 11f, eyeY - 7f, ex + 11f, eyeY + 7f), p)
            p.color = Color.rgb(244, 244, 246)
            c.drawOval(RectF(ex - 9f, eyeY - 4.8f, ex + 9f, eyeY + 4.8f), p)
            p.color = Color.rgb(66, 44, 28)
            c.drawCircle(ex, eyeY, 4.3f, p)
            p.color = Color.rgb(18, 14, 14)
            c.drawCircle(ex, eyeY, 2.2f, p)
            p.color = Color.WHITE
            c.drawCircle(ex + 1.4f, eyeY - 1.5f, 1.1f, p)
            // Upper lash line
            p.color = Color.rgb(30, 20, 16)
            p.strokeWidth = 2.4f
            c.drawLine(ex - 9.5f, eyeY - 3.5f, ex + 9.5f, eyeY - 3.5f, p)
        }

        // Nose bridge shadow & nostril line
        p.color = Color.argb(70, 145, 92, 66)
        p.strokeWidth = 2.2f
        c.drawLine(cx - 4f, h * 0.56f, cx + 4f, h * 0.56f, p)
        p.color = Color.argb(38, 255, 238, 220)
        c.drawLine(cx, h * 0.44f, cx, h * 0.54f, p)

        // Lips & chin crease
        p.color = Color.rgb(168, 102, 86)
        p.strokeWidth = 3.2f
        c.drawLine(cx - 12f, h * 0.64f, cx + 12f, h * 0.64f, p)
        p.color = Color.argb(95, 110, 58, 45)
        p.strokeWidth = 1.6f
        c.drawLine(cx - 10f, h * 0.655f, cx + 10f, h * 0.655f, p)

        return bmp
    }

    private fun buildHairBitmap(): Bitmap {
        val s = 128
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(215, 205, 198)) // Neutral light so tint gives rich dark brown / blonde / cyan hair
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (x in 0 until s step 4) {
            p.color = if ((x / 4) % 2 == 0) Color.argb(50, 40, 35, 30) else Color.argb(40, 255, 255, 255)
            p.strokeWidth = 2f
            c.drawLine(x.toFloat(), 0f, (x + 8).toFloat(), s.toFloat(), p)
        }
        return bmp
    }

    private fun buildHoodieBitmap(): Bitmap {
        // Neutral light-gray fabric detail map so multiplying by character tint produces crisp colored hoodies/jackets
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(245, 247, 250), Color.rgb(206, 210, 218), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // Subtle cotton fabric grain
        p.color = Color.argb(18, 80, 90, 105)
        for (y in 0 until h step 4) {
            c.drawLine(0f, y.toFloat(), w.toFloat(), y.toFloat(), p)
        }

        val cx = w * 0.5f
        // Neckline collar V-shadow & drawstrings (matching Adam sleeveless hoodie!)
        p.color = Color.argb(85, 65, 72, 85)
        val collar = Path().apply {
            moveTo(cx - 32f, 0f)
            lineTo(cx, 38f)
            lineTo(cx + 32f, 0f)
            close()
        }
        c.drawPath(collar, p)

        p.color = Color.argb(165, 50, 55, 65)
        p.strokeWidth = 3f
        c.drawLine(cx - 8f, 24f, cx - 10f, 68f, p)
        c.drawLine(cx + 8f, 24f, cx + 10f, 68f, p)

        // Subtle chest pectoral & rib fold shading
        p.shader = RadialGradient(cx - 28f, 82f, 34f, Color.argb(36, 40, 45, 55), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx - 28f, 82f, 34f, p)
        p.shader = RadialGradient(cx + 28f, 82f, 34f, Color.argb(36, 40, 45, 55), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx + 28f, 82f, 34f, p)
        p.shader = null

        // Center tactical zipper / seam
        p.color = Color.argb(75, 50, 55, 68)
        p.strokeWidth = 2.5f
        c.drawLine(cx, 38f, cx, h.toFloat(), p)

        // Chest emblem ("FIREZONE" badge like "FREE FIRE" on Adam's hoodie)
        p.color = Color.rgb(42, 46, 54)
        p.textSize = 15f
        p.isFakeBoldText = true
        c.drawText("FIRE", cx + 14f, 76f, p)
        p.color = Color.rgb(235, 130, 20)
        c.drawText("ZONE", cx + 14f, 92f, p)

        // Lower hoodie kangaroo pocket outline & ribbed hem
        p.color = Color.argb(70, 55, 60, 70)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f
        c.drawRect(cx - 42f, h * 0.66f, cx + 42f, h * 0.88f, p)
        p.style = Paint.Style.FILL
        p.color = Color.argb(45, 40, 45, 55)
        c.drawRect(0f, h * 0.90f, w.toFloat(), h.toFloat(), p)

        return bmp
    }

    private fun buildCargoPantsBitmap(): Bitmap {
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Base neutral light-gray ripstop denim/cargo weave (tinted per character)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(225, 228, 224), Color.rgb(175, 178, 174), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // Ripstop grid weave
        p.color = Color.argb(18, 40, 45, 40)
        for (i in 0 until w step 8) {
            c.drawLine(i.toFloat(), 0f, i.toFloat(), h.toFloat(), p)
            c.drawLine(0f, i.toFloat(), w.toFloat(), i.toFloat(), p)
        }

        // Waistband + dark leather belt + silver buckle
        p.color = Color.rgb(42, 34, 30)
        c.drawRect(0f, 0f, w.toFloat(), 26f, p)
        p.color = Color.rgb(198, 204, 212)
        c.drawRect(w * 0.44f, 3f, w * 0.56f, 23f, p)
        p.color = Color.rgb(55, 58, 64)
        c.drawRect(w * 0.47f, 7f, w * 0.53f, 19f, p)

        // Side cargo pocket flaps
        p.color = Color.argb(85, 35, 38, 35)
        c.drawRect(w * 0.14f, h * 0.25f, w * 0.35f, h * 0.48f, p)
        c.drawRect(w * 0.65f, h * 0.25f, w * 0.86f, h * 0.48f, p)
        p.color = Color.argb(110, 220, 220, 220)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        c.drawRect(w * 0.14f, h * 0.25f, w * 0.35f, h * 0.31f, p)
        c.drawRect(w * 0.65f, h * 0.25f, w * 0.86f, h * 0.31f, p)

        // Distressed white skull / tactical patch near thigh/knee (inspired by Adam 3D model!)
        p.style = Paint.Style.FILL
        p.color = Color.argb(200, 238, 238, 235)
        c.drawCircle(w * 0.50f, h * 0.46f, 16f, p)
        c.drawRect(w * 0.44f, h * 0.50f, w * 0.56f, h * 0.56f, p)
        p.color = Color.rgb(32, 32, 34)
        c.drawCircle(w * 0.46f, h * 0.46f, 3.6f, p)
        c.drawCircle(w * 0.54f, h * 0.46f, 3.6f, p)

        // Knee pad crease lines
        p.color = Color.argb(70, 30, 30, 30)
        p.strokeWidth = 3f
        c.drawLine(0f, h * 0.62f, w.toFloat(), h * 0.62f, p)
        c.drawLine(0f, h * 0.68f, w.toFloat(), h * 0.68f, p)

        return bmp
    }

    private fun buildBootsBitmap(): Bitmap {
        val s = 128
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(46, 44, 48))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // Laces along front
        p.color = Color.rgb(185, 185, 190)
        p.strokeWidth = 3f
        for (i in 0..4) {
            val y = 18f + i * 14f
            c.drawLine(46f, y, 82f, y + 8f, p)
            c.drawLine(82f, y, 46f, y + 8f, p)
        }
        // Thick rubber sole at bottom
        p.color = Color.rgb(22, 22, 24)
        c.drawRect(0f, 102f, s.toFloat(), s.toFloat(), p)
        return bmp
    }

    // -------------------------------------------------------------------------
    // WORLD & PROP TEXTURES (Bermuda & Hangar Warehouse PBR Maps)
    // -------------------------------------------------------------------------

    private fun buildBermudaGrassBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(88, 114, 58))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(42)

        // Soft organic earth & moss blotches
        for (i in 0 until 65) {
            val x = rng.nextFloat() * s
            val y = rng.nextFloat() * s
            val r = 10f + rng.nextFloat() * 28f
            p.color = when (i % 3) {
                0 -> Color.argb(42, 62, 86, 36)
                1 -> Color.argb(38, 122, 138, 68)
                else -> Color.argb(32, 112, 92, 58)
            }
            c.drawCircle(x, y, r, p)
        }
        // Crisp grass blade strokes
        p.strokeWidth = 1.6f
        for (i in 0 until 450) {
            val x = rng.nextFloat() * s
            val y = rng.nextFloat() * s
            p.color = if (i % 2 == 0) Color.argb(95, 134, 158, 76) else Color.argb(90, 56, 78, 32)
            c.drawLine(x, y, x + (rng.nextFloat() - 0.5f) * 5f, y - 5f - rng.nextFloat() * 6f, p)
        }
        return bmp
    }

    private fun buildAsphaltRoadBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(66, 70, 76))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(7)

        for (i in 0 until 700) {
            val v = 45 + rng.nextInt(48)
            p.color = Color.argb(90, v, v + 2, v + 5)
            c.drawPoint(rng.nextFloat() * s, rng.nextFloat() * s, p)
        }
        // Concrete curbs on top & bottom edges
        p.color = Color.rgb(138, 140, 134)
        c.drawRect(0f, 0f, s.toFloat(), 14f, p)
        c.drawRect(0f, s - 14f, s.toFloat(), s.toFloat(), p)

        // Dashed center highway line
        p.color = Color.rgb(225, 200, 95)
        for (x in 12 until s step 64) {
            c.drawRect(x.toFloat(), s * 0.5f - 4f, (x + 36).toFloat(), s * 0.5f + 4f, p)
        }
        return bmp
    }

    private fun buildWarehouseFloorBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(86, 92, 99))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rng = Random(99)

        for (i in 0 until 450) {
            val g = 65 + rng.nextInt(45)
            p.color = Color.argb(65, g, g + 2, g + 4)
            c.drawCircle(rng.nextFloat() * s, rng.nextFloat() * s, 1f + rng.nextFloat() * 4f, p)
        }
        // Slab expansion joints
        p.color = Color.argb(120, 42, 46, 52)
        p.strokeWidth = 3f
        c.drawLine(0f, s * 0.5f, s.toFloat(), s * 0.5f, p)
        c.drawLine(s * 0.5f, 0f, s * 0.5f, s.toFloat(), p)
        c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), p.apply { style = Paint.Style.STROKE })
        return bmp
    }

    private fun buildHazardPadBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(52, 54, 58))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.color = Color.rgb(228, 178, 22)
        p.strokeWidth = 18f
        var x = -s.toFloat()
        while (x < s * 2f) {
            c.drawLine(x, 0f, x + s, s.toFloat(), p)
            x += 42f
        }
        // Dark border frame
        p.color = Color.rgb(228, 178, 22)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 12f
        c.drawRect(6f, 6f, s - 6f, s - 6f, p)
        return bmp
    }

    private fun buildCorrugatedMetalBitmap(): Bitmap {
        // Neutral grayscale ribbed steel container / hangar wall so tinting creates blue, orange, or galvanized steel!
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(210, 214, 220))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vertical corrugated ribs
        for (x in 0 until s step 16) {
            p.color = Color.argb(85, 70, 75, 85)
            c.drawRect(x.toFloat(), 0f, (x + 6).toFloat(), s.toFloat(), p)
            p.color = Color.argb(75, 255, 255, 255)
            c.drawRect((x + 6).toFloat(), 0f, (x + 11).toFloat(), s.toFloat(), p)
        }
        // Top & bottom structural frame beams
        p.color = Color.argb(120, 48, 52, 60)
        c.drawRect(0f, 0f, s.toFloat(), 16f, p)
        c.drawRect(0f, s - 16f, s.toFloat(), s.toFloat(), p)
        c.drawRect(0f, 0f, 12f, s.toFloat(), p)
        c.drawRect(s - 12f, 0f, s.toFloat(), s.toFloat(), p)

        // Subtle weathering grime along bottom
        p.shader = LinearGradient(0f, s * 0.65f, 0f, s.toFloat(), Color.TRANSPARENT, Color.argb(95, 45, 36, 30), Shader.TileMode.CLAMP)
        c.drawRect(0f, s * 0.65f, s.toFloat(), s.toFloat(), p)
        return bmp
    }

    private fun buildVanBodyBitmap(): Bitmap {
        // Retro camper van side/front windows & two-tone trim
        val w = 256
        val h = 256
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Upper cream cabin
        c.drawColor(Color.rgb(230, 228, 214))
        // Lower mint-green body
        p.color = Color.rgb(78, 124, 100)
        c.drawRect(0f, h * 0.50f, w.toFloat(), h.toFloat(), p)
        // Chrome belt trim
        p.color = Color.rgb(212, 216, 220)
        c.drawRect(0f, h * 0.47f, w.toFloat(), h * 0.52f, p)

        // Dark tinted side windows with mullions
        p.color = Color.rgb(34, 48, 58)
        for (i in 0 until 4) {
            val wx = 18f + i * 58f
            c.drawRoundRect(RectF(wx, 34f, wx + 46f, h * 0.42f), 8f, 8f, p)
        }
        // Subtle diagonal window reflection
        p.color = Color.argb(60, 210, 238, 255)
        for (i in 0 until 4) {
            val wx = 22f + i * 58f
            c.drawLine(wx, 75f, wx + 24f, 40f, p.apply { strokeWidth = 4f })
        }
        return bmp
    }

    private fun buildBuildingFacadeBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(198, 192, 178))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Plaster weathering
        val rng = Random(13)
        for (i in 0 until 40) {
            p.color = Color.argb(26, 90, 82, 72)
            c.drawCircle(rng.nextFloat() * s, rng.nextFloat() * s, 14f + rng.nextFloat() * 18f, p)
        }
        // Concrete floor divider band
        p.color = Color.rgb(122, 118, 110)
        c.drawRect(0f, s * 0.46f, s.toFloat(), s * 0.53f, p)
        c.drawRect(0f, s - 16f, s.toFloat(), s.toFloat(), p)

        // Framed tactical windows (2 rows x 2 cols)
        for (row in 0..1) {
            val wy = if (row == 0) 26f else s * 0.58f
            for (col in 0..1) {
                val wx = if (col == 0) 30f else 148f
                p.color = Color.rgb(88, 72, 58)
                c.drawRect(wx - 4f, wy - 4f, wx + 78f, wy + 74f, p)
                p.color = Color.rgb(32, 48, 62)
                c.drawRect(wx, wy, wx + 74f, wy + 70f, p)
                // Window mullions
                p.color = Color.rgb(138, 128, 116)
                c.drawRect(wx + 35f, wy, wx + 39f, wy + 70f, p)
                c.drawRect(wx, wy + 33f, wx + 74f, wy + 37f, p)
            }
        }
        return bmp
    }

    private fun buildWoodCrateBitmap(): Bitmap {
        val s = 128
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(142, 98, 60))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        for (y in 0 until s step 24) {
            p.color = Color.argb(70, 68, 42, 22)
            c.drawLine(0f, y.toFloat(), s.toFloat(), y.toFloat(), p.apply { strokeWidth = 2.5f })
        }
        // Frame & diagonal cross brace
        p.color = Color.rgb(112, 74, 42)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 12f
        c.drawRect(6f, 6f, s - 6f, s - 6f, p)
        p.strokeWidth = 9f
        c.drawLine(10f, 10f, s - 10f, s - 10f, p)
        return bmp
    }

    private fun buildGlooHexBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.argb(135, 18, 190, 240))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.argb(190, 180, 248, 255)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f

        val radius = 26f
        val dx = radius * 1.5f
        val dy = radius * 1.732f
        for (col in -1..8) {
            for (row in -1..8) {
                val cx = col * dx
                val cy = row * dy + if (col % 2 != 0) dy * 0.5f else 0f
                val path = Path()
                for (k in 0 until 6) {
                    val a = k * PI / 3.0
                    val vx = cx + radius * cos(a).toFloat()
                    val vy = cy + radius * sin(a).toFloat()
                    if (k == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
                }
                path.close()
                c.drawPath(path, p)
            }
        }
        return bmp
    }

    private fun buildPedestalMetalBitmap(): Bitmap {
        val s = 256
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(34, 42, 54))
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.color = Color.argb(120, 255, 179, 0)
        p.strokeWidth = 4f
        c.drawCircle(s * 0.5f, s * 0.5f, s * 0.42f, p)
        p.color = Color.argb(90, 0, 229, 255)
        p.strokeWidth = 2.5f
        c.drawCircle(s * 0.5f, s * 0.5f, s * 0.30f, p)
        return bmp
    }
}
