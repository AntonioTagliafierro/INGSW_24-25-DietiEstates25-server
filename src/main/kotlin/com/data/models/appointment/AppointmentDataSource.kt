package com.data.models.appointment



interface AppointmentDataSource {
    suspend fun createAppointemnt(appointment: Appointment, firstMessage: AppointmentMessage): Boolean
    suspend fun addAppointmentMessage(appointmentId: String, newMessage: AppointmentMessage): Boolean
    suspend fun acceptAppointment(appointmentId: String):Boolean
    suspend fun declineAppointment(appointmentId: String):Boolean
    suspend fun getAppointmentsByUserOrAgent(userId: String): List<Appointment>
    suspend fun getAppointment(propertyId: String , buyerName: String): Appointment?
    suspend fun getAppointment(propertyId: String): Appointment?
    suspend fun getSummaryAppointments(propertyId :String): List<AppointmentSummary>
    suspend fun getAppointmentsByListing(listingId: String): List<Appointment>
    suspend fun getAppointmentsByUserAndListing(userId: String, listingId: String): List<Appointment>

}