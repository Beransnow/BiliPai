package com.android.purebilibili.feature.anime4k.gl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Fsr1ShadersTest {

    @Test
    fun `EASU 使用 AMD 标准十二采样核并直接读取 OES`() {
        assertTrue(Fsr1Shaders.EASU.contains("GL_OES_EGL_image_external_essl3"))
        assertTrue(Fsr1Shaders.EASU.contains("uniform samplerExternalOES uTexture"))
        assertEquals(
            12,
            Regex("fsrEasuTap\\(accumulatedColor").findAll(Fsr1Shaders.EASU).count()
        )
    }

    @Test
    fun `RCAS 使用公开五点十字核和动态锐度参数`() {
        assertTrue(Fsr1Shaders.RCAS.contains("vec3 b = texture"))
        assertTrue(Fsr1Shaders.RCAS.contains("vec3 d = texture"))
        assertTrue(Fsr1Shaders.RCAS.contains("vec3 e = texture"))
        assertTrue(Fsr1Shaders.RCAS.contains("vec3 f = texture"))
        assertTrue(Fsr1Shaders.RCAS.contains("vec3 h = texture"))
        assertTrue(Fsr1Shaders.RCAS.contains("uniform float uSharpnessStops"))
        assertTrue(Fsr1Shaders.RCAS.contains("exp2(-clamp(uSharpnessStops, 0.0, 2.0))"))
    }
}
