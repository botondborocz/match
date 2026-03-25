package org.ttproject.database.places_import

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.batchInsert
import org.ttproject.database.tables.LocationType
import org.ttproject.database.tables.Locations
import java.io.File

fun importGeoJsonToDatabase(filePath: String) {
    // 1. Read and parse the JSON file
    val jsonString = File(filePath).readText()
    val collection = Json { ignoreUnknownKeys = true }.decodeFromString<GeoJsonCollection>(jsonString)

    // Helper function to safely extract strings from the JsonObject
    fun JsonObject.getString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    transaction {
        // 2. Use batchInsert for fast, bulk database operations
        Locations.batchInsert(collection.features, shouldReturnGeneratedValues = false) { feature ->
            val props = feature.properties
            val coords = feature.geometry.coordinates

            // --- MAP LAT / LON ---
            // GeoJSON coordinates are always [Longitude, Latitude]
            this[Locations.longitude] = coords[0]
            this[Locations.latitude] = coords[1]

            // --- MAP NAME ---
            // Fallback to a generic name if missing, and truncate to your 100 char limit
            val rawName = props.getString("name") ?: "Table Tennis Table"
            this[Locations.name] = rawName.take(100)

            // --- MAP TYPE (Indoor / Outdoor) ---
            // OSM uses various tags to denote indoor spaces. If any are true, it's indoor.
            val isIndoor = props.getString("indoor") == "room" ||
                    props.getString("indoor") == "yes" ||
                    props.getString("building") == "yes" ||
                    props.getString("leisure") == "sports_centre" ||
                    props.getString("leisure") == "sports_hall"

            this[Locations.type] = if (isIndoor) LocationType.Indoor else LocationType.Outdoor

            // --- MAP IS FREE ---
            // If there's a fee or charge tag, it's not free. Otherwise, assume free.
            val hasFee = props.getString("fee") == "yes" || props.getString("charge") != null
            this[Locations.isFree] = !hasFee

            // --- MAP TABLE COUNT ---
            // OSM mappers use different tags for the number of tables. We check the most common ones.
            val tableCountStr = props.getString("table_tennis:tables")
                ?: props.getString("number_of_tables")
                ?: props.getString("capacity")
                ?: props.getString("count")
                ?: "1"

            this[Locations.tableCount] = tableCountStr.toIntOrNull() ?: 1

            // --- MAP ADDRESS ---
            // Combine OSM address parts into a single string
            val city = props.getString("addr:city")
            val street = props.getString("addr:street")
            val houseNum = props.getString("addr:housenumber")

            val fullAddress = listOfNotNull(city, street, houseNum).joinToString(", ")
            this[Locations.address] = fullAddress.ifBlank { null }

            // createdBy remains null as per your schema configuration for bulk imports
        }
    }

    println("Successfully imported ${collection.features.size} locations!")
}

// Example execution
fun main() {
    val file = File("server/src/main/resources/table_tennis_locations.json")

    println("Looking for file at: ${file.absolutePath}")

    if (!file.exists()) {
        println("❌ Error: File not found!")
        return
    }
    Database.connect(
        url = "jdbc:postgresql://localhost:5433/match_db",
        user = "ktor_user",
        password = "ktor_password"
    )
    importGeoJsonToDatabase(file.path)
}