package com.xxx.antifraud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.antifraud.entity.AntiFraudArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 防骗知识库文章 Mapper
 */
@Mapper
public interface AntiFraudArticleMapper extends BaseMapper<AntiFraudArticle> {
}

