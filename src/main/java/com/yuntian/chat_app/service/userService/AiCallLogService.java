package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.dto.AiCallLogDailyRecordDTO;
import com.yuntian.chat_app.dto.DailyTokenDTO;
import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.entity.AiCallLogDO;
import com.yuntian.chat_app.mapper.userMapper.AiCallLogMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiCallLogService {

    private final AiCallLogMapper aiCallLogMapper;

    @Async("metricsExecutor")
    public void saveCall(String userId, String characterId, String modelName, String memoryId,
                         String status, Integer inputTokens, Integer outputTokens, Integer totalTokens,
                         Long durationMs, Long requestStartTs) {
        try {
            AiCallLogDO row = new AiCallLogDO();
            row.setUserId(userId);
            row.setCharacterId(characterId);
            row.setModelName(modelName != null ? modelName : "unknown");
            row.setMemoryId(memoryId);
            row.setStatus(status);
            row.setInputTokens(inputTokens != null ? inputTokens : 0);
            row.setOutputTokens(outputTokens != null ? outputTokens : 0);
            row.setTotalTokens(totalTokens != null ? totalTokens : 0);
            row.setDurationMs(durationMs != null ? durationMs.intValue() : 0);
            row.setRequestTs(new Date(requestStartTs != null ? requestStartTs : System.currentTimeMillis()));

            aiCallLogMapper.insert(row);

            log.debug("AI 对话记录已保存 - userId: {}, characterId: {}, memoryId: {}, tokens: {}",
                    userId, characterId, memoryId, totalTokens);
        } catch (Exception e) {
            log.error("保存 AI 对话记录失败 - userId: {}, characterId: {}, memoryId: {}",
                    userId, characterId, memoryId, e);
        }
    }

    public TokenStatDTO getTokenStatByMemoryId(String memoryId) {
        try {
            List<AiCallLogDO> allRecords = aiCallLogMapper.findAllByMemoryId(memoryId);

            if (allRecords == null || allRecords.isEmpty()) {
                log.warn("未找到会话记录 - memoryId: {}", memoryId);
                return TokenStatDTO.empty();
            }

            AiCallLogDO latest = allRecords.get(0);
            int totalInput = allRecords.stream()
                    .mapToInt(r -> r.getInputTokens() != null ? r.getInputTokens() : 0)
                    .sum();
            int totalOutput = allRecords.stream()
                    .mapToInt(r -> r.getOutputTokens() != null ? r.getOutputTokens() : 0)
                    .sum();
            int totalTokens = allRecords.stream()
                    .mapToInt(r -> r.getTotalTokens() != null ? r.getTotalTokens() : 0)
                    .sum();
            int totalDuration = allRecords.stream()
                    .mapToInt(r -> r.getDurationMs() != null ? r.getDurationMs() : 0)
                    .sum();

            TokenStatDTO stat = new TokenStatDTO();
            stat.setMemoryId(memoryId);
            stat.setInputTokens(totalInput);
            stat.setOutputTokens(totalOutput);
            stat.setTotalTokens(totalTokens);
            stat.setDurationMs(totalDuration);
            stat.setLastCallTime(latest.getRequestTs());
            return stat;
        } catch (Exception e) {
            log.error("查询 Token 统计失败 - memoryId: {}", memoryId, e);
            return TokenStatDTO.empty();
        }
    }

    public List<DailyTokenDTO> getDailyTokenUsage(String userId, LocalDate begin, LocalDate end) {
        try {
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

            List<AiCallLogDailyRecordDTO> records = aiCallLogMapper.getTokenUsageByDateRange(
                    userId, beginTime, endTime);

            log.info("查询到 {} 条记录 - userId: {}, begin: {}, end: {}",
                    records.size(), userId, begin, end);

            Map<String, SessionLastRecord> sessionLastRecords = new HashMap<>();
            for (AiCallLogDailyRecordDTO record : records) {
                String memoryId = record.getMemoryId();
                LocalDateTime requestTs = record.getRequestTs();
                if (memoryId == null || memoryId.isBlank() || requestTs == null) {
                    continue;
                }

                Long totalTokens = record.getTotalTokens() == null ? 0L : record.getTotalTokens();
                SessionLastRecord existing = sessionLastRecords.get(memoryId);
                if (existing == null || requestTs.isAfter(existing.getRequestTs())) {
                    sessionLastRecords.put(memoryId, new SessionLastRecord(memoryId, requestTs, totalTokens));
                }
            }

            log.info("统计到 {} 个会话", sessionLastRecords.size());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Map<String, Long> dailyTokenMap = new HashMap<>();
            for (SessionLastRecord session : sessionLastRecords.values()) {
                String dateKey = session.getRequestTs().toLocalDate().format(formatter);
                dailyTokenMap.merge(dateKey, session.getTotalTokens(), Long::sum);
            }

            List<DailyTokenDTO> result = new ArrayList<>();
            LocalDate current = begin;
            while (!current.isAfter(end)) {
                String dateStr = current.format(formatter);
                result.add(new DailyTokenDTO(dateStr, dailyTokenMap.getOrDefault(dateStr, 0L)));
                current = current.plusDays(1);
            }

            long totalTokens = result.stream().mapToLong(DailyTokenDTO::getTotalTokens).sum();
            log.info("统计完成 - userId: {}, 天数: {}, 会话数: {}, 总 Token: {}",
                    userId, result.size(), sessionLastRecords.size(), totalTokens);
            return result;
        } catch (Exception e) {
            log.error("查询用户每日 Token 使用量失败 - userId: {}, begin: {}, end: {}",
                    userId, begin, end, e);
            return Collections.emptyList();
        }
    }

    @Data
    @AllArgsConstructor
    private static class SessionLastRecord {
        private String memoryId;
        private LocalDateTime requestTs;
        private Long totalTokens;
    }
}
