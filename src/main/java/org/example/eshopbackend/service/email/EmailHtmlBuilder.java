package org.example.eshopbackend.service.email;

import lombok.RequiredArgsConstructor;
import org.example.eshopbackend.entity.OrderEntity;
import org.example.eshopbackend.entity.OrderItemEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EmailHtmlBuilder {

    @Value("${ppl.sender.name:}") private String sellerName;
    @Value("${ppl.sender.street:}") private String sellerStreet;
    @Value("${ppl.sender.city:}") private String sellerCity;
    @Value("${ppl.sender.zip-code:}") private String sellerZip;
    @Value("${ppl.sender.country:CZ}") private String sellerCountry;
    @Value("${ppl.sender.phone:}") private String sellerPhone;
    @Value("${ppl.sender.email:}") private String sellerEmail;

    public String buildHtml(OrderEntity o, String qrCid, String vs, String iban, String paymentMsg) {
        StringBuilder items = new StringBuilder();
        if (o.getItems() != null) {
            for (OrderItemEntity it : o.getItems()) {
                String name = escape(firstNonBlank(it.getNameOfProduct(), it.getName(), "Položka"));
                long line = safeLong(it.getLineTotalCzk());
                long unit = safeLong(it.getUnitPriceCzk());
                int qty = it.getAmountOfProducts() > 0 ? it.getAmountOfProducts() : 1;
                items.append("""
                    <tr>
                      <td style="padding:8px;border-bottom:1px solid #eee">%s</td>
                      <td style="padding:8px;border-bottom:1px solid #eee;text-align:right">%s</td>
                      <td style="padding:8px;border-bottom:1px solid #eee;text-align:right">%d×</td>
                      <td style="padding:8px;border-bottom:1px solid #eee;text-align:right"><b>%s</b></td>
                    </tr>
                """.formatted(name, fmtCzk(unit), qty, fmtCzk(line)));
            }
        }
        return """
            <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;line-height:1.45;color:#222">
              <h2 style="margin:0 0 8px 0">Děkujeme za vaši objednávku!</h2>
              <p style="margin:4px 0">Objednávka č. <b>%s</b> byla přijata a zpracovává se.</p>
              <h3 style="margin:16px 0 8px 0">Souhrn objednávky</h3>
              <table style="width:100%%;border-collapse:collapse">
                <thead><tr><th style="text-align:left;padding:8px;border-bottom:2px solid #333">Položka</th><th style="text-align:right;padding:8px;border-bottom:2px solid #333">Cena/ks</th><th style="text-align:right;padding:8px;border-bottom:2px solid #333">Množství</th><th style="text-align:right;padding:8px;border-bottom:2px solid #333">Řádek</th></tr></thead>
                <tbody>%s</tbody>
                <tfoot>
                  <tr><td colspan="3" style="padding:8px;text-align:right"><b>Mezisoučet</b></td><td style="padding:8px;text-align:right"><b>%s</b></td></tr>
                  <tr><td colspan="3" style="padding:8px;text-align:right">Doprava</td><td style="padding:8px;text-align:right">%s</td></tr>
                  <tr><td colspan="3" style="padding:8px;text-align:right;font-size:16px"><b>Celkem k úhradě</b></td><td style="padding:8px;text-align:right;font-size:16px"><b>%s</b></td></tr>
                </tfoot>
              </table>
              <h3 style="margin:20px 0 8px 0">Platba převodem / QR</h3>
              <table style="width:auto">
                <tr><td style="vertical-align:top;padding-right:16px"><img src="cid:%s" alt="QR platba" width="180" height="180" style="border:1px solid #eee;border-radius:8px" /></td>
                    <td style="vertical-align:top"><p style="margin:4px 0"><b>Částka:</b> %s</p><p style="margin:4px 0"><b>IBAN:</b> %s</p>%s<p style="margin:4px 0"><b>Zpráva příjemci:</b> %s</p></td></tr>
              </table>
              <h3 style="margin:20px 0 8px 0">Dodací údaje</h3>
              <p style="margin:0">%s %s</p><p style="margin:0">%s</p><p style="margin:0">%s %s</p><p style="margin:0">%s</p>%s
              <hr style="margin:20px 0;border:none;border-top:1px solid #eee" /><h4 style="margin:0 0 6px 0">Kontakt prodejce</h4>%s
              <p style="margin-top:16px;color:#555">V příloze najdete Obchodní podmínky.</p>
            </div>
        """.formatted(escape(o.getOrderNumber()), items.toString(), fmtCzk(safeLong(o.getSubtotalCzk())), fmtCzk(safeLong(o.getShippingCzk())), fmtCzk(safeLong(o.getTotalCzk())),
                qrCid, fmtCzk(safeLong(o.getTotalCzk())), escape(iban), (vs != null ? "<p style=\"margin:4px 0\"><b>VS:</b> " + escape(vs) + "</p>" : ""), escape(paymentMsg),
                escape(orEmpty(o.getCustomerFirstName())), escape(orEmpty(o.getCustomerLastName())), escape(orEmpty(o.getShipStreet())) + (StringUtils.hasText(o.getShipHouseNumber()) ? " " + escape(o.getShipHouseNumber()) : "") + (StringUtils.hasText(o.getShipOrientationNumber()) ? "/" + escape(o.getShipOrientationNumber()) : ""),
                escape(orEmpty(o.getShipPostalCode())), escape(orEmpty(o.getShipCity())), escape(orEmpty(o.getShipCountryCode())), (StringUtils.hasText(o.getCustomerPhone()) ? "<p style=\"margin:0\">Tel: " + escape(o.getCustomerPhone()) + "</p>" : ""), sellerBlockHtml());
    }

    public String buildShippedHtml(OrderEntity o) {
        return """
          <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;line-height:1.45;color:#222">
            <h2 style="margin:0 0 8px 0">Vaše objednávka byla předána dopravci</h2>
            <p style="margin:4px 0">Objednávka č. <b>%s</b> byla předána společnosti <b>PPL</b> k doručení.</p>
            <h3 style="margin:16px 0 8px 0">Dodací údaje</h3>
            <p style="margin:0">%s %s</p><p style="margin:0">%s</p><p style="margin:0">%s %s</p><p style="margin:0">%s</p>%s
            <p style="margin-top:16px">Jakmile bude k dispozici sledovací číslo zásilky, obdržíte ho od PPL (nebo v dalším e-mailu).</p>
            <hr style="margin:20px 0;border:none;border-top:1px solid #eee" /><h4 style="margin:0 0 6px 0">Kontakt prodejce</h4>%s
          </div>
        """.formatted(escape(o.getOrderNumber()), escape(orEmpty(o.getCustomerFirstName())), escape(orEmpty(o.getCustomerLastName())),
                escape(orEmpty(o.getShipStreet())) + (StringUtils.hasText(o.getShipHouseNumber()) ? " " + escape(o.getShipHouseNumber()) : "") + (StringUtils.hasText(o.getShipOrientationNumber()) ? "/" + escape(o.getShipOrientationNumber()) : ""),
                escape(orEmpty(o.getShipPostalCode())), escape(orEmpty(o.getShipCity())), escape(orEmpty(o.getShipCountryCode())), (StringUtils.hasText(o.getCustomerPhone()) ? "<p style=\"margin:0\">Tel: " + escape(o.getCustomerPhone()) + "</p>" : ""), sellerBlockHtml());
    }

    public String buildHandoverHtml(OrderEntity o, String trackingNumber, String trackingUrl) {
        String trackingRow = "";
        if (StringUtils.hasText(trackingNumber)) trackingRow += "<p style=\"margin:4px 0\"><b>Sledovací číslo:</b> " + escape(trackingNumber) + "</p>";
        if (StringUtils.hasText(trackingUrl)) trackingRow += "<p style=\"margin:10px 0\"><a href=\"%s\" target=\"_blank\" style=\"display:inline-block;padding:10px 14px;border-radius:8px;background:#111;color:#fff;text-decoration:none\">Sledovat zásilku</a></p>".formatted(escape(trackingUrl));
        return """
        <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;line-height:1.45;color:#222">
          <h2 style="margin:0 0 8px 0">Vaše objednávka byla předána dopravci PPL</h2>
          <p style="margin:4px 0">Objednávka č. <b>%s</b> byla dnes předána kurýrní službě PPL k doručení.</p>
          %s
          <h3 style="margin:16px 0 8px 0">Rekapitulace</h3>
          <p style="margin:4px 0"><b>Celkem k úhradě:</b> %s</p>
          <p style="margin:4px 0"><b>Doručovací adresa:</b><br/>%s %s<br/>%s<br/>%s %s<br/>%s</p>
          <hr style="margin:20px 0;border:none;border-top:1px solid #eee" /><h4 style="margin:0 0 6px 0">Kontakt prodejce</h4>%s
          <p style="margin-top:16px;color:#555">Děkujeme za nákup.</p>
        </div>
    """.formatted(escape(o.getOrderNumber()), trackingRow, fmtCzk(safeLong(o.getTotalCzk())), escape(orEmpty(o.getCustomerFirstName())), escape(orEmpty(o.getCustomerLastName())),
                escape(orEmpty(o.getShipStreet())) + (StringUtils.hasText(o.getShipHouseNumber()) ? " " + escape(o.getShipHouseNumber()) : "") + (StringUtils.hasText(o.getShipOrientationNumber()) ? "/" + escape(o.getShipOrientationNumber()) : ""),
                escape(orEmpty(o.getShipPostalCode())), escape(orEmpty(o.getShipCity())), escape(orEmpty(o.getShipCountryCode())), sellerBlockHtml());
    }

    public String buildPaymentReceivedHtml(OrderEntity o) {
        StringBuilder items = new StringBuilder();
        if (o.getItems() != null) {
            for (OrderItemEntity it : o.getItems()) {
                String name = escape(firstNonBlank(it.getNameOfProduct(), it.getName(), "Položka"));
                items.append("<tr><td style=\"padding:8px;border-bottom:1px solid #eee\">%s</td><td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right\">%s</td><td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right\">%d×</td><td style=\"padding:8px;border-bottom:1px solid #eee;text-align:right\"><b>%s</b></td></tr>"
                        .formatted(name, fmtCzk(safeLong(it.getUnitPriceCzk())), (it.getAmountOfProducts() > 0 ? it.getAmountOfProducts() : 1), fmtCzk(safeLong(it.getLineTotalCzk()))));
            }
        }
        return """
            <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;line-height:1.45;color:#222">
              <h2 style="margin:0 0 8px 0; color: #2e7d32;">Platba přijata</h2>
              <p style="margin:4px 0">Potvrzujeme přijetí platby k objednávce č. <b>%s</b>.</p><p style="margin:4px 0">Zboží začínáme připravovat k odeslání.</p>
              <h3 style="margin:16px 0 8px 0">Rekapitulace zaplacené objednávky</h3>
              <table style="width:100%%;border-collapse:collapse">
                <thead><tr><th style="text-align:left;padding:8px;border-bottom:2px solid #333">Položka</th><th style="text-align:right;padding:8px;border-bottom:2px solid #333">Cena/ks</th><th style="text-align:right;padding:8px;border-bottom:2px solid #333">Množství</th><th style="text-align:right;padding:8px;border-bottom:2px solid #333">Řádek</th></tr></thead>
                <tbody>%s</tbody>
                <tfoot>
                  <tr><td colspan="3" style="padding:8px;text-align:right"><b>Mezisoučet</b></td><td style="padding:8px;text-align:right"><b>%s</b></td></tr>
                  <tr><td colspan="3" style="padding:8px;text-align:right">Doprava</td><td style="padding:8px;text-align:right">%s</td></tr>
                  <tr><td colspan="3" style="padding:8px;text-align:right;font-size:16px"><b>Celkem uhrazeno</b></td><td style="padding:8px;text-align:right;font-size:16px;color:#2e7d32"><b>%s</b></td></tr>
                </tfoot>
              </table>
              <h3 style="margin:20px 0 8px 0">Dodací údaje</h3>
              <p style="margin:0">%s %s</p><p style="margin:0">%s</p><p style="margin:0">%s %s</p><p style="margin:0">%s</p>%s
              <hr style="margin:20px 0;border:none;border-top:1px solid #eee" /><h4 style="margin:0 0 6px 0">Kontakt prodejce</h4>%s
            </div>
        """.formatted(escape(o.getOrderNumber()), items.toString(), fmtCzk(safeLong(o.getSubtotalCzk())), fmtCzk(safeLong(o.getShippingCzk())), fmtCzk(safeLong(o.getTotalCzk())),
                escape(orEmpty(o.getCustomerFirstName())), escape(orEmpty(o.getCustomerLastName())), escape(orEmpty(o.getShipStreet())) + (StringUtils.hasText(o.getShipHouseNumber()) ? " " + escape(o.getShipHouseNumber()) : "") + (StringUtils.hasText(o.getShipOrientationNumber()) ? "/" + escape(o.getShipOrientationNumber()) : ""),
                escape(orEmpty(o.getShipPostalCode())), escape(orEmpty(o.getShipCity())), escape(orEmpty(o.getShipCountryCode())), (StringUtils.hasText(o.getCustomerPhone()) ? "<p style=\"margin:0\">Tel: " + escape(o.getCustomerPhone()) + "</p>" : ""), sellerBlockHtml());
    }

    private String sellerBlockHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<p style=\"margin:0\"><b>").append(escape(sellerName)).append("</b></p>");
        if (StringUtils.hasText(sellerStreet)) sb.append("<p style=\"margin:0\">").append(escape(sellerStreet)).append("</p>");
        if (StringUtils.hasText(sellerCity) || StringUtils.hasText(sellerZip)) sb.append("<p style=\"margin:0\">").append(escape(sellerZip)).append(" ").append(escape(sellerCity)).append("</p>");
        if (StringUtils.hasText(sellerCountry)) sb.append("<p style=\"margin:0\">").append(escape(sellerCountry)).append("</p>");
        if (StringUtils.hasText(sellerPhone)) sb.append("<p style=\"margin:0\">Tel: ").append(escape(sellerPhone)).append("</p>");
        if (StringUtils.hasText(sellerEmail)) sb.append("<p style=\"margin:0\">E-mail: ").append(escape(sellerEmail)).append("</p>");
        return sb.toString();
    }

    private String firstNonBlank(String a, String b, String fallback) { return StringUtils.hasText(a) ? a : (StringUtils.hasText(b) ? b : fallback); }
    private String escape(String s) { return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    private String orEmpty(String s) { return s == null ? "" : s; }
    private long safeLong(Number n) { return n == null ? 0L : n.longValue(); }
    private String fmtCzk(long v) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("cs", "CZ"));
        nf.setMaximumFractionDigits(0); nf.setMinimumFractionDigits(0);
        return nf.format(v) + " Kč";
    }
}