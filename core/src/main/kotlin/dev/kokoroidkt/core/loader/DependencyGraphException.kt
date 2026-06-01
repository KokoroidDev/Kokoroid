// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

/**
 * Exception thrown when dependency graph resolution fails.
 *
 * @property errorType The specific category of error that occurred.
 */
class DependencyGraphException(
    override val message: String,
    val errorType: DependencyErrorType,
) : Exception()

/**
 * Categorizes the type of error that can occur during dependency resolution.
 */
enum class DependencyErrorType {
    CIRCULAR_DEPENDENCY,
    MISSING_DEPENDENCY,
    INVALID_DEPENDENCY_TYPE,
    DUPLICATE_EXTENSION,
}
