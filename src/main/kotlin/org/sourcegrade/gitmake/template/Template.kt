/*
 *   GitMake - SourceGrade.org
 *   Copyright (C) 2021-2022 Alexander Staeding
 *   Copyright (C) 2021-2022 Contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.sourcegrade.gitmake.template

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs
import kotlinx.serialization.Serializable
import org.sourcegrade.gitmake.Placeholder
import org.sourcegrade.gitmake.PlaceholderConfig
import org.sourcegrade.gitmake.ResolvedPlaceholder
import org.sourcegrade.gitmake.encodeThrowable
import org.sourcegrade.gitmake.resolve
import org.sourcegrade.gitmake.toAnsi

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
    println("Successfully resolved template".toAnsi { fgGreen() })
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
