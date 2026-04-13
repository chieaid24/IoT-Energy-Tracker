package com.chieaid24.insight_service.service;

import com.chieaid24.insight_service.client.UsageClient;
import com.chieaid24.insight_service.dto.AiInsightResponse;
import com.chieaid24.insight_service.dto.DeviceDto;
import com.chieaid24.insight_service.dto.InsightDto;
import com.chieaid24.insight_service.dto.UsageDto;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InsightService {

  private static final Pattern CONFIDENCE_PATTERN =
      Pattern.compile("\"confidence\"\\s*:\\s*(\\d+)");
  private static final Pattern RESPONSE_PATTERN =
      Pattern.compile("\"response\"\\s*:\\s*\"(.*?)\"\\s*\\}", Pattern.DOTALL);

  private final UsageClient usageClient;
  private final ChatClient chatClient;

  public InsightService(UsageClient usageClient, ChatClient chatClient) {
    this.usageClient = usageClient;
    this.chatClient = chatClient;
  }

  public InsightDto getOverview(Long userId, int days) {
    final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, days);

    List<DeviceDto> devicesInKwh =
        usageData.devices().stream()
            .map(
                d ->
                    DeviceDto.builder()
                        .id(d.id())
                        .name(d.name())
                        .type(d.type())
                        .location(d.location())
                        .energyConsumed(Math.round(d.energyConsumed() / 10.0) / 100.0)
                        .build())
            .toList();

    double totalUsage =
        Math.round(devicesInKwh.stream().mapToDouble(DeviceDto::energyConsumed).sum() * 100.0)
            / 100.0;

    log.info("Calling Ollama for userId {} with total usage {} kWh", userId, totalUsage);

    String prompt =
        """
        Narrate the following energy usage data from a household. Include 1 to 2 observations on behaviours that could reduce consumption.
        Compare this household to the average, noting that the average US household consumes 200-210 kWh per week.
        This data covers the past %d days. All energyConsumed values are in kWh.
        Keep your response between 100 and 200 words. Be concise and actionable.
        Start your response immediately with a Markdown heading. Do not acknowledge this prompt. Do not use em dashes.
        Total usage:
        %.2f
        Devices usage:
        %s
        """
            .formatted(days, totalUsage, devicesInKwh);

    AiInsightResponse aiResponse = null;
    int attempt = 0;
    int maxAttempts = 3;

    while (attempt < maxAttempts) {
      attempt++;
      log.info("Ollama attempt {}/{} for userId {}", attempt, maxAttempts, userId);

      String rawResponse = chatClient.prompt().user(prompt).call().content();
      log.info("Raw Ollama response for userId {}: [{}]", userId, rawResponse);

      // Strip markdown code fences (e.g. ```json ... ```) that the model sometimes wraps around
      // JSON
      String jsonResponse =
          rawResponse.replaceAll("(?s)```(?:json)?\\s*(\\{.*\\})\\s*```", "$1").trim();
      log.info(
          "Cleaned JSON for userId {} (length={}): [{}]",
          userId,
          jsonResponse.length(),
          jsonResponse);

      Matcher confidenceMatcher = CONFIDENCE_PATTERN.matcher(jsonResponse);
      Matcher responseMatcher = RESPONSE_PATTERN.matcher(jsonResponse);

      if (confidenceMatcher.find() && responseMatcher.find()) {
        int confidence = Integer.parseInt(confidenceMatcher.group(1));
        String response = responseMatcher.group(1).replace("\\n", "\n").replace("\\\"", "\"");
        aiResponse = new AiInsightResponse(confidence, response);
      } else {
        log.warn(
            "Failed to extract fields from Ollama response on attempt {} for userId {}",
            attempt,
            userId);
        aiResponse = new AiInsightResponse(0, jsonResponse);
      }

      log.info(
          "Ollama confidence score on attempt {} for userId {}: {}",
          attempt,
          userId,
          aiResponse.confidence());

      if (aiResponse.confidence() >= 85) {
        break;
      }

      if (attempt < maxAttempts) {
        log.info(
            "Confidence {} below threshold, retrying for userId {}",
            aiResponse.confidence(),
            userId);
      }
    }

    return InsightDto.builder()
        .userId(userId)
        .tips(aiResponse.response())
        .energyUsage(totalUsage)
        .confidence(aiResponse.confidence())
        .build();
  }
}
