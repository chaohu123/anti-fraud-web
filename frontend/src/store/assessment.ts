import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';
import type { RiskLevel } from '../stores/user';

export type DimensionKey = 'info' | 'finance' | 'psych';

export interface AssessmentDimension {
  dimension: DimensionKey;
  name: string;
  score: number;
  level: RiskLevel;
}

export interface AssessmentReport {
  id?: string; // 报告ID，用于详情页
  score: number;
  level: RiskLevel;
  explanation: string;
  dimensions: AssessmentDimension[];
  suggestions: string[];
  createdAt: string;
}

function getAssessmentStorageKey(userId: number | null | undefined) {
  // 账号隔离：不同用户使用不同 key，避免切换账号串数据
  return userId ? `af_assessment_u_${userId}` : 'af_assessment_guest';
}

export const useAssessmentStore = defineStore('assessment', {
  state: () => ({
    storageKey: 'af_assessment_guest',
    lastReport: null as AssessmentReport | null,
    reportHistory: [] as AssessmentReport[], // 历史报告列表
  }),
  actions: {
    hydrate(userId?: number | null) {
      this.storageKey = getAssessmentStorageKey(userId ?? null);
      const data = loadJson(this.storageKey, null as any);
      // 如果该用户（或游客）本地没有任何评估数据，重置为初始状态，避免串号
      if (!data) {
        this.lastReport = null;
        this.reportHistory = [];
        return;
      }
      this.lastReport = data.lastReport ?? null;
      this.reportHistory = Array.isArray(data.reportHistory) ? data.reportHistory : [];
    },
    persist() {
      saveJson(this.storageKey, {
        lastReport: this.lastReport,
        reportHistory: this.reportHistory,
      });
    },
    clear(userId?: number | null) {
      const key = getAssessmentStorageKey(userId ?? null);
      try {
        localStorage.removeItem(key);
      } catch {
        // ignore
      }
      if (key === this.storageKey) {
        this.lastReport = null;
        this.reportHistory = [];
      }
    },
    setReport(report: AssessmentReport) {
      // 生成报告ID
      if (!report.id) {
        report.id = `report_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
      }
      this.lastReport = report;
      // 添加到历史记录（去重，相同ID不重复添加）
      const existingIndex = this.reportHistory.findIndex(r => r.id === report.id);
      if (existingIndex >= 0) {
        this.reportHistory[existingIndex] = report;
      } else {
        this.reportHistory.unshift(report); // 最新的在前面
      }
      // 限制历史记录数量（最多保留50条）
      if (this.reportHistory.length > 50) {
        this.reportHistory = this.reportHistory.slice(0, 50);
      }
      this.persist();
    },
    getReportById(id: string): AssessmentReport | null {
      const found = this.reportHistory.find(r => r.id === id);
      if (found) return found;
      if (this.lastReport?.id === id) return this.lastReport;
      return null;
    },
  },
});

