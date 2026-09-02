package com.two_m.yourbarber.service;

public interface MailService {

    void sendVerificationCode(String toEmail, String name, String code);
}
