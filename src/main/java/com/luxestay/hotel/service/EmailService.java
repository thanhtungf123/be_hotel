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

    public void sendRefundInfoRequestEmail(String toEmail, String customerName,
                                            Integer bookingId, String roomName,
                                            String totalPrice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("💳 Yêu cầu cung cấp thông tin hoàn tiền - Aurora Palace Hotel");

        String content = String.format("""
            Kính chào %s,

            Yêu cầu hủy đặt phòng của quý khách đã được phê duyệt.

            Thông tin đặt phòng đã hủy:
            • Mã đặt phòng: #%d
            • Phòng: %s
            • Số tiền cần hoàn: %s

            Để chúng tôi có thể tiến hành hoàn tiền, quý khách vui lòng cung cấp thông tin tài khoản ngân hàng:
            1. Đăng nhập vào tài khoản tại website của chúng tôi
            2. Vào phần "Lịch sử đặt phòng"
            3. Tìm đơn đặt phòng #%d (trạng thái: Đã hủy)
            4. Điền đầy đủ thông tin:
               - Chủ tài khoản ngân hàng
               - Số tài khoản ngân hàng
               - Tên ngân hàng
            5. Bấm "Gửi thông tin" để hoàn tất

            Sau khi nhận được thông tin, chúng tôi sẽ tiến hành hoàn tiền trong vòng 5-7 ngày làm việc.

            Nếu có bất kỳ thắc mắc nào, xin vui lòng liên hệ:
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s

            Trân trọng,
            Aurora Palace Hotel
            """,
            safe(customerName), bookingId, safe(roomName), safe(totalPrice), bookingId, fromEmail);

        message.setText(content);
        try {
            mailSender.send(message);
            System.out.println("✅ Refund info request email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send refund info request email to: " + toEmail);
            e.printStackTrace();
        }
    }

    public void sendRefundCompletedEmail(String toEmail, String customerName,
                                         Integer bookingId, String roomName,
                                         String refundAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("✅ Xác nhận hoàn tiền thành công - Aurora Palace Hotel");

        String content = String.format("""
            Kính chào %s,

            Chúng tôi xin thông báo rằng quá trình hoàn tiền cho đơn đặt phòng của quý khách đã được hoàn tất.

            Thông tin hoàn tiền:
            • Mã đặt phòng: #%d
            • Phòng: %s
            • Số tiền đã hoàn: %s

            Số tiền đã được chuyển vào tài khoản ngân hàng mà quý khách đã cung cấp. 
            Vui lòng kiểm tra tài khoản của quý khách trong vòng 24-48 giờ.

            Nếu quý khách không nhận được tiền hoàn, vui lòng liên hệ với chúng tôi ngay:
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s

            Cảm ơn quý khách đã tin tưởng và sử dụng dịch vụ của Aurora Palace Hotel.
            Chúng tôi rất mong được phục vụ quý khách trong tương lai.

            Trân trọng,
            Aurora Palace Hotel
            """,
            safe(customerName), bookingId, safe(roomName), safe(refundAmount), fromEmail);

        message.setText(content);
        try {
            mailSender.send(message);
            System.out.println("✅ Refund completed email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send refund completed email to: " + toEmail);
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

