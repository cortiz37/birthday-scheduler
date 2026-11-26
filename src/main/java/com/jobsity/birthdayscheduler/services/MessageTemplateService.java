package com.jobsity.birthdayscheduler.services;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Map;

@Service
public class MessageTemplateService {

    private final Configuration freemarkerConfiguration;

    public MessageTemplateService(Configuration freemarkerConfiguration) {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    public String render(String templateName, Map<String, Object> model) {
        try {
            Template template = freemarkerConfiguration.getTemplate(templateName);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (IOException | TemplateException ex) {
            throw new IllegalStateException("Failed to render message template '" + templateName + "'", ex);
        }
    }
}
