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

    public void sendBookingConfirmation(String toEmail, String customerName,
                                        String roomName, String checkIn, String checkOut,
                                        String paymentState, String checkInCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("✅ Xác nhận đặt phòng & Mã Check-in - Aurora Palace Hotel");

        String content = String.format("""
            Kính chào %s,

            Cảm ơn quý khách đã đặt phòng tại Aurora Palace Hotel.

            Thông tin đặt phòng:
            • Phòng: %s
            • Ngày nhận phòng: %s
            • Ngày trả phòng: %s
            • Trạng thái thanh toán: %s

            Mã check-in của quý khách: %s
            Vui lòng cung cấp mã này tại quầy lễ tân khi nhận phòng.

            Nếu có bất kỳ thắc mắc nào, xin vui lòng liên hệ:
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s

            Trân trọng,
            Aurora Palace Hotel
            """, 
            safe(customerName), safe(roomName), safe(checkIn), safe(checkOut),
            safe(mapPaymentState(paymentState)), safe(checkInCode), fromEmail);

        message.setText(content);
        try {
            mailSender.send(message);
            System.out.println("✅ Booking email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send booking email to: " + toEmail);
            e.printStackTrace();
        }
    }

    private String safe(String s){ return s==null? "-" : s; }
    private String mapPaymentState(String s){
        if (s == null) return "unpaid";
        return switch (s) {
            case "paid_in_full" -> "Đã thanh toán đủ";
            case "deposit_paid" -> "Đã thanh toán tiền cọc";
            default -> "Chưa thanh toán";
        };
    }
}

