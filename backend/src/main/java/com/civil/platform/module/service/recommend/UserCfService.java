package com.civil.platform.module.service.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.civil.platform.module.entity.*;
import com.civil.platform.module.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 真实 UserCF 协同过滤核心服务。
 *
 * 算法：余弦相似度 + IDF 热门惩罚 + 倒排索引剪枝。
 * 物品空间：COURSE / EXAM / NEWS（统一向量）。
 * 在线读取：Redis ZSET recommend:simuser:{uid}（Top-50）。
 */
@Service
public class UserCfService {

    private static final Logger log = LoggerFactory.getLogger(UserCfService.class);

    public static final String SIM_USER_KEY_PREFIX = "recommend:simuser:";
    public static final String IDF_KEY_PREFIX = "recommend:item:idf:";
    public static final String CF_STATUS_KEY = "recommend:cf:status";

    public static final int TOP_SIMILAR_USERS = 50;
    public static final int ONLINE_TOP_K = 12;
    public static final double SIMILARITY_FLOOR = 0.05;
    public static final int MIN_COMMON_ITEMS = 2;

    public static final String ITEM_COURSE = "COURSE";
    public static final String ITEM_EXAM = "EXAM";
    public static final String ITEM_NEWS = "NEWS";

    private final UserUserSimilarityMapper userUserSimilarityMapper;
    private final UserItemInteractionMapper userItemInteractionMapper;
    private final ItemPopularityMapper itemPopularityMapper;
    private final UserCourseMapper userCourseMapper;
    private final UserCourseStudyProgressMapper progressMapper;
    private final ExamRecordMapper examRecordMapper;
    private final RecommendFeedbackMapper feedbackMapper;
    private final NewsCommentMapper newsCommentMapper;
    private final NewsShareLogMapper newsShareLogMapper;
    private final UserQuestionFavoriteMapper favoriteMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redis;

    public UserCfService(UserUserSimilarityMapper userUserSimilarityMapper,
                         UserItemInteractionMapper userItemInteractionMapper,
                         ItemPopularityMapper itemPopularityMapper,
                         UserCourseMapper userCourseMapper,
                         UserCourseStudyProgressMapper progressMapper,
                         ExamRecordMapper examRecordMapper,
                         RecommendFeedbackMapper feedbackMapper,
                         NewsCommentMapper newsCommentMapper,
                         NewsShareLogMapper newsShareLogMapper,
                         UserQuestionFavoriteMapper favoriteMapper,
                         SysUserMapper userMapper,
                         StringRedisTemplate redis) {
        this.userUserSimilarityMapper = userUserSimilarityMapper;
        this.userItemInteractionMapper = userItemInteractionMapper;
        this.itemPopularityMapper = itemPopularityMapper;
        this.userCourseMapper = userCourseMapper;
        this.progressMapper = progressMapper;
        this.examRecordMapper = examRecordMapper;
        this.feedbackMapper = feedbackMapper;
        this.newsCommentMapper = newsCommentMapper;
        this.newsShareLogMapper = newsShareLogMapper;
        this.favoriteMapper = favoriteMapper;
        this.userMapper = userMapper;
        this.redis = redis;
    }

    // -----------------------------------------------------------
    // 一、用户行为聚合（按口径写入 user_item_interaction）
    // -----------------------------------------------------------

