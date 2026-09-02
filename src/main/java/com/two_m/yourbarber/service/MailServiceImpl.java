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

        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send verification email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
