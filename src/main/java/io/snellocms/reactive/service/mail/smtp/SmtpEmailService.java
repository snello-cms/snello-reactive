package io.snellocms.reactive.service.mail.smtp;


import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.snellocms.reactive.service.mail.Email;
import io.snellocms.reactive.service.mail.EmailService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.snellocms.reactive.management.AppConstants.*;

@Singleton
public class SmtpEmailService implements EmailService {

    Logger logger = Logger.getLogger(getClass());

    @Inject
    ReactiveMailer reactiveMailer;

    @ConfigProperty(name = EMAIL_SMTP_PORT)
    String smtp_port;

    @ConfigProperty(name = EMAIL_SMTP_AUTH)
    String smtp_auth;

    @ConfigProperty(name = EMAIL_SMTP_STARTSSL_ENABLE)
    String starttls_enable;

    @ConfigProperty(name = EMAIL_SMTP_HOST)
    String smtp_host;

    @ConfigProperty(name = EMAIL_SMTP_USERNAME)
    String smtp_username;

    @ConfigProperty(name = EMAIL_SMTP_PASSWORD)
    String smtp_password;

    @ConfigProperty(name = EMAIL_MAIL_FROM)
    String mail_from;

    @Override
    public void send(Email email) throws Exception {

        // Step1
        logger.info("\n 1st ===> setup Mail Server Properties..");
//        Properties mailServerProperties = System.getProperties();
//        mailServerProperties.put("mail.smtp.host", smtp_host);
//        mailServerProperties.put("mail.smtp.port", smtp_port);
//        mailServerProperties.put("mail.smtp.auth", smtp_auth);
//        mailServerProperties.put("mail.smtp.socketFactory.port", smtp_port);
//        mailServerProperties.put("mail.smtp.starttls.enable", starttls_enable);
//        mailServerProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        logger.info("Mail Server Properties have been setup successfully..");

        // Step2
        logger.info("\n\n 2nd ===> get Mail Session..");
//        Session session = Session.getInstance(mailServerProperties,
//                new javax.mail.Authenticator() {
//                    protected PasswordAuthentication getPasswordAuthentication() {
//                        return new PasswordAuthentication(smtp_username, smtp_password);
//                    }
//                });
//        MimeMessage mimeMessage = new MimeMessage(session);
//        mimeMessage.setFrom(new InternetAddress(mail_from));
//        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(email.recipient));
//        mimeMessage.setSubject(email.subject);
//        mimeMessage.setContent(email.body, "text/html");
        logger.info("Mail Session has been created successfully..");

        // Step3
        logger.info("\n\n 3rd ===> Get Session and Send mail");
//        Transport.send(mimeMessage);
        Mail mail = Mail.withHtml(
                email.recipient,
                email.subject,
                email.body);
        mail.setFrom(mail_from);
        reactiveMailer.send(mail);
    }

}
