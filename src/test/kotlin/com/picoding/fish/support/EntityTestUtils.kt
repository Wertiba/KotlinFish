package com.picoding.fish.support

import com.picoding.fish.database.models.AuditableEntity
import com.picoding.fish.database.models.BaseEntity
import com.picoding.fish.database.models.User
import java.time.Instant
import java.util.UUID

private fun <T : BaseEntity> T.assignId(id: UUID): T {
    val field = BaseEntity::class.java.getDeclaredField("id")
    field.isAccessible = true
    field.set(this, id)
    return this
}

private fun <T : BaseEntity> T.assignCreatedAt(createdAt: Instant): T {
    val field = BaseEntity::class.java.getDeclaredField("createdAt")
    field.isAccessible = true
    field.set(this, createdAt)
    return this
}

private fun <T : AuditableEntity> T.assignUpdatedAt(updatedAt: Instant): T {
    val field = AuditableEntity::class.java.getDeclaredField("updatedAt")
    field.isAccessible = true
    field.set(this, updatedAt)
    return this
}

/** Simulates what JPA does on insert: assigns id/createdAt/updatedAt and the @PrePersist default for createdBy. */
fun User.persisted(
    id: UUID = UUID.randomUUID(),
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = createdAt,
): User {
    assignId(id)
    assignCreatedAt(createdAt)
    assignUpdatedAt(updatedAt)
    if (createdBy == null) createdBy = id
    return this
}
