package com.scasp.rmi;

import java.io.Serial;

/**
 * Business/authorisation failure reported to the remote client with a safe message (no server stack details).
 * It is a checked application exception, NOT a RemoteException: RMI wraps RemoteExceptions thrown by a remote
 * method in ServerException, whereas an application exception declared on the interface arrives unchanged.
 */
public class RemoteAdminException extends Exception {
    @Serial private static final long serialVersionUID = 1L;
    public RemoteAdminException(String message) { super(message); }
}
