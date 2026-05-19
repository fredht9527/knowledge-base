import { createRouter, createWebHistory } from 'vue-router'
import HomeChat from '../views/HomeChat.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: HomeChat
  },
  {
    path: '/knowledge',
    name: 'KnowledgeList',
    component: () => import('../views/KnowledgeList.vue')
  },
  {
    path: '/knowledge/:id',
    name: 'KnowledgeDetail',
    component: () => import('../views/KnowledgeDetail.vue'),
    props: true
  },
  {
    path: '/edit/:id',
    name: 'KnowledgeEdit',
    component: () => import('../views/KnowledgeEdit.vue'),
    props: true
  },
  {
    path: '/categories',
    name: 'CategoryManage',
    component: () => import('../views/CategoryManage.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面检查 token
router.beforeEach((to, from) => {
  const token = localStorage.getItem('kb_token')
  const isAuthPage = ['Login', 'Register'].includes(to.name)

  // 未登录 → 跳转登录
  if (!token && !isAuthPage) {
    return { name: 'Login' }
  }
  // 已登录访问登录/注册页 → 跳转首页
  if (token && isAuthPage) {
    return { name: 'Home' }
  }
})

export default router
