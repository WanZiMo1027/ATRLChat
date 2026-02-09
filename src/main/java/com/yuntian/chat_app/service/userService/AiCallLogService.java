package com.yuntian.chat_app.service.userService;

import com.yuntian.chat_app.dto.DailyTokenDTO;
import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.entity.AiCallLogDO;
import com.yuntian.chat_app.mapper.userMapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiCallLogService {

    private final AiCallLogMapper aiCallLogMapper;

    /**
     * 异步保存 AI 对话记录
     */
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

    /**
     * 查询指定会话的 Token 消耗统计（一次查询 + 内存计算）
     *
     * @param memoryId 会话 ID
     * @return Token 统计
     */
    public TokenStatDTO getTokenStatByMemoryId(String memoryId) {
        try {
            // 一次查询所有记录（已按时间倒序）
            List<AiCallLogDO> allRecords = aiCallLogMapper.findAllByMemoryId(memoryId);

            if (allRecords == null || allRecords.isEmpty()) {
                log.warn("未找到会话记录 - memoryId: {}", memoryId);
                return TokenStatDTO.empty();
            }

            AiCallLogDO latest = allRecords.get(0);
            // 第一条就是最后一次调用
            int totalInput = allRecords.stream()
                    .mapToInt(r -> r.getInputTokens() != null ? r.getInputTokens() : 0)
                    .sum();

            // 内存计算：累加所有 output 和耗时
            int totalOutput = allRecords.stream()
                    .mapToInt(record -> record.getOutputTokens() != null ? record.getOutputTokens() : 0)
                    .sum();

            int totalTokens = allRecords.stream()
                    .mapToInt(r -> r.getTotalTokens() != null ? r.getTotalTokens() : 0)
                    .sum();

            int totalDuration = allRecords.stream()
                    .mapToInt(record -> record.getDurationMs() != null ? record.getDurationMs() : 0)
                    .sum();

            TokenStatDTO stat = new TokenStatDTO();
            stat.setMemoryId(memoryId);
            stat.setInputTokens(totalInput);   //  改为累加值
            stat.setOutputTokens(totalOutput); //  改为累加值
            stat.setTotalTokens(totalTokens);  //  改为累加值
            stat.setDurationMs(totalDuration);
            stat.setLastCallTime(latest.getRequestTs());

            return stat;

        } catch (Exception e) {
            log.error("查询 Token 统计失败 - memoryId: {}", memoryId, e);
            return TokenStatDTO.empty();
        }
    }


    /**
     * 查询用户每天的 Token 使用量（使用 Map 传参）
     *
     * @param userId 用户ID
     * @param begin 开始日期
     * @param end 结束日期
     * @return 每天的 Token 统计
     */
    public List<DailyTokenDTO> getDailyTokenUsage(String userId, LocalDate begin, LocalDate end) {
        try {
            // 1. 转换为时间范围
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

            // 2. 一次查询所有数据
            List<Map<String, Object>> records = aiCallLogMapper.getTokenUsageByDateRange(
                    userId, beginTime, endTime);

            log.info("查询到 {} 条记录 - userId: {}, begin: {}, end: {}",
                    records.size(), userId, begin, end);

            // 3. 按 memory_id 分组，找出每个会话的最后一条记录
            Map<String, SessionLastRecord> sessionLastRecords = new HashMap<>();

            for (Map<String, Object> record : records) {
                String memoryId = (String) record.get("memory_id");

                // 解析时间
                LocalDateTime requestTs = parseDateTime(record.get("request_ts"));
                if (requestTs == null) {
                    continue;
                }

                Long totalTokens = ((Number) record.get("totalTokens")).longValue();

                // 更新该会话的最后一条记录
                if (!sessionLastRecords.containsKey(memoryId)) {
                    sessionLastRecords.put(memoryId,
                            new SessionLastRecord(memoryId, requestTs, totalTokens));
                } else {
                    SessionLastRecord existing = sessionLastRecords.get(memoryId);
                    if (requestTs.isAfter(existing.getRequestTs())) {
                        sessionLastRecords.put(memoryId,
                                new SessionLastRecord(memoryId, requestTs, totalTokens));
                    }
                }
            }

            log.info("统计到 {} 个会话", sessionLastRecords.size());

            // 4. 按日期分组统计（每个会话的 token 算在最后一条记录的日期）
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Map<String, Long> dailyTokenMap = new HashMap<>();

            for (SessionLastRecord session : sessionLastRecords.values()) {
                String dateKey = session.getRequestTs().toLocalDate().format(formatter);
                dailyTokenMap.merge(dateKey, session.getTotalTokens(), Long::sum);
            }

            // 5. 生成完整的日期列表（补全没有数据的日期为 0）
            List<DailyTokenDTO> result = new ArrayList<>();
            LocalDate current = begin;

            while (!current.isAfter(end)) {
                String dateStr = current.format(formatter);
                Long tokens = dailyTokenMap.getOrDefault(dateStr, 0L);
                result.add(new DailyTokenDTO(dateStr, tokens));
                current = current.plusDays(1);
            }

            Long totalTokens = result.stream().mapToLong(DailyTokenDTO::getTotalTokens).sum();
            log.info("统计完成 - userId: {}, 天数: {}, 会话数: {}, 总 Token: {}",
                    userId, result.size(), sessionLastRecords.size(), totalTokens);

            return result;

        } catch (Exception e) {
            log.error("查询用户每日 Token 使用量失败 - userId: {}, begin: {}, end: {}",
                    userId, begin, end, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析数据库返回的时间对象
     */
    private LocalDateTime parseDateTime(Object timestampObj) {
        if (timestampObj instanceof LocalDateTime) {
            return (LocalDateTime) timestampObj;
        } else if (timestampObj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) timestampObj).toLocalDateTime();
        } else if (timestampObj instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) timestampObj).getTime()).toLocalDateTime();
        }
        return null;
    }

    /**
     * 内部类：记录每个会话的最后一条记录
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class SessionLastRecord {
        private String memoryId;
        private LocalDateTime requestTs;
        private Long totalTokens;
    }



}