package com.joprelys.backend.notification.infrastructure.mail;

import java.util.Properties;
import org.springframework.core.env.Environment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration(proxyBeanMethods = false)
public class MailSenderConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    JavaMailSender javaMailSender(Environment environment) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(environment.getProperty("spring.mail.host", "mail.joprelys.com"));
        sender.setPort(environment.getProperty("spring.mail.port", Integer.class, 465));
        sender.setUsername(environment.getProperty("spring.mail.username", "noreply@joprelys.com"));
        sender.setPassword(environment.getProperty("spring.mail.password", ""));
        sender.setJavaMailProperties(mailProperties(environment));
        return sender;
    }

    private static Properties mailProperties(Environment environment) {
        Properties properties = new Properties();
        setProperty(properties, environment, "auth", "true");
        setProperty(properties, environment, "ssl.enable", "true");
        setProperty(properties, environment, "starttls.enable", "false");
        setProperty(properties, environment, "connectiontimeout", "3000");
        setProperty(properties, environment, "timeout", "3000");
        setProperty(properties, environment, "writetimeout", "3000");
        return properties;
    }

    private static void setProperty(Properties properties, Environment environment, String name, String fallback) {
        String key = "mail.smtp." + name;
        properties.setProperty(key, environment.getProperty("spring.mail.properties." + key, fallback));
    }
}
