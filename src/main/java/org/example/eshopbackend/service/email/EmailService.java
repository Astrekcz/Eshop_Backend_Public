package org.example.eshopbackend.service.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.util.QrGenerator;
import org.example.eshopbackend.util.Spayd;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailHtmlBuilder htmlBuilder;

    @Value("${spring.mail.username}") private String from;
    @Value("${payments.bank.iban}") private String iban;
    @Value("${payments.bank.bic:}") private String bic;
    @Value("${mail.terms.classpath:static/Obchodni_podminky.pdf}") private String termsClasspath;
    @Value("${ppl.sender.name:}") private String sellerName;

    private String paymentMessage(OrderEntity o) {
        return "Objednávka " + o.getOrderNumber() + " – " + (StringUtils.hasText(sellerName) ? sellerName : "Prodejce");
    }

    public void sendOrderConfirmation(OrderEntity order) {
        try {
            String vs = safeVs(order);
            String pMsg = paymentMessage(order);
            String spayd = Spayd.build(iban, toHaler(order.getTotalCzk()), vs, pMsg, StringUtils.hasText(bic) ? bic : null);
            byte[] qrPng = QrGenerator.toPng(spayd, 360);
            String qrCid = "qr-" + order.getOrderNumber() + "@zeniq";
            String html = htmlBuilder.buildHtml(order, qrCid, vs, iban, pMsg);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(order.getCustomerEmail()); helper.setBcc(from);
            helper.setSubject("Potvrzení objednávky č. " + order.getOrderNumber());
            helper.setText(html, true);
            helper.addInline(qrCid, (InputStreamSource) () -> new java.io.ByteArrayInputStream(qrPng), "image/png");
            attachTermsIfPresent(helper);
            mailSender.send(message);
        } catch (Exception e) { throw new RuntimeException("Nepodařilo se poslat potvrzení objednávky", e); }
    }

    public void sendOrderShipped(OrderEntity order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(order.getCustomerEmail()); helper.setBcc(from);
            helper.setSubject("Objednávka " + order.getOrderNumber() + " byla předána dopravci (PPL)");
            helper.setText(htmlBuilder.buildShippedHtml(order), true);
            mailSender.send(message);
        } catch (Exception e) { throw new RuntimeException("Nepodařilo se poslat e-mail o předání dopravci", e); }
    }

    public void sendShipmentHandedOver(OrderEntity order, String trackingNumber, String trackingUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(order.getCustomerEmail()); helper.setBcc(from);
            helper.setSubject("Objednávka č. " + order.getOrderNumber() + " byla předána dopravci PPL");
            helper.setText(htmlBuilder.buildHandoverHtml(order, trackingNumber, trackingUrl), true);
            mailSender.send(message);
        } catch (Exception e) { throw new RuntimeException("Nepodařilo se poslat e-mail o předání dopravci", e); }
    }

    public void sendPaymentReceived(OrderEntity order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from); helper.setTo(order.getCustomerEmail()); helper.setBcc(from);
            helper.setSubject("Potvrzení platby - objednávka č. " + order.getOrderNumber());
            helper.setText(htmlBuilder.buildPaymentReceivedHtml(order), true);
            mailSender.send(message);
        } catch (Exception e) { throw new RuntimeException("Nepodařilo se poslat potvrzení o platbě", e); }
    }

    private void attachTermsIfPresent(MimeMessageHelper helper) {
        try {
            Resource res = new ClassPathResource(termsClasspath);
            if (res.exists() && res.isReadable()) helper.addAttachment("Obchodni_podminky.pdf", res);
        } catch (Exception ex) { log.warn("Failed to attach terms PDF: {}", ex.getMessage()); }
    }

    private Long toHaler(Number czk) { return czk == null ? null : czk.longValue() * 100L; }
    private String safeVs(OrderEntity o) {
        if (StringUtils.hasText(o.getBankVs())) return o.getBankVs().replaceAll("\\D+", "");
        if (StringUtils.hasText(o.getOrderNumber())) {
            String onlyDigits = o.getOrderNumber().replaceAll("\\D+", "");
            if (onlyDigits.length() > 0 && onlyDigits.length() <= 10) return onlyDigits;
            if (onlyDigits.length() > 10) return onlyDigits.substring(onlyDigits.length() - 10);
        }
        return null;
    }
}