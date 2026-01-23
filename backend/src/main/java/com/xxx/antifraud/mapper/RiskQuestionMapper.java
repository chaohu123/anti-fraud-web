package com.xxx.antifraud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.antifraud.entity.RiskQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险测评题目 Mapper
 */
@Mapper
public interface RiskQuestionMapper extends BaseMapper<RiskQuestion> {
}

