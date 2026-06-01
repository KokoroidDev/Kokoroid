// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-FileContributor: moran0710
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.extension

/**
 * Represents the type of an extension in the Kokoroid framework.
 *
 * Defines dependency resolution rules between extension types via [canDependOn].
 */
enum class ExtensionType {
    DRIVER,
    ADAPTER,
    PLUGIN,
    EXTENSION,
    ;

    /**
     * Determines whether this extension type can declare a dependency on [dependencyType].
     *
     * Rules:
     * - DRIVER can only depend on DRIVER
     * - ADAPTER can depend on DRIVER or ADAPTER
     * - PLUGIN can depend on DRIVER, ADAPTER, or PLUGIN
     * - EXTENSION cannot depend on anything
     */
    fun canDependOn(dependencyType: ExtensionType): Boolean =
        when (this) {
            DRIVER -> dependencyType == DRIVER
            ADAPTER -> dependencyType == DRIVER || dependencyType == ADAPTER
            PLUGIN -> dependencyType == ADAPTER || dependencyType == PLUGIN || dependencyType == DRIVER
            EXTENSION -> false
        }
}
