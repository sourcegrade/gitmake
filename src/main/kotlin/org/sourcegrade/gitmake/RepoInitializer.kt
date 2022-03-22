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

class RepoInitializer {

    private val workingDir: Path = createTempDirectory("gitmake")
    private val templateLocalPath: Path
    private val templateConfig: Template
    private val promptCache = mutableMapOf<String, String>()

    init {
        val templateRemoteUrl = workingDir.initTemplate()
        println("Successfully cloned $templateRemoteUrl".toAnsi { fgGreen() })
        val entries = workingDir.listDirectoryEntries()
        assert(entries.size == 1) {
            "Expected exactly one entry in $workingDir but got ${entries.sortedDescending()}"
        }
        templateLocalPath = entries.first()
        templateConfig = try {
            @OptIn(ExperimentalSerializationApi::class)
            Json.decodeFromStream<TemplateConfig>(templateLocalPath.resolve(TEMPLATE_CONFIG_PATH).inputStream().buffered())
        } catch (e: Exception) {
            encodeThrowable("Could not parse $TEMPLATE_CONFIG_PATH", e)
        }
        if (templateConfig.hasCycle()) {
            encodeThrowable("Discovered cycle during placeholder resolution")
        }
        println("Successfully resolved config".toAnsi { fgGreen() })
    }

    fun block() {
        do {
            with(templateConfig) {
                workingDir.useTemplate(templateLocalPath, promptCache)
            }
        } while (KInquirer.promptConfirm("Create another repository from template?"))
    }

    fun useTemplate(templateLocalPath: Path) {
        val resolvedTemplate = templateConfig.resolve()
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
            workingDir.createRepoFromTemplate(templateLocalPath, replacements)
        }
    }
}

private class RepoOps(private val replacements: List<ReplacementNode>) {

    context(ResolvedTemplate)
    fun Path.createRepoFromTemplate(templateLocalPath: Path) {
        val repoName = promptReplaced("Repository name")
        val remoteUrl = promptReplaced("Target remote url")
        try {
            cloneAndInitialize(templateLocalPath, repoName, remoteUrl)
        } catch (e: Throwable) {
            println("\u001B[31mAn error occurred")
            e.printStackTrace()
            when (KInquirer.promptList("Try again?", listOf("Yes", "No", "Other template"))) {
                "Yes" -> createRepoFromTemplate(templateLocalPath)
                "Other template" -> return
                else -> exitProcess(0)
            }
        }
    }

    context(Template)
    fun promptReplaced(message: String): String {
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
    ) {
        runCmdThrowing("git", "clone", templateLocalPath.toString(), name)
        val repoDir = resolve(name)
        println(buildString {
            append("Cloned local template into ".toAnsi { fgGreen() })
            append(repoDir.toString().toAnsi { fgCyan() })
        })
        repoDir.processRepo(remoteUrl)
    }


    private fun Path.processRepo(remoteUrl: String) {
        handleReplacements(this)
        runCmdThrowing("git", "add", "-A")
        runCmdThrowing("git", "commit", "-m", "Initialize template")
        runCmdThrowing("git", "remote", "set-url", "origin", remoteUrl)
        runCmdThrowing("git", "push", "-u", "origin", "master", "-f")
    }

    private fun Path.handleReplacements(root: Path) {
        val entries = listDirectoryEntries()
        for (entry in entries) {
            val relativePath = entry.relativeTo(root)
            if (relativePath == Path("template")) {
                Files.walk(entry)
                    .sorted(Comparator.reverseOrder())
                    .forEach { it.deleteExisting() }
                continue
            }
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
}

