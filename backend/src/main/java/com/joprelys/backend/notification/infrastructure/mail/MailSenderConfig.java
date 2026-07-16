package com.joprelys.backend.notification.infrastructure.mail;

import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration(proxyBeanMethods = false)
public class MailSenderConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    JavaMailSender javaMailSender(
            @Value("${spring.mail.host:mail.joprelys.com}") String host,
            @Value("${spring.mail.port:465}") int port,
            @Value("${spring.mail.username:noreply@joprelys.com}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean authenticationEnabled,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}") boolean sslEnabled,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean startTlsEnabled) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setJavaMailProperties(mailProperties(authenticationEnabled, sslEnabled, startTlsEnabled));
        return sender;
    }

    private static Properties mailProperties(
            boolean authenticationEnabled,
            boolean sslEnabled,
            boolean startTlsEnabled) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.auth", Boolean.toString(authenticationEnabled));
        properties.setProperty("mail.smtp.ssl.enable", Boolean.toString(sslEnabled));
        properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(startTlsEnabled));
        return properties;
    }
}
