package com.data.models.appointment

interface AppointmentDataSource {
    suspend fun insertAppointment(appointment: Appointment): Boolean
    suspend fun getAppointmentsByAgent(email: String): List<Appointment>
    suspend fun updateStatus(id: String, status: AppointmentStatus): Boolean
    suspend fun getAppointmentById(id: String): Appointment?
    suspend fun getAppointmentsByUserEmail(email: String): List<Appointment>
}