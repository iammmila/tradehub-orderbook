package com.ab.authservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SendResult {
    private boolean success;
    private String providerMessageId;
    private String errorCode;
    private String errorMessage;

    public static SendResult ok(String id) {
        return new SendResult(true, id, null, null);
    }

    public static SendResult fail(String code, String msg) {
        return new SendResult(false, null, code, msg);
    }
}
