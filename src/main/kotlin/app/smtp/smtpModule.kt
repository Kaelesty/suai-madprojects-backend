package app.smtp

import org.koin.dsl.module

val smtpModule = module {

    single<EmailService> {
        EmailService(SmtpConfig)
    }
}