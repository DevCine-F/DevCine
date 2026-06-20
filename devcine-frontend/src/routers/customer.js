export default [
  {
    path: '/',
    component: () => import('../layouts/CustomerLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../views/customer/HomeView.vue')
      },
      {
        path: 'lich-chieu',
        name: 'lich-chieu',
        component: () => import('../views/customer/LichChieuView.vue')
      },
      {
        path: 'he-thong-rap',
        name: 'he-thong-rap',
        component: () => import('../views/customer/HeThongRapView.vue')
      },
      {
        path: 'he-thong-rap/:id',
        name: 'cinema-detail',
        component: () => import('../views/customer/CinemaDetailView.vue')
      },
      {
        path: 'khuyen-mai',
        name: 'khuyen-mai',
        component: () => import('../views/customer/KhuyenMaiView.vue')
      },
      {
        path: 'movie/:id',
        name: 'movie-detail',
        component: () => import('../views/customer/MovieDetail.vue')
      },
      {
        path: 'booking',
        name: 'booking',
        component: () => import('../views/customer/BookingView.vue')
      },
      {
        path: 'success',
        name: 'success',
        component: () => import('../views/customer/BookingSuccessView.vue')
      },
      {
        path: 'profile',
        component: () => import('../views/customer/ProfileView.vue'),
        children: [
          {
            path: '',
            name: 'profile-info',
            component: () => import('../views/customer/ProfileInfoView.vue')
          },
          {
            path: 'history',
            name: 'profile-history',
            component: () => import('../views/customer/BookingHistoryView.vue')
          },
          {
            path: 'password',
            name: 'profile-password',
            component: () => import('../views/customer/ChangePasswordView.vue')
          },
          {
            path: 'vouchers',
            name: 'profile-vouchers',
            component: () => import('../views/customer/VouchersView.vue')
          }
        ]
      },
      {
        path: 'contact',
        name: 'contact',
        component: () => import('../views/customer/ContactView.vue')
      },
      {
        path: 'search',
        name: 'search',
        component: () => import('../views/customer/SearchView.vue')
      },
      {
        path: 'notifications',
        name: 'notifications',
        component: () => import('../views/customer/NotificationView.vue')
      },
      {
        path: 'faq',
        name: 'faq',
        component: () => import('../views/customer/FAQView.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('../layouts/LoginLayout.vue'),
    children: [
      {
        path: '',
        name: 'login',
        component: () => import('../views/customer/LoginView.vue')
      }
    ]
  },
  {
    path: '/style-guide',
    name: 'style-guide',
    component: () => import('../views/customer/StyleGuideView.vue')
  }
];
