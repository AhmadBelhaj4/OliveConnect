package tn.oliveconnect.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oliveconnect.jwt")
public record JwtProperties(String secret, long expirationSeconds) {}
