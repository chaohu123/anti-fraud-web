import { defineStore } from 'pinia';
import { loadJson, saveJson } from '../utils/storage';

export const useKnowledgeStore = defineStore('knowledge', {
  state: () => ({
    readIds: new Set<number>(),
  }),
  getters: {
    isRead: (state) => (id: number) => state.readIds.has(id),
    readCount: (state) => state.readIds.size,
  },
  actions: {
    hydrate() {
      const data = loadJson('af_knowledge', { readIds: [] as number[] });
      this.readIds = new Set(data.readIds || []);
    },
    persist() {
      saveJson('af_knowledge', { readIds: Array.from(this.readIds) });
    },
    markRead(id: number) {
      this.readIds.add(id);
      this.persist();
    },
    toggleRead(id: number) {
      if (this.readIds.has(id)) this.readIds.delete(id);
      else this.readIds.add(id);
      this.persist();
    },
  },
});

