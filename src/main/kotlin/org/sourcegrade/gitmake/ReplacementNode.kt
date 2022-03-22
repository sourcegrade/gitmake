package org.sourcegrade.gitmake

interface ReplacementNode {
    val name: String
    val pattern: String
    fun calculate(): String
}

fun ResolvedTemplate.createReplacementNodes(): List<ReplacementNode> {
    val cache = mutableMapOf<String, ReplacementNode>()
    for (placeholder in placeholders) {
        placeholder.calculateReplacementNode(cache)
    }
    return cache.values.toList()
}

context(ResolvedTemplate)
fun ResolvedPlaceholder.calculateReplacementNode(replacementNodes: MutableMap<String, ReplacementNode>): ReplacementNode =
    replacementNodes.getOrPut(name) { createReplacementNode(replacementNodes) }

context(ResolvedTemplate)
fun ResolvedPlaceholder.createReplacementNode(replacementNodes: MutableMap<String, ReplacementNode>): ReplacementNode {
    val replacements = mutableMapOf<IntRange, ReplacementNode>()
    for (placeholder in placeholders) {
        val ranges = resolution.findRanges(placeholder.pattern)
        if (ranges.isEmpty()) {
            continue
        }
        val replacer = placeholder.calculateReplacementNode(replacementNodes)
        for (range in ranges) {
            replacements[range] = replacer
        }
    }
    return if (replacements.isEmpty()) {
        ReplacedNode(name, pattern, resolution)
    } else {
        ReplacingNode(name, pattern) { resolution.replace(replacements) }
    }
}

private fun String.findRanges(pattern: String): List<IntRange> {
    var index = indexOf(pattern)
    if (index == -1) {
        return listOf()
    }
    val ranges = mutableListOf<IntRange>()
    while (index != -1) {
        ranges += index..index + pattern.length
        index += pattern.length
        index = indexOf(pattern, index)
    }
    return ranges
}

private fun String.replace(replacements: Map<IntRange, ReplacementNode>): String {
    val builder = StringBuilder(this)
    val sortedReplacements = replacements.toSortedMap { a, b -> a.first.compareTo(b.first) }
    // delta must be calculated as the indices in replacements are only valid for the original string
    var delta = 0
    for ((range, replacement) in sortedReplacements) {
        val calc = replacement.calculate()
        builder.replace(range.first + delta, range.last + delta, calc)
        // new length - old length
        delta += calc.length - (range.last - range.first)
    }
    return builder.toString()
}

private data class ReplacedNode(
    override val name: String,
    override val pattern: String,
    val resolvedValue: String,
) : ReplacementNode {
    override fun calculate(): String = resolvedValue
}

private data class ReplacingNode(
    override val name: String,
    override val pattern: String,
    val replacer: () -> String,
) : ReplacementNode {
    override fun calculate(): String = replacer()
}
