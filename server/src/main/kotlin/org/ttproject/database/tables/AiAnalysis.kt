package org.ttproject.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

enum class StrokeType { Forehand, Backhand, Serve, Footwork }
enum class AiStatus { Uploading, Processing, Completed, Failed }

object AiAnalyses : Table("ai_analyses") {
    val id = uuid("id").autoGenerate()
    val userId = reference("user_id", Users.id)
    val videoUrl = text("video_url")
    val strokeType = enumerationByName("stroke_type", 20, StrokeType::class)
    val status = enumerationByName("status", 20, AiStatus::class).default(AiStatus.Uploading)

    // Store JSON as a string for now. Can be upgraded to true JSONB later.
    val metrics = text("metrics").nullable()
    val coachFeedback = text("coach_feedback").nullable()
    val thumbnailUrl = text("thumbnail_url").nullable()

    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}