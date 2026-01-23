package com.xxx.antifraud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.antifraud.entity.LearningRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 防骗知识学习记录 Mapper
 */
@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
}

