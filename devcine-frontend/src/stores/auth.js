import { defineStore } from 'pinia'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || null,
    role: localStorage.getItem('role') || null,
    permissions: JSON.parse(localStorage.getItem('permissions')) || {},
    permissionsLoaded: false,
    // Buộc đổi mật khẩu ở lần đăng nhập đầu (tài khoản dùng mật khẩu mặc định)
    mustChangePassword: localStorage.getItem('mustChangePassword') === 'true',
  }),
  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdmin: (state) => state.role === 'admin',
    isManager: (state) => state.role === 'manager',
    isStaff: (state) => state.role === 'staff',
    // Cơ sở của tài khoản đang đăng nhập. ADMIN không thuộc cơ sở nào -> null.
    cinemaName: (state) => state.user?.cinemaName || null,
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
      this.mustChangePassword = !!userData.mustChangePassword;
      localStorage.setItem('user', JSON.stringify(userData));
      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('mustChangePassword', String(this.mustChangePassword));
      localStorage.removeItem('permissions');
    },
    clearMustChangePassword() {
      this.mustChangePassword = false;
      localStorage.setItem('mustChangePassword', 'false');
    },
    async fetchPermissions(force = false) {
      if (!this.isAuthenticated || !['admin', 'manager', 'staff'].includes(this.role)) {
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
      this.mustChangePassword = false;
      localStorage.removeItem('user');
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('permissions');
      localStorage.removeItem('mustChangePassword');
    },
    updateUser(partialUser) {
      if (!this.user) return;
      this.user = { ...this.user, ...partialUser };
      localStorage.setItem('user', JSON.stringify(this.user));
    },
    async fetchProfile() {
      if (!this.isAuthenticated || !this.user?.id) return;
      try {
        const { data } = await api.get(`/auth/profile/${this.user.id}`);
        const profile = data?.data || data;
        if (profile) {
          this.updateUser({
            fullName: profile.fullName || this.user.fullName,
            email: profile.email || this.user.email,
            phone: profile.phone || this.user.phone,
            avatarUrl: profile.avatarUrl || '',
          });
        }
      } catch (err) {
        // im lặng nếu không nạp được profile ngầm
      }
    }
  }
})
