package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.module.dto.NewsCommentSaveRequest;
import com.civil.platform.module.dto.NewsSaveRequest;
import com.civil.platform.module.entity.News;
import com.civil.platform.module.entity.NewsCategory;
import com.civil.platform.module.entity.NewsComment;
import com.civil.platform.module.entity.NewsShareLog;
import com.civil.platform.module.entity.SysFileRecord;
import com.civil.platform.module.entity.UserNewsCategorySubscription;
import com.civil.platform.module.mapper.NewsCategoryMapper;
import com.civil.platform.module.mapper.NewsCommentMapper;
import com.civil.platform.module.mapper.NewsMapper;
import com.civil.platform.module.mapper.NewsShareLogMapper;
import com.civil.platform.module.mapper.SysFileRecordMapper;
import com.civil.platform.module.mapper.SysUserMapper;
import com.civil.platform.module.mapper.UserNewsCategorySubscriptionMapper;
import com.civil.platform.security.LoginUser;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewsService {

    private static final Duration READ_DEDUP_WINDOW = Duration.ofMinutes(30);

    private static final Set<String> ALLOWED_NEWS_CATEGORY_NAMES = new HashSet<>(Arrays.asList(
            "国考", "省考", "选调", "事业单位", "军队文职", "面试"
    ));
    private static final List<String> INFO_CATEGORIES = Arrays.asList(
            "考试公告", "考试大纲", "职位表", "考试时间", "报名入口", "报名人数",
            "准考证打印", "成绩查询", "面试名单", "考试政策", "招考信息"
    );
    private static final List<String> PROVINCES = Arrays.asList(
            "全国", "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
            "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南",
            "广东", "广西", "海南", "重庆", "四川", "贵州", "云南", "西藏", "陕西", "甘肃",
            "青海", "宁夏", "新疆"
    );

    private final NewsMapper newsMapper;
    private final NewsCategoryMapper newsCategoryMapper;
    private final NewsCommentMapper newsCommentMapper;
    private final NewsShareLogMapper newsShareLogMapper;
    private final SysFileRecordMapper sysFileRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final UserNewsCategorySubscriptionMapper userNewsCategorySubscriptionMapper;
    private final StringRedisTemplate redis;

    public NewsService(NewsMapper newsMapper,
                       NewsCategoryMapper newsCategoryMapper,
                       NewsCommentMapper newsCommentMapper,
                       NewsShareLogMapper newsShareLogMapper,
                       SysFileRecordMapper sysFileRecordMapper,
                       SysUserMapper sysUserMapper,
                       UserNewsCategorySubscriptionMapper userNewsCategorySubscriptionMapper,
                       StringRedisTemplate redis) {
        this.newsMapper = newsMapper;
        this.newsCategoryMapper = newsCategoryMapper;
        this.newsCommentMapper = newsCommentMapper;
        this.newsShareLogMapper = newsShareLogMapper;
        this.sysFileRecordMapper = sysFileRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.userNewsCategorySubscriptionMapper = userNewsCategorySubscriptionMapper;
        this.redis = redis;
    }

    public PageResult<Map<String, Object>> pageNews(Long pageNo, Long pageSize, String keyword, Long categoryId,
                                                    String infoCategory, String province) {
        Page<News> page = newsMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .like(StringUtils.hasText(keyword), News::getTitle, keyword)
                .eq(categoryId != null, News::getCategoryId, categoryId)
                .eq(StringUtils.hasText(infoCategory), News::getInfoCategory, infoCategory)
                .eq(StringUtils.hasText(province), News::getProvince, province)
                .orderByDesc(News::getPublishedAt));
        List<Map<String, Object>> records = page.getRecords().stream().map(this::buildNewsItem).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), pageNo, pageSize, records);
    }

    public Map<String, Object> detail(Long newsId) {
        News news = newsMapper.selectById(newsId);
        if (news == null || !"PUBLISHED".equals(news.getStatus())) {
            throw new BizException(404, "资讯不存在");
        }
        if (shouldCountRead(newsId)) {
            news.setReadCount((news.getReadCount() == null ? 0 : news.getReadCount()) + 1);
            newsMapper.updateById(news);
        }
        List<Map<String, Object>> comments = newsCommentMapper.selectList(new LambdaQueryWrapper<NewsComment>()
                        .eq(NewsComment::getNewsId, newsId)
                        .orderByDesc(NewsComment::getCreatedAt))
                .stream().map(comment -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", comment.getId());
                    item.put("content", comment.getContent());
                    item.put("createdAt", comment.getCreatedAt());
                    item.put("nickname", sysUserMapper.selectById(comment.getUserId()).getNickname());
                    return item;
                }).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("news", buildNewsItem(news));
        result.put("comments", comments);
        result.put("categories", categories());
        result.put("hotNews", hotNews());
        result.put("relatedNews", newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getCategoryId, news.getCategoryId())
                .eq(News::getStatus, "PUBLISHED")
                .ne(News::getId, newsId)
                .orderByDesc(News::getPublishedAt)
                .last("limit 3")).stream().map(this::buildNewsItem).collect(Collectors.toList()));
        result.put("previousNews", buildAdjacentNews(news, true));
        result.put("nextNews", buildAdjacentNews(news, false));
        return result;
    }

    public List<Map<String, Object>> hotNews() {
        return newsMapper.selectList(new LambdaQueryWrapper<News>()
                        .eq(News::getStatus, "PUBLISHED")
                        .orderByDesc(News::getReadCount)
                        .orderByDesc(News::getShareCount)
                        .last("limit 5"))
                .stream()
                .map(this::buildNewsItem)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> categories() {
        Long userId = currentUserIdOrNull();
        Set<Long> subscribedCategoryIds = userId == null ? java.util.Collections.emptySet()
                : userNewsCategorySubscriptionMapper.selectList(new LambdaQueryWrapper<UserNewsCategorySubscription>()
                        .eq(UserNewsCategorySubscription::getUserId, userId))
                .stream()
                .map(UserNewsCategorySubscription::getCategoryId)
                .collect(Collectors.toSet());
        return newsCategoryMapper.selectList(new LambdaQueryWrapper<NewsCategory>().orderByAsc(NewsCategory::getSortNo))
                .stream()
                .map(category -> buildCategoryItem(category, subscribedCategoryIds.contains(category.getId())))
                .collect(Collectors.toList());
    }

    public Map<String, Object> filterOptions() {
        Map<String, Object> result = new HashMap<>();
        result.put("examProjects", categories());
        result.put("infoCategories", INFO_CATEGORIES);
        result.put("provinces", PROVINCES);
        return result;
    }

    public Map<String, Object> subscribeCategory(Long categoryId) {
        Long userId = currentUserId();
        NewsCategory category = newsCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(404, "资讯分类不存在");
        }
        UserNewsCategorySubscription exists = userNewsCategorySubscriptionMapper.selectOne(new LambdaQueryWrapper<UserNewsCategorySubscription>()
                .eq(UserNewsCategorySubscription::getUserId, userId)
                .eq(UserNewsCategorySubscription::getCategoryId, categoryId)
                .last("limit 1"));
        if (exists == null) {
            UserNewsCategorySubscription subscription = new UserNewsCategorySubscription();
            subscription.setUserId(userId);
            subscription.setCategoryId(categoryId);
            subscription.setCreatedAt(LocalDateTime.now());
            userNewsCategorySubscriptionMapper.insert(subscription);
        }
        return buildCategoryItem(category, true);
    }

    public Map<String, Object> unsubscribeCategory(Long categoryId) {
        Long userId = currentUserId();
        NewsCategory category = newsCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(404, "资讯分类不存在");
        }
        userNewsCategorySubscriptionMapper.delete(new LambdaQueryWrapper<UserNewsCategorySubscription>()
                .eq(UserNewsCategorySubscription::getUserId, userId)
                .eq(UserNewsCategorySubscription::getCategoryId, categoryId));
        return buildCategoryItem(category, false);
    }

    public PageResult<News> adminPage(Long pageNo, Long pageSize, String keyword, Long categoryId,
                                      String infoCategory, String province) {
        Page<News> page = newsMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<News>()
                .like(StringUtils.hasText(keyword), News::getTitle, keyword)
                .eq(categoryId != null, News::getCategoryId, categoryId)
                .eq(StringUtils.hasText(infoCategory), News::getInfoCategory, infoCategory)
                .eq(StringUtils.hasText(province), News::getProvince, province)
                .orderByDesc(News::getPublishedAt));
        return new PageResult<>(page.getTotal(), pageNo, pageSize, page.getRecords());
    }

    public void save(NewsSaveRequest request) {
        validateAdminNewsCategory(request.getCategoryId());
        News news = request.getId() == null ? new News() : newsMapper.selectById(request.getId());
        if (news == null) {
            throw new BizException("资讯不存在");
        }
        BeanUtils.copyProperties(request, news, "coverFileId", "coverUrl");
        applyNewsTaxonomy(news);
        applyNewsCover(news, request);
        if (news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }
        if (!StringUtils.hasText(news.getStatus())) {
            news.setStatus("PUBLISHED");
        }
        if (news.getId() == null) {
            newsMapper.insert(news);
        } else {
            newsMapper.updateById(news);
        }
    }

    public void remove(Long id) {
        newsMapper.deleteById(id);
    }

    public void saveComment(Long newsId, NewsCommentSaveRequest request) {
        News news = newsMapper.selectById(newsId);
        if (news == null || !"PUBLISHED".equals(news.getStatus())) {
            throw new BizException("资讯不存在");
        }
        Long userId = currentUserId();
        NewsComment comment = new NewsComment();
        comment.setNewsId(newsId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        newsCommentMapper.insert(comment);
    }

    public void share(Long newsId) {
        News news = newsMapper.selectById(newsId);
        if (news == null || !"PUBLISHED".equals(news.getStatus())) {
            throw new BizException("资讯不存在");
        }
        NewsShareLog log = new NewsShareLog();
        log.setNewsId(newsId);
        log.setUserId(currentUserIdOrNull());
        log.setShareChannel("LINK");
        newsShareLogMapper.insert(log);
        news.setShareCount((news.getShareCount() == null ? 0 : news.getShareCount()) + 1);
        newsMapper.updateById(news);
    }

    private Map<String, Object> buildAdjacentNews(News current, boolean previous) {
        if (current.getPublishedAt() == null) {
            return null;
        }
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<News>()
                .eq(News::getStatus, "PUBLISHED")
                .ne(News::getId, current.getId());
        if (previous) {
            wrapper.gt(News::getPublishedAt, current.getPublishedAt())
                    .orderByAsc(News::getPublishedAt);
        } else {
            wrapper.lt(News::getPublishedAt, current.getPublishedAt())
                    .orderByDesc(News::getPublishedAt);
        }
        News adjacent = newsMapper.selectOne(wrapper.last("limit 1"));
        return adjacent == null ? null : buildNewsItem(adjacent);
    }

    private Map<String, Object> buildCategoryItem(NewsCategory category, boolean subscribed) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", category.getId());
        item.put("name", category.getName());
        item.put("sortNo", category.getSortNo());
        item.put("subscribed", subscribed);
        return item;
    }

    private Map<String, Object> buildNewsItem(News news) {
        NewsCategory category = newsCategoryMapper.selectById(news.getCategoryId());
        Map<String, Object> item = new HashMap<>();
        item.put("id", news.getId());
        item.put("title", news.getTitle());
        item.put("infoCategory", news.getInfoCategory());
        item.put("province", news.getProvince());
        item.put("summary", news.getSummary());
        item.put("coverFileId", news.getCoverFileId());
        item.put("coverUrl", news.getCoverUrl());
        item.put("content", news.getContent());
        item.put("tags", news.getTags());
        item.put("readCount", news.getReadCount());
        item.put("shareCount", news.getShareCount() == null ? 0 : news.getShareCount());
        item.put("commentCount", newsCommentMapper.selectCount(new LambdaQueryWrapper<NewsComment>().eq(NewsComment::getNewsId, news.getId())));
        item.put("status", news.getStatus());
        item.put("publishedAt", news.getPublishedAt());
        item.put("categoryId", news.getCategoryId());
        item.put("categoryName", category == null ? null : category.getName());
        return item;
    }

    private void applyNewsTaxonomy(News news) {
        if (!StringUtils.hasText(news.getInfoCategory())) {
            news.setInfoCategory("考试公告");
        }
        if (!INFO_CATEGORIES.contains(news.getInfoCategory())) {
            throw new BizException("资讯分类只能选择目标站支持的分类");
        }
        if (!StringUtils.hasText(news.getProvince())) {
            news.setProvince("全国");
        }
        if (!PROVINCES.contains(news.getProvince())) {
            throw new BizException("招考省份只能选择目标站支持的省份");
        }
        news.setSourceName(null);
        news.setAuthorName(null);
    }

    private void applyNewsCover(News news, NewsSaveRequest request) {
        if (request.getCoverFileId() != null) {
            SysFileRecord coverFile = sysFileRecordMapper.selectById(request.getCoverFileId());
            if (coverFile == null) {
                throw new BizException("资讯封面文件不存在");
            }
            news.setCoverFileId(coverFile.getId());
            news.setCoverUrl("/api/files/view/" + coverFile.getId());
            return;
        }
        if (StringUtils.hasText(request.getCoverUrl())) {
            news.setCoverUrl(request.getCoverUrl());
            return;
        }
        news.setCoverUrl(null);
    }

    private void validateAdminNewsCategory(Long categoryId) {
        NewsCategory category = newsCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException("资讯分类不存在");
        }
        if (!ALLOWED_NEWS_CATEGORY_NAMES.contains(category.getName())) {
            throw new BizException("资讯分类只能选择：国考、省考、选调、事业单位、军队文职、面试");
        }
    }

    private boolean shouldCountRead(Long newsId) {
        try {
            String key = "news:read:dedupe:" + newsId + ":" + readerIdentity();
            Boolean firstRead = redis.opsForValue().setIfAbsent(key, "1", READ_DEDUP_WINDOW);
            return Boolean.TRUE.equals(firstRead);
        } catch (Exception ignored) {
            return true;
        }
    }

    private String readerIdentity() {
        Long userId = currentUserIdOrNull();
        if (userId != null) {
            return "user:" + userId;
        }
        HttpServletRequest request = currentRequest();
        String raw = request == null
                ? "guest:unknown"
                : "guest:" + clientIp(request) + ":" + StringUtils.trimWhitespace(request.getHeader("User-Agent"));
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
    }

    private Long currentUserId() {
        Long userId = currentUserIdOrNull();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return ((LoginUser) authentication.getPrincipal()).getUserId();
    }
}