    /**
     * 单用户行为聚合，返回 Map<itemKey, weight>。itemKey 形如 "COURSE:123"。
     */
    public Map<String, BigDecimal> aggregateUserVector(Long userId) {
        Map<String, BigDecimal> vec = new LinkedHashMap<>();
        Map<String, LocalDateTime> lastActed = new HashMap<>();

        // 购课 1.0
        userCourseMapper.selectList(new LambdaQueryWrapper<UserCourse>().eq(UserCourse::getUserId, userId))
                .forEach(uc -> {
                    String key = ITEM_COURSE + ":" + uc.getCourseId();
                    addWeight(vec, key, 1.0);
                    touch(lastActed, key, uc.getCreatedAt());
                });

        // 学习进度
        progressMapper.selectList(new LambdaQueryWrapper<UserCourseStudyProgress>().eq(UserCourseStudyProgress::getUserId, userId))
                .forEach(p -> {
                    if (p.getProgressPercent() == null) return;
                    double pct = p.getProgressPercent().doubleValue();
                    String key = ITEM_COURSE + ":" + p.getCourseId();
                    if (pct >= 80) addWeight(vec, key, 1.0);
                    else if (pct >= 30) addWeight(vec, key, 0.8);
                    else if (pct > 0) addWeight(vec, key, 0.4);
                    touch(lastActed, key, p.getLastStudyAt());
                });

        // 模考提交 0.7
        examRecordMapper.selectList(new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getUserId, userId)
                        .eq(ExamRecord::getStatus, "SUBMITTED"))
                .forEach(e -> {
                    if (e.getExamId() == null) return;
                    String key = ITEM_EXAM + ":" + e.getExamId();
                    addWeight(vec, key, 0.7);
                    touch(lastActed, key, e.getSubmittedAt());
                });

        // 资讯：评论 0.6 + 分享 0.5
        newsCommentMapper.selectList(new LambdaQueryWrapper<NewsComment>().eq(NewsComment::getUserId, userId))
                .forEach(c -> {
                    String key = ITEM_NEWS + ":" + c.getNewsId();
                    addWeight(vec, key, 0.6);
                    touch(lastActed, key, c.getCreatedAt());
                });
        newsShareLogMapper.selectList(new LambdaQueryWrapper<NewsShareLog>().eq(NewsShareLog::getUserId, userId))
                .forEach(s -> {
                    String key = ITEM_NEWS + ":" + s.getNewsId();
                    addWeight(vec, key, 0.5);
                    touch(lastActed, key, s.getCreatedAt());
                });

        // 不感兴趣反馈：扣分
        feedbackMapper.selectList(new LambdaQueryWrapper<RecommendFeedback>()
                        .eq(RecommendFeedback::getUserId, userId)
                        .eq(RecommendFeedback::getFeedbackType, "NOT_INTERESTED"))
                .forEach(f -> {
                    if (f.getResourceType() == null || f.getResourceId() == null) return;
                    String type = mapFeedbackType(f.getResourceType());
                    if (type == null) return;
                    String key = type + ":" + f.getResourceId();
                    addWeight(vec, key, -0.5);
                });

