package com.scasp.web;

import com.scasp.model.Role;
import com.scasp.repository.UserAccount;
import com.scasp.repository.UserAccountRepository;
import com.scasp.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class WebSecurityTest {
    static final String HASH = PasswordHasher.hash("student123".toCharArray());
    final Map<String, UserAccount> store = Map.of(
            "STU001", new UserAccount("STU001", "Alex Banda", Role.STUDENT, true, HASH),
            "STU009", new UserAccount("STU009", "Suspended", Role.STUDENT, false, HASH),
            "STAFF01", new UserAccount("STAFF01", "Grace Tembo", Role.STAFF, true, PasswordHasher.hash("staff123".toCharArray())));
    final Authenticator auth = new Authenticator(new UserAccountRepository() {
        public Optional<UserAccount> findAccount(String id) { return Optional.ofNullable(store.get(id)); }
        public void updatePasswordHash(String id, String h) { throw new UnsupportedOperationException(); }
    });

    @Test void hashedPasswordsVerify() {
        assertTrue(PasswordHasher.verify("student123".toCharArray(), HASH));
        assertFalse(PasswordHasher.verify("Student123".toCharArray(), HASH));
        assertNotEquals(HASH, PasswordHasher.hash("student123".toCharArray()), "random salt");
        assertFalse(PasswordHasher.verify("x".toCharArray(), "plain-text"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), "pbkdf2-sha256$1$!!$!!"));
    }
    @Test void authenticationUsesTheUserStore() {
        CurrentUser u = auth.authenticate("stu001", "student123".toCharArray()).orElseThrow();
        assertEquals("STU001", u.userId()); assertEquals(Role.STUDENT, u.role()); assertFalse(u.isStaff());
        assertTrue(auth.authenticate("STAFF01", "staff123".toCharArray()).orElseThrow().isStaff());
        assertTrue(auth.authenticate("STU001", "wrong".toCharArray()).isEmpty());
        assertTrue(auth.authenticate("NOBODY", "student123".toCharArray()).isEmpty());
        assertTrue(auth.authenticate("STU009", "student123".toCharArray()).isEmpty(), "inactive account");
        assertTrue(auth.authenticate("' OR 1=1 --", "x".toCharArray()).isEmpty(), "malformed id rejected before lookup");
        assertTrue(auth.authenticate("STU001", new char[0]).isEmpty());
    }
    @Test void validationCollectsFieldErrors() {
        Validation.Errors e = new Validation.Errors();
        assertEquals("CSC301", e.code("courseCode", " csc301 "));
        assertFalse(e.any());
        e.code("courseCode", "bad code!"); e.integer("capacity", "abc", 1, 10); e.integer("priority", "9", 1, 5); e.text("title", "ab", 3, 10);
        assertEquals(4, e.asMap().size());
        assertEquals("Must be a whole number", e.asMap().get("capacity"));
        assertTrue(Validation.isId("STAFF01")); assertFalse(Validation.isId("<script>"));
    }
    @Test void csrfTokensAreRandom() {
        assertNotEquals(AuthFilter.newToken(), AuthFilter.newToken());
        assertTrue(AuthFilter.newToken().length() >= 32);
    }
}
