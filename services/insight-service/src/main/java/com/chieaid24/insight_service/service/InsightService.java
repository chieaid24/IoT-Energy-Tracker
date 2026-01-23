package com.chieaid24.insight_service.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Service;

import com.chieaid24.insight_service.client.UsageClient;
import com.chieaid24.insight_service.dto.DeviceDto;
import com.chieaid24.insight_service.dto.InsightDto;
import com.chieaid24.insight_service.dto.UsageDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InsightService {

    private final UsageClient usageClient;
    private OllamaChatModel ollamaChatModel;

    public InsightService(UsageClient usageClient, OllamaChatModel ollamaChatModel) {
        this.usageClient = usageClient;
        this.ollamaChatModel = ollamaChatModel;
    }

    public InsightDto getSavingTips(Long userId) {
        // fetch data from the usage service
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, 3);

        double totalUsage = usageData.devices().stream()
            .mapToDouble(DeviceDto::energyConsumed)
            .sum();
        
        log.info("Calling Ollama for userId {} with total usage {}", userId, totalUsage);

        String prompt = new StringBuilder()
            .append("This is my total consumption over the past 3 days.")
            .append("How can I reduce my energy usage? How does it compare to average households? ")
            .append("Total energy consumption: \n")
            .append(totalUsage)
            .toString();

        ChatResponse response = ollamaChatModel.call(
            Prompt.builder()
            .content(prompt)
            .build());
        
        return InsightDto.builder()
            .userId(userId)
            .tips(response.getResult().getOutput().getText())
            .energyUsage(totalUsage)
            .build();
        }

    public InsightDto getOverview(Long userId) {
        // fetch data from the usage service
        final UsageDto usageData = usageClient.getXDaysUsageForUser(userId, 3);

        double totalUsage = usageData.devices().stream()
            .mapToDouble(DeviceDto::energyConsumed)
            .sum();
        
        log.info("Calling Ollama for userId {} with total usage {}", userId, totalUsage);

        String prompt = new StringBuilder()
            .append("Analyze the following energy usage data and provide a concise overview with actionable insights to reduce consumption.\n")
            .append("This data is the aggregate data for the past 3 days. ")
            .append("Usage Data: \n")
            .append(usageData.devices())
            .toString();

        ChatResponse response = ollamaChatModel.call(
            Prompt.builder()
            .content(prompt)
            .build());
        
        return InsightDto.builder()
            .userId(userId)
            .tips(response.getResult().getOutput().getText())
            .energyUsage(totalUsage)
            .build();
        }
    }
