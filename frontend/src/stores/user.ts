import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';
import http from '../api/http';

export type RiskLevel = 'low' | 'medium' | 'high';

export const useUserStore = defineStore('user', {
  state: () => ({
    // 后端用户ID（登录成功后写入）
    userId: null as number | null,
    username: null as string | null,
    name: '访客',
    riskIndex: 0,
    riskLevel: 'low' as RiskLevel,
    badges: [] as string[],
    trainingCount: 0,
    riskHistory: [] as { at: string; score: number }[],
  }),
  getters: {
    isAdmin(): boolean {
      return (this.username || '').toLowerCase() === 'admin';
    },
  },
  actions: {
    hydrate() {
      const data = loadJson('af_user', null as any);
      if (!data) return;
      this.userId = data.userId ?? this.userId;
      this.username = data.username ?? this.username;
      this.name = data.name ?? this.name;
      this.riskIndex = data.riskIndex ?? this.riskIndex;
      this.riskLevel = data.riskLevel ?? this.riskLevel;
      this.badges = data.badges ?? this.badges;
      this.trainingCount = data.trainingCount ?? this.trainingCount;
      this.riskHistory = data.riskHistory ?? this.riskHistory;
    },
    persist() {
      saveJson('af_user', {
        userId: this.userId,
        username: this.username,
        name: this.name,
        riskIndex: this.riskIndex,
        riskLevel: this.riskLevel,
        badges: this.badges,
        trainingCount: this.trainingCount,
        riskHistory: this.riskHistory,
      });
    },
    async register(username: string, password: string, nickname?: string) {
      const resp = await http.post('/users/register', { 
        username, 
        password, 
        nickname: nickname || username 
      });
      this.userId = resp.data as number;
      this.username = username;
      this.name = nickname || username;
      this.persist();
      return this.userId;
    },
    async login(username: string, password: string) {
      const resp = await http.post('/users/login', { username, password });
      const data = resp.data as { userId: number; username: string };
      this.userId = data.userId;
      this.username = data.username;
      this.name = username;
      this.persist();
      return this.userId;
    },
    async fetchUserInfo() {
      if (!this.userId) return;
      const resp = await http.get(`/users/${this.userId}`);
      const u = resp.data as { id: number; username: string; nickname?: string; riskLevel?: string | null };
      this.username = u.username || this.username;
      this.name = u.nickname || u.username || this.name;
      if (u.riskLevel) {
        // 后端为大写枚举（LOW/MEDIUM/HIGH），前端统一使用小写
        const lv = u.riskLevel.toString().toLowerCase() as RiskLevel;
        this.riskLevel = lv;
      }
      this.persist();
    },
    logout() {
      this.userId = null;
      this.username = null;
      this.name = '访客';
      this.persist();
    },
    setRisk(score: number, level: string) {
      this.riskIndex = score;
      const lv = (level || 'low').toString().toLowerCase() as RiskLevel;
      this.riskLevel = lv;
      this.riskHistory.push({ at: new Date().toISOString(), score });
      this.persist();
    },
    awardBadge(badge: string) {
      if (!this.badges.includes(badge)) {
        this.badges.push(badge);
        this.persist();
      }
    },
    recordTraining() {
      this.trainingCount += 1;
      if (this.trainingCount >= 3) {
        this.awardBadge('训练达人');
      }
      this.persist();
    },
  },
});
