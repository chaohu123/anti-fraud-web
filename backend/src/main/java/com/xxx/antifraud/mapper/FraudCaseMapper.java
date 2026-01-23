package com.xxx.antifraud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxx.antifraud.entity.FraudCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 诈骗案例 Mapper
 */
@Mapper
public interface FraudCaseMapper extends BaseMapper<FraudCase> {
}

