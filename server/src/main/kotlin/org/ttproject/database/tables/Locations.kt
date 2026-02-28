package org.ttproject.database.tables

import org.jetbrains.exposed.sql.Table

enum class LocationType { Indoor, Outdoor }

object Locations : Table("locations") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val type = enumerationByName("type", 20, LocationType::class)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val address = text("address").nullable()
    val isFree = bool("is_free").default(true)
    val tableCount = integer("table_count").default(1)
    val createdBy = reference("created_by", Users.id).nullable()

    override val primaryKey = PrimaryKey(id)
}