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

import com.devcine.backend.dto.CancellationEmailData;
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
            = DateTimeFormatter.ofPattern("HH:mm | dd/MM/yyyy");

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
     * Gửi email THÔNG BÁO HỦY VÉ khi cụm rạp đóng cửa đột xuất (kèm mã voucher đền bù nếu có).
     * Best-effort & {@link Async}: một email lỗi không chặn cả batch đền bù; tắt mail thì bỏ qua.
     */
    @Async
    public void sendCancellationEmail(CancellationEmailData data) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua email hủy vé đơn {}", data.bookingCode());
            return;
        }
        if (data.toEmail() == null || data.toEmail().isBlank()) {
            log.warn("Bỏ qua email hủy vé đơn {}: khách chưa có email", data.bookingCode());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(data.toEmail());
            helper.setSubject("DevCine • Thông báo hủy vé đơn " + data.bookingCode());
            helper.setText(buildCancellationHtml(data), true);
            mailSender.send(message);
            log.info("Đã gửi email hủy vé tới {} cho đơn {}", data.toEmail(), data.bookingCode());
        } catch (Exception e) {
            log.error("Gửi email hủy vé thất bại cho đơn {}: {}", data.bookingCode(), e.getMessage(), e);
        }
    }

    /**
     * Gửi email THÔNG BÁO ĐỔI GHẾ SỰ CỐ & ĐỀN BÙ VOUCHER (kèm mã QR voucher và mã QR vé mới).
     */
    /*
    @Async
    public void sendIncidentRelocateEmail(com.devcine.backend.dto.IncidentRelocateEmailData data) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua email đổi ghế sự cố đơn {}", data.bookingCode());
            return;
        }
        if (data.toEmail() == null || data.toEmail().isBlank()) {
            log.warn("Bỏ qua email đổi ghế sự cố đơn {}: khách chưa có email", data.bookingCode());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(data.toEmail());
            String subject = data.voucherIssued()
                    ? "DevCine • Thông báo đổi ghế & Ưu đãi đền bù đơn " + data.bookingCode()
                    : "DevCine • Cập nhật vị trí ghế đơn " + data.bookingCode();
            helper.setSubject(subject);
            helper.setText(buildIncidentRelocateHtml(data), true);
            mailSender.send(message);
            log.info("Đã gửi email đổi ghế sự cố tới {} cho đơn {}", data.toEmail(), data.bookingCode());
        } catch (Exception e) {
            log.error("Gửi email đổi ghế sự cố thất bại cho đơn {}: {}", data.bookingCode(), e.getMessage(), e);
        }
    }
    */

    private String buildCancellationHtml(CancellationEmailData data) {
        String time = data.startTime() != null ? data.startTime().format(TIME_FMT) : "—";

        StringBuilder seatBlock = new StringBuilder();
        if (data.seatLabels() != null && !data.seatLabels().isEmpty()) {
            seatBlock.append("<div style=\"margin-top:6px;\">");
            for (String label : data.seatLabels()) {
                seatBlock.append("<span style=\"display:inline-block;background:#fbecec;color:#8a1f1f;border-radius:6px;padding:4px 10px;margin:3px 4px 0 0;font-weight:700;font-size:13px;\">%s</span>"
                        .formatted(escape(label)));
            }
            seatBlock.append("</div>");
        }

        String voucherBlock = "";
        if (data.voucherIssued() && data.voucherCode() != null) {
            String voucherQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data="
                    + URLEncoder.encode(data.voucherCode() == null ? "" : data.voucherCode(), StandardCharsets.UTF_8);
            voucherBlock = """
                    <div style="font-weight:700;color:#111;margin:22px 0 8px;">Đền bù dành cho bạn</div>
                    <div style="text-align:center;background:#faf6e6;border:1px dashed #e0b400;border-radius:12px;padding:20px;">
                      <div style="font-size:13px;color:#8a6d00;font-weight:700;margin-bottom:8px;">%s</div>
                      <div style="font-size:24px;font-weight:800;letter-spacing:4px;color:#111;margin-bottom:12px;">%s</div>
                      <img src="%s" alt="QR Voucher" width="160" height="160" style="border:1px solid #ddd;border-radius:8px;background:#fff;" />
                      <div style="font-size:12px;color:#888;margin-top:12px;">Mã đã được lưu vào ví <b>"Ưu đãi của tôi"</b>, hiệu lực 90 ngày. Đưa mã QR tại quầy hoặc áp dụng khi đặt vé trực tuyến.</div>
                    </div>
                    """.formatted(escape(data.voucherLabel() != null ? data.voucherLabel() : "Voucher đền bù"),
                                  escape(data.voucherCode()),
                                  voucherQrUrl);
        }

        return """
                <div style="max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#c0392b,#8a1f1f);padding:22px 24px;">
                    <div style="color:#fff;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#ffd9d3;font-size:13px;margin-top:4px;">Thông báo hủy suất chiếu</div>
                  </div>
                  <div style="padding:24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 4px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">DevCine rất tiếc phải thông báo suất chiếu của bạn đã bị <b>hủy</b> do <b>%s</b>. Chúng tôi thành thật xin lỗi vì sự bất tiện này.</p>

                    <table style="width:100%%;border-collapse:collapse;background:#fafafa;border-radius:10px;">
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Mã đặt vé</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#8a1f1f;font-size:15px;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Phim</td><td style="padding:8px 14px;text-align:right;font-weight:600;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Rạp / Phòng</td><td style="padding:8px 14px;text-align:right;color:#111;">%s • %s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Suất chiếu</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                    </table>

                    <div style="font-weight:700;color:#111;margin:18px 0 4px;">Ghế đã hủy</div>
                    %s
                    %s

                    <p style="font-size:12px;color:#999;margin-top:24px;line-height:1.6;">Nếu cần hỗ trợ thêm, vui lòng liên hệ hotline 1900 1234.<br/>Đây là email tự động, vui lòng không trả lời — DevCine Cinema</p>
                  </div>
                </div>
                """.formatted(
                escape(data.customerName()),
                escape(data.reason() != null ? data.reason() : "sự cố tại rạp"),
                escape(data.bookingCode()),
                escape(data.movieTitle()),
                escape(data.cinemaName()), escape(data.roomName()),
                escape(time),
                seatBlock.toString(),
                voucherBlock);
    }

    private String buildIncidentRelocateHtml(com.devcine.backend.dto.IncidentRelocateEmailData data) {
        String time = data.startTime() != null ? data.startTime().format(TIME_FMT) : "—";

        // Khối Lý do sự cố
        String reasonBlock = "";
        if (data.reason() != null && !data.reason().isBlank()) {
            reasonBlock = """
                    <div style="background:#fff8e6;border-left:4px solid #e0b400;border-radius:8px;padding:12px 16px;margin:16px 0 18px;">
                      <div style="font-size:11px;color:#8a6d00;font-weight:700;text-transform:uppercase;letter-spacing:.5px;">Lý do / Ghi chú sự cố</div>
                      <div style="font-size:14px;color:#333;margin-top:4px;font-weight:600;">%s</div>
                    </div>
                    """.formatted(escape(data.reason()));
        }

        // Khối hoán đổi ghế (cũ -> mới)
        StringBuilder swapBlock = new StringBuilder();
        if (data.swaps() != null && !data.swaps().isEmpty()) {
            swapBlock.append("<div style=\"margin:16px 0;\"><div style=\"font-weight:700;color:#111;margin-bottom:8px;font-size:14px;\">Chi tiết thay đổi chỗ ngồi</div>");
            swapBlock.append("<table style=\"width:100%;border-collapse:collapse;background:#fafafa;border-radius:8px;overflow:hidden;\">");
            for (com.devcine.backend.dto.IncidentRelocateEmailData.SeatSwapLine s : data.swaps()) {
                swapBlock.append("""
                        <tr style="border-bottom:1px solid #eee;">
                          <td style="padding:10px 14px;font-size:13px;color:#888;">Ghế cũ: <span style="font-weight:700;color:#c0392b;text-decoration:line-through;">%s</span></td>
                          <td style="padding:10px 14px;text-align:right;font-size:13px;color:#111;">Ghế mới: <span style="font-weight:800;color:#0a8f08;background:#e8f8e8;padding:3px 10px;border-radius:6px;">%s</span></td>
                        </tr>
                        """.formatted(escape(s.oldSeatLabel()), escape(s.newSeatLabel())));
            }
            swapBlock.append("</table></div>");
        }

        // Khối Đền bù Voucher kèm QR Code
        String voucherBlock = "";
        if (data.voucherIssued() && data.voucherCode() != null) {
            String voucherQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data="
                    + URLEncoder.encode(data.voucherCode() == null ? "" : data.voucherCode(), StandardCharsets.UTF_8);
            String valueLabel = "";
            if (data.voucherValue() != null && data.voucherValue().signum() > 0) {
                valueLabel = "Trị giá: " + formatMoney(data.voucherValue());
            } else if ("GIFT_FNB".equalsIgnoreCase(data.voucherType())) {
                valueLabel = "Quà tặng: 01 Combo Bắp Nước";
            }
            voucherBlock = """
                    <div style="margin:22px 0 8px;">
                      <div style="font-weight:700;color:#111;margin-bottom:6px;font-size:14px;">🎁 Ưu đãi đền bù dành cho bạn</div>
                      <div style="text-align:center;background:#faf6e6;border:1px dashed #e0b400;border-radius:12px;padding:20px;">
                        <div style="font-size:13px;color:#8a6d00;font-weight:700;">%s</div>
                        <div style="font-size:12px;color:#666;margin:4px 0 10px;">%s</div>
                        <div style="font-size:24px;font-weight:800;letter-spacing:5px;color:#111;margin-bottom:12px;">%s</div>
                        <img src="%s" alt="QR Voucher" width="160" height="160" style="border:1px solid #ddd;border-radius:8px;background:#fff;" />
                        <div style="font-size:12px;color:#888;margin-top:12px;line-height:1.5;">
                          Đưa mã QR trên tại quầy hoặc áp dụng khi đặt vé online.<br/>
                          Mã đã được lưu vào ví <b>"Ưu đãi của tôi"</b> (Hạn sử dụng: 90 ngày).
                        </div>
                      </div>
                    </div>
                    """.formatted(
                    escape(data.voucherLabel() != null ? data.voucherLabel() : "Voucher đền bù sự cố"),
                    escape(valueLabel),
                    escape(data.voucherCode()),
                    voucherQrUrl);
        }

        // Khối Combo F&B kèm đơn ban đầu (nếu có)
        StringBuilder fnbBlock = new StringBuilder();
        if (data.fnbs() != null && !data.fnbs().isEmpty()) {
            fnbBlock.append("<div style=\"margin-top:16px;\"><div style=\"font-weight:700;color:#111;margin-bottom:6px;\">Combo / Đồ ăn kèm</div>");
            for (TicketEmailData.FnbLine f : data.fnbs()) {
                fnbBlock.append("<div style=\"font-size:14px;color:#444;\">• %s × %d</div>"
                        .formatted(escape(f.name()), f.quantity() == null ? 1 : f.quantity()));
            }
            fnbBlock.append("</div>");
        }

        // QR soát vé phải dùng QR hiện hành của từng Ticket, không dùng bookingCode ổn định của đơn.
        StringBuilder ticketQrItems = new StringBuilder();
        if (data.seats() != null) {
            for (TicketEmailData.SeatLine seat : data.seats()) {
                if (seat == null || seat.qrCode() == null || seat.qrCode().isBlank()) continue;
                String ticketQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data="
                        + URLEncoder.encode(seat.qrCode(), StandardCharsets.UTF_8);
                ticketQrItems.append("""
                        <div style="display:inline-block;vertical-align:top;text-align:center;background:#fafafa;border:1px solid #eee;border-radius:12px;padding:16px;margin:4px;">
                          <div style="font-size:13px;color:#111;font-weight:800;margin-bottom:8px;">Ghế %s</div>
                          <img src="%s" alt="QR vé ghế %s" width="160" height="160" style="border:1px solid #eee;border-radius:10px;background:#fff;" />
                          <div style="font-size:11px;color:#888;margin-top:8px;">QR vé hiện hành</div>
                        </div>
                        """.formatted(escape(seat.seatLabel()), ticketQrUrl, escape(seat.seatLabel())));
            }
        }

        String ticketQrContent = ticketQrItems.isEmpty()
                ? "<div style=\"font-size:13px;color:#8a6d00;background:#fff8e6;border-radius:10px;padding:14px;\">"
                  + "Chưa thể hiển thị QR vé hiện hành. Vui lòng mở vé trong tài khoản hoặc liên hệ quầy hỗ trợ.</div>"
                : ticketQrItems.toString();
        String ticketQrBlock = """
                <div style="margin:20px 0 8px;">
                  <div style="font-weight:700;color:#111;margin-bottom:6px;font-size:14px;">QR vé mới vào phòng chiếu</div>
                  <div style="text-align:center;">%s</div>
                  <div style="font-size:12px;color:#888;text-align:center;margin-top:10px;line-height:1.5;">
                    Chỉ sử dụng QR mới tương ứng với từng ghế. QR vé trước khi đổi ghế đã bị thu hồi.<br/>
                    Mã đặt vé <b>%s</b> vẫn được giữ nguyên để tra cứu giao dịch.
                  </div>
                </div>
                """.formatted(ticketQrContent, escape(data.bookingCode()));

        return """
                <div style="max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">Thông báo đổi ghế & Cập nhật vé xem phim</div>
                  </div>
                  <div style="padding:24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 4px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 14px;">DevCine xin thông báo vé của bạn đã được <b>chuyển sang vị trí ghế mới</b> do phát sinh sự cố tại phòng chiếu. Chúng tôi thành thật xin lỗi vì sự bất tiện này.</p>

                    %s

                    <table style="width:100%%;border-collapse:collapse;background:#fafafa;border-radius:10px;">
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Mã đặt vé</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#8a6d00;font-size:15px;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Phim</td><td style="padding:8px 14px;text-align:right;font-weight:600;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Rạp / Phòng</td><td style="padding:8px 14px;text-align:right;color:#111;">%s • %s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Suất chiếu</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                    </table>

                    %s
                    %s
                    %s
                    %s

                    <p style="font-size:12px;color:#999;margin-top:24px;line-height:1.6;">
                      Nếu cần hỗ trợ thêm, vui lòng liên hệ trực tiếp nhân viên tại quầy hoặc hotline 1900 1234.<br/>
                      Đây là email tự động, vui lòng không trả lời — DevCine Cinema
                    </p>
                  </div>
                </div>
                """.formatted(
                escape(data.customerName()),
                reasonBlock,
                escape(data.bookingCode()),
                escape(data.movieTitle()),
                escape(data.cinemaName()), escape(data.roomName()),
                escape(time),
                swapBlock.toString(),
                voucherBlock,
                ticketQrBlock,
                fnbBlock.toString());
    }

    /**
     * Gửi mã OTP đặt lại mật khẩu (đồng bộ, NÉM lỗi để service báo người dùng
     * nếu SMTP lỗi).
     *
     * @param toEmail email người nhận
     * @param code mã OTP 6 số
     * @param ttlMin số phút hiệu lực (đưa vào nội dung mail)
     */
    @Async
    public void sendOtpEmail(String toEmail, String code, int ttlMin) {
        if (!enabled || toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("DevCine • Mã xác minh đặt lại mật khẩu");
            helper.setText(buildOtpHtml(code, ttlMin), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Gửi OTP đặt lại mật khẩu thất bại cho: {}", toEmail, e);
        }
    }

    /**
     * Gửi email cấp tài khoản nhân viên (username + mật khẩu mặc định). Best-effort:
     * KHÔNG ném lỗi — trả về true nếu gửi thành công, false nếu tắt mail/lỗi SMTP.
     */
    @Async
    public void sendStaffCredentials(String toEmail, String fullName, String username, String password) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua gửi email cấp tài khoản cho {}", username);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Bỏ qua gửi email cấp tài khoản {}: thiếu email", username);
            return;
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
        } catch (Exception e) {
            log.error("Gửi email cấp tài khoản thất bại cho {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Gửi email chiến dịch báo MÃ ưu đãi tới khách (chỉ thông báo mã — khách tự nhập khi đặt vé).
     * Best-effort & @Async: một email lỗi không chặn cả chiến dịch; tắt mail thì bỏ qua.
     */
    @Async
    public void sendPromotionEmail(String toEmail, String fullName, com.devcine.backend.entity.Promotion promo) {
        if (!enabled || toEmail == null || toEmail.isBlank() || promo == null) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("DevCine • Ưu đãi mới dành cho bạn — mã " + (promo.getCode() != null ? promo.getCode() : ""));
            helper.setText(buildPromotionHtml(fullName, promo), true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Gửi email chiến dịch tới {} thất bại: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Gửi email phản hồi yêu cầu hỗ trợ (CSKH) tới khách. Best-effort & @Async:
     * lỗi gửi mail KHÔNG chặn việc lưu phản hồi. Trả về true nếu đã gửi.
     *
     * @param toEmail       email khách
     * @param fullName      tên khách
     * @param ticketId      mã ticket (đưa vào tiêu đề để khách tra cứu)
     * @param subjectLabel  nhãn chủ đề đã dịch (vd "Vấn đề về vé")
     * @param originalMessage nội dung khách đã gửi (trích lại cho khách nhớ ngữ cảnh)
     * @param replyMessage  nội dung phản hồi của CSKH
     */
    public boolean sendSupportReply(String toEmail, String fullName, Integer ticketId,
                                    String subjectLabel, String originalMessage, String replyMessage) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua gửi phản hồi ticket #{}", ticketId);
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Bỏ qua gửi phản hồi ticket #{}: khách chưa có email", ticketId);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject("DevCine • Phản hồi yêu cầu hỗ trợ #" + ticketId);
            helper.setText(buildSupportReplyHtml(fullName, subjectLabel, originalMessage, replyMessage), true);
            mailSender.send(message);
            log.info("Đã gửi phản hồi ticket #{} tới {}", ticketId, toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi phản hồi ticket #{} tới {} thất bại: {}", ticketId, toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String buildSupportReplyHtml(String fullName, String subjectLabel,
                                         String originalMessage, String replyMessage) {
        String origBlock = (originalMessage == null || originalMessage.isBlank()) ? "" : """
                <div style="background:#fafafa;border:1px solid #eee;border-left:3px solid #e0b400;border-radius:8px;padding:12px 14px;margin:0 0 18px;">
                  <div style="font-size:12px;color:#888;margin-bottom:6px;">Yêu cầu của bạn%s</div>
                  <div style="font-size:14px;color:#555;white-space:pre-wrap;">%s</div>
                </div>
                """.formatted(
                        (subjectLabel == null || subjectLabel.isBlank()) ? "" : " • " + escape(subjectLabel),
                        escape(originalMessage));
        return """
                <div style="max-width:520px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">Phản hồi từ bộ phận Chăm sóc khách hàng</div>
                  </div>
                  <div style="padding:28px 24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 14px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">Cảm ơn bạn đã liên hệ DevCine. Dưới đây là phản hồi cho yêu cầu của bạn:</p>
                    %s
                    <div style="background:#faf6e6;border:1px solid #f0e4b8;border-radius:10px;padding:16px 18px;margin:0 0 18px;">
                      <div style="font-size:14px;color:#111;white-space:pre-wrap;">%s</div>
                    </div>
                    <p style="font-size:13px;color:#888;margin:0;">Nếu cần hỗ trợ thêm, bạn có thể phản hồi lại email này hoặc gọi hotline 1900 1234.</p>
                    <p style="font-size:12px;color:#999;margin-top:22px;line-height:1.6;">Trân trọng — DevCine Cinema</p>
                  </div>
                </div>
                """.formatted(escape(fullName), origBlock, escape(replyMessage));
    }

    private String buildPromotionHtml(String fullName, com.devcine.backend.entity.Promotion promo) {
        boolean percent = "PERCENTAGE".equalsIgnoreCase(promo.getDiscountType());
        String discountText = percent
                ? "Giảm " + promo.getDiscountValue().toBigInteger() + "%"
                : "Giảm " + String.format("%,d", promo.getDiscountValue().toBigInteger()) + "đ";
        String expiry = promo.getEndDate() != null
                ? promo.getEndDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Không giới hạn";
        String minOrder = (promo.getMinOrderValue() != null && promo.getMinOrderValue().signum() > 0)
                ? "Áp dụng cho đơn từ " + String.format("%,d", promo.getMinOrderValue().toBigInteger()) + "đ." : "";
        String name = promo.getName() != null && !promo.getName().isBlank() ? promo.getName() : "Ưu đãi DevCine";
        return """
                <div style="max-width:480px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">Ưu đãi mới dành riêng cho bạn</div>
                  </div>
                  <div style="padding:28px 24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 6px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">DevCine gửi tặng bạn ưu đãi <b>%s</b> — <b style="color:#8a6d00;">%s</b>.</p>
                    <div style="text-align:center;margin:8px 0 18px;">
                      <div style="font-size:12px;color:#888;margin-bottom:6px;">Mã ưu đãi của bạn</div>
                      <span style="display:inline-block;font-size:26px;font-weight:800;letter-spacing:6px;color:#111;background:#faf6e6;border:1px dashed #e0b400;border-radius:10px;padding:12px 22px;">%s</span>
                    </div>
                    <p style="font-size:13px;color:#555;margin:0 0 10px;">%s</p>
                    <div style="background:#faf6e6;border:1px solid #f0e4b8;border-radius:10px;padding:12px 14px;margin:0 0 12px;">
                      <p style="font-size:12px;color:#8a6d00;font-weight:700;margin:0 0 8px;">Cách sử dụng mã</p>
                      <p style="font-size:13px;color:#444;margin:0 0 6px;"><b>1.</b> Nhập mã ở bước "Ưu đãi" khi đặt vé để được áp dụng ngay.</p>
                      <p style="font-size:13px;color:#444;margin:0;"><b>2.</b> Hoặc lưu mã vào ví <b>"Ưu đãi của tôi"</b> để dùng cho lần đặt vé sau.</p>
                    </div>
                    <p style="font-size:13px;color:#888;margin:0;">Hạn sử dụng: <b>%s</b></p>
                    <p style="font-size:12px;color:#999;margin-top:22px;line-height:1.6;">Đây là email tự động, vui lòng không trả lời — DevCine Cinema</p>
                  </div>
                </div>
                """.formatted(escape(fullName), escape(name), discountText, escape(promo.getCode()), minOrder, expiry);
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
                    <p style="font-size:12px;color:#999;margin-top:22px;line-height:1.6;">Đây là email tự động, vui lòng không trả lời — DevCine Cinema</p>
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
                    <p style="font-size:12px;color:#999;margin-top:22px;line-height:1.6;">Đây là email tự động, vui lòng không trả lời — DevCine Cinema</p>
                  </div>
                </div>
                """.formatted(escape(code), ttlMin);
    }

    private void doSend(TicketEmailData data) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(data.toEmail());
        helper.setSubject(data.showQr()
                ? "DevCine • Vé điện tử đơn " + data.bookingCode()
                : "DevCine • Hoá đơn thanh toán đơn " + data.bookingCode());
        helper.setText(buildHtml(data), true);
        mailSender.send(message);
    }

    private String buildHtml(TicketEmailData data) {
        String time = data.startTime() != null ? data.startTime().format(TIME_FMT) : "—";
        String price = formatMoney(data.finalPrice());

        String movieDisplay = data.movieTitle() != null ? data.movieTitle() : "Phim";
        if (data.formatName() != null && !data.formatName().isBlank()) {
            if (!movieDisplay.toLowerCase().contains(data.formatName().toLowerCase())) {
                movieDisplay = movieDisplay + " (" + data.formatName() + ")";
            }
        }

        String seatDisplay = formatSeats(data.seats());

        StringBuilder fnbBlock = new StringBuilder();
        if (data.fnbs() != null && !data.fnbs().isEmpty()) {
            fnbBlock.append("<div style=\"margin-top:18px;\"><div style=\"font-weight:700;color:#111;margin-bottom:6px;\">Combo / Đồ ăn kèm</div>");
            for (TicketEmailData.FnbLine f : data.fnbs()) {
                fnbBlock.append("<div style=\"font-size:14px;color:#444;\">• %s × %d</div>"
                        .formatted(escape(f.name()), f.quantity() == null ? 1 : f.quantity()));
            }
            fnbBlock.append("</div>");
        }

        // Tiêu đề phụ + lời mở + khối chính (QR hoặc lời cảm ơn) + chân trang đổi theo showQr.
        String subtitle;
        String intro;
        String mainBlock;
        String footer;
        if (data.showQr()) {
            // ĐƠN ONLINE → hiện 1 mã QR chung để khách ra rạp quét in vé.
            String bookingQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data="
                    + URLEncoder.encode(data.bookingCode() == null ? "" : data.bookingCode(), StandardCharsets.UTF_8);
            subtitle = "Vé điện tử của bạn đã sẵn sàng";
            intro = "Cảm ơn bạn đã đặt vé tại DevCine. Vui lòng đưa mã QR bên dưới tại quầy để check-in.";
            mainBlock = """
                    <div style="font-weight:700;color:#111;margin:22px 0 8px;">Vé & mã QR</div>
                    <div style="text-align:center;background:#fafafa;border:1px solid #eee;border-radius:12px;padding:22px;">
                      <img src="%s" alt="QR đơn hàng" width="200" height="200" style="border:1px solid #eee;border-radius:10px;background:#fff;" />
                      <div style="font-size:12px;color:#888;margin-top:14px;">Quý khách vui lòng tới quầy dịch vụ xuất trình mã vé này để được nhận vé.</div>
                    </div>
                    """.formatted(bookingQrUrl);
            footer = "Vui lòng đến trước giờ chiếu 15–30 phút. Mã QR đại diện cho cả đơn — chỉ cần quét một lần duy nhất tại quầy.<br/>Đây là email tự động, vui lòng không trả lời — DevCine Cinema";
        } else {
            // ĐƠN POS / ĐÃ IN VÉ GIẤY → ẩn QR, chỉ hoá đơn + lời cảm ơn.
            subtitle = "Hoá đơn thanh toán đơn hàng";
            intro = "Cảm ơn bạn đã đặt vé tại DevCine. Dưới đây là hoá đơn thanh toán của bạn.";
            mainBlock = """
                    <div style="text-align:center;background:#faf6e6;border:1px solid #f0e4b8;border-radius:12px;padding:26px;margin-top:8px;">
                      <div style="font-size:15px;color:#8a6d00;font-weight:700;">Cảm ơn bạn đã sử dụng dịch vụ tại DevCine.</div>
                      <div style="font-size:14px;color:#555;margin-top:6px;">Chúc bạn xem phim vui vẻ!</div>
                    </div>
                    """;
            footer = "Vé giấy đã được in tại quầy — vui lòng giữ vé để vào phòng chiếu.<br/>Đây là email tự động, vui lòng không trả lời — DevCine Cinema";
        }

        return """
                <div style="max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#f5c518,#e0b400);padding:22px 24px;">
                    <div style="color:#3d2f00;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#6b5200;font-size:13px;margin-top:4px;">%s</div>
                  </div>
                  <div style="padding:24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 4px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">%s</p>

                    <table style="width:100%%;border-collapse:collapse;background:#fafafa;border-radius:10px;">
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Mã đặt vé</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#8a6d00;font-size:15px;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Phim</td><td style="padding:8px 14px;text-align:right;font-weight:600;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Rạp / Phòng</td><td style="padding:8px 14px;text-align:right;color:#111;">%s • %s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Suất chiếu</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Ghế ngồi</td><td style="padding:8px 14px;text-align:right;font-weight:600;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Thanh toán</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Tổng tiền</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#111;font-size:16px;">%s</td></tr>
                    </table>

                    %s
                    %s

                    <p style="font-size:12px;color:#999;margin-top:24px;line-height:1.6;">%s</p>
                  </div>
                </div>
                """.formatted(
                escape(subtitle),
                escape(data.customerName()),
                escape(intro),
                escape(data.bookingCode()),
                escape(movieDisplay),
                escape(data.cinemaName()), escape(data.roomName()),
                escape(time),
                escape(seatDisplay),
                escape(paymentLabel(data.paymentMethod())),
                price,
                mainBlock,
                fnbBlock.toString(),
                footer);   // footer chứa <br/> nên KHÔNG escape
    }

    private String formatSeatType(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        return switch (type.trim().toUpperCase()) {
            case "STANDARD", "NORMAL" -> "Thường";
            case "VIP" -> "VIP";
            case "SWEETBOX", "DOUBLE", "COUPLE" -> "Sweetbox";
            default -> type.trim();
        };
    }

    private String formatSeats(java.util.List<TicketEmailData.SeatLine> seats) {
        if (seats == null || seats.isEmpty()) {
            return "—";
        }
        java.util.Map<String, java.util.List<String>> byType = new java.util.LinkedHashMap<>();
        for (TicketEmailData.SeatLine s : seats) {
            if (s == null || s.seatLabel() == null || s.seatLabel().isBlank()) {
                continue;
            }
            String type = formatSeatType(s.seatType());
            byType.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(s.seatLabel());
        }
        if (byType.isEmpty()) {
            return "—";
        }
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, java.util.List<String>> entry : byType.entrySet()) {
            String labels = String.join(", ", entry.getValue());
            if (!entry.getKey().isBlank()) {
                parts.add(labels + " (" + entry.getKey() + ")");
            } else {
                parts.add(labels);
            }
        }
        return String.join(", ", parts);
    }

    private String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount.longValue()) + "đ";
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
