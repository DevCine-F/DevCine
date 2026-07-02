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
        meta: { permission: { feature: 'dashboard_stats', action: 'view' } },
        component: () => import('../views/admin/Dashboard.vue')
      },
      {
        path: 'movies',
        name: 'admin-movies',
        meta: { permission: { feature: 'movies', action: 'view' } },
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
        meta: { permission: { feature: 'cinemas', action: 'view' } },
        component: () => import('../views/admin/CinemaManager.vue')
      },
      {
        path: 'staff',
        name: 'admin-staff',
        meta: { permission: { feature: 'staff_management', action: 'view' } },
        component: () => import('../views/admin/StaffManager.vue')
      },
      {
        path: 'settings',
        name: 'admin-settings',
        meta: { permission: { feature: 'settings', action: 'view' } },
        component: () => import('../views/admin/AdminSettings.vue')
      },
      {
        path: 'seat-map',
        name: 'admin-seat-map',
        meta: { permission: { feature: 'cinemas', action: 'view' } },
        component: () => import('../views/admin/SeatMapEditor.vue')
      },
      {
        path: 'promotions',
        name: 'admin-promotions',
        meta: { permission: { feature: 'promotions', action: 'view' } },
        component: () => import('../views/admin/AdminPromotions.vue')
      },
      {
        path: 'logs',
        name: 'admin-logs',
        meta: { adminOnly: true },
        component: () => import('../views/admin/AdminLogs.vue')
      },
      {
        path: 'pricing',
        name: 'admin-pricing',
        meta: { permission: { feature: 'pricing', action: 'view' } },
        component: () => import('../views/admin/AdminPricing.vue')
      },
      {
        path: 'master-scheduling',
        name: 'admin-master-scheduling',
        meta: { permission: { feature: 'schedules', action: 'view' } },
        component: () => import('../views/admin/AdminMasterScheduling.vue')
      },
      {
        path: 'categories',
        name: 'admin-movie-categories',
        meta: { permission: { feature: 'movies', action: 'view' } },
        component: () => import('../views/admin/MovieCategoryManager.vue')
      },
      {
        path: 'inventory',
        name: 'admin-inventory',
        meta: { permission: { feature: 'pos_inventory', action: 'view' } },
        component: () => import('../views/admin/InventoryManagement.vue')
      },
      {
        path: 'fnb',
        name: 'admin-fnb',
        meta: { permission: { feature: 'pos_inventory', action: 'view' } },
        component: () => import('../views/admin/FnbMenuManager.vue')
      },
      {
        path: 'staff-shifts',
        name: 'admin-staff-shifts',
        meta: { permission: { feature: 'staff_management', action: 'view' } },
        component: () => import('../views/admin/StaffShiftManagement.vue')
      },
      {
        path: 'account',
        name: 'admin-account',
        component: () => import('../views/admin/AdminProfile.vue')
      },
      {
        path: 'bookings',
        name: 'admin-bookings',
        meta: { permission: { feature: 'pos_ticketing', action: 'view' } },
        component: () => import('../views/admin/AdminBookings.vue')
      },
      {
        path: 'ticketing',
        name: 'admin-ticketing',
        meta: { permission: { feature: 'pos_ticketing', action: 'view' } },
        component: () => import('../views/admin/TicketingPOS.vue')
      },
      {
        path: 'check-in',
        name: 'admin-check-in',
        meta: { permission: { feature: 'pos_ticketing', action: 'view' } },
        component: () => import('../views/admin/TicketCheckIn.vue')
      },
      {
        path: 'customer-support',
        name: 'admin-customer-support',
        meta: { permission: { feature: 'support', action: 'view' } },
        component: () => import('../views/admin/CustomerSupport.vue')
      },
      {
        path: 'faqs',
        name: 'admin-faqs',
        meta: { permission: { feature: 'support', action: 'view' } },
        component: () => import('../views/admin/FaqManager.vue')
      },
      {
        path: 'banners',
        name: 'admin-banners',
        meta: { permission: { feature: 'banners', action: 'view' } },
        component: () => import('../views/admin/AdminBanners.vue')
      },
      {
        path: 'permissions',
        name: 'admin-permissions',
        meta: { adminOnly: true },
        component: () => import('../views/admin/AdminPermissions.vue')
      },
      {
        path: 'customers',
        name: 'admin-customers',
        meta: { permission: { feature: 'support', action: 'view' } },
        component: () => import('../views/admin/AdminCustomers.vue')
      },
      {
        path: 'reviews',
        name: 'admin-reviews',
        meta: { permission: { feature: 'support', action: 'view' } },
        component: () => import('../views/admin/AdminReviews.vue')
      }
    ]
  }
];

