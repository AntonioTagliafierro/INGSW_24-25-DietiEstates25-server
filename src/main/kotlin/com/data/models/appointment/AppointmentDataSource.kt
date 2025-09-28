package com.data.models.appointment

interface AppointmentDataSource {
    suspend fun createAppointemnt(appointment: Appointment, firstMessage: AppointmentMessage): Boolean
    suspend fun addAppointmentMessage(appointmentId: String, newMessage: AppointmentMessage): Boolean
    suspend fun acceptAppointment(appointmentId: String):Boolean
    suspend fun declineAppointment(appointmentId: String):Boolean
    suspend fun getAllAppointments(): List<AppointmentSummary>
    suspend fun getAppointmentsByUserOrAgent(userId: String): List<Appointment>
    suspend fun getAppointmentByListingId(ListingId: String): List<Appointment>

}