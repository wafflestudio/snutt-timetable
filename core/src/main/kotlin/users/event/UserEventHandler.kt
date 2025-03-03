package com.wafflestudio.snutt.users.event

import com.wafflestudio.snutt.timetables.service.TimetableService
import com.wafflestudio.snutt.users.event.data.SignupEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class UserEventHandler(
    private val timetableService: TimetableService,
) {
    @EventListener
    suspend fun handleSignupEvent(event: SignupEvent) {
        val defaultTimetable = timetableService.createDefaultTable(event.userId)
        timetableService.setPrimary(event.userId, defaultTimetable.id!!)
    }
}
