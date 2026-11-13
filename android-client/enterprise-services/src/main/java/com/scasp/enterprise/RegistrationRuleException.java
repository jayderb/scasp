package com.scasp.enterprise;

import jakarta.ejb.ApplicationException;

/**
 * Business-rule failure (duplicate, unknown course, inactive course ...). As an application exception with
 * rollback=true it reaches the client unwrapped AND the container rolls the JTA transaction back.
 */
@ApplicationException(rollback = true)
public class RegistrationRuleException extends RuntimeException {
    private final boolean notFound;
    public RegistrationRuleException(String message, boolean notFound) { super(message); this.notFound = notFound; }
    public boolean isNotFound() { return notFound; }
}
