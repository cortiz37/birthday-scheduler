package com.jobsity.birthdayscheduler.services;

import com.jobsity.birthdayscheduler.domain.EmailMessage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Map;

@Service
public class MessageTemplateService {

    private static final Logger log = LoggerFactory.getLogger(MessageTemplateService.class);

    private final JavaMailSender mailSender;
    private final Configuration freemarkerConfiguration;
    private final String fromAddress;

    public MessageTemplateService(JavaMailSender mailSender,
                                  Configuration freemarkerConfiguration,
                                  @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.freemarkerConfiguration = freemarkerConfiguration;
        this.fromAddress = fromAddress;
    }

    public void sendTemplatedEmail(String recipient,
                                   String subject,
                                   String templateName,
                                   Map<String, Object> model) {
        String body = renderTemplate(templateName, model);
        sendEmail(new EmailMessage(recipient, subject, body));
    }

    private String renderTemplate(String templateName, Map<String, Object> model) {
        try {
            Template template = freemarkerConfiguration.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (IOException | TemplateException ex) {
            throw new IllegalStateException("Failed to render email template '" + templateName + "'", ex);
        }
    }

    private void sendEmail(EmailMessage message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(message.recipient());
            mail.setSubject(message.subject());
            mail.setText(message.body());
            mailSender.send(mail);
        } catch (MailException ex) {
            log.error("Failed to send email to {}", message.recipient(), ex);
        }
    }
}
