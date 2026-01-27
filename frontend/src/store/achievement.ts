import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';
import http from '../api/http';
import { useUserStore } from '../stores/user';
import { useKnowledgeStore } from './knowledge';
import { useAssessmentStore } from './assessment';

function getAchievementStorageKey(userId: number | null | undefined) {
  return userId ? `af_achievement_u_${userId}` : 'af_achievement_guest';
}

export type AchievementCategory = 'training' | 'learning' | 'assessment' | 'special';

export type AchievementStatus = 'locked' | 'progress' | 'unlocked';

export interface Achievement {
  id: string;
  name: string;
  description: string;
  category: AchievementCategory;
  icon: string;
  expReward: number; // 经验值奖励
  condition: {
    type: 'training' | 'learning' | 'assessment' | 'special';
    target: number; // 目标值
    key?: string; // 特殊条件键
  };
  status: AchievementStatus;
  progress: number; // 当前进度 (0-100)
  unlockedAt?: string; // 解锁时间
}

export const useAchievementStore = defineStore('achievement', {
  state: () => ({
    storageKey: 'af_achievement_guest',
    level: 1, // 当前等级
    exp: 0, // 当前经验值
    totalExp: 0, // 累计经验值
    achievements: [] as Achievement[],
    unlockedAchievements: new Set<string>(), // 已解锁的成就ID
  }),

  getters: {
    // 当前等级所需经验
    expForNextLevel(): number {
      return this.level * 100; // 每级需要 等级 * 100 经验
    },
    
    // 当前等级经验进度百分比
    expProgress(): number {
      // exp 在 addExp() 中会在升级时扣减阈值，因此这里的 exp 表示“当前等级已获得经验”
      // 进度应为：当前经验 / 当前等级升级所需经验
      const needed = this.expForNextLevel || 1;
      return Math.min(100, Math.max(0, (this.exp / needed) * 100));
    },

    // 等级称号
    levelTitle(): string {
      const titles = [
        '防骗新手',
        '防骗学徒',
        '防骗达人',
        '防骗专家',
        '防骗大师',
        '防骗宗师',
        '防骗传奇',
      ];
      return titles[Math.min(this.level - 1, titles.length - 1)] || '防骗新手';
    },

    // 按分类获取成就
    achievementsByCategory: (state) => (category: AchievementCategory) => {
      return state.achievements.filter((a) => a.category === category);
    },

    // 获取成就状态
    getAchievementStatus: (state) => (id: string): AchievementStatus => {
      if (state.unlockedAchievements.has(id)) return 'unlocked';
      const achievement = state.achievements.find((a) => a.id === id);
      if (!achievement) return 'locked';
      return achievement.progress > 0 ? 'progress' : 'locked';
    },
  },

  actions: {
    async hydrate(userId?: number | null) {
      this.storageKey = getAchievementStorageKey(userId ?? null);
      const data = loadJson(this.storageKey, {
        level: 1,
        exp: 0,
        totalExp: 0,
        achievements: [],
        unlockedAchievements: [] as string[],
      });
      this.level = data.level ?? 1;
      this.exp = data.exp ?? 0;
      this.totalExp = data.totalExp ?? 0;
      this.unlockedAchievements = new Set(data.unlockedAchievements || []);

      // 优先尝试从后端加载成就配置，失败则回退到本地默认配置
      const loadedFromBackend = await this.loadFromBackendConfig().catch(() => false);
      if (!loadedFromBackend) {
        this.initAchievements();
      }
      // 确保已解锁列表只包含当前成就集合中的合法 ID
      this.sanitizeUnlocked();
      this.checkAchievements();
    },

    persist() {
      saveJson(this.storageKey, {
        level: this.level,
        exp: this.exp,
        totalExp: this.totalExp,
        achievements: this.achievements,
        unlockedAchievements: Array.from(this.unlockedAchievements),
      });
    },

    clear(userId?: number | null) {
      const key = getAchievementStorageKey(userId ?? null);
      try {
        localStorage.removeItem(key);
      } catch {
        // ignore
      }
      if (key === this.storageKey) {
        this.level = 1;
        this.exp = 0;
        this.totalExp = 0;
        this.achievements = [];
        this.unlockedAchievements = new Set<string>();
      }
    },

    // 清理不存在于当前成就列表中的“脏”已解锁ID，避免统计显示异常
    sanitizeUnlocked() {
      const validIds = new Set(this.achievements.map((a) => a.id));
      this.unlockedAchievements = new Set(
        Array.from(this.unlockedAchievements).filter((id) => validIds.has(id))
      );
    },

    // 从后端成就规则表加载配置（管理员端维护）
    async loadFromBackendConfig(): Promise<boolean> {
      try {
        // 直接复用管理员成就列表接口，只取启用状态的规则
        const resp = await http.get('/admin/achievements', {
          params: {
            page: 1,
            size: 100,
            status: 'ACTIVE',
          },
        });
        const records =
          resp.data?.data?.content ??
          resp.data?.content ??
          resp.data ??
          [];

        if (!Array.isArray(records) || records.length === 0) {
          return false;
        }

        // 将后端成就规则映射为前端成就模型
        const mapped: Achievement[] = records.map((raw: any, index: number) => {
          const id = String(raw.id ?? `server_${index}`);
          const name = raw.name ?? '未命名成就';
          const description = raw.description ?? '';
          const conditionType = String(raw.condition || raw.conditionType || '').toUpperCase();
          const conditionValue = Number(raw.conditionValue ?? 0) || 1;
          const rewardExp = Number(raw.rewardExp ?? 0) || 0;
          const icon = raw.icon || '🏆';

          // 后端条件类型（condition_type） → 前端分类与条件
          let category: AchievementCategory = 'special';
          let type: Achievement['condition']['type'] = 'special';
          let key: string | undefined;

          switch (conditionType) {
            // 训练相关
            case 'TRAINING_COUNT':
            case 'TRAINING_CORRECT':
              category = 'training';
              type = 'training';
              if (conditionType === 'TRAINING_CORRECT') {
                key = 'training_correct';
              }
              break;

            // 学习相关
            case 'LEARNING_COUNT':
              category = 'learning';
              type = 'learning';
              break;

            // 测评相关
            case 'ASSESSMENT_COMPLETE':
              category = 'assessment';
              type = 'assessment';
              break;
            case 'RISK_LEVEL_UP':
              category = 'assessment';
              type = 'assessment';
              key = 'risk_level_up';
              break;

            // 特殊 / 全局统计类
            case 'TOTAL_EXP':
              category = 'special';
              type = 'special';
              key = 'total_exp';
              break;
            case 'LOGIN_STREAK':
              category = 'special';
              type = 'special';
              key = 'login_streak';
              break;
            case 'SPECIAL_ALL':
              category = 'special';
              type = 'special';
              key = 'all_categories';
              break;
            case 'LEVEL_REACH':
              category = 'special';
              type = 'special';
              key = 'level';
              break;

            default:
              // 未识别类型统一归为特殊成就，仅展示不影响逻辑
              category = 'special';
              type = 'special';
          }

          return {
            id,
            name,
            description,
            category,
            icon,
            expReward: rewardExp,
            condition: {
              type,
              target: conditionValue,
              key,
            },
            status: 'locked',
            progress: 0,
          };
        });

        this.achievements = mapped;
        // 后端重新下发规则后同步清理一次已解锁ID
        this.sanitizeUnlocked();
        return true;
      } catch {
        // 接口不可用时保持静默，回退到本地内置成就
        return false;
      }
    },

    // 初始化成就列表（本地内置规则）
    initAchievements() {
      if (this.achievements.length > 0) return; // 已初始化

      this.achievements = [
        // 训练成就
        {
          id: 'train_1',
          name: '初出茅庐',
          description: '完成1次识别训练',
          category: 'training',
          icon: '🎯',
          expReward: 10,
          condition: { type: 'training', target: 1 },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'train_3',
          name: '训练达人',
          description: '完成3次识别训练',
          category: 'training',
          icon: '🏆',
          expReward: 30,
          condition: { type: 'training', target: 3 },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'train_10',
          name: '训练专家',
          description: '完成10次识别训练',
          category: 'training',
          icon: '⭐',
          expReward: 100,
          condition: { type: 'training', target: 10 },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'train_50',
          name: '训练大师',
          description: '完成50次识别训练',
          category: 'training',
          icon: '👑',
          expReward: 500,
          condition: { type: 'training', target: 50 },
          status: 'locked',
          progress: 0,
        },
        // 学习成就
        {
          id: 'learn_1',
          name: '知识启蒙',
          description: '学习1条防骗知识',
          category: 'learning',
          icon: '📚',
          expReward: 10,
          condition: { type: 'learning', target: 1 },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'learn_5',
          name: '知识积累',
          description: '学习5条防骗知识',
          category: 'learning',
          icon: '📖',
          expReward: 50,
          condition: { type: 'learning', target: 5 },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'learn_20',
          name: '知识达人',
          description: '学习20条防骗知识',
          category: 'learning',
          icon: '🎓',
          expReward: 200,
          condition: { type: 'learning', target: 20 },
          status: 'locked',
          progress: 0,
        },
        // 测评成就
        {
          id: 'assess_1',
          name: '首次测评',
          description: '完成1次风险测评',
          category: 'assessment',
          icon: '📊',
          expReward: 20,
          condition: { type: 'assessment', target: 1 },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'assess_low',
          name: '低风险守护者',
          description: '获得低风险评级',
          category: 'assessment',
          icon: '🛡️',
          expReward: 50,
          condition: { type: 'assessment', target: 1, key: 'low_risk' },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'assess_3',
          name: '持续关注',
          description: '完成3次风险测评',
          category: 'assessment',
          icon: '📈',
          expReward: 100,
          condition: { type: 'assessment', target: 3 },
          status: 'locked',
          progress: 0,
        },
        // 特殊成就
        {
          id: 'special_all',
          name: '全能防骗者',
          description: '完成所有类型的成就',
          category: 'special',
          icon: '🌟',
          expReward: 500,
          condition: { type: 'special', target: 1, key: 'all_categories' },
          status: 'locked',
          progress: 0,
        },
        {
          id: 'special_level5',
          name: '防骗专家',
          description: '达到5级',
          category: 'special',
          icon: '💎',
          expReward: 300,
          condition: { type: 'special', target: 5, key: 'level' },
          status: 'locked',
          progress: 0,
        },
      ];
    },

    // 检查并更新成就进度
    checkAchievements() {
      const userStore = useUserStore();
      const knowledgeStore = useKnowledgeStore();
      const assessmentStore = useAssessmentStore();

      this.achievements.forEach((achievement) => {
        if (this.unlockedAchievements.has(achievement.id)) {
          achievement.status = 'unlocked';
          achievement.progress = 100;
          return;
        }

        let current = 0;
        let target = achievement.condition.target;

        switch (achievement.condition.type) {
          case 'training':
            // 训练相关成就：目前只统计训练次数
            current = userStore.trainingCount;
            break;
          case 'learning':
            // 学习相关成就：使用已学习知识数
            current = knowledgeStore.readCount;
            break;
          case 'assessment':
            if (achievement.condition.key === 'low_risk') {
              // 特殊：获得低风险评级
              current = userStore.riskLevel === 'low' ? 1 : 0;
              target = 1;
            } else if (achievement.condition.key === 'risk_level_up') {
              // 风险等级提升：简单约定为“最近一次风险评分优于历史平均”
              const history = userStore.riskHistory;
              if (history.length >= 2) {
                const latest = history[history.length - 1].score;
                const avg =
                  history.slice(0, -1).reduce((sum, it) => sum + it.score, 0) /
                  (history.length - 1);
                current = latest < avg ? 1 : 0;
                target = 1;
              } else {
                current = 0;
                target = 1;
              }
            } else {
              // 默认按测评次数统计
              current = userStore.riskHistory.length;
            }
            break;
          case 'special':
            if (achievement.condition.key === 'level') {
              current = this.level;
            } else if (achievement.condition.key === 'all_categories') {
              // 检查是否在其他分类都有至少一个成就
              const hasTraining = this.achievements
                .filter((a) => a.category === 'training')
                .some((a) => this.unlockedAchievements.has(a.id));
              const hasLearning = this.achievements
                .filter((a) => a.category === 'learning')
                .some((a) => this.unlockedAchievements.has(a.id));
              const hasAssessment = this.achievements
                .filter((a) => a.category === 'assessment')
                .some((a) => this.unlockedAchievements.has(a.id));
              current = hasTraining && hasLearning && hasAssessment ? 1 : 0;
              target = 1;
            } else if (achievement.condition.key === 'total_exp') {
              // 累计经验值成就
              current = this.totalExp;
            } else if (achievement.condition.key === 'login_streak') {
              // 登录连击目前前端未精确统计，这里先占位为 0，方便后续扩展
              current = 0;
            }
            break;
        }

        achievement.progress = Math.min(100, (current / target) * 100);

        if (current >= target && !this.unlockedAchievements.has(achievement.id)) {
          this.unlockAchievement(achievement.id);
        } else if (current > 0) {
          achievement.status = 'progress';
        } else {
          achievement.status = 'locked';
        }
      });
    },

    // 解锁成就
    unlockAchievement(id: string) {
      if (this.unlockedAchievements.has(id)) return;

      const achievement = this.achievements.find((a) => a.id === id);
      if (!achievement) return;

      this.unlockedAchievements.add(id);
      achievement.status = 'unlocked';
      achievement.progress = 100;
      achievement.unlockedAt = new Date().toISOString();

      // 奖励经验值
      this.addExp(achievement.expReward);

      this.persist();
      return achievement;
    },

    // 添加经验值
    addExp(amount: number) {
      const oldLevel = this.level;
      this.exp += amount;
      this.totalExp += amount;

      // 检查升级
      while (this.exp >= this.expForNextLevel) {
        this.exp -= this.expForNextLevel;
        this.level += 1;
      }

      this.persist();

      // 返回是否升级
      return {
        leveledUp: this.level > oldLevel,
        oldLevel,
        newLevel: this.level,
        expGained: amount,
      };
    },

    // 手动刷新成就（用于页面加载时）
    refresh() {
      this.checkAchievements();
      this.persist();
    },
  },
});
