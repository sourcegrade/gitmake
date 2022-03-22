package org.sourcegrade.gitmake

import org.fusesource.jansi.Ansi

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
