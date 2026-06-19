export default [
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('../views/admin/AdminLoginView.vue')
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('../views/admin/Dashboard.vue')
      },
      {
        path: 'movies',
        name: 'admin-movies',
        component: () => import('../views/admin/AdminMovies.vue')
      },
      {
        // Màn hình lịch chiếu cũ (mock) đã hợp nhất vào Điều phối lịch chiếu
        path: 'schedule',
        redirect: { name: 'admin-master-scheduling' }
      },
      {
        path: 'cinemas',
        name: 'admin-cinemas',
        component: () => import('../views/admin/CinemaManager.vue')
      },
      {
        path: 'staff',
        name: 'admin-staff',
        component: () => import('../views/admin/StaffManager.vue')
      },
      {
        path: 'settings',
        name: 'admin-settings',
        component: () => import('../views/admin/AdminSettings.vue')
      },
      {
        path: 'seat-map',
        name: 'admin-seat-map',
        component: () => import('../views/admin/SeatMapEditor.vue')
      },
      {
        path: 'promotions',
        name: 'admin-promotions',
        component: () => import('../views/admin/AdminPromotions.vue')
      },
      {
        path: 'logs',
        name: 'admin-logs',
        component: () => import('../views/admin/AdminLogs.vue')
      },
      {
        path: 'pricing',
        name: 'admin-pricing',
        component: () => import('../views/admin/AdminPricing.vue')
      },
      {
        path: 'master-scheduling',
        name: 'admin-master-scheduling',
        component: () => import('../views/admin/AdminMasterScheduling.vue')
      },
      {
        path: 'categories',
        name: 'admin-movie-categories',
        component: () => import('../views/admin/MovieCategoryManager.vue')
      },
      {
        path: 'inventory',
        name: 'admin-inventory',
        component: () => import('../views/admin/InventoryManagement.vue')
      },
      {
        path: 'fnb',
        name: 'admin-fnb',
        component: () => import('../views/admin/FnbMenuManager.vue')
      },
      {
        path: 'staff-shifts',
        name: 'admin-staff-shifts',
        component: () => import('../views/admin/StaffShiftManagement.vue')
      },
      {
        path: 'account',
        name: 'admin-account',
        component: () => import('../views/admin/AdminProfile.vue')
      },
      {
        path: 'ticketing',
        name: 'admin-ticketing',
        component: () => import('../views/admin/TicketingPOS.vue')
      },
      {
        path: 'check-in',
        name: 'admin-check-in',
        component: () => import('../views/admin/TicketCheckIn.vue')
      },
      {
        path: 'customer-support',
        name: 'admin-customer-support',
        component: () => import('../views/admin/CustomerSupport.vue')
      },
      {
        path: 'banners',
        name: 'admin-banners',
        component: () => import('../views/admin/AdminBanners.vue')
      },
      {
        path: 'permissions',
        name: 'admin-permissions',
        component: () => import('../views/admin/AdminPermissions.vue')
      },
      {
        path: 'customers',
        name: 'admin-customers',
        component: () => import('../views/admin/AdminCustomers.vue')
      }
    ]
  }
];