        // clip 到 [-1, 2]
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : vec.entrySet()) {
            double v = e.getValue().doubleValue();
            v = Math.max(-1.0, Math.min(2.0, v));
            if (Math.abs(v) > 0.01) {
                result.put(e.getKey(), BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP));
            }
        }

        // 持久化到 user_item_interaction（先删再插）
        persistUserVector(userId, result, lastActed);
        return result;
    }

    private String mapFeedbackType(String resourceType) {
        if (resourceType == null) return null;
        switch (resourceType.toUpperCase()) {
            case "COURSE": return ITEM_COURSE;
            case "EXAM": return ITEM_EXAM;
            case "NEWS": return ITEM_NEWS;
            default: return null;
        }
    }

    private void persistUserVector(Long userId, Map<String, BigDecimal> vec, Map<String, LocalDateTime> lastActed) {
        userItemInteractionMapper.delete(new LambdaQueryWrapper<UserItemInteraction>().eq(UserItemInteraction::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, BigDecimal> entry : vec.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            UserItemInteraction row = new UserItemInteraction();
            row.setUserId(userId);
            row.setItemType(parts[0]);
            row.setItemId(Long.valueOf(parts[1]));
            row.setWeight(entry.getValue());
            row.setLastActedAt(lastActed.getOrDefault(entry.getKey(), now));
            userItemInteractionMapper.insert(row);
        }
    }

    // -----------------------------------------------------------
    // 二、IDF 计算
    // -----------------------------------------------------------

    /**
     * 全量重算 item_popularity + Redis idf 缓存。
     * @return itemKey -> idfWeight
     */
    public Map<String, Double> rebuildItemPopularity() {
        long totalUsers = Math.max(1L, userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEnabled, true)));
        List<UserItemInteraction> all = userItemInteractionMapper.selectList(null);
        Map<String, Set<Long>> itemUsers = new HashMap<>();
        for (UserItemInteraction r : all) {
            if (r.getWeight() == null || r.getWeight().doubleValue() <= 0) continue;
            String key = r.getItemType() + ":" + r.getItemId();
            itemUsers.computeIfAbsent(key, k -> new HashSet<>()).add(r.getUserId());
        }

        itemPopularityMapper.delete(null);
        Map<String, Double> idfMap = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, Set<Long>> e : itemUsers.entrySet()) {
            String[] parts = e.getKey().split(":");
            int userCount = e.getValue().size();
            double idf = Math.log(1.0 + (double) totalUsers / Math.max(1, userCount));
            idfMap.put(e.getKey(), idf);

            ItemPopularity pop = new ItemPopularity();
            pop.setItemType(parts[0]);
            pop.setItemId(Long.valueOf(parts[1]));
            pop.setUserCount(userCount);
            pop.setIdfWeight(BigDecimal.valueOf(idf).setScale(6, RoundingMode.HALF_UP));
            pop.setComputedAt(now);
            itemPopularityMapper.insert(pop);

            try {
                redis.opsForValue().set(IDF_KEY_PREFIX + e.getKey(), String.valueOf(idf), Duration.ofHours(24));
            } catch (Exception ignored) {
            }
        }
        return idfMap;
    }

    /**
     * 批量加载若干用户对某类物品的交互记录（在线召回阶段使用）。
     */
    public List<UserItemInteraction> loadInteractions(Set<Long> userIds, String itemType) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptyList();
        return userItemInteractionMapper.selectList(new LambdaQueryWrapper<UserItemInteraction>()
                .in(UserItemInteraction::getUserId, userIds)
                .eq(UserItemInteraction::getItemType, itemType));
    }

    public double idfFor(String itemType, Long itemId) {
        String key = itemType + ":" + itemId;
        try {
            String v = redis.opsForValue().get(IDF_KEY_PREFIX + key);
            if (v != null) return Double.parseDouble(v);
        } catch (Exception ignored) {
        }
        ItemPopularity pop = itemPopularityMapper.selectOne(new LambdaQueryWrapper<ItemPopularity>()
                .eq(ItemPopularity::getItemType, itemType)
                .eq(ItemPopularity::getItemId, itemId)
                .last("limit 1"));
        return pop == null ? 1.0 : pop.getIdfWeight().doubleValue();
    }

    // -----------------------------------------------------------
    // 三、相似度计算
    // -----------------------------------------------------------

    /**
     * 对单个目标用户计算 Top-50 相似用户（基于内存中已加载的 userVectors / invertedIndex / idfMap）。
     */
    public LinkedHashMap<Long, Double> computeSimilarity(Long targetUserId,
                                                          Map<Long, Map<String, Double>> userVectors,
                                                          Map<String, Set<Long>> invertedIndex,
                                                          Map<String, Double> idfMap) {
        Map<String, Double> targetVec = userVectors.get(targetUserId);
        if (targetVec == null || targetVec.isEmpty()) return new LinkedHashMap<>();

        Map<String, Double> targetWeighted = applyIdf(targetVec, idfMap);
        double targetNorm = norm(targetWeighted);
        if (targetNorm <= 0) return new LinkedHashMap<>();

        // 倒排剪枝：候选 = 与目标用户至少共享一个物品的用户
        Set<Long> candidates = new HashSet<>();
        for (String item : targetVec.keySet()) {
            Set<Long> us = invertedIndex.get(item);
            if (us != null) candidates.addAll(us);
        }
        candidates.remove(targetUserId);

        Map<Long, Double> simMap = new HashMap<>();
        for (Long candidate : candidates) {
            Map<String, Double> candVec = userVectors.get(candidate);
            if (candVec == null) continue;
            // 共同物品
            Set<String> common = new HashSet<>(targetVec.keySet());
            common.retainAll(candVec.keySet());
            if (common.size() < MIN_COMMON_ITEMS) continue;

            Map<String, Double> candWeighted = applyIdf(candVec, idfMap);
            double dot = 0;
            for (String i : common) {
                dot += targetWeighted.getOrDefault(i, 0.0) * candWeighted.getOrDefault(i, 0.0);
            }
            double sim = dot / (targetNorm * norm(candWeighted) + 1e-9);
            if (sim > SIMILARITY_FLOOR) simMap.put(candidate, sim);
        }

        return simMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_SIMILAR_USERS)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Double> applyIdf(Map<String, Double> vec, Map<String, Double> idfMap) {
        Map<String, Double> w = new HashMap<>();
        for (Map.Entry<String, Double> e : vec.entrySet()) {
            double idf = idfMap.getOrDefault(e.getKey(), 1.0);
            w.put(e.getKey(), e.getValue() * idf);
        }
        return w;
    }

    private double norm(Map<String, Double> vec) {
        double s = 0;
        for (Double v : vec.values()) s += v * v;
        return Math.sqrt(s);
    }

    // -----------------------------------------------------------
    // 四、Redis 写入与读取
    // -----------------------------------------------------------

    public void writeSimUserZSet(Long userId, LinkedHashMap<Long, Double> simUsers) {
        String key = SIM_USER_KEY_PREFIX + userId;
        try {
            redis.delete(key);
            if (simUsers.isEmpty()) return;
            Set<ZSetOperations.TypedTuple<String>> tuples = simUsers.entrySet().stream()
                    .map(e -> (ZSetOperations.TypedTuple<String>) new org.springframework.data.redis.core.DefaultTypedTuple<>(
                            String.valueOf(e.getKey()), e.getValue()))
                    .collect(Collectors.toSet());
            redis.opsForZSet().add(key, tuples);
            redis.expire(key, Duration.ofHours(6));
        } catch (Exception ex) {
            log.warn("写入 simuser ZSET 失败 userId={}: {}", userId, ex.getMessage());
        }
    }

    /**
     * 在线读取 Top-K 相似用户。
     * @return Map<similarUserId, similarity>，若 Redis 不可用或缓存为空返回 null（调用方走 fallback）。
     */
    public LinkedHashMap<Long, BigDecimal> readSimilarUsersFromCache(Long userId, int topK) {
        try {
            String key = SIM_USER_KEY_PREFIX + userId;
            Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
                    .reverseRangeWithScores(key, 0, topK - 1);
            if (tuples == null || tuples.isEmpty()) return null;
            LinkedHashMap<Long, BigDecimal> result = new LinkedHashMap<>();
            for (ZSetOperations.TypedTuple<String> t : tuples) {
                if (t.getValue() == null || t.getScore() == null) continue;
                result.put(Long.valueOf(t.getValue()),
                        BigDecimal.valueOf(t.getScore()).setScale(6, RoundingMode.HALF_UP));
            }
            return result.isEmpty() ? null : result;
        } catch (Exception ex) {
            log.warn("读取 simuser ZSET 失败 userId={}: {}", userId, ex.getMessage());
            return null;
        }
    }

    /**
     * DB 持久化（Redis 重启重建源）。
     */
    public void persistSimilarity(Long userId, LinkedHashMap<Long, Double> simUsers) {
        userUserSimilarityMapper.delete(new LambdaQueryWrapper<UserUserSimilarity>().eq(UserUserSimilarity::getUserId, userId));
        if (simUsers.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        int rank = 1;
        for (Map.Entry<Long, Double> e : simUsers.entrySet()) {
            UserUserSimilarity row = new UserUserSimilarity();
            row.setUserId(userId);
            row.setSimilarUserId(e.getKey());
            row.setSimilarity(BigDecimal.valueOf(e.getValue()).setScale(6, RoundingMode.HALF_UP));
            row.setRankNo(rank++);
            row.setComputedAt(now);
            userUserSimilarityMapper.insert(row);
        }
    }

    // -----------------------------------------------------------
    // 工具方法
    // -----------------------------------------------------------

    private void addWeight(Map<String, BigDecimal> vec, String key, double w) {
        vec.merge(key, BigDecimal.valueOf(w), BigDecimal::add);
    }

    private void touch(Map<String, LocalDateTime> map, String key, LocalDateTime t) {
        if (t == null) return;
        LocalDateTime existing = map.get(key);
        if (existing == null || t.isAfter(existing)) map.put(key, t);
    }
}
