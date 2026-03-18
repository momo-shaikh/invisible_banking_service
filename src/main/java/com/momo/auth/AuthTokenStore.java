package com.momo.auth;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenStore {
    private final SecureRandom tokenGenerator = new SecureRandom();
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public String issueToken(long holderId) {
        byte[] value = new byte[32];
        tokenGenerator.nextBytes(value);
        String token = HexFormat.of().formatHex(value);
        tokens.put(token, holderId);
        return token;
    }

    public Long getHolderId(String token) {
        return tokens.get(token);
    }
}
