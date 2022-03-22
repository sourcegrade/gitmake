package org.sourcegrade.gitmake

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import kotlinx.serialization.Serializable

interface Placeholder {
    val name: String
    val description: String

    /**
     * The pattern to match in the template. The entire matched sequence is replaced with the resolved value.
     */
    val pattern: String

    /**
     * Whether to prompt the user to provide a value for this placeholder.
     *
     * If [resolution] is null, the user will always be prompted.
     */
    val prompt: Boolean

    /**
     * The default value for this placeholder. May contain other placeholders.
     *
     * Note: Cyclic references will throw an exception.
     *
     * // TODO: Support inserting groups from [pattern] regex
     */
    val resolution: String?
}

interface ResolvedPlaceholder : Placeholder {
    override val resolution: String
}

fun Placeholder.resolveWith(resolution: String): ResolvedPlaceholder =
    ResolvedPlaceholderImpl(name, description, pattern, prompt, resolution)

context(Template)
fun Placeholder.resolve(): ResolvedPlaceholder {
    val resolution = resolution
    return when {
        this is ResolvedPlaceholder -> this
        resolution == null -> resolveWith(promptInput(name))
        prompt -> resolveWith(promptOverride(name, resolution))
        else -> resolveWith(resolution)
    }
}

context(Template)
fun promptInput(name: String): String {
    val message = buildString {
        append("Placeholder ".toAnsi { bold() })
        append(name.toAnsi { fgCyan(); bold() })
        append(" value? ".toAnsi { bold() })
    }
    val invalidPattern = placeholders.first { it.name == name }.pattern
    return KInquirer.promptInput(
        message,
        validation = { !it.contains(invalidPattern) },
        transform = { it.highlightPatterns(invalidPattern) }
    )
}

context(Template)
fun promptOverride(name: String, existingResolution: String): String {
    val message = buildString {
        append("Placeholder ".toAnsi { bold() })
        append(name.toAnsi { fgCyan(); bold() })
        append(" has existing resolution ".toAnsi { bold() })
        append(existingResolution.toAnsi { fgCyan(); bold() })
        append(" - Override?".toAnsi { fgMagenta(); bold() })
    }
    return if (KInquirer.promptConfirm(message)) {
        // TODO: Ability to cancel prompt?
        promptInput(name)
    } else {
        existingResolution
    }
}

context(Template)
fun String.highlightPatterns(invalidPattern: String? = null): String {
    val builder = StringBuilder(this)
    for (pattern in placeholders.map { it.pattern }.filter { it != invalidPattern }) {
        builder.replace(pattern, pattern.toAnsi { fgMagenta(); bold() }.toString())
    }
    if (invalidPattern != null) {
        builder.replace(invalidPattern, invalidPattern.toAnsi { fgRed(); bold() }.toString())
    }
    return builder.toString()
}

@Serializable
internal data class PlaceholderConfig(
    override val name: String,
    override val description: String,
    override val pattern: String,
    override val prompt: Boolean = true,
    override val resolution: String? = null,
) : Placeholder

internal data class ResolvedPlaceholderImpl(
    override val name: String,
    override val description: String,
    override val pattern: String,
    override val prompt: Boolean,
    override val resolution: String,
) : ResolvedPlaceholder
