package com.institutojf.mottainai.service;

import com.institutojf.mottainai.config.StaffProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envia links de recuperação e ativação após a transação ser confirmada.
 */
@RequiredArgsConstructor
@Service
public class PasswordResetEmailService {

    private final JavaMailSender mailSender;

    private final MailProperties mailProperties;

    private final StaffProperties staffProperties;

    public void sendRecoveryLink(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(email);
        message.setSubject("Mottainai password recovery");
        message.setText("Use this link to reset your password: " + staffProperties.passwordResetUrl() + "?token="
                + token + "\n\nThis link expires in 15 minutes.");
        mailSender.send(message);
    }

    // Envia o link para o funcionário definir a senha e ativar a conta
    public void sendInvitationLink(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getUsername());
        message.setTo(email);
        message.setSubject("Mottainai employee invitation");
        message.setText("Use this link to define your password and activate your account: "
                + staffProperties.passwordResetUrl() + "?token=" + token + "\n\nThis link expires in 48 hours.");
        mailSender.send(message);
    }

}
