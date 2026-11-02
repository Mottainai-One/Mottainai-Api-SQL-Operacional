package com.institutojf.mottainai.exception;

public class InvitationDeliveryException extends RuntimeException {

    public InvitationDeliveryException(Integer employeeId, Throwable cause) {
        super("Employee " + employeeId + " was saved inactive, but the invitation could not be delivered. "
                + "Use POST /api/v1/employees/" + employeeId + "/invite to try again.", cause);
    }

}
