package org.sourcegrade.gitmake

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs
import kotlinx.serialization.Serializable

interface Template {
    val name: String
    val placeholders: List<Placeholder>
}

interface ResolvedTemplate : Template {
    override val placeholders: List<ResolvedPlaceholder>
}

fun Template.resolve(): ResolvedTemplate {
    var resolvedTemplate: ResolvedTemplate
    while (true) {
        resolvedTemplate = ResolvedTemplateImpl(name, placeholders.map { it.resolve() })
        if (!resolvedTemplate.hasCycle()) {
            break;
        }
        println("Resolution graph has cycle".toAnsi { fgRed() })
    }
    return resolvedTemplate
}

@Suppress("UnstableApiUsage")
fun Template.generateDependencyGraph(): Graph<String> {
    val graph = GraphBuilder.directed().build<String>()
    placeholders.asSequence().map { it.pattern }.forEach(graph::addNode)
    for (placeholder in placeholders) {
        placeholder.resolution?.let { resolution ->
            if (resolution.contains(placeholder.pattern)) {
                encodeThrowable(buildString {
                    append("Placeholder ".toAnsi { fgRed() })
                    append(placeholder.name.toAnsi { fgCyan() })
                    append("'s resolution ".toAnsi { fgRed() })
                    append(resolution.toAnsi { fgCyan() })
                    append(" may not contain own pattern ".toAnsi { fgRed() })
                    append(placeholder.pattern.toAnsi { fgCyan() })
                })
            }
            for (pattern in placeholders.asSequence().map { it.pattern }.filter { it != placeholder.pattern }) {
                if (resolution.contains(pattern)) {
                    graph.putEdge(placeholder.pattern, pattern)
                }
            }
        }
    }
    return graph
}

@Suppress("UnstableApiUsage")
fun Template.hasCycle(): Boolean = Graphs.hasCycle(generateDependencyGraph())

@Serializable
internal data class TemplateConfig(
    override val name: String,
    override val placeholders: List<PlaceholderConfig>,
) : Template

internal data class ResolvedTemplateImpl(
    override val name: String,
    override val placeholders: List<ResolvedPlaceholder>,
) : ResolvedTemplate
