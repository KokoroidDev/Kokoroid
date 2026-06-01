// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.exceptions

class ExtensionDependencyException(
    val extensionId: String,
    val missingDependencyId: String,
    override val message: String = "Extension '$extensionId' depends on '$missingDependencyId' which is not available",
) : CoreException()
