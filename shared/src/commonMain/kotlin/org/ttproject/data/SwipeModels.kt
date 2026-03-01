package org.ttproject.data

import kotlinx.serialization.Serializable

@Serializable
data class SwipeRequest(
    val isLiked: Boolean
)

@Serializable
data class SwipeResponse(
    val isMatch: Boolean
)