package com.civil.platform.module.service.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.module.entity.SysUser;
import com.civil.platform.module.entity.UserItemInteraction;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserItemInteractionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UserCF 全量 / 增量重建任务编排。
 */
@Service
public class UserCfRebuildService {

    private static final Logger log = LoggerFactory.getLogger(UserCfRebuildService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserCfService cfService;
    private final SysUserMapper userMapper;
    private final UserItemInteractionMapper userItemInteractionMapper;
    private final StringRedisTemplate redis;

    private final AtomicBoolean fullRebuildRunning = new AtomicBoolean(false);

    public UserCfRebuildService(UserCfService cfService,
                                SysUserMapper userMapper,
                                UserItemInteractionMapper userItemInteractionMapper,
                                StringRedisTemplate redis) {
        this.cfService = cfService;
        this.userMapper = userMapper;
        this.userItemInteractionMapper = userItemInteractionMapper;
        this.redis = redis;
    }

    /**
     * 全量重建：聚合全体启用用户的行为 → IDF → Top-50 相似用户。
     */
    public synchronized Map<String, Object> fullRebuild() {
        if (!fullRebuildRunning.compareAndSet(false, true)) {
            log.info("UserCF 全量重建已在进行中，跳过");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("skipped", true);
            return result;
        }
        long start = System.currentTimeMillis();
        writeStatus("IN_PROGRESS", LocalDateTime.now());
        try {
            List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnabled, true));
            log.info("UserCF 全量重建开始，候选用户={}", users.size());

            // 1) 聚合每个用户的向量（同时写入 user_item_interaction）
            int aggregated = 0;
            for (SysUser u : users) {
                cfService.aggregateUserVector(u.getId());
                aggregated++;
                if (aggregated % 100 == 0) {
                    log.info("UserCF 行为聚合进度 {}/{}", aggregated, users.size());
                }
            }

            // 2) 重算 IDF
            Map<String, Double> idfMap = cfService.rebuildItemPopularity();

            // 3) 加载全量交互到内存（构建倒排索引）
            List<UserItemInteraction> all = userItemInteractionMapper.selectList(null);
            Map<Long, Map<String, Double>> userVectors = new HashMap<>();
            Map<String, Set<Long>> invertedIndex = new HashMap<>();
            for (UserItemInteraction r : all) {
                if (r.getWeight() == null || r.getWeight().doubleValue() <= 0) continue;
                String item = r.getItemType() + ":" + r.getItemId();
                userVectors.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                        .put(item, r.getWeight().doubleValue());
                invertedIndex.computeIfAbsent(item, k -> new HashSet<>()).add(r.getUserId());
            }

            // 4) 对每个有向量的用户计算 Top-50 相似用户并写 Redis + DB
            int computed = 0, withSim = 0;
            for (Long uid : userVectors.keySet()) {
                LinkedHashMap<Long, Double> simUsers = cfService.computeSimilarity(uid, userVectors, invertedIndex, idfMap);
                cfService.writeSimUserZSet(uid, simUsers);
                cfService.persistSimilarity(uid, simUsers);
                computed++;
                if (!simUsers.isEmpty()) withSim++;
                if (computed % 100 == 0) {
                    log.info("UserCF 相似度计算进度 {}/{}", computed, userVectors.size());
                }
            }

            long ms = System.currentTimeMillis() - start;
            writeStatus("LAST_FULL_AT", LocalDateTime.now());
            writeStatus("LAST_FULL_DURATION_MS", ms);
            writeStatus("LAST_FULL_COVERED_USERS", computed);
            writeStatus("LAST_FULL_WITH_SIM_USERS", withSim);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalUsers", users.size());
            result.put("aggregated", aggregated);
            result.put("computed", computed);
            result.put("withSimilarUsers", withSim);
            result.put("durationMs", ms);
            log.info("UserCF 全量重建完成，耗时 {} ms，覆盖 {} 用户，{} 用户至少 1 个相似用户",
                    ms, computed, withSim);
            return result;
        } finally {
            fullRebuildRunning.set(false);
        }
    }

    /**
     * 单用户增量：聚合该用户向量 + 用现有内存 idf 重算其相似用户。
     */
    public void refreshUser(Long userId) {
        try {
            cfService.aggregateUserVector(userId);

            // 加载全量交互（增量任务也用同样的剪枝逻辑；规模小）
            List<UserItemInteraction> all = userItemInteractionMapper.selectList(null);
            Map<Long, Map<String, Double>> userVectors = new HashMap<>();
            Map<String, Set<Long>> invertedIndex = new HashMap<>();
            for (UserItemInteraction r : all) {
                if (r.getWeight() == null || r.getWeight().doubleValue() <= 0) continue;
                String item = r.getItemType() + ":" + r.getItemId();
                userVectors.computeIfAbsent(r.getUserId(), k -> new HashMap<>())
                        .put(item, r.getWeight().doubleValue());
                invertedIndex.computeIfAbsent(item, k -> new HashSet<>()).add(r.getUserId());
            }

            // 直接读 IDF 缓存（Redis），缺失时按 1.0
            Map<String, Double> idfMap = new HashMap<>();
            for (String item : invertedIndex.keySet()) {
                String[] parts = item.split(":");
                idfMap.put(item, cfService.idfFor(parts[0], Long.valueOf(parts[1])));
            }

            LinkedHashMap<Long, Double> simUsers = cfService.computeSimilarity(userId, userVectors, invertedIndex, idfMap);
            cfService.writeSimUserZSet(userId, simUsers);
            cfService.persistSimilarity(userId, simUsers);
            writeStatus("LAST_INCR_AT", LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("UserCF 单用户增量失败 userId={}: {}", userId, ex.getMessage());
        }
    }

    @Async
    public void refreshUserAsync(Long userId) {
        if (userId == null) return;
        refreshUser(userId);
    }

    /**
     * 增量任务：刷新近 N 分钟有行为变更的用户。简化实现：刷新所有最近模考/学习活跃用户。
     */
    public synchronized void incrementalRefresh() {
        // 简化：在没有埋点表的情况下，扫近 30min 内 user_item_interaction.last_acted_at 的用户
        LocalDateTime since = LocalDateTime.now().minusMinutes(35);
        List<UserItemInteraction> recent = userItemInteractionMapper.selectList(
                new LambdaQueryWrapper<UserItemInteraction>().ge(UserItemInteraction::getLastActedAt, since));
        Set<Long> userIds = new HashSet<>();
        recent.forEach(r -> userIds.add(r.getUserId()));
        log.info("UserCF 增量刷新候选用户={}", userIds.size());
        for (Long uid : userIds) {
            refreshUser(uid);
        }
        writeStatus("LAST_INCR_AT", LocalDateTime.now());
    }

    public Map<String, String> readStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        for (String suffix : Arrays.asList("LAST_FULL_AT", "LAST_INCR_AT", "LAST_FULL_DURATION_MS",
                "LAST_FULL_COVERED_USERS", "LAST_FULL_WITH_SIM_USERS")) {
            try {
                String v = redis.opsForValue().get(UserCfService.CF_STATUS_KEY + ":" + suffix);
                if (v != null) status.put(suffix, v);
            } catch (Exception ignored) {
            }
        }
        status.put("running", String.valueOf(fullRebuildRunning.get()));
        return status;
    }

    private void writeStatus(String suffix, Object value) {
        try {
            String v = value instanceof LocalDateTime ? ((LocalDateTime) value).format(FMT) : String.valueOf(value);
            redis.opsForValue().set(UserCfService.CF_STATUS_KEY + ":" + suffix, v, Duration.ofDays(7));
        } catch (Exception ignored) {
        }
    }
}
