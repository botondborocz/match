package org.ttproject.database.tables

import org.jetbrains.exposed.sql.Table

object MessageReactions : Table("message_reactions") {
    val messageId = uuid("message_id").references(Messages.id)
    val userId = uuid("user_id").references(Users.id)
    val emoji = varchar("emoji", 10)

    // Ensures a user can only leave ONE reaction per message (will overwrite if they change it)
    init {
        uniqueIndex(messageId, userId)
    }
}