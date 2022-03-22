package org.sourcegrade.gitmake

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.system.exitProcess

fun runInitMode() {
    val workingDir = createTempDirectory("gitmake")
    val templateRemoteUrl = workingDir.initTemplate()
    println("\u001B[32mSuccessfully cloned ${templateRemoteUrl}\u001b[0m")
    val entries = workingDir.listDirectoryEntries()
    assert(entries.size == 1) {
        "Expected exactly one entry in $workingDir but got ${entries.sortedDescending()}"
    }
    val templateLocalPath = entries.first()
    val templateConfig = try {
        @OptIn(ExperimentalSerializationApi::class)
        Json.decodeFromStream<TemplateConfig>(templateLocalPath.resolve(TEMPLATE_CONFIG_PATH).inputStream().buffered())
    } catch (e: Exception) {
        encodeThrowable("Could not parse $TEMPLATE_CONFIG_PATH", e)
    }
    if (templateConfig.hasCycle()) {
        encodeThrowable("Discovered cycle during placeholder resolution")
    }
    println("Successfully resolved config".toAnsi { fgGreen() })
    do {
        with(templateConfig) {
            workingDir.useTemplate(templateLocalPath)
        }
    } while (KInquirer.promptConfirm("Create another repository from template?"))
}

context(Template)
fun Path.useTemplate(templateLocalPath: Path) {
    val resolvedTemplate = resolve()
    val replacements = resolvedTemplate.createReplacementNodes()
    println("Successfully resolved template".toAnsi { fgGreen() })
    for (placeholder in resolvedTemplate.placeholders) {
        println(buildString {
            append("[ OK ] ".toAnsi { fgGreen(); bold() })
            append(placeholder.name.toAnsi { fgCyan(); bold() })
            append(" :: ".toAnsi { fgGreen() })
            append(placeholder.pattern.toAnsi { fgMagenta(); bold() })
            append(" = ".toAnsi { fgGreen() })
            with(resolvedTemplate) {
                append(placeholder.resolution.highlightPatterns())
            }
            append(" = ".toAnsi { fgGreen() })
            append(placeholder.resolution.replaceAll(replacements).toAnsi { fgCyan(); bold() })
        })
    }
    with(resolvedTemplate) {
        createRepoFromTemplate(templateLocalPath, replacements)
    }
}

context(ResolvedTemplate)
fun Path.createRepoFromTemplate(templateLocalPath: Path, replacements: List<ReplacementNode>) {
    val repoName = promptReplaced("Repository name", replacements)
    val remoteUrl = promptReplaced("Target remote url", replacements)
    try {
        cloneAndInitialize(templateLocalPath, repoName, remoteUrl, replacements)
    } catch (e: Throwable) {
        println("\u001B[31mAn error occurred")
        e.printStackTrace()
        when (KInquirer.promptList("Try again?", listOf("Yes", "No", "Other template"))) {
            "Yes" -> createRepoFromTemplate(templateLocalPath, replacements)
            "Other template" -> return
            else -> exitProcess(0)
        }
    }
}

context(Template)
fun promptReplaced(message: String, replacements: List<ReplacementNode>): String {
    val result = KInquirer.promptInput(message, transform = { it.highlightPatterns() }).replaceAll(replacements)
    println(buildString {
        append("[ OK ] ".toAnsi { fgGreen(); bold() })
        append("$message = ".toAnsi { bold() })
        append(result.toAnsi { fgCyan(); bold() })
    })
    return result
}

private fun Path.cloneAndInitialize(
    templateLocalPath: Path,
    name: String,
    remoteUrl: String,
    replacements: List<ReplacementNode>,
) {
    runCmdThrowing("git", "clone", templateLocalPath.toString(), name)
    val repoDir = resolve(name)
    println(buildString {
        append("Cloned local template into ".toAnsi { fgGreen() })
        append(repoDir.toString().toAnsi { fgCyan() })
    })
    repoDir.processRepo(remoteUrl, replacements)
}


private fun Path.processRepo(
    remoteUrl: String,
    replacements: List<ReplacementNode>,
) {
    handleReplacements(this, replacements)
    runCmdThrowing("git", "add", "-A")
    runCmdThrowing("git", "commit", "-m", "Initialize template")
    runCmdThrowing("git", "remote", "set-url", "origin", remoteUrl)
    runCmdThrowing("git", "push", "-u", "origin", "master", "-f")
}

private fun Path.handleReplacements(root: Path, replacements: List<ReplacementNode>) {
    val entries = listDirectoryEntries()
    for (entry in entries) {
        val relativePath = entry.relativeTo(root)
        val originalRelativePathString = relativePath.toString()
        if (entry.isRegularFile() && entry.isReadable() && entry.isWritable()) {
            println(buildString {
                append("Processing ".toAnsi { bold() })
                append(relativePath.toString().toAnsi { fgCyan() })
            })
            val originalText = entry.readText()
            val replacementText = originalText.replaceAll(replacements)
            val newPath = originalRelativePathString.checkPathMove(root, replacements)
            if (newPath != null) {
                entry.deleteExisting()
                newPath.writeText(replacementText)
            } else if (originalText != replacementText) {
                entry.writeText(replacementText)
            }
        } else if (entry.isDirectory() && !entry.contains(Path(".git"))) {
            entry.handleReplacements(root, replacements)
        } else {
            println(buildString {
                append("Skipping ".toAnsi { fgRed() })
                append(originalRelativePathString.toAnsi { fgCyan() })
            })
        }
    }
}

/**
 * Takes a relative path as a string and returns a transformed absolute path only if it was transformed
 */
private fun String.checkPathMove(root: Path, replacements: List<ReplacementNode>): Path? {
    val replacement = replaceAll(replacements)
    return if (this == replacement) {
        null
    } else {
        println(buildString {
            append("Moving ".toAnsi { bold() })
            append(toAnsi { fgCyan() }) // TODO: Highlight patterns and replacements
            append(" -> ".toAnsi { bold() })
            append(replacement.toAnsi { fgCyan() })
        })
        val target = root.resolve(replacement)
        Files.createDirectories(target.parent)
        target
    }
}
