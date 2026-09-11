package com.two_m.yourbarber.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    @Value("${verification.log-code-on-failure:false}")
    private boolean logCodeOnFailure;

    @Override
    public void sendVerificationCode(String toEmail, String name, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(toEmail);
        message.setSubject("Confirme seu cadastro - Seu Barbeiro");
        message.setText(
                "Olá, "
                        + name
                        + "!\n\n"
                        + "Seu código de confirmação é: "
                        + code
                        + "\n\n"
                        + "Ele expira em alguns minutos. Se você não solicitou este cadastro,"
                        + " ignore este e-mail.");

        // Logged unconditionally (not just on a caught failure): a real SMTP relay accepts
        // RCPT TO for a syntactically valid but undeliverable address (e.g. dev+barber@example.com)
        // and only bounces asynchronously later, so `send()` below won't throw for that case --
        // this is the only reliable way to surface the code for a fake dev address.
        if (logCodeOnFailure) {
            log.warn("[DEV] Verification code for {} ({}): {}", toEmail, name, code);
        }

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send verification email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
