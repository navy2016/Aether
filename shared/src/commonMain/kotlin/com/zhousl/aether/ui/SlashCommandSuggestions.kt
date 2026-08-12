package com.zhousl.aether.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/** Pi built-ins exposed by Aether's non-TUI composer. */
val PiBuiltinSlashCommands = listOf(
    SlashCommandSuggestion("/compact", "Manually compact the session context"),
    SlashCommandSuggestion("/thinking", "Cycle thinking: off, low, medium, high, xhigh"),
)

data class SlashCommandSuggestion(
    val command: String,
    val description: String,
    val icon: SlashCommandIcon = SlashCommandIcon.Command,
    val argumentHint: String = "",
    val extensionId: String = "",
    val action: String = "",
    val actionArgs: Map<String, String> = emptyMap(),
)

enum class SlashCommandIcon { Command, Skill, Extension }

fun slashCommandSuggestions(
    input: String,
    extensionCommands: List<SlashCommandSuggestion> = emptyList(),
): List<SlashCommandSuggestion> {
    if (input.isEmpty() || input.first() != '/') return emptyList()
    val commandToken = input.drop(1).takeWhile { !it.isWhitespace() }
    val allCommands = PiBuiltinSlashCommands + extensionCommands.map {
        it.copy(
            command = "/${it.command.removePrefix("/")}",
            icon = SlashCommandIcon.Extension,
        )
    }
    val exactCommand = allCommands.any {
        it.command.removePrefix("/").equals(commandToken, ignoreCase = true)
    }
    if (input.drop(1 + commandToken.length).trimStart().isNotEmpty() && exactCommand) return emptyList()
    val prefix = commandToken.lowercase()
    return allCommands
        .filter {
            val candidate = it.command.removePrefix("/").lowercase()
            candidate.startsWith(prefix) || prefix.startsWith(candidate)
        }
        .distinctBy { it.command.lowercase() }
        .take(50)
}

data class ParsedSlashCommand(
    val name: String,
    val args: String,
    val raw: String,
)

fun parseSlashCommand(input: String): ParsedSlashCommand? {
    val raw = input.trim()
    if (!raw.startsWith('/')) return null
    val body = raw.drop(1)
    val name = body.takeWhile { !it.isWhitespace() }.trim()
    if (name.isEmpty()) return null
    return ParsedSlashCommand(
        name = name,
        args = body.drop(name.length).trimStart(),
        raw = raw,
    )
}

fun findSlashCommand(
    input: String,
    commands: List<SlashCommandSuggestion>,
): Pair<SlashCommandSuggestion, ParsedSlashCommand>? {
    val parsed = parseSlashCommand(input) ?: return null
    val command = commands.firstOrNull {
        it.command.removePrefix("/").equals(parsed.name, ignoreCase = true)
    } ?: return null
    return command to parsed
}

private val ThinkingCycle = listOf("off", "low", "medium", "high", "xhigh")

fun nextThinkingLevel(
    current: String,
    supportedLevels: List<String> = emptyList(),
    clamps: Map<String, String> = emptyMap(),
): String {
    val allowed = ThinkingCycle
        .map { clamps[it] ?: it }
        .filter { supportedLevels.isEmpty() || it in supportedLevels }
        .distinct()
        .ifEmpty { listOf("off") }
    val effectiveCurrent = clamps[current] ?: current
    val currentIndex = allowed.indexOf(effectiveCurrent)
    return allowed[(currentIndex + 1).mod(allowed.size)]
}

fun slashDisplayName(command: String): String = command.removePrefix("/")
    .replace('-', ' ')
    .replace(':', ' ')
    .trim()
    .replaceFirstChar { it.titlecase() }

fun slashHighlightedName(command: String, input: String): AnnotatedString = buildAnnotatedString {
    val label = slashDisplayName(command)
    val query = input.drop(1).takeWhile { !it.isWhitespace() }.replace('-', ' ')
    val match = label.lowercase().indexOf(query.lowercase().trim())
    if (match >= 0 && query.isNotBlank()) {
        append(label.substring(0, match))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(label.substring(match, (match + query.length).coerceAtMost(label.length)))
        }
        append(label.drop((match + query.length).coerceAtMost(label.length)))
    } else append(label)
}
