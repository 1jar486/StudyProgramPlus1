import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import TaskBoard from '../views/TaskBoard.vue';
import StudyCopilot from "../views/StudyCopilot.vue";
const routes = [
    { path: '/', redirect: '/tasks' },
    { path: '/login', name: 'Login', component: Login },
    { path: '/tasks', name: 'Tasks', component: TaskBoard, meta: { requiresAuth: true } }, // 需要登录才能访问
    { path: '/copilot', name: 'Copilot', component: StudyCopilot, meta: { requiresAuth: true } } // 新增路由配置
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

// 路由守卫：每次跳转前检查
router.beforeEach((to, from, next) => {
    const hasToken = localStorage.getItem('token');
    if (to.meta.requiresAuth && !hasToken) {
        // 想去需要权限的页面，但没token，打回登录页
        next('/login');
    } else if (to.path === '/login' && hasToken) {
        // 已经登录了还去登录页，直接进看板
        next('/tasks');
    } else {
        next();
    }
});

export default router;