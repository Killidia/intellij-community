// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.project

import com.intellij.openapi.project.Project
import com.jetbrains.rhizomedb.*
import fleet.kernel.DurableEntityType
import org.jetbrains.annotations.ApiStatus

/**
 * Represents a project entity that can be shared between backend and frontends.
 * The entity is created on project initialization before any services and components are loaded.
 *
 * To convert a project to the entity use [asEntityOrNull] or [asEntity]
 */
@ApiStatus.Internal
data class ProjectEntity(override val eid: EID) : Entity {
  /**
   * Represents a unique identifier for a [Project].
   * This [ProjectId] can be shared between frontend and backend.
   */
  val projectId: ProjectId by ProjectIdValue

  internal companion object : DurableEntityType<ProjectEntity>(ProjectEntity::class.java.name, "com.intellij", ::ProjectEntity) {
    val ProjectIdValue = requiredValue("projectId", ProjectId.serializer(), Indexing.UNIQUE)
  }
}

/**
 * Represents a local project entity that can be used to retrieve [Project] instance.
 * The entity is created on project initialization before any services and components are loaded.
 *
 * To convert a project to the entity use [asEntityOrNull] or [asEntity]
 */
@ApiStatus.Internal
data class LocalProjectEntity(override val eid: EID) : Entity {
  val sharedEntity: ProjectEntity by ProjectEntityValue
  val project: Project by ProjectValue

  companion object : EntityType<LocalProjectEntity>(LocalProjectEntity::class, ::LocalProjectEntity) {
    val ProjectEntityValue = requiredRef<ProjectEntity>("sharedEntity", RefFlags.CASCADE_DELETE_BY)
    val ProjectValue = requiredTransient<Project>("project", Indexing.INDEXED)
  }
}

/**
 * Converts a given project to its corresponding [LocalProjectEntity].
 *
 * The method has to be called in a kernel context - see [com.intellij.platform.kernel.withKernel]
 *
 * @return The [LocalProjectEntity] instance associated with the provided project,
 *         or null if no such entity is found
 */
@ApiStatus.Internal
fun Project.asLocalEntityOrNull(): LocalProjectEntity? {
  return entities<LocalProjectEntity, Project>(LocalProjectEntity.ProjectValue, this).singleOrNull()
}

/**
 * Converts a given project to its corresponding [LocalProjectEntity].
 *
 * The method has to be called in a kernel context - see [com.intellij.platform.kernel.withKernel]
 *
 * @return The [LocalProjectEntity] instance associated with the provided project,
 * @throws kotlin.IllegalStateException if no such entity is found
 */
@ApiStatus.Internal
fun Project.asLocalEntity(): LocalProjectEntity {
  return asLocalEntityOrNull() ?: error("LocalProjectEntity is not found for $this")
}

/**
 * Converts a given project to its corresponding [ProjectEntity].
 *
 * The method has to be called in a kernel context - see [com.intellij.platform.kernel.withKernel]
 *
 * @return The [ProjectEntity] instance associated with the provided project,
 *         or null if no such entity is found
 */
@ApiStatus.Internal
fun Project.asEntityOrNull(): ProjectEntity? {
  return asLocalEntityOrNull()?.sharedEntity
}

/**
 * Converts a given project to its corresponding [ProjectEntity].
 *
 * The method has to be called in a kernel context - see [com.intellij.platform.kernel.withKernel]
 *
 * @return The [ProjectEntity] instance associated with the provided project,
 * @throws kotlin.IllegalStateException if no such entity is found
 */
@ApiStatus.Internal
fun Project.asEntity(): ProjectEntity {
  return asEntityOrNull() ?: error("ProjectEntity is not found for $this")
}

/**
 * Converts a given project entity to its corresponding [Project].
 *
 * The method has to be called in a kernel context - see [com.intellij.platform.kernel.withKernel]
 *
 * @return The [Project] instance associated with the provided entity,
 *         or null if no such project is found (for example, if [ProjectEntity] doesn't exist anymore).
 */
@ApiStatus.Internal
fun ProjectEntity.asProjectOrNull(): Project? {
  return entities<LocalProjectEntity, ProjectEntity>(LocalProjectEntity.ProjectEntityValue, this).singleOrNull()?.project
}

/**
 * Converts a given project entity to its corresponding [Project].
 *
 * The method has to be called in a kernel context - see [com.intellij.platform.kernel.withKernel]
 *
 * @return The [Project] instance associated with the provided entity,
 * @throws IllegalStateException if no such project is found (for example, if [ProjectEntity] doesn't exist anymore).
 */
@ApiStatus.Internal
fun ProjectEntity.asProject(): Project {
  return asProjectOrNull() ?: error("Project is not found for $this")
}

