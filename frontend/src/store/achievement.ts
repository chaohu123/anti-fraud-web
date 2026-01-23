import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';
import { useUserStore } from '../stores/user';
import { useKnowledgeStore } from './knowledge';
import { useAssessmentStore } from './assessment';

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
      const currentLevelExp = (this.level - 1) * 100;
      const expInCurrentLevel = this.exp - currentLevelExp;
      const expNeeded = this.expForNextLevel - currentLevelExp;
      return Math.min(100, Math.max(0, (expInCurrentLevel / expNeeded) * 100));
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
    hydrate() {
      const data = loadJson('af_achievement', {
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
      this.initAchievements();
      this.checkAchievements();
    },

    persist() {
      saveJson('af_achievement', {
        level: this.level,
        exp: this.exp,
        totalExp: this.totalExp,
        achievements: this.achievements,
        unlockedAchievements: Array.from(this.unlockedAchievements),
      });
    },

    // 初始化成就列表
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
            current = userStore.trainingCount;
            break;
          case 'learning':
            current = knowledgeStore.readCount;
            break;
          case 'assessment':
            if (achievement.condition.key === 'low_risk') {
              current = userStore.riskLevel === 'low' ? 1 : 0;
              target = 1;
            } else {
              // 统计测评次数（从历史记录）
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
