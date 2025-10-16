package com.yuntian.chat_app.service;

import com.yuntian.chat_app.dto.TokenStatDTO;
import com.yuntian.chat_app.entity.AiCallLogDO;
import com.yuntian.chat_app.mapper.userMapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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

            // 第一条就是最后一次调用
            AiCallLogDO latest = allRecords.get(0);

            // 内存计算：累加所有 output 和耗时
            int totalOutput = allRecords.stream()
                    .mapToInt(record -> record.getOutputTokens() != null ? record.getOutputTokens() : 0)
                    .sum();

            int totalDuration = allRecords.stream()
                    .mapToInt(record -> record.getDurationMs() != null ? record.getDurationMs() : 0)
                    .sum();

            // 组装结果
            TokenStatDTO stat = new TokenStatDTO();
            stat.setMemoryId(memoryId);
            stat.setInputTokens(latest.getInputTokens());
            stat.setOutputTokens(totalOutput);
            stat.setTotalTokens(latest.getTotalTokens());
            stat.setDurationMs(totalDuration);
            stat.setLastCallTime(latest.getRequestTs());

            log.info("查询 Token 统计 - memoryId: {}, 记录数: {}, input: {}, output: {}, total: {}",
                    memoryId, allRecords.size(),
                    stat.getInputTokens(), stat.getOutputTokens(), stat.getTotalTokens());

            return stat;

        } catch (Exception e) {
            log.error("查询 Token 统计失败 - memoryId: {}", memoryId, e);
            return TokenStatDTO.empty();
        }
    }

    /**
     * 查询指定会话的所有调用明细
     *
     * @param memoryId 会话 ID
     * @return 调用明细列表
     */
    public List<AiCallLogDO> getCallDetailsByMemoryId(String memoryId) {
        try {
            List<AiCallLogDO> details = aiCallLogMapper.findAllByMemoryId(memoryId);
            log.info("查询调用明细 - memoryId: {}, count: {}", memoryId, details != null ? details.size() : 0);
            return details != null ? details : List.of();

        } catch (Exception e) {
            log.error("查询调用明细失败 - memoryId: {}", memoryId, e);
            return List.of();
        }
    }

    /**
     * 查询指定会话的最后一条记录
     *
     * @param memoryId 会话 ID
     * @return 最后一条记录
     */
    public AiCallLogDO getLatestCallByMemoryId(String memoryId) {
        try {
            AiCallLogDO latest = aiCallLogMapper.findLatestByMemoryId(memoryId);

            if (latest == null) {
                log.warn("未找到会话记录 - memoryId: {}", memoryId);
                return null;
            }

            log.info("查询最后一条记录 - memoryId: {}, tokens: {}", memoryId, latest.getTotalTokens());
            return latest;

        } catch (Exception e) {
            log.error("查询最后一条记录失败 - memoryId: {}", memoryId, e);
            return null;
        }
    }

    
}