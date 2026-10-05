package com.joprelys.backend.notification.infrastructure.mail;

public record BrandedMailContent(String subject, String plainText, String html) {}
