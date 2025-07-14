package com.service

import javax.mail.Session
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Transport
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication



class EmailService {

    fun sendPasswordEmail(toEmail: String, generatedPassword: String) {
        val fromEmail = "dietiestates25@gmail.com"
        val password = System.getenv("EMAIL_PASSWORD")

        val properties = System.getProperties()
        properties["mail.smtp.host"] = "smtp.gmail.com"
        properties["mail.smtp.port"] = "587"
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"

        val session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                // Converte la password in un CharArray
                return PasswordAuthentication(fromEmail, password)
            }
        })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(fromEmail))
            message.addRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
            message.subject = "Welcome to DIETIESTATES25!"
            message.setText("Your admin account has been created successfully.\n\nYour password is: $generatedPassword")

            Transport.send(message)
            println("Email sent successfully")
        } catch (e: MessagingException) {
            e.printStackTrace()
        }
    }


}