// SPDX-FileCopyrightText: 2026 Kokoroid Contributors
//
// SPDX-Contributor: plan-v2
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dev.kokoroidkt.core.loader

/**
 * Directed acyclic graph (DAG) for managing extension dependencies.
 *
 * Extensions are added via [addNode] and must be resolved via [resolve]
 * before the ordered list can be used for loading.
 *
 * Resolution validates dependencies (type rules, missing refs, duplicates),
 * detects cycles, and produces a topological ordering via Kahn's algorithm.
 */
class DependencyGraph {
    private val nodes: MutableMap<String, ExtensionDescriptor> = mutableMapOf()
    private var addNodeCount: Int = 0

    /**
     * Adds an extension descriptor to the graph.
     *
     * @return this graph for chaining.
     */
    fun addNode(descriptor: ExtensionDescriptor): DependencyGraph {
        nodes[descriptor.identifier] = descriptor
        addNodeCount++
        return this
    }

    /**
     * Resolves the dependency graph by validating dependencies, detecting cycles,
     * and computing a topological ordering using Kahn's algorithm.
     *
     * @return A [ResolvedDependencyGraph] containing the topologically sorted extensions.
     * @throws DependencyGraphException if:
     * - A circular dependency is detected ([DependencyErrorType.CIRCULAR_DEPENDENCY])
     * - A referenced dependency does not exist ([DependencyErrorType.MISSING_DEPENDENCY])
     * - A dependency type violates rules ([DependencyErrorType.INVALID_DEPENDENCY_TYPE])
     * - A duplicate identifier is found ([DependencyErrorType.DUPLICATE_EXTENSION])
     */
    fun resolve(): ResolvedDependencyGraph {
        validateNoDuplicates()

        for ((identifier, descriptor) in nodes) {
            for (depId in descriptor.dependencyIdentifiers) {
                val depDescriptor =
                    nodes[depId]
                        ?: throw DependencyGraphException(
                            "Extension '$identifier' depends on '$depId' which is not registered",
                            DependencyErrorType.MISSING_DEPENDENCY,
                        )
                if (!descriptor.type.canDependOn(depDescriptor.type)) {
                    throw DependencyGraphException(
                        "Extension '$identifier' of type ${descriptor.type} cannot depend on " +
                            "'${depDescriptor.identifier}' of type ${depDescriptor.type}",
                        DependencyErrorType.INVALID_DEPENDENCY_TYPE,
                    )
                }
            }
        }

        val result = topologicalSort()

        if (result.size != nodes.size) {
            throw DependencyGraphException(
                "Circular dependency detected among extensions",
                DependencyErrorType.CIRCULAR_DEPENDENCY,
            )
        }

        return ResolvedDependencyGraph(
            orderedExtensions = result,
            classLoaderMap = emptyMap(),
        )
    }

    private fun validateNoDuplicates() {
        if (addNodeCount != nodes.size) {
            throw DependencyGraphException(
                "Duplicate extension identifier detected",
                DependencyErrorType.DUPLICATE_EXTENSION,
            )
        }
    }

    /**
     * Kahn's algorithm for topological sort.
     * Returns nodes in topologically sorted order if possible.
     */
    private fun topologicalSort(): List<ExtensionDescriptor> {
        val inDegree = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        // Initialize all nodes
        for (id in nodes.keys) {
            inDegree[id] = 0
            adjacency[id] = mutableListOf()
        }

        // Build adjacency: edge from dep → dependent
        for ((identifier, descriptor) in nodes) {
            for (depId in descriptor.dependencyIdentifiers) {
                if (depId in nodes) {
                    adjacency[depId]!!.add(identifier)
                    inDegree[identifier] = inDegree[identifier]!! + 1
                }
            }
        }

        // Start with nodes that have zero in-degree
        val queue = ArrayDeque<String>()
        for ((id, degree) in inDegree) {
            if (degree == 0) {
                queue.addLast(id)
            }
        }

        // Process the queue
        val sorted = mutableListOf<ExtensionDescriptor>()
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            sorted.add(nodes[current]!!)
            for (neighbor in adjacency[current]!!) {
                val newDegree = inDegree[neighbor]!! - 1
                inDegree[neighbor] = newDegree
                if (newDegree == 0) {
                    queue.addLast(neighbor)
                }
            }
        }

        return sorted
    }

    /**
     * Result of a successful dependency graph resolution.
     *
     * @property orderedExtensions Extensions in topological (load) order.
     * @property classLoaderMap Map from extension identifier to its [ClassLoader].
     *   Populated by [ExtensionPreloader] after resolution.
     */
    data class ResolvedDependencyGraph(
        val orderedExtensions: List<ExtensionDescriptor>,
        val classLoaderMap: Map<String, ClassLoader>,
    ) {
        /**
         * Returns a human-readable string of the topological order.
         */
        fun topologicalOrderString(): String =
            buildString {
                appendLine("Topological Order:")
                orderedExtensions.forEachIndexed { index, desc ->
                    appendLine("  ${index + 1}. [${desc.type}] ${desc.identifier}")
                }
            }
    }

    /**
     * Returns a human-readable dump of the full dependency graph structure.
     * Shows each node, its type, and its declared dependencies.
     */
    fun dumpGraph(): String =
        buildString {
            appendLine("=== Dependency Graph ===")
            appendLine("Nodes (${nodes.size}):")
            for ((id, desc) in nodes.entries.sortedBy { it.key }) {
                append("- [${desc.type}] $id")
                if (desc.dependencyIdentifiers.isEmpty()) {
                    appendLine(" (no dependencies)")
                } else {
                    appendLine()
                    for (depId in desc.dependencyIdentifiers) {
                        val resolved = if (depId in nodes) "" else " [UNRESOLVED]"
                        appendLine("    └─ depends on: $depId$resolved")
                    }
                }
            }
        }
}
