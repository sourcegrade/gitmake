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

package org.sourcegrade.gitmake.page

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.sourcegrade.gitmake.TEMPLATE_CONFIG_PATH
import org.sourcegrade.gitmake.template.TemplateConfig
import org.sourcegrade.gitmake.encodeThrowable
import org.sourcegrade.gitmake.template.hasCycle
import org.sourcegrade.gitmake.template.initTemplate
import org.sourcegrade.gitmake.page.Page.Factory.Companion.createFactory
import org.sourcegrade.gitmake.toAnsi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries

class TemplateInitPage(private val controller: PageController) : Page<TemplateInitPage.Config> {

    override fun start(config: Config) {
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
            controller.goToPage(
                RepoInitPage.Factory,
                RepoInitPage.Config(templateConfig, workingDir, templateLocalPath),
            )
        } while (KInquirer.promptConfirm("Create another repository from template?"))
    }

    class Config {

    }

    /**
     * Persistent state shared between instances.
     */
    object State {

    }

    object Factory : Page.Factory<Config> by createFactory("template-init", ::TemplateInitPage)
}

