package com.data.models.appointment

import com.data.models.appointment.AppointmentStatus


data class UpdateStatusRequest(
    val status: AppointmentStatus
)