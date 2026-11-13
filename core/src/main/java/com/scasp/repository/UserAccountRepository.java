package com.scasp.repository;

import java.util.Optional;

public interface UserAccountRepository {
    Optional<UserAccount> findAccount(String userId);
    void updatePasswordHash(String userId, String passwordHash);
}
