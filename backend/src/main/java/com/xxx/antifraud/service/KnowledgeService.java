package com.xxx.antifraud.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xxx.antifraud.entity.AntiFraudArticle;
import com.xxx.antifraud.vo.knowledge.KnowledgeArticleVO;
import com.xxx.antifraud.vo.knowledge.KnowledgeDetailVO;
import com.xxx.antifraud.vo.knowledge.LearningProgressVO;

import java.util.List;

/**
 * 防骗知识库与学习进度 Service
 */
public interface KnowledgeService extends IService<AntiFraudArticle> {

    List<KnowledgeArticleVO> listArticles(String category, String keyword);

    KnowledgeDetailVO getDetail(Long id);

    LearningProgressVO learn(Long articleId, Long userId, Integer progress);
}

