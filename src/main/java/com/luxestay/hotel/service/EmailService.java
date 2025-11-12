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

    public void sendInvoiceEmail(String toEmail, com.luxestay.hotel.dto.invoice.InvoiceDTO invoice) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("📄 Hóa đơn đặt phòng - Aurora Palace Hotel");

        StringBuilder servicesText = new StringBuilder();
        if (invoice.getServices() != null && !invoice.getServices().isEmpty()) {
            servicesText.append("\n\nDỊCH VỤ ĐÃ CHỌN:\n");
            for (var service : invoice.getServices()) {
                servicesText.append(String.format("• %s: %,d VNĐ\n", 
                    service.getName(), service.getPrice().intValue()));
            }
        }

        String content = String.format("""
            Kính chào %s,
            
            Đây là hóa đơn cho đặt phòng của quý khách tại Aurora Palace Hotel.
            
            ═══════════════════════════════════════════
            HÓA ĐƠN THANH TOÁN
            ═══════════════════════════════════════════
            
            Mã hóa đơn: %s
            Ngày xuất: %s
            Mã check-in: %s
            
            ───────────────────────────────────────────
            THÔNG TIN KHÁCH HÀNG
            ───────────────────────────────────────────
            Họ tên: %s
            Số điện thoại: %s
            Email: %s
            
            ───────────────────────────────────────────
            THÔNG TIN PHÒNG
            ───────────────────────────────────────────
            • Phòng: %s
            • Check-in: %s
            • Check-out: %s
            • Số đêm: %d
            • Số khách: %d người lớn%s
            %s
            ───────────────────────────────────────────
            CHI TIẾT GIÁ
            ───────────────────────────────────────────
            Tiền phòng (%d đêm)         %,d VNĐ
            Dịch vụ bổ sung              %,d VNĐ
                                    ──────────────
            Tạm tính                     %,d VNĐ
            Thuế VAT (10%%)               %,d VNĐ
            Phí dịch vụ (5%%)             %,d VNĐ
                                    ──────────────
            TỔNG CỘNG                    %,d VNĐ
            ═══════════════════════════════════════════
            
            THÔNG TIN THANH TOÁN
            ───────────────────────────────────────────
            Trạng thái: %s
            Đã thanh toán: %,d VNĐ
            %s
            ───────────────────────────────────────────
            
            Cảm ơn quý khách đã tin tưởng và sử dụng dịch vụ!
            
            Thời gian check-in: 14:00 | Check-out: 12:00
            
            Nếu có thắc mắc, vui lòng liên hệ:
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s
            
            Trân trọng,
            Aurora Palace Hotel
            """,
            safe(invoice.getCustomerName()),
            safe(invoice.getInvoiceNumber()),
            invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : "",
            safe(invoice.getCheckInCode()),
            safe(invoice.getCustomerName()),
            safe(invoice.getCustomerPhone()),
            safe(invoice.getCustomerEmail()),
            safe(invoice.getRoomName()),
            invoice.getCheckIn() != null ? invoice.getCheckIn().toString() : "",
            invoice.getCheckOut() != null ? invoice.getCheckOut().toString() : "",
            invoice.getNights() != null ? invoice.getNights() : 0,
            invoice.getAdults() != null ? invoice.getAdults() : 0,
            invoice.getChildren() != null && invoice.getChildren() > 0 ? ", " + invoice.getChildren() + " trẻ em" : "",
            servicesText.toString(),
            invoice.getNights() != null ? invoice.getNights() : 0,
            invoice.getRoomTotal() != null ? invoice.getRoomTotal().intValue() : 0,
            invoice.getServicesTotal() != null ? invoice.getServicesTotal().intValue() : 0,
            invoice.getSubtotal() != null ? invoice.getSubtotal().intValue() : 0,
            invoice.getTax() != null ? invoice.getTax().intValue() : 0,
            invoice.getServiceFee() != null ? invoice.getServiceFee().intValue() : 0,
            invoice.getTotal() != null ? invoice.getTotal().intValue() : 0,
            mapPaymentState(invoice.getPaymentState()),
            invoice.getPaidAmount() != null ? invoice.getPaidAmount().intValue() : 0,
            invoice.getDepositAmount() != null && invoice.getDepositAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                ? String.format("Tiền cọc: %,d VNĐ", invoice.getDepositAmount().intValue())
                : "",
            fromEmail);

        message.setText(content);
        try {
            mailSender.send(message);
            System.out.println("✅ Invoice email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send invoice email to: " + toEmail);
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

    public void sendRoomIssueEmail(String toEmail, String customerName,
                                   Integer bookingId, String roomName,
                                   String issueDescription) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("⚠️ Thông báo về vấn đề phòng - Aurora Palace Hotel");

        String content = String.format("""
            Kính chào %s,
            
            Chúng tôi rất tiếc phải thông báo rằng phòng trong đơn đặt của quý khách đang gặp một số vấn đề.
            
            Thông tin đặt phòng:
            • Mã đặt phòng: #%d
            • Phòng: %s
            
            Vấn đề: %s
            
            Chúng tôi sẽ tiến hành hoàn trả toàn bộ số tiền quý khách đã thanh toán.
            
            Để thực hiện hoàn tiền, quý khách vui lòng cung cấp thông tin tài khoản ngân hàng:
            1. Đăng nhập vào tài khoản tại website
            2. Vào phần "Lịch sử đặt phòng"
            3. Tìm đơn đặt phòng #%d
            4. Điền đầy đủ thông tin:
               - Chủ tài khoản ngân hàng
               - Số tài khoản ngân hàng
               - Tên ngân hàng
            5. Bấm "Gửi thông tin" để hoàn tất
            
            Sau khi nhận được thông tin, chúng tôi sẽ hoàn tiền trong vòng 3-5 ngày làm việc.
            
            Chúng tôi chân thành xin lỗi vì sự bất tiện này.
            
            Nếu có thắc mắc, vui lòng liên hệ:
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s
            
            Trân trọng,
            Aurora Palace Hotel
            """,
            safe(customerName), bookingId, safe(roomName), safe(issueDescription),
            bookingId, fromEmail);

        message.setText(content);
        try {
            mailSender.send(message);
            System.out.println("✅ Room issue email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send room issue email to: " + toEmail);
            e.printStackTrace();
        }
    }

    public void sendRoomConfirmedEmail(String toEmail, String customerName,
                                        String roomName, String checkIn, String checkOut,
                                        String checkInCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("✅ Xác nhận đặt phòng thành công - Aurora Palace Hotel");

        String content = String.format("""
            Kính chào %s,
            
            Đặt phòng của quý khách đã được xác nhận thành công!
            
            Phòng đã được kiểm tra và sẵn sàng cho chuyến đi của quý khách.
            
            ═══════════════════════════════════════════
            THÔNG TIN ĐẶT PHÒNG
            ═══════════════════════════════════════════
            
            Phòng: %s
            Check-in: %s (14:00)
            Check-out: %s (12:00)
            
            ───────────────────────────────────────────
            MÃ CHECK-IN
            ───────────────────────────────────────────
            %s
            ───────────────────────────────────────────
            
            ⚠️ Vui lòng xuất trình mã này khi check-in tại khách sạn.
            
            Lưu ý:
            • Giờ nhận phòng: 14:00
            • Giờ trả phòng: 12:00
            • Vui lòng mang theo CCCD/CMND khi check-in
            
            Chúng tôi rất mong được phục vụ quý khách!
            
            Nếu có thắc mắc, vui lòng liên hệ:
            📞 Hotline: +84 123 456 789
            ✉️ Email: %s
            
            Trân trọng,
            Aurora Palace Hotel
            """,
            safe(customerName), safe(roomName), safe(checkIn), safe(checkOut),
            safe(checkInCode), fromEmail);

        message.setText(content);
        try {
            mailSender.send(message);
            System.out.println("✅ Room confirmed email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send room confirmed email to: " + toEmail);
            e.printStackTrace();
        }
    }
}

