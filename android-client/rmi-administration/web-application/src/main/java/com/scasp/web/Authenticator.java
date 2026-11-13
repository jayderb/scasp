package com.scasp.web;

import com.scasp.repository.UserAccount;
import com.scasp.repository.UserAccountRepository;
import com.scasp.security.PasswordHasher;

import java.util.Optional;

/**
 * Checks credentials against the user store (app_user.password_hash, PBKDF2). Unknown user and wrong password
 * produce the same result, and a dummy hash is verified for unknown users so response time does not reveal
 * which user ids exist.
 */
public class Authenticator {
    private static final String DUMMY = PasswordHasher.hash("dummy-password".toCharArray());
    private final UserAccountRepository accounts;
    public Authenticator(UserAccountRepository accounts) { this.accounts = accounts; }

    public Optional<CurrentUser> authenticate(String userId, char[] password) {
        if (!Validation.isId(userId) || password == null || password.length == 0 || password.length > 128) return Optional.empty();
        Optional<UserAccount> acc = accounts.findAccount(userId.trim().toUpperCase());
        if (acc.isEmpty() || acc.get().passwordHash() == null) { PasswordHasher.verify(password, DUMMY); return Optional.empty(); }
        UserAccount a = acc.get();
        if (!PasswordHasher.verify(password, a.passwordHash()) || !a.active()) return Optional.empty();
        return Optional.of(new CurrentUser(a.userId(), a.fullName(), a.role()));
    }
}
