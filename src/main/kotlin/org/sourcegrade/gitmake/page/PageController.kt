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

interface PageController {

    val pages: List<Page<*>>

    fun <C> goToPage(pageFactory: Page.Factory<C>, config: C)

    fun back()
}

val PageController.currentPage: Page<*>
    get() = pages.last()

private class PageControllerImpl : PageController {

    private val _pages = mutableListOf<Page<*>>()
    override val pages: List<Page<*>> by ::_pages

    override fun <C> goToPage(pageFactory: Page.Factory<C>, config: C) {
        _pages.add(pageFactory.create(this))
    }

    override fun back() {
        _pages.removeLast()
    }
}
