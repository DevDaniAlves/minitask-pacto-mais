package com.example.minitask_pacto_mais.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class CompositeOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(CompositeOtpSender.class);

    private final BrevoProperties brevoProperties;
    private final EvolutionProperties evolutionProperties;
    private final BrevoOtpSender brevoOtpSender;
    private final EvolutionOtpSender evolutionOtpSender;
    private final LoggingOtpSender loggingOtpSender;

    @Override
    public void send(OtpChannel channel, String destination, String message, String emailFallback) {
        switch (channel) {
            case EMAIL -> sendEmail(destination, message);
            case WHATSAPP -> sendWhatsAppWithEmailFallback(destination, message, emailFallback);
        }
    }

    private void sendWhatsAppWithEmailFallback(String phone, String message, String emailFallback) {
        try {
            if (evolutionProperties.isConfigured()) {
                evolutionOtpSender.sendWhatsApp(phone, message);
                return;
            }
            loggingOtpSender.send(OtpChannel.WHATSAPP, phone, message, emailFallback);
        } catch (Exception ex) {
            log.warn("Falha Evolution, tentando e-mail. phone={} erro={}", phone, ex.getMessage());
            if (emailFallback == null || emailFallback.isBlank()) {
                throw new RuntimeException("Falha ao enviar OTP por WhatsApp e sem e-mail de fallback", ex);
            }
            sendEmail(emailFallback, message);
        }
    }

    private void sendEmail(String email, String message) {
        try {
            if (brevoProperties.isConfigured()) {
                brevoOtpSender.sendEmail(email, message);
            } else {
                loggingOtpSender.send(OtpChannel.EMAIL, email, message, null);
            }
        } catch (Exception ex) {
            log.error("Falha ao enviar OTP por e-mail destination={}: {}", email, ex.getMessage());
            throw new RuntimeException("Falha ao enviar OTP", ex);
        }
    }
}
