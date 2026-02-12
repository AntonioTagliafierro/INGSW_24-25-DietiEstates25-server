package com.service.mailservice

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
@JsonInclude(JsonInclude.Include.NON_NULL)
data class EmailRequest(
    val from: Recipient,
    val to: List<Recipient>,
    val subject: String,
    val text: String? = null,
    val html: String? = null,
    val variables: List<Variable>? = null,
    @JsonProperty("template_id") val templateId: String? = null,
    val personalization: List<CustomPersonalization>? = null,
) {
    data class Recipient(
        val email: String,
        val name: String
    )

    data class Variable(
        val email: String,
        val substitutions: List<Substitution>
    ) {
        data class Substitution(
            @JsonProperty("var") val variable: String,
            val value: String
        )
    }

    data class CustomPersonalization(
        val email: String,
        val data: Map<String, Any>
    )
}

