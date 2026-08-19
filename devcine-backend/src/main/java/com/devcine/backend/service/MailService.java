package com.devcine.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.devcine.backend.dto.CancellationEmailData;
import com.devcine.backend.dto.CompensationEmailData;
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
            voucherBlock = """
                    <div style="font-weight:700;color:#111;margin:22px 0 8px;">Đền bù dành cho bạn</div>
                    <div style="text-align:center;background:#faf6e6;border:1px dashed #e0b400;border-radius:12px;padding:20px;">
                      <div style="font-size:13px;color:#8a6d00;margin-bottom:8px;">%s</div>
                      <span style="display:inline-block;font-size:22px;font-weight:800;letter-spacing:4px;color:#111;">%s</span>
                      <div style="font-size:12px;color:#888;margin-top:10px;">Mã đã được lưu vào ví <b>"Ưu đãi của tôi"</b>, hiệu lực 90 ngày.</div>
                    </div>
                    """.formatted(escape(data.voucherLabel() != null ? data.voucherLabel() : "Voucher đền bù"),
                                  escape(data.voucherCode()));
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

    /**
     * Email ĐỢT 1 khi cụm rạp đóng cửa đột xuất: chỉ XIN LỖI + báo suất đã hủy, TUYỆT ĐỐI CHƯA
     * kèm voucher. Thông tin đền bù sẽ gửi ở email Đợt 2 sau khi Admin phê duyệt phát hàng loạt.
     * Best-effort & {@link Async}: một email lỗi không chặn cả batch; tắt mail thì bỏ qua.
     */
    @Async
    public void sendEmergencyApologyEmail(CancellationEmailData data) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua email xin lỗi (Đợt 1) đơn {}", data.bookingCode());
            return;
        }
        if (data.toEmail() == null || data.toEmail().isBlank()) {
            log.warn("Bỏ qua email xin lỗi (Đợt 1) đơn {}: khách chưa có email", data.bookingCode());
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(data.toEmail());
            helper.setSubject("DevCine • Thông báo hủy suất chiếu đơn " + data.bookingCode());
            helper.setText(buildEmergencyApologyHtml(data), true);
            mailSender.send(message);
            log.info("Đã gửi email xin lỗi (Đợt 1) tới {} cho đơn {}", data.toEmail(), data.bookingCode());
        } catch (Exception e) {
            log.error("Gửi email xin lỗi (Đợt 1) thất bại cho đơn {}: {}", data.bookingCode(), e.getMessage(), e);
        }
    }

    private String buildEmergencyApologyHtml(CancellationEmailData data) {
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

        return """
                <div style="max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#c0392b,#8a1f1f);padding:22px 24px;">
                    <div style="color:#fff;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#ffd9d3;font-size:13px;margin-top:4px;">Thông báo khẩn — Hủy suất chiếu</div>
                  </div>
                  <div style="padding:24px;">
                    <p style="font-size:15px;color:#111;margin:0 0 4px;">Xin chào <b>%s</b>,</p>
                    <p style="font-size:14px;color:#555;margin:0 0 18px;">DevCine vô cùng xin lỗi phải thông báo suất chiếu của bạn đã bị <b>hủy</b> do <b>%s</b>. Chúng tôi thành thật xin lỗi vì sự bất tiện ngoài ý muốn này.</p>

                    <table style="width:100%%;border-collapse:collapse;background:#fafafa;border-radius:10px;">
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Mã đặt vé</td><td style="padding:8px 14px;text-align:right;font-weight:700;color:#8a1f1f;font-size:15px;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Phim</td><td style="padding:8px 14px;text-align:right;font-weight:600;color:#111;">%s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Rạp / Phòng</td><td style="padding:8px 14px;text-align:right;color:#111;">%s • %s</td></tr>
                      <tr><td style="padding:8px 14px;color:#888;font-size:13px;">Suất chiếu</td><td style="padding:8px 14px;text-align:right;color:#111;">%s</td></tr>
                    </table>

                    <div style="font-weight:700;color:#111;margin:18px 0 4px;">Ghế đã hủy</div>
                    %s

                    <div style="margin-top:22px;background:#fff8e6;border:1px solid #f0d98a;border-radius:12px;padding:16px 18px;">
                      <div style="font-weight:700;color:#8a6d00;margin-bottom:4px;">Về việc đền bù</div>
                      <div style="font-size:13px;color:#7a6a2f;line-height:1.6;">Bộ phận chăm sóc khách hàng của DevCine đang xử lý phương án đền bù xứng đáng cho bạn. Chúng tôi sẽ gửi <b>thông tin đền bù chi tiết trong một email tiếp theo</b> trong thời gian sớm nhất. Rất mong bạn thông cảm và chờ trong giây lát.</div>
                    </div>

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
                seatBlock.toString());
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

        int seatCount = data.seats() != null ? data.seats().size() : 0;

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
                      <div style="font-size:12px;color:#888;margin-top:14px;">Đưa mã QR này tại quầy để check-in cho <b>toàn bộ đơn</b> (%d ghế)</div>
                    </div>
                    """.formatted(bookingQrUrl, seatCount);
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
                escape(data.movieTitle()),
                escape(data.cinemaName()), escape(data.roomName()),
                escape(time),
                escape(paymentLabel(data.paymentMethod())),
                price,
                mainBlock,
                fnbBlock.toString(),
                footer);   // footer chứa <br/> nên KHÔNG escape
    }

    // ===================== EMAIL ĐỀN BÙ ĐỢT 2 (4 MẪU) =====================

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String fmtDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "—";
    }

    /**
     * Gửi email ĐỀN BÙ Đợt 2 sau khi Admin duyệt phát voucher (hoặc email đổi ghế Mẫu 3). Định tuyến
     * 1 trong 4 mẫu theo {@link CompensationEmailData#templateType()} — giữ NGUYÊN VĂN nội dung mẫu.
     * Best-effort & {@link Async}: một email lỗi không chặn cả batch; tắt mail thì bỏ qua.
     */
    @Async
    public void sendCompensationEmail(CompensationEmailData data) {
        if (!enabled) {
            log.info("mail.enabled=false → bỏ qua email đền bù (Đợt 2) mẫu {}", data.templateType());
            return;
        }
        if (data.toEmail() == null || data.toEmail().isBlank()) {
            log.warn("Bỏ qua email đền bù (Đợt 2) mẫu {}: khách chưa có email", data.templateType());
            return;
        }
        try {
            String subject;
            String html;
            switch (data.templateType()) {
                case CompensationEmailData.MONEY_VOUCHER -> {
                    subject = "[DevCine] Gửi tặng Voucher đền bù sự cố hủy suất chiếu - Phim " + data.movieTitle();
                    html = buildMoneyVoucherHtml(data);
                }
                case CompensationEmailData.TICKET_VOUCHER -> {
                    subject = "[DevCine] Gửi tặng Voucher đền bù sự cố hủy suất chiếu - Phim " + data.movieTitle();
                    html = buildTicketVoucherHtml(data);
                }
                case CompensationEmailData.SEAT_CHANGE -> {
                    subject = "[DevCine] Cáo lỗi về sự cố vị trí ghế ngồi suất chiếu - Phim " + data.movieTitle();
                    html = buildSeatChangeHtml(data);
                }
                case CompensationEmailData.DUAL -> {
                    subject = "[DevCine] Cáo lỗi sự cố gián đoạn suất chiếu & Thông tin đền bù đặc biệt - Phim " + data.movieTitle();
                    html = buildDualHtml(data);
                }
                default -> {
                    log.warn("Loại email đền bù không hợp lệ: {} (đơn phim {})", data.templateType(), data.movieTitle());
                    return;
                }
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(data.toEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email đền bù (mẫu {}) tới {} (phim {})", data.templateType(), data.toEmail(), data.movieTitle());
        } catch (Exception e) {
            log.error("Gửi email đền bù (mẫu {}) thất bại tới {}: {}", data.templateType(), data.toEmail(), e.getMessage(), e);
        }
    }

    /** Khung chung (header DevCine + ghi chú email tự động) bọc quanh nội dung mẫu. */
    private String compShell(String subtitle, String innerHtml) {
        return """
                <div style="max-width:560px;margin:0 auto;font-family:Arial,Helvetica,sans-serif;background:#fff;border:1px solid #eee;border-radius:14px;overflow:hidden;">
                  <div style="background:linear-gradient(135deg,#c0392b,#8a1f1f);padding:22px 24px;">
                    <div style="color:#fff;font-size:22px;font-weight:800;letter-spacing:.5px;">DevCine</div>
                    <div style="color:#ffd9d3;font-size:13px;margin-top:4px;">%s</div>
                  </div>
                  <div style="padding:24px;font-size:14px;color:#333;line-height:1.7;">
                %s
                  </div>
                </div>
                """.formatted(escape(subtitle), innerHtml);
    }

    /** Thẻ mã voucher + danh sách quyền lợi (lines đã là HTML an toàn — giá trị động đã escape khi dựng). */
    private String voucherCard(String code, List<String> lines) {
        StringBuilder li = new StringBuilder();
        for (String l : lines) li.append("<li style=\"margin:4px 0;\">").append(l).append("</li>");
        return """
                <div style="text-align:center;background:#faf6e6;border:1px dashed #e0b400;border-radius:12px;padding:18px;margin:12px 0 6px;">
                  <div style="font-size:12px;color:#8a6d00;margin-bottom:6px;">Mã Voucher</div>
                  <span style="display:inline-block;font-size:22px;font-weight:800;letter-spacing:4px;color:#111;">%s</span>
                </div>
                <ul style="font-size:13px;color:#444;line-height:1.7;padding-left:18px;margin:6px 0 0;">%s</ul>
                """.formatted(escape(code), li.toString());
    }

    /** Mẫu 1 — Quy đổi thành Voucher mệnh giá tiền (đơn phức tạp, giá trị cao). */
    private String buildMoneyVoucherHtml(CompensationEmailData d) {
        String inner = """
                <p>Chào bạn,</p>
                <p>DevCine một lần nữa chân thành xin lỗi bạn vì sự cố kỹ thuật dẫn đến việc hủy suất chiếu <b>%s</b> vào ngày <b>%s</b> vừa qua.</p>
                <p>Để hỗ trợ xử lý sự cố nhanh chóng và đảm bảo quyền lợi cho bạn, thay cho quy trình hoàn tiền qua cổng thanh toán, DevCine xin gửi tặng bạn 01 Mã Voucher Đặc Biệt có giá trị <b>%s</b> giá trị đơn hàng bạn đã thanh toán. Bạn có thể sử dụng mã này để đặt vé cho bất kỳ suất chiếu nào khác trong thời gian tới. Thông tin Voucher của bạn:</p>
                %s
                <p style="font-size:12px;color:#777;margin-top:10px;">(Lưu ý: Voucher có thể áp dụng đồng thời cho cả giao dịch mua bắp, nước tại rạp).</p>
                <p>DevCine rất hy vọng mã voucher này sẽ giúp bạn có một trải nghiệm điện ảnh trọn vẹn và thoải mái hơn trong lần ghé thăm sắp tới. Nếu bạn cần bất kỳ hỗ trợ nào thêm, xin vui lòng liên hệ hotline hoặc phản hồi lại email này.</p>
                <p>Cảm ơn bạn đã thấu hiểu và đồng hành cùng DevCine!</p>
                <p style="margin-top:14px;">Trân trọng,<br/><b>DevCine Cinema</b></p>
                """.formatted(
                escape(d.movieTitle()),
                escape(fmtDate(d.showDate())),
                escape(d.voucherBenefitLabel()),
                voucherCard(d.voucherCode(), List.of(
                        "Giá trị ưu đãi: Giảm <b>" + escape(d.voucherBenefitLabel()) + "</b> cho tổng hóa đơn.",
                        "Hạn sử dụng: Đến hết ngày <b>" + escape(fmtDate(d.voucherExpiry())) + "</b>.",
                        "Phạm vi áp dụng: Đặt vé trực tuyến trên hệ thống DevCine hoặc mua trực tiếp tại quầy.")));
        return compShell("Voucher đền bù sự cố hủy suất chiếu", inner);
    }

    /** Mẫu 2 — Quy đổi thành Voucher đổi vé xem phim miễn phí (đơn thuần vé). */
    private String buildTicketVoucherHtml(CompensationEmailData d) {
        String inner = """
                <p>Chào bạn,</p>
                <p>Liên quan đến sự cố hủy suất chiếu <b>%s</b> ngày <b>%s</b>, DevCine rất tiếc vì đã làm gián đoạn kế hoạch giải trí của bạn.</p>
                <p>Thay cho lời tạ lỗi sâu sắc nhất và để đền bù cho đơn vé đã bị hủy, DevCine xin gửi tặng bạn Voucher giảm 100%% vé phim cho lần đặt tới, giúp bạn dễ dàng chọn lại một suất chiếu khác thay thế hoặc thưởng thức một bộ phim bất kỳ tại hệ thống rạp của chúng tôi. Thông tin Mã Đổi Vé:</p>
                %s
                <p>Mong bạn thông cảm cho sự cố ngoài ý muốn này. DevCine luôn mong muốn mang lại chất lượng phục vụ tốt nhất và rất mong sớm được gặp lại bạn tại rạp.</p>
                <p style="margin-top:14px;">Thân mến,<br/><b>DevCine Cinema</b></p>
                """.formatted(
                escape(d.movieTitle()),
                escape(fmtDate(d.showDate())),
                voucherCard(d.voucherCode(), List.of(
                        "Quyền lợi: Giảm 100% số tiền dựa trên số vé bị huỷ (Tương ứng với số vé bạn đã mua).",
                        "Hạn sử dụng: Đến hết ngày <b>" + escape(fmtDate(d.voucherExpiry())) + "</b>.",
                        "Hướng dẫn sử dụng: Bạn chỉ cần nhập mã Code này tại bước Thanh toán trên Website/App DevCine khi đặt vé online hoặc lưu vào kho ưu đãi trên tài khoản của bạn tại hệ thống DevCine.")));
        return compShell("Voucher đền bù sự cố hủy suất chiếu", inner);
    }

    /** Mẫu 3 — Nhóm sự cố nhẹ (đổi ghế, khách vẫn xem phim). Hai block: quà tại quầy HOẶC voucher. */
    private String buildSeatChangeHtml(CompensationEmailData d) {
        String giftBlock;
        if (d.counterGift()) {
            giftBlock = """
                    <p>Gửi tặng bạn <b>%s</b> miễn phí. Bạn chỉ cần đưa email này cho nhân viên tại quầy F&B trước khi vào phòng chiếu để nhận quà.</p>
                    """.formatted(escape(d.counterGiftLabel() != null ? d.counterGiftLabel() : "phần quà"));
        } else {
            giftBlock = """
                    <p>Gửi tặng bạn 01 Mã Voucher ưu đãi cho lần xem phim tiếp theo. Thông tin Voucher của bạn:</p>
                    %s
                    """.formatted(voucherCard(d.voucherCode(), List.of(
                    "Giá trị ưu đãi: Giảm <b>" + escape(d.voucherBenefitLabel()) + "</b> cho hóa đơn đặt vé hoặc bắp nước.",
                    "Hạn sử dụng: Đến hết ngày <b>" + escape(fmtDate(d.voucherExpiry())) + "</b>.")));
        }
        String inner = """
                <p>Chào bạn,</p>
                <p>DevCine chân thành xin lỗi bạn vì một số yếu tố kỹ thuật khách quan dẫn đến việc chúng tôi phải thay đổi vị trí ghế ngồi của bạn trong suất chiếu <b>%s</b> vào ngày <b>%s</b>.</p>
                <p>Dù suất chiếu vẫn diễn ra bình thường, nhưng chúng tôi hiểu sự thay đổi đột xuất này có thể mang lại chút bất tiện và ảnh hưởng đến trải nghiệm xem phim của bạn.</p>
                <p>Để gửi lời xin lỗi và cảm ơn sự thông cảm của bạn, DevCine xin gửi tặng bạn một phần quà nhỏ:</p>
                %s
                <p>DevCine luôn nỗ lực mang đến dịch vụ tốt nhất. Nếu bạn cần bất kỳ hỗ trợ nào thêm, xin vui lòng liên hệ hotline hoặc phản hồi lại email này.</p>
                <p>Cảm ơn bạn đã thấu hiểu và đồng hành cùng DevCine!</p>
                <p style="margin-top:14px;">Trân trọng,<br/><b>DevCine Cinema</b></p>
                """.formatted(
                escape(d.movieTitle()),
                escape(fmtDate(d.showDate())),
                giftBlock);
        return compShell("Cáo lỗi về sự cố vị trí ghế ngồi", inner);
    }

    /** Mẫu 4 — Đền bù kép (Voucher hoàn giá trị đơn + Voucher F&B) cho khách VIP / đơn có F&B. */
    private String buildDualHtml(CompensationEmailData d) {
        String inner = """
                <p>Chào bạn,</p>
                <p>DevCine vô cùng xin lỗi bạn vì sự cố <b>%s</b> buộc chúng tôi phải tạm ngưng hoạt động phòng chiếu và hủy suất chiếu <b>%s</b> vào ngày <b>%s</b> vừa qua. Chúng tôi hiểu sự việc này đã ảnh hưởng lớn đến kế hoạch giải trí của bạn.</p>
                <p>Thay cho lời tạ lỗi sâu sắc nhất và để đền bù xứng đáng cho sự bất tiện này, DevCine xin gửi tặng bạn <b>Gói Đền Bù Kép</b>, bao gồm Voucher hoàn trả toàn bộ giá trị đơn hàng và Quà tặng dịch vụ kèm theo. Thông tin chi tiết như sau:</p>
                <div style="font-weight:700;color:#111;margin:16px 0 2px;">1. Voucher đền bù giá trị đơn hàng</div>
                %s
                <div style="font-weight:700;color:#111;margin:18px 0 2px;">2. Voucher quà tặng Bắp Nước (F&B)</div>
                %s
                <p style="font-size:12px;color:#777;margin-top:10px;">(Lưu ý: Cả 2 voucher đều có thể sử dụng trực tuyến trên hệ thống DevCine hoặc mua trực tiếp tại quầy, và có thể áp dụng đồng thời trong cùng một lần ghé thăm).</p>
                <p>DevCine rất hy vọng gói đền bù này sẽ phần nào bù đắp được sự gián đoạn vừa qua, và mong bạn sẽ cho chúng tôi cơ hội được phục vụ bạn trọn vẹn hơn trong lần tới. Nếu bạn cần bất kỳ hỗ trợ nào thêm, xin vui lòng liên hệ hotline hoặc phản hồi lại email này.</p>
                <p>Cảm ơn bạn đã thấu hiểu và tiếp tục ủng hộ DevCine!</p>
                <p style="margin-top:14px;">Trân trọng,<br/><b>DevCine Cinema</b></p>
                """.formatted(
                escape(d.incidentReason() != null ? d.incidentReason() : "bảo trì đột xuất"),
                escape(d.movieTitle()),
                escape(fmtDate(d.showDate())),
                voucherCard(d.voucherCode(), List.of(
                        "Giá trị ưu đãi: Giảm <b>" + escape(d.voucherBenefitLabel()) + "</b> cho tổng hóa đơn đặt vé.",
                        "Hạn sử dụng: Đến hết ngày <b>" + escape(fmtDate(d.voucherExpiry())) + "</b>.")),
                voucherCard(d.fnbVoucherCode(), List.of(
                        "Quyền lợi: Đổi miễn phí <b>" + escape(d.fnbBenefitLabel() != null ? d.fnbBenefitLabel() : "phần bắp nước") + "</b> tại quầy.",
                        "Hạn sử dụng: Đến hết ngày <b>" + escape(fmtDate(d.fnbExpiry())) + "</b>.")));
        return compShell("Thông tin đền bù đặc biệt", inner);
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
