package com.derscalismatakibi.app.backup

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.AuthenticationFailedException
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * study_tracker2.py'de karsiligi yok - Android'e ozgu, kullaniciyla netlesen
 * "ayni Gmail hesabi hem gonderen hem alici" tasarimini uygular. Host/port TEK
 * bir yerde sabit tutuluyor ki ileride farkli bir saglayiciya gecmek kolay olsun.
 */
object SmtpBackupSender {
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"

    sealed class Result {
        data object Success : Result()
        data class TransientFailure(val message: String) : Result()
        data class PermanentFailure(val message: String) : Result()
    }

    fun send(toAndFrom: String, appPassword: String, attachments: List<File>): Result {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", SMTP_HOST)
            put("mail.smtp.port", SMTP_PORT)
        }
        val session = Session.getInstance(
            props,
            object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(toAndFrom, appPassword)
            },
        )
        return try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(toAndFrom))
                setRecipient(Message.RecipientType.TO, InternetAddress(toAndFrom))
                subject = "Ders Calisma Takibi - Gunluk Yedek (${todayLabel()})"
            }
            val body = MimeMultipart().apply {
                addBodyPart(
                    MimeBodyPart().apply {
                        setText("Ekte ${todayLabel()} tarihli otomatik gunluk yedek bulunuyor.")
                    }
                )
                for (file in attachments) {
                    addBodyPart(
                        MimeBodyPart().apply {
                            dataHandler = DataHandler(FileDataSource(file))
                            fileName = file.name
                        }
                    )
                }
            }
            message.setContent(body)
            Transport.send(message)
            Result.Success
        } catch (e: AuthenticationFailedException) {
            Result.PermanentFailure("kimlik dogrulama hatasi - uygulama sifresini kontrol et")
        } catch (e: MessagingException) {
            Result.TransientFailure(e.message ?: "gonderim hatasi")
        }
    }

    private fun todayLabel(): String = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date())
}
