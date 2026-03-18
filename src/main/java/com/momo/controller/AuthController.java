package com.momo.controller;

import com.momo.auth.AuthTokenStore;
import com.momo.dto.AuthLoginRequest;
import com.momo.dto.AuthResponse;
import com.momo.dto.AuthSignupRequest;
import com.momo.model.AccountHolder;
import com.momo.store.JdbcStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JdbcStore store;
    private final AuthTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(JdbcStore store, AuthTokenStore tokenStore) {
        this.store = store;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody AuthSignupRequest request) {
        if (store.getHolderByEmail(request.email()) != null) {
            throw new IllegalStateException("Email already exists");
        }

        AccountHolder holder = store.saveHolder(request.fullName(), request.email());
        store.saveCredentials(holder.id(), passwordEncoder.encode(request.password()));
        String token = tokenStore.issueToken(holder.id());
        return new AuthResponse(holder.id(), token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
        AccountHolder holder = store.getHolderByEmail(request.email());
        if (holder == null) {
            throw new IllegalStateException("Invalid login credentials");
        }

        String passwordHash = store.getPasswordHashByHolderId(holder.id());
        if (passwordHash == null || !passwordEncoder.matches(request.password(), passwordHash)) {
            throw new IllegalStateException("Invalid login credentials");
        }

        String token = tokenStore.issueToken(holder.id());
        return new AuthResponse(holder.id(), token);
    }
}
