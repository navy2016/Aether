package com.zhousl.aether.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlashCommandSuggestionsTest {
    @Test
    fun builtInsIncludeCompactAndThinking() {
        val commands = slashCommandSuggestions("/").map { it.command }

        assertTrue("/compact" in commands)
        assertTrue("/thinking" in commands)
    }

    @Test
    fun extensionCommandsAreNormalizedAndFilterByPrefix() {
        val suggestions = slashCommandSuggestions(
            input = "/rev",
            extensionCommands = listOf(
                SlashCommandSuggestion(
                    command = "review",
                    description = "Review changes",
                    extensionId = "extension",
                    action = "review",
                )
            ),
        )

        assertEquals(listOf("/review"), suggestions.map { it.command })
        assertEquals(SlashCommandIcon.Extension, suggestions.single().icon)
    }

    @Test
    fun exactCommandWithArgumentsClosesSuggestionsAndParsesArguments() {
        val commands = listOf(
            SlashCommandSuggestion("/review", "Review changes", extensionId = "extension")
        )

        val allCommands = PiBuiltinSlashCommands + commands
        assertTrue(slashCommandSuggestions("/review security", commands).isEmpty())
        val match = assertNotNull(findSlashCommand(" /review security ", allCommands))
        assertEquals("review", match.second.name)
        assertEquals("security", match.second.args)
        assertEquals("/review security", match.second.raw)
        assertNull(findSlashCommand("hello", commands))
    }

    @Test
    fun thinkingCyclesThroughRequestedFiveLevels() {
        assertEquals("low", nextThinkingLevel("off"))
        assertEquals("medium", nextThinkingLevel("low"))
        assertEquals("high", nextThinkingLevel("medium"))
        assertEquals("xhigh", nextThinkingLevel("high"))
        assertEquals("off", nextThinkingLevel("xhigh"))
    }

    @Test
    fun thinkingCycleRespectsSupportedLevelsAndClamps() {
        assertEquals(
            "high",
            nextThinkingLevel(
                current = "low",
                supportedLevels = listOf("off", "low", "high"),
            ),
        )
        assertEquals(
            "high",
            nextThinkingLevel(
                current = "low",
                supportedLevels = listOf("off", "low", "high"),
                clamps = mapOf("medium" to "high", "xhigh" to "high"),
            ),
        )
    }
}
