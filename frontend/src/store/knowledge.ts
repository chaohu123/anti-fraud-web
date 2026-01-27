import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';

function getKnowledgeStorageKey(userId: number | null | undefined) {
  return userId ? `af_knowledge_u_${userId}` : 'af_knowledge_guest';
}

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    storageKey: 'af_knowledge_guest',
    // 当前页面勾选为“已学习”的知识ID集合
    readIds: new Set<number>(),
    // 曾经至少学过一次的知识ID集合（用于防止反复标记刷经验）
    everReadIds: new Set<number>(),
  }),
  getters: {
    isRead: (state) => (id: number) => state.readIds.has(id),
    readCount: (state) => state.readIds.size,
    hasEverRead: (state) => (id: number) => state.everReadIds.has(id),
  },
  actions: {
    hydrate(userId?: number | null) {
      this.storageKey = getKnowledgeStorageKey(userId ?? null);
      const data = loadJson(this.storageKey, { readIds: [] as number[], everReadIds: [] as number[] });
      const readArray: number[] = Array.isArray(data.readIds) ? data.readIds : [];
      const everArray: number[] =
        Array.isArray(data.everReadIds) && data.everReadIds.length > 0 ? data.everReadIds : readArray;
      this.readIds = new Set(readArray);
      this.everReadIds = new Set(everArray);
    },
    persist() {
      saveJson(this.storageKey, {
        readIds: Array.from(this.readIds),
        everReadIds: Array.from(this.everReadIds),
      });
    },
    clear(userId?: number | null) {
      const key = getKnowledgeStorageKey(userId ?? null);
      try {
        localStorage.removeItem(key);
      } catch {
        // ignore
      }
      if (key === this.storageKey) {
        this.readIds = new Set<number>();
        this.everReadIds = new Set<number>();
      }
    },
    markRead(id: number) {
      this.readIds.add(id);
      this.everReadIds.add(id);
      this.persist();
    },
    toggleRead(id: number) {
      if (this.readIds.has(id)) {
        this.readIds.delete(id);
      } else {
        this.readIds.add(id);
        this.everReadIds.add(id);
      }
      this.persist();
    },
    /**
     * 用后端返回的“已完成学习”的ID列表同步前端状态。
     * - readIds：覆盖为后端完成列表
     * - everReadIds：与后端完成列表做并集，保证经验只加一次
     */
    syncFinishedFromBackend(ids: number[]) {
      const normalized = Array.from(new Set(ids || [])).map((x) => Number(x)).filter((x) => !Number.isNaN(x));
      this.readIds = new Set(normalized);
      const union = new Set(this.everReadIds);
      normalized.forEach((id) => union.add(id));
      this.everReadIds = union;
      this.persist();
    },
  },
});

