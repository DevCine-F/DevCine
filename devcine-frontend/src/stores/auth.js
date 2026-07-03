import { defineStore } from 'pinia'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    role: localStorage.getItem('role') || null,
    permissions: JSON.parse(localStorage.getItem('permissions')) || {},
    permissionsLoaded: false,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.role === 'admin',
    isStaff: (state) => state.role === 'staff',
    hasPermission: (state) => (feature, action = 'view') => {
      if (state.role === 'admin') return true
      const actions = state.permissions?.[feature]
      return Array.isArray(actions) && actions.includes(action)
    },
  },
  actions: {
    login(userData, token, role) {
      this.user = userData;
      this.token = token;
      this.role = role;
      this.permissions = {};
      this.permissionsLoaded = role === 'admin';
      localStorage.setItem('user', JSON.stringify(userData));
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.removeItem('permissions');
    },
    async fetchPermissions(force = false) {
      if (!this.isAuthenticated || !['admin', 'staff'].includes(this.role)) {
        this.permissions = {};
        this.permissionsLoaded = true;
        localStorage.removeItem('permissions');
        return this.permissions;
      }

      if (this.isAdmin) {
        this.permissions = {};
        this.permissionsLoaded = true;
        localStorage.removeItem('permissions');
        return this.permissions;
      }

      if (!force && this.permissionsLoaded) return this.permissions;

      const { data } = await api.get('/admin/roles/me/permissions');
      this.permissions = data?.permissions || {};
      this.permissionsLoaded = true;
      localStorage.setItem('permissions', JSON.stringify(this.permissions));
      return this.permissions;
    },
    logout() {
      this.user = null;
      this.token = null;
      this.role = null;
      this.permissions = {};
      this.permissionsLoaded = false;
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('permissions');
    }
  }
})
