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
import com.github.kinquirer.components.promptList
import org.sourcegrade.gitmake.template.Template
import java.nio.file.Path

class TemplateStrategyPage : Page<TemplateStrategyPage.Config> {
    override fun start(config: Config) {
        KInquirer.promptList(
            "Choose a template strategy",

        )
    }

    data class Config(
        val template: Template,
        val workingDir: Path,
        val templateLocalPath: Path,
    )
}
