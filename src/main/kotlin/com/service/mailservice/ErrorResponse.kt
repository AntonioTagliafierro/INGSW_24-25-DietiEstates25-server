package com.service.mailservice

data class ErrorResponse(
    val errors: Map<String, List<String>>? = null,
    val message: String? = null
)
