package org.sourcegrade.gitmake

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import java.nio.file.Path

fun Path.initTemplate(): String {
    while (true) {
        val result = promptTemplate()
        if (result.success) {
            return result.url
        } else {
            println(buildString {
                append("Could not clone template URL ".toAnsi { fgRed() })
                append(result.url.toAnsi { fgCyan() })
            })
        }
    }
}

private data class TemplateResult(val url: String, val success: Boolean)

private fun Path.promptTemplate(): TemplateResult {
    val gitUrl = KInquirer.promptInput("URL for git template repository")
    return TemplateResult(gitUrl, runCmdBlocking("git", "clone", gitUrl))
}
