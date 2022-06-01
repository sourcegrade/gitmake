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

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptInput
import org.sourcegrade.gitmake.runCmdBlocking
import org.sourcegrade.gitmake.toAnsi
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
