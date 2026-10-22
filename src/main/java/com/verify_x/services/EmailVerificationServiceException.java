
package com.verify_x.services;

public class EmailVerificationServiceException extends RuntimeException {

    public EmailVerificationServiceException(String message) {
        super(message);
    }

    public EmailVerificationServiceException(
            String message, Throwable cause) {
        super(message, cause);
    }
}
