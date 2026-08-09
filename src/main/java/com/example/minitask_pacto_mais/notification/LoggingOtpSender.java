package com.example.minitask_pacto_mais.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(OtpChannel channel, String destination, String message, String emailFallback) {
        log.info("[OTP-DEV] channel={} destination={} emailFallback={} message={}",
                channel, destination, emailFallback, message);
    }
}
