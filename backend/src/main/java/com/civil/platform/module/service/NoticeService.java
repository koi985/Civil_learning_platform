package com.civil.platform.module.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.civil.platform.common.api.PageResult;
import com.civil.platform.common.exception.BizException;
import com.civil.platform.module.entity.Notice;
import com.civil.platform.module.mapper.NoticeMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public PageResult<Map<String, Object>> pagePublicNotices(Long pageNo, Long pageSize, String keyword) {
        Page<Notice> page = noticeMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, "PUBLISHED")
                .like(StringUtils.hasText(keyword), Notice::getTitle, keyword)
                .orderByDesc(Notice::getPublishedAt));
        return new PageResult<>(page.getTotal(), pageNo, pageSize,
                page.getRecords().stream().map(this::buildNoticeItem).collect(Collectors.toList()));
    }

    public Map<String, Object> detail(Long noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null || !"PUBLISHED".equals(notice.getStatus())) {
            throw new BizException(404, "公告不存在");
        }
        notice.setReadCount((notice.getReadCount() == null ? 0 : notice.getReadCount()) + 1);
        noticeMapper.updateById(notice);
        Map<String, Object> result = new HashMap<>();
        result.put("notice", buildNoticeItem(notice));
        result.put("latestNotices", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, "PUBLISHED")
                .ne(Notice::getId, noticeId)
                .orderByDesc(Notice::getPublishedAt)
                .last("limit 5"))
                .stream().map(this::buildNoticeItem).collect(Collectors.toList()));
        return result;
    }

    public Map<String, Object> buildNoticeItem(Notice notice) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", notice.getId());
        item.put("title", notice.getTitle());
        item.put("summary", notice.getSummary());
        item.put("content", notice.getContent());
        item.put("readCount", notice.getReadCount() == null ? 0 : notice.getReadCount());
        item.put("publishedAt", notice.getPublishedAt());
        item.put("status", notice.getStatus());
        return item;
    }
}
