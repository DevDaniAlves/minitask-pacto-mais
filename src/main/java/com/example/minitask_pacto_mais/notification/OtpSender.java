package com.example.minitask_pacto_mais.notification;

public interface OtpSender {

    void send(OtpChannel channel, String destination, String message, String emailFallback);
}
