package org.ttproject.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// A globally accessible object to broadcast refresh events
object NotificationEventBus {
    // extraBufferCapacity = 1 ensures the event doesn't get lost if the app is busy
    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvents = _refreshEvents.asSharedFlow()

    fun triggerRefresh() {
        _refreshEvents.tryEmit(Unit)
    }
}