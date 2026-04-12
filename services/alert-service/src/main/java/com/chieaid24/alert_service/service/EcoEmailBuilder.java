package com.chieaid24.alert_service.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EcoEmailBuilder {

  private static final List<String> ECO_TIPS =
      List.of(
          "Unplug devices when not in use — standby power can account for 10% of household energy.",
          "Switch to LED bulbs — they use 75% less energy than incandescent lighting.",
          "Set your thermostat 2°C lower in winter and 2°C higher in summer to save up to 10%.",
          "Use smart power strips to eliminate phantom energy loads from idle electronics.",
          "Air-dry clothes instead of using a dryer to save up to 5 kWh per load.",
          "Run dishwashers and washing machines only with full loads to maximize efficiency.",
          "Regular HVAC maintenance can improve energy efficiency by up to 15%.",
          "Use natural light during the day — open curtains before reaching for the switch.",
          "Enable power-saving mode on all your IoT devices to reduce idle consumption.",
          "Plant shade trees near your home to reduce cooling costs by up to 25%.",
          "Seal air leaks around windows and doors — they can waste up to 30% of heating energy.",
          "Cook with lids on pots — it uses 70% less energy than cooking without them.");

  public String buildHtmlEmail(String message, double threshold, double energyConsumed) {
    char grade = calculateEcoScore(threshold, energyConsumed);
    String scoreColor = getScoreColor(grade);
    String tip = ECO_TIPS.get(ThreadLocalRandom.current().nextInt(ECO_TIPS.size()));
    double ratio = threshold > 0 ? (energyConsumed / threshold) * 100 : 0;

    // Convert Wh to kWh for display and tree calculation
    double thresholdKwh = threshold / 1000.0;
    double energyConsumedKwh = energyConsumed / 1000.0;
    int treesNeeded = calculateTreesNeeded(thresholdKwh, energyConsumedKwh);

    String fontStack =
        "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html><head><meta charset=\"UTF-8\"></head>");
    html.append(
        String.format(
            "<body style=\"margin:0;padding:0;background-color:#f0f7f0;font-family:%s;\">",
            fontStack));

    // Outer wrapper table
    html.append("<table width=\"100%\" style=\"background-color:#f0f7f0;\">");
    html.append("<tr><td align=\"center\" style=\"padding:32px 16px;\">");

    // Inner card table
    html.append(
        "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\""
            + " style=\"background:#ffffff;border-radius:12px;"
            + "box-shadow:0 1px 4px rgba(0,0,0,0.08),0 4px 12px rgba(0,0,0,0.06);"
            + "overflow:hidden;\">");

    // Header banner
    html.append("<tr><td style=\"background:#2e7d32;padding:20px 24px;text-align:center;\">");
    html.append(
        "<h1 style=\"color:#ffffff;margin:0;font-size:22px;font-weight:600;"
            + "letter-spacing:0.01em;\">&#127793; Energy Usage Alert &#127793;</h1>");
    html.append(
        "<p style=\"color:#c8e6c9;margin:6px 0 0;font-size:14px;font-weight:400;\">"
            + "IoT Energy Tracker</p>");
    html.append("</td></tr>");

    // Eco Score badge
    html.append("<tr><td style=\"padding:28px 24px 20px;text-align:center;\">");
    html.append(
        "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\"><tr><td"
            + " align=\"center\">");
    html.append(
        String.format(
            "<div style=\"display:inline-block;width:72px;height:72px;border-radius:50%%;"
                + "background:%s;color:#ffffff;font-size:32px;line-height:72px;"
                + "font-weight:700;text-align:center;\">%c</div>",
            scoreColor, grade));
    html.append("</td></tr></table>");
    html.append(
        "<p style=\"color:#333;margin:12px 0 0;font-size:14px;font-weight:600;"
            + "text-transform:uppercase;letter-spacing:0.05em;\">Your Eco Score</p>");
    html.append(
        String.format(
            "<p style=\"color:#888;margin:4px 0 0;font-size:14px;font-weight:400;\">"
                + "You used %.0f%% of your threshold</p>",
            ratio));
    html.append("</td></tr>");

    // Alert details card
    html.append("<tr><td style=\"padding:0 24px;\">");
    html.append(
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\""
            + " style=\"background:#f6faf6;border-radius:8px;border:1px solid #e0ece0;\">");
    html.append(
        "<tr><td style=\"color:#2e7d32;font-weight:600;font-size:14px;width:130px;"
            + "padding:12px 16px;border-bottom:1px solid #e0ece0;\">Alert</td>");
    html.append(
        String.format(
            "<td style=\"color:#333;font-size:15px;padding:12px 16px;"
                + "border-bottom:1px solid #e0ece0;\">%s</td></tr>",
            message));
    html.append(
        "<tr><td style=\"color:#2e7d32;font-weight:600;font-size:14px;"
            + "padding:12px 16px;border-bottom:1px solid #e0ece0;\">Threshold</td>");
    html.append(
        String.format(
            "<td style=\"color:#333;font-size:15px;padding:12px 16px;"
                + "border-bottom:1px solid #e0ece0;\">%.2f kWh</td></tr>",
            thresholdKwh));
    html.append(
        "<tr><td style=\"color:#2e7d32;font-weight:600;font-size:14px;"
            + "padding:12px 16px;\">Energy Consumed</td>");
    html.append(
        String.format(
            "<td style=\"color:#333;font-size:15px;padding:12px 16px;\">%.2f kWh</td></tr>",
            energyConsumedKwh));
    html.append("</table>");
    html.append("</td></tr>");

    // Trees needed section (only if excess usage)
    if (treesNeeded > 0) {
      html.append("<tr><td style=\"padding:24px;text-align:center;\">");
      html.append(
          "<p style=\"color:#555;margin:0 0 6px;font-size:14px;font-weight:600;\">"
              + "To offset your excess energy usage, you&#39;d need to plant approximately</p>");
      html.append(
          String.format(
              "<p style=\"font-size:30px;font-weight:700;color:#2e7d32;margin:0;"
                  + "letter-spacing:-0.01em;\">%d tree%s &#127794;</p>",
              treesNeeded, treesNeeded == 1 ? "" : "s"));
      html.append(
          String.format(
              "<p style=\"color:#999;font-size:13px;margin:6px 0 0;\">Based on %.2f kWh excess"
                  + " usage</p>",
              energyConsumedKwh - thresholdKwh));
      html.append("</td></tr>");
    } else {
      html.append("<tr><td style=\"padding:24px;text-align:center;\">");
      html.append(
          "<p style=\"font-size:16px;color:#2e7d32;font-weight:600;margin:0;\">&#127808; Great"
              + " job! You&#39;re within your energy threshold! &#127808;</p>");
      html.append("</td></tr>");
    }

    // Eco tip callout
    html.append("<tr><td style=\"padding:8px 24px 24px;\">");
    html.append(
        "<div style=\"background:#edf7ed;border-left:3px solid #2e7d32;padding:12px 16px;"
            + "border-radius:0 6px 6px 0;\">");
    html.append(
        String.format(
            "<strong style=\"color:#2e7d32;font-size:14px;\">&#127793; Eco Tip:</strong>"
                + " <span style=\"color:#444;font-size:14px;\">%s</span>",
            tip));
    html.append("</div>");
    html.append("</td></tr>");

    // Footer
    html.append(
        "<tr><td style=\"background:#f6faf6;padding:16px 24px;text-align:center;"
            + "border-top:1px solid #e0ece0;\">");
    html.append(
        "<p style=\"color:#777;font-size:13px;margin:0;\">&#127807; Together we can make a"
            + " greener future. &#127807;</p>");
    html.append(
        "<p style=\"color:#aaa;font-size:11px;margin:4px 0 0;\">IoT Energy Tracker &mdash;"
            + " Saving energy, one device at a time.</p>");
    html.append("</td></tr>");

    // Close tables
    html.append("</table>");
    html.append("</td></tr></table>");
    html.append("</body></html>");

    return html.toString();
  }

  private char calculateEcoScore(double threshold, double energyConsumed) {
    if (threshold <= 0) {
      return 'F';
    }
    double ratio = energyConsumed / threshold;
    if (ratio <= 0.5) {
      return 'A';
    } else if (ratio <= 0.75) {
      return 'B';
    } else if (ratio <= 1.0) {
      return 'C';
    } else if (ratio <= 1.25) {
      return 'D';
    } else if (ratio <= 1.5) {
      return 'E';
    } else {
      return 'F';
    }
  }

  private String getScoreColor(char grade) {
    return switch (grade) {
      case 'A' -> "#2d8a4e";
      case 'B' -> "#4caf50";
      case 'C' -> "#8bc34a";
      case 'D' -> "#ff9800";
      case 'E' -> "#f44336";
      case 'F' -> "#b71c1c";
      default -> "#888888";
    };
  }

  private int calculateTreesNeeded(double threshold, double energyConsumed) {
    if (energyConsumed <= threshold) {
      return 0;
    }
    double excessKwh = energyConsumed - threshold;
    return (int) Math.ceil(excessKwh / 7.5);
  }
}
