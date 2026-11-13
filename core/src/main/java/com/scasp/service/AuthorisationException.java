package com.scasp.service;

/** Thrown when an actor attempts an operation their role does not permit. Mapped to HTTP 403 by the web tier. */
public class AuthorisationException extends RuntimeException {
    public AuthorisationException(String message) { super(message); }
}
