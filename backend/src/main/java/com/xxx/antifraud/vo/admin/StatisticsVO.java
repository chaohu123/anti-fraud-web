package com.xxx.antifraud.vo.admin;

import lombok.Data;
import java.util.List;

/**
 * 统计数据 VO
 */
public class StatisticsVO {

    /**
     * 用户总数响应
     */
    @Data
    public static class UserCount {
        private Long total;
    }

    /**
     * 风险等级分布响应
     */
    @Data
    public static class RiskDistribution {
        private List<RiskItem> data;

        @Data
        public static class RiskItem {
            private String name;
            private Integer value;
        }
    }

    /**
     * 活跃趋势响应
     */
    @Data
    public static class ActiveTrend {
        private List<TrendItem> data;

        @Data
        public static class TrendItem {
            private String date;
            private Integer count;
        }
    }
}
