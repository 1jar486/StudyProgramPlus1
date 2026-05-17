import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import TaskBoard from '../views/TaskBoard.vue';
import StudyCopilot from "../views/StudyCopilot.vue";
import NotebookList from "../views/NotebookList.vue"
const routes = [
    { path: '/', redirect: '/tasks' },
    { path: '/login', name: 'Login', component: Login },
    { path: '/tasks', name: 'Tasks', component: () => import('../views/TaskBoard.vue'), meta: { requiresAuth: true } },
    { path: '/copilot/:notebookId', name: 'Copilot', component: () => import("../views/StudyCopilot.vue"), meta: { requiresAuth: true } },
    { path: '/notebooks', name: 'Notebooks', component: () => import("../views/NotebookList.vue"), meta: { requiresAuth: true } },
    { path: '/decks', name: 'Decks', component: () => import('@/views/DeckList.vue'), meta: { requiresAuth: true } },
    { path: '/decks/:deckId/cards', name: 'DeckCards', component: () => import('@/views/CardList.vue'), meta: { requiresAuth: true } },
    { path: '/decks/:deckId/study', name: 'StudyMode', component: () => import('@/views/StudyMode.vue'), meta: { requiresAuth: true } },
    { path: '/stats', name: 'Stats', component: () => import('../views/Stats.vue'), meta: { requiresAuth: true } }
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



// ...

export default router;