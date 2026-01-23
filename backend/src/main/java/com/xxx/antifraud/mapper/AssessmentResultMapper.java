package com.xxx.antifraud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.antifraud.entity.AssessmentResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测评结果表 Mapper（对应 af_assessment_result 表）
 */
@Mapper
public interface AssessmentResultMapper extends BaseMapper<AssessmentResult> {
}
