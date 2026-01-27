import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';
import http from '../api/http';
import { useAssessmentStore } from '../store/assessment';
import { useKnowledgeStore } from '../store/knowledge';
import { useAchievementStore } from '../store/achievement';

export type RiskLevel = 'low' | 'medium' | 'high';

const USER_SESSION_KEY = 'af_user_session';

function getUserStorageKey(userId: number | null | undefined) {
  return userId ? `af_user_u_${userId}` : 'af_user_guest';
}

function normalizeRiskLevel(input: any): RiskLevel {
  if (input == null) return 'low';
  const s = String(input).toLowerCase();
  if (s === 'low' || s === '0') return 'low';
  if (s === 'medium' || s === '1') return 'medium';
  if (s === 'high' || s === '2' || s === '3') return 'high';
  return 'low';
}

export const useUserStore = defineStore('user', {
  state: () => ({
    // 后端用户ID（登录成功后写入）
    userId: null as number | null,
    username: null as string | null,
    name: '访客',
    avatar: null as string | null, // 头像（base64 或 URL）
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
    resetProfile() {
      this.name = '访客';
      this.avatar = null;
      this.riskIndex = 0;
      this.riskLevel = 'low';
      this.badges = [];
      this.trainingCount = 0;
      this.riskHistory = [];
    },
    hydrate() {
      // 仅用 session 记录“当前是谁”，具体资料/统计按 userId 分桶存储
      const session = loadJson<{ userId: number | null; username: string | null }>(USER_SESSION_KEY, {
        userId: null,
        username: null,
      });
      this.userId = session.userId ?? null;
      this.username = session.username ?? null;

      const key = getUserStorageKey(this.userId);
      const data = loadJson<any>(key, null as any);
      if (!data) return;
      this.name = data.name ?? this.name;
      this.avatar = data.avatar ?? this.avatar;
      this.riskIndex = data.riskIndex ?? this.riskIndex;
      this.riskLevel = normalizeRiskLevel(data.riskLevel ?? this.riskLevel);
      this.badges = data.badges ?? this.badges;
      this.trainingCount = data.trainingCount ?? this.trainingCount;
      this.riskHistory = data.riskHistory ?? this.riskHistory;
    },
    persist() {
      // session：当前登录态
      saveJson(USER_SESSION_KEY, {
        userId: this.userId,
        username: this.username,
      });

      // profile：按 userId 分桶，避免不同用户互相覆盖
      const key = getUserStorageKey(this.userId);
      saveJson(key, {
        name: this.name,
        avatar: this.avatar,
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
      // 新用户：确保不会继承游客/上一账号的统计
      this.resetProfile();
      this.name = nickname || username;
      this.persist();
      // 账号切换后，按 userId 重新加载各模块本地数据
      useAssessmentStore().hydrate(this.userId);
      useKnowledgeStore().hydrate(this.userId);
      useAchievementStore().hydrate(this.userId);
      return this.userId;
    },
    async login(username: string, password: string) {
      const resp = await http.post('/users/login', { username, password });
      const data = resp.data as { userId: number; username: string };
      this.userId = data.userId;
      this.username = data.username;
      // 切换账号：先清空，再从该 userId 的桶中恢复
      this.resetProfile();
      // 尝试恢复本地该用户的资料/统计（如果此前登录过）
      const bucket = loadJson<any>(getUserStorageKey(this.userId), null as any);
      if (bucket) {
        this.name = bucket.name ?? this.name;
        this.avatar = bucket.avatar ?? this.avatar;
        this.riskIndex = bucket.riskIndex ?? this.riskIndex;
        this.riskLevel = bucket.riskLevel ?? this.riskLevel;
        this.badges = bucket.badges ?? this.badges;
        this.trainingCount = bucket.trainingCount ?? this.trainingCount;
        this.riskHistory = bucket.riskHistory ?? this.riskHistory;
      } else {
        this.name = username;
      }
      this.persist();
      // 账号切换后，按 userId 重新加载各模块本地数据
      useAssessmentStore().hydrate(this.userId);
      useKnowledgeStore().hydrate(this.userId);
      useAchievementStore().hydrate(this.userId);
      return this.userId;
    },
    async fetchUserInfo() {
      if (!this.userId) return;
      const resp = await http.get(`/users/${this.userId}`);
      const u = resp.data as { id: number; username: string; nickname?: string; avatar?: string; riskLevel?: string | null };
      this.username = u.username || this.username;
      this.name = u.nickname || u.username || this.name;
      if (u.avatar) {
        this.avatar = u.avatar;
      }
      if (u.riskLevel) {
        // 后端为大写枚举（LOW/MEDIUM/HIGH），前端统一使用小写
        this.riskLevel = normalizeRiskLevel(u.riskLevel);
      }
      this.persist();
    },
    async updateAvatar(avatarUrl: string) {
      if (!this.userId) {
        throw new Error('用户未登录');
      }
      await http.put(`/users/${this.userId}/avatar`, { avatar: avatarUrl });
      this.avatar = avatarUrl;
      this.persist();
    },
    logout() {
      const oldUserId = this.userId;
      this.userId = null;
      this.username = null;
      this.resetProfile();
      this.persist();
      // 退出后切回“游客桶”，避免继续展示上一个账号的数据
      useAssessmentStore().hydrate(null);
      useKnowledgeStore().hydrate(null);
      useAchievementStore().hydrate(null);
      // 如需更严格：也可以清掉游客桶的历史/已学记录（避免公共电脑残留）
      // useAssessmentStore().clear(oldUserId);
      // useKnowledgeStore().clear(oldUserId);
    },
    setAvatar(avatar: string | null) {
      this.avatar = avatar;
      this.persist();
    },
    setRisk(score: number, level: string) {
      this.riskIndex = score;
      this.riskLevel = normalizeRiskLevel(level || 'low');
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
    async updateProfile(nickname: string) {
      if (!this.userId) {
        throw new Error('用户未登录');
      }
      await http.put(`/users/${this.userId}/info`, { nickname });
      this.name = nickname || this.name;
      this.persist();
    },
    async changePassword(oldPassword: string, newPassword: string) {
      if (!this.userId) {
        throw new Error('用户未登录');
      }
      await http.put(`/users/${this.userId}/password`, {
        oldPassword,
        newPassword,
      });
    },
  },
});
