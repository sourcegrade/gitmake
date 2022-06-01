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

package org.sourcegrade.gitmake

import org.fusesource.jansi.Ansi
import org.sourcegrade.gitmake.template.ReplacementNode
import org.sourcegrade.gitmake.template.runInitMode

const val TEMPLATE_CONFIG_PATH = "template/config.json"

fun main() {
    runInitMode()
}

fun encodeThrowable(e: Throwable): Nothing {
    print("\u001B[31m")
    throw e;
}

fun encodeThrowable(message: String): Nothing = encodeThrowable(IllegalStateException(message))

fun encodeThrowable(message: String, cause: Throwable): Nothing = encodeThrowable(IllegalStateException(message, cause))

fun String.toAnsi(func: Ansi.() -> Unit = {}): Ansi {
    val ansi = Ansi.ansi()
    ansi.func()
    return ansi.a(this).reset()
}

fun StringBuilder.replace(pattern: String, replacement: String) {
    var index = indexOf(pattern)
    while (index != -1) {
        replace(index, index + pattern.length, replacement)
        index += replacement.length
        index = indexOf(pattern, index)
    }
}

fun StringBuilder.replace(replacement: ReplacementNode) = replace(replacement.pattern, replacement.calculate())

fun String.replaceAll(replacements: List<ReplacementNode>): String {
    val builder = StringBuilder(this)
    for (replacement in replacements) {
        builder.replace(replacement)
    }
    return builder.toString()
}
