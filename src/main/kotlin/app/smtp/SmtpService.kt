package app.smtp

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService(private val smtpConfig: SmtpConfig) {

    fun sendEmail(to: String, subject: String, body: String) {
        val props = Properties().apply {
            put("mail.smtp.host", smtpConfig.host)
            put("mail.smtp.port", smtpConfig.port)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpConfig.username, smtpConfig.password)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(smtpConfig.fromEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                setText(body)
            }

            Transport.send(message)
        } catch (e: MessagingException) {
            e.printStackTrace()
        }
    }
}