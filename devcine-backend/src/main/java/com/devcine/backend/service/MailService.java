package com.devcine.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.devcine.backend.dto.TicketEmailData;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi email vé điện tử (mã QR + thông tin suất chiếu) sau khi thanh toán thành
 * công. Chạy bất đồng bộ ({@link Async}) và nuốt lỗi gửi mail để KHÔNG ảnh
 * hưởng giao dịch đặt vé.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:DevCine <devcinecinema@gmail.com>}")
    private String from;

    @Value("${mail.enabled:true}")
    private boolean enabled;

    private static final DateTimeFormatter TIME_FMT
            = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    @Async
    public void sendTicketEmail(TicketEmailData data) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua gửi vé đơn {}", data.bookingCode());
            return;
        }
        if (data.toEmail() == null || data.toEmail().isBlank()) {
            log.warn("Bỏ qua gửi mail vé đơn {}: khách hàng chưa có email", data.bookingCode());
            return;
        }
        try {
            doSend(data);
            log.info("Đã gửi vé qua email tới {} cho đơn {}", data.toEmail(), data.bookingCode());
        } catch (Exception e) {
            log.error("Gửi email vé thất bại cho đơn {}: {}", data.bookingCode(), e.getMessage(), e);
        }
    }

    /**
     * Gửi mã OTP đặt lại mật khẩu (đồng bộ, NÉM lỗi để service báo người dùng
     * nếu SMTP lỗi).
     *
     * @param toEmail email người nhận
     * @param code mã OTP 6 số
     * @param ttlMin số phút hiệu lực (đưa vào nội dung mail)
     */
    public void sendOtpEmail(String toEmail, String code, int ttlMin) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject("DevCine • Mã xác minh đặt lại mật khẩu");
        helper.setText(buildOtpHtml(code, ttlMin), true);
        mailSender.send(message);
    }

    /**
     * Gửi email cấp tài khoản nhân viên (username + mật khẩu mặc định). Best-effort:
     * KHÔNG ném lỗi — trả về true nếu gửi thành công, false nếu tắt mail/lỗi SMTP.
     */
    public boolean sendStaffCredentials(String toEmail, String fullName, String username, String password) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua gửi email cấp tài khoản cho {}", username);
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Bỏ qua gửi email cấp tài khoản {}: thiếu email", username);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("DevCine • Tài khoản nhân viên đã được khởi tạo");
            helper.setText(buildStaffCredentialsHtml(fullName, username, password), true);
            mailSender.send(message);
            log.info("Đã gửi email cấp tài khoản tới {} (username {})", toEmail, username);
            return true;
        } catch (Exception e) {
            log.error("Gửi email cấp tài khoản thất bại cho {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String buildStaffCredentialsHtml(String fullName, String username, String password) {
        return """
                <div style="max-width:480px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">Tài khoản nhân viên đã được khởi tạo</div>
                  </div>
                  <div style="padding:28px 24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 14px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">Tài khoản nội bộ DevCine của bạn đã được tạo. Thông tin đăng nhập:</p>
                    <table style="width:100%%;border-collapse:collapse;background:#fafafa;border-radius:10px;">
                      <tr><td style="padding:10px 14px;color:#888;font-size:13px;">Tên đăng nhập</td><td style="padding:10px 14px;text-align:right;font-weight:700;color:#111;">%s</td></tr>
                      <tr><td style="padding:10px 14px;color:#888;font-size:13px;">Mật khẩu mặc định</td><td style="padding:10px 14px;text-align:right;font-weight:700;color:#8a6d00;font-size:15px;">%s</td></tr>
                    </table>
                    <p style="font-size:13px;color:#c0392b;margin:18px 0 0;font-weight:600;">Vui lòng đăng nhập và ĐỔI MẬT KHẨU ngay để kích hoạt tài khoản.</p>
                    <p style="font-size:12px;color:#999;margin-top:22px;line-height:1.6;">Đây là email tự động, vui lòng không trả lời. — DevCine Cinema</p>
                  </div>
                </div>
                """.formatted(escape(fullName), escape(username), escape(password));
    }

    private String buildOtpHtml(String code, int ttlMin) {
        return """
                <div style="max-width:480px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">Yêu cầu đặt lại mật khẩu</div>
                  </div>
                  <div style="padding:28px 24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 14px;">Bạn vừa yêu cầu đặt lại mật khẩu tài khoản DevCine.</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">Nhập mã xác minh dưới đây để tiếp tục:</p>
                    <div style="text-align:center;margin:8px 0 20px;">
                      <span style="display:inline-block;font-size:34px;font-weight:800;letter-spacing:10px;color:#111;background:#faf6e6;border:1px dashed #e0b400;border-radius:10px;padding:14px 22px;">%s</span>
                    </div>
                    <p style="font-size:13px;color:#888;margin:0;">Mã có hiệu lực trong <b>%d phút</b>. Nếu bạn không yêu cầu, vui lòng bỏ qua email này.</p>
                    <p style="font-size:12px;color:#999;margin-top:22px;line-height:1.6;">Đây là email tự động, vui lòng không trả lời. — DevCine Cinema</p>
                  </div>
                </div>
                """.formatted(escape(code), ttlMin);
    }

    private void doSend(TicketEmailData data) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(data.toEmail());
        helper.setSubject("DevCine • Vé điện tử đơn " + data.bookingCode());
        helper.setText(buildHtml(data), true);
        mailSender.send(message);
    }

    private String buildHtml(TicketEmailData data) {
        String time = data.startTime() != null ? data.startTime().format(TIME_FMT) : "—";
        String price = formatMoney(data.finalPrice());

        StringBuilder seatRows = new StringBuilder();
        if (data.seats() != null) {
            for (TicketEmailData.SeatLine s : data.seats()) {
                String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data="
                        + URLEncoder.encode(s.qrCode() == null ? "" : s.qrCode(), StandardCharsets.UTF_8);
                seatRows.append("""
                        <tr>
                          <td style="padding:14px;border-bottom:1px solid #eee;vertical-align:top;">
                            <div style="font-size:20px;font-weight:700;color:#111;">Ghế %s</div>
                            <div style="font-size:13px;color:#666;margin-top:2px;">Loại vé: %s</div>
                            <div style="font-size:12px;color:#999;margin-top:6px;">Mã: %s</div>
                          </td>
                          <td style="padding:14px;border-bottom:1px solid #eee;text-align:right;">
                            <img src="%s" alt="QR %s" width="120" height="120" style="border:1px solid #eee;border-radius:8px;" />
                          </td>
                        </tr>
                        """.formatted(
                        escape(s.seatLabel()), escape(ticketTypeLabel(s.ticketType())),
                        escape(s.qrCode()), qrUrl, escape(s.seatLabel())));
            }
        }

        StringBuilder fnbBlock = new StringBuilder();
        if (data.fnbs() != null && !data.fnbs().isEmpty()) {
            fnbBlock.append("<div style=\"margin-top:18px;\"><div style=\"font-weight:700;color:#111;margin-bottom:6px;\">Combo / Đồ ăn kèm</div>");
            for (TicketEmailData.FnbLine f : data.fnbs()) {
                fnbBlock.append("<div style=\"font-size:14px;color:#444;\">• %s × %d</div>"
                        .formatted(escape(f.name()), f.quantity() == null ? 1 : f.quantity()));
            }
            fnbBlock.append("</div>");
        }

        return """
                <div style="max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">Vé điện tử của bạn đã sẵn sàng</div>
                  </div>
                  <div style="padding:24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 4px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">Cảm ơn bạn đã đặt vé tại DevCine. Vui lòng đưa mã QR bên dưới tại quầy để check-in.</p>

                    <table style="width:100%%;border-collapse:collapse;background:#fafafa;border-radius:10px;">
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Mã đặt vé</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#8a6d00;font-size:15px;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Phim</td><td style="padding:8px 14px;text-align:right;font-weight:600;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Rạp / Phòng</td><td style="padding:8px 14px;text-align:right;color:#111;">%s • %s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Suất chiếu</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Thanh toán</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Tổng tiền</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#111;font-size:16px;">%s</td></tr>
                    </table>

                    <div style="font-weight:700;color:#111;margin:22px 0 4px;">Vé & mã QR</div>
                    <table style="width:100%%;border-collapse:collapse;">%s</table>
                    %s

                    <p style="font-size:12px;color:#999;margin-top:24px;line-height:1.6;">
                      Vui lòng đến trước giờ chiếu 15–30 phút. Mỗi mã QR chỉ dùng để check-in một lần.<br/>
                      Đây là email tự động, vui lòng không trả lời. — DevCine Cinema
                    </p>
                  </div>
                </div>
                """.formatted(
                escape(data.customerName()),
                escape(data.bookingCode()),
                escape(data.movieTitle()),
                escape(data.cinemaName()), escape(data.roomName()),
                escape(time),
                escape(paymentLabel(data.paymentMethod())),
                price,
                seatRows.toString(),
                fnbBlock.toString());
    }

    private String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount.longValue()) + "đ";
    }

    private String ticketTypeLabel(String type) {
        if (type == null) {
            return "Người lớn";
        }
        return switch (type.toUpperCase()) {
            case "STUDENT" ->
                "Học sinh/Sinh viên";
            case "CHILD" ->
                "Trẻ em";
            case "SENIOR" ->
                "Người cao tuổi";
            default ->
                "Người lớn";
        };
    }

    private String paymentLabel(String method) {
        if (method == null) {
            return "—";
        }
        return switch (method.toUpperCase()) {
            case "VNPAY" ->
                "VNPAY";
            case "CASH" ->
                "Tiền mặt";
            case "CARD" ->
                "Thẻ";
            case "TRANSFER" ->
                "Chuyển khoản";
            default ->
                method;
        };
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
