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
  score: number;
  level: RiskLevel;
  explanation: string;
  dimensions: AssessmentDimension[];
  suggestions: string[];
  createdAt: string;
}

export const useAssessmentStore = defineStore('assessment', {
  state: () => ({
    lastReport: null as AssessmentReport | null,
  }),
  actions: {
    hydrate() {
      const data = loadJson('af_assessment', null as any);
      if (!data) return;
      this.lastReport = data.lastReport ?? this.lastReport;
    },
    persist() {
      saveJson('af_assessment', {
        lastReport: this.lastReport,
      });
    },
    setReport(report: AssessmentReport) {
      this.lastReport = report;
      this.persist();
    },
  },
});

