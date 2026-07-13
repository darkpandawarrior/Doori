package com.mileway.feature.agent.engine.llm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InjectableTextGeneratorTest {
    @Test
    fun degradesWhenNothingInjected() =
        runTest {
            val seam = InjectableTextGenerator()

            assertFalse(seam.isAvailable())
            assertNull(seam.generate("hi"))
        }

    @Test
    fun delegatesWhenInjectedAndAvailable() =
        runTest {
            val seam = InjectableTextGenerator()
            seam.generator =
                object : TextGenerator {
                    override fun isAvailable() = true

                    override suspend fun generate(prompt: String) = "echo: $prompt"
                }

            assertTrue(seam.isAvailable())
            assertEquals("echo: hi", seam.generate("hi"))
        }

    @Test
    fun degradesWhenInjectedButUnavailable() =
        runTest {
            val seam = InjectableTextGenerator()
            seam.generator =
                object : TextGenerator {
                    override fun isAvailable() = false

                    override suspend fun generate(prompt: String) = error("must not be called when unavailable")
                }

            assertFalse(seam.isAvailable())
            assertNull(seam.generate("hi"))
        }
}
