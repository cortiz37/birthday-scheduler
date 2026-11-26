package com.jobsity.birthdayscheduler.services;

import freemarker.template.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageTemplateServiceTest {

    private MessageTemplateService service;

    @BeforeEach
    void setUp() {
        Configuration freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_34);
        freemarkerConfiguration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/templates");
        freemarkerConfiguration.setDefaultEncoding("UTF-8");

        service = new MessageTemplateService(freemarkerConfiguration);
    }

    @Test
    void rendersTemplateAsPlainTextWithoutHtmlEscaping() {
        String rendered = service.render("birthday-email.ftl", Map.of("firstName", "D'Artagnan"));

        assertThat(rendered).isEqualTo("Happy birthday, dear D'Artagnan!");
    }

    @Test
    void throwsIllegalStateExceptionWhenTemplateIsMissing() {
        assertThatThrownBy(() -> service.render("missing-template.ftl", Map.of("firstName", "Alice")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to render message template 'missing-template.ftl'");
    }

    @Test
    void throwsIllegalStateExceptionWhenRequiredModelVariableIsMissing() {
        assertThatThrownBy(() -> service.render("birthday-email.ftl", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to render message template 'birthday-email.ftl'");
    }
}
