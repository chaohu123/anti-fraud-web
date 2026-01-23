package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxx.antifraud.common.BusinessException;
import com.xxx.antifraud.common.ErrorCode;
import com.xxx.antifraud.common.enums.LearningStatus;
import com.xxx.antifraud.entity.AntiFraudArticle;
import com.xxx.antifraud.entity.LearningRecord;
import com.xxx.antifraud.mapper.AntiFraudArticleMapper;
import com.xxx.antifraud.mapper.LearningRecordMapper;
import com.xxx.antifraud.service.KnowledgeService;
import com.xxx.antifraud.vo.knowledge.KnowledgeArticleVO;
import com.xxx.antifraud.vo.knowledge.KnowledgeDetailVO;
import com.xxx.antifraud.vo.knowledge.LearningProgressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 防骗知识库与学习进度 Service 实现
 */
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl extends ServiceImpl<AntiFraudArticleMapper, AntiFraudArticle>
        implements KnowledgeService {

    private final LearningRecordMapper learningRecordMapper;

    @Override
    public List<KnowledgeArticleVO> listArticles(String category, String keyword) {
        LambdaQueryWrapper<AntiFraudArticle> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(category)) {
            // 前端分类多为中文（如：短信/电话/网站/社交），这里保持原值做精确匹配
            wrapper.eq(AntiFraudArticle::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AntiFraudArticle::getTitle, keyword)
                    .or().like(AntiFraudArticle::getSummary, keyword));
        }
        wrapper.orderByAsc(AntiFraudArticle::getId);
        List<AntiFraudArticle> list = this.list(wrapper);
        return list.stream().map(a -> {
            KnowledgeArticleVO vo = new KnowledgeArticleVO();
            vo.setId(a.getId());
            vo.setCategory(a.getCategory());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public KnowledgeDetailVO getDetail(Long id) {
        AntiFraudArticle article = this.getById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        KnowledgeDetailVO vo = new KnowledgeDetailVO();
        vo.setId(article.getId());
        vo.setCategory(article.getCategory());
        vo.setTitle(article.getTitle());
        vo.setSummary(article.getSummary());
        vo.setContent(article.getContent());
        return vo;
    }

    @Override
    public LearningProgressVO learn(Long articleId, Long userId, Integer progress) {
        AntiFraudArticle article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (progress == null || progress < 0) {
            progress = 0;
        }
        if (progress > 100) {
            progress = 100;
        }
        LearningRecord record = learningRecordMapper.selectOne(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getArticleId, articleId));
        if (record == null) {
            record = new LearningRecord();
            record.setUserId(userId);
            record.setArticleId(articleId);
        }
        record.setProgress(progress);
        record.setStatus(progress >= 100 ? LearningStatus.FINISHED.name() : LearningStatus.READING.name());
        record.setLearnedAt(LocalDateTime.now());
        if (record.getId() == null) {
            learningRecordMapper.insert(record);
        } else {
            learningRecordMapper.updateById(record);
        }

        // 统计用户整体学习进度与成就等级
        List<LearningRecord> all = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>().eq(LearningRecord::getUserId, userId));
        long totalCount = this.count();
        long finishedCount = all.stream()
                .filter(r -> LearningStatus.FINISHED.name().equals(r.getStatus()))
                .count();
        double completionRate = totalCount == 0 ? 0.0 : finishedCount * 1.0 / totalCount;

        String level;
        if (finishedCount == 0) {
            level = "新手防骗者";
        } else if (finishedCount <= 3) {
            level = "入门守护者";
        } else if (finishedCount <= 6) {
            level = "进阶识骗者";
        } else {
            level = "资深防骗达人";
        }

        LearningProgressVO vo = new LearningProgressVO();
        vo.setUserId(userId);
        vo.setTotalArticles(totalCount);
        vo.setFinishedArticles(finishedCount);
        vo.setCompletionRate(completionRate);
        vo.setLevel(level);
        return vo;
    }
}

