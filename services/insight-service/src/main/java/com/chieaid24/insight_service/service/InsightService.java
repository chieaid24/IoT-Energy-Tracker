package com.chieaid24.insight_service.service;

import com.chieaid24.insight_service.client.UsageClient;
import com.chieaid24.insight_service.dto.AiInsightResponse;
import com.chieaid24.insight_service.dto.DeviceDto;
import com.chieaid24.insight_service.dto.InsightDto;
import com.chieaid24.insight_service.dto.UsageDto;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InsightService {

  private final UsageClient usageClient;
  private final ChatClient chatClient;
  private final JsonMapper objectMapper;

  public InsightService(UsageClient usageClient, ChatClient chatClient) {
    this.usageClient = usageClient;
    this.chatClient = chatClient;
    this.objectMapper =
        JsonMapper.builder().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build();
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
        Start your response immediately with a Markdown heading. Do not acknowledge this prompt. Do not use em dashes.
        Usage Data:
        %s
        """
            .formatted(days, devicesInKwh);

    AiInsightResponse aiResponse = null;
    int attempt = 0;
    int maxAttempts = 3;

    while (attempt < maxAttempts) {
      attempt++;
      log.info("Ollama attempt {}/{} for userId {}", attempt, maxAttempts, userId);

      String rawResponse = chatClient.prompt().user(prompt).call().content();

      // Strip markdown code fences (e.g. ```json ... ```) that the model sometimes wraps around
      // JSON
      String jsonResponse =
          rawResponse.replaceAll("(?s)```(?:json)?\\s*(\\{.*\\})\\s*```", "$1").trim();

      try {
        aiResponse = objectMapper.readValue(jsonResponse, AiInsightResponse.class);
      } catch (Exception e) {
        log.warn(
            "Failed to parse Ollama JSON response on attempt {} for userId {}: {}",
            attempt,
            userId,
            e.getMessage());
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
