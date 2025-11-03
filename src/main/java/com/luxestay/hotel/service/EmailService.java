package com.luxestay.hotel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("🔐 Mã OTP đặt lại mật khẩu - Aurora Palace Hotel");
        
        String emailContent = String.format("""
            Kính chào quý khách,
            
            Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn tại Aurora Palace Hotel.
            
            Mã OTP của bạn: %s
            
            ⚠️ Mã OTP này chỉ có hiệu lực trong 10 phút.
            
            Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
            
            Trân trọng,
            Đội ngũ Aurora Palace Hotel
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s
            """, otp, fromEmail);

        message.setText(emailContent);
        
        try {
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to: " + toEmail);
            e.printStackTrace();
        }
    }
}

