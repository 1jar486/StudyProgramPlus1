import axios from 'axios';

// 创建 axios 实例
const service = axios.create({
    baseURL: 'http://localhost:9090/start/', // 后端服务基地址
    timeout: 60000
});

let isRefreshing = false; // 刷新状态锁
let requestsQueue = [];   // 挂起队列

/**
 * 🔒 智能辅助函数 1：判断本地的 JWT 访问令牌是否快过期了
 */
function isTokenExpired(token) {
    if (!token) return true;
    const cleanToken = token.replace('SUCCESS_TOKEN_', '');
    const parts = cleanToken.split('.');
    if (parts.length !== 3) return false; // 如果不是JWT格式（比如老Token），当作没过期安全放行
    try {
        // 解开 JWT 的 Payload 部分
        const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
        // 当前时间如果大于过期时间减去 5 秒（预留网络延迟缓冲），就认为它过期了
        return Date.now() >= (payload.exp * 1000 - 5000);
    } catch (e) {
        return false;
    }
}

/**
 * 🔓 智能辅助函数 2：从 JWT 中逆向解析出用户 ID（用于老版本数据平滑过渡）
 */
function getUserIdFromJwt(token) {
    if (!token) return null;
    const cleanToken = token.replace('SUCCESS_TOKEN_', '');
    const parts = cleanToken.split('.');
    if (parts.length !== 3) return null;
    try {
        const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
        return payload.userId || null;
    } catch (e) {
        return null;
    }
}

// 1. 请求拦截器：在请求离开浏览器前，完成【无感刷新】和【Header格式降维兼容】
service.interceptors.request.use(
    async config => {
        // 如果是登录或刷新接口本身，直接放行
        if (config.url.includes('/auth/login') || config.url.includes('/auth/refresh')) {
            return config;
        }

        const accessToken = localStorage.getItem('accessToken');
        const refreshToken = localStorage.getItem('refreshToken');

        // 🔥 核心机制：如果发现在发送请求前 AccessToken 已经过期，且有 RefreshToken，直接启动拦截换新证
        if (accessToken && isTokenExpired(accessToken) && refreshToken) {
            if (!isRefreshing) {
                isRefreshing = true;
                try {
                    // 异步调用后端刷新接口换取新令牌
                    const refreshRes = await axios.post(`${service.defaults.baseURL}auth/refresh`, {
                        refreshToken: refreshToken
                    });

                    if (refreshRes.data && refreshRes.data.accessToken) {
                        const newAccessToken = refreshRes.data.accessToken;
                        localStorage.setItem('accessToken', newAccessToken);
                        localStorage.setItem('token', newAccessToken);
                        if (refreshRes.data.refreshToken) {
                            localStorage.setItem('refreshToken', refreshRes.data.refreshToken);
                        }
                        if (refreshRes.data.userId) {
                            localStorage.setItem('userId', refreshRes.data.userId);
                        }

                        // 换证成功，释放并重新执行被挂起的队列
                        requestsQueue.forEach(cb => cb(newAccessToken));
                        requestsQueue = [];
                    }
                } catch (err) {
                    // 连刷新也失败了，说明彻底过期，清空重定向
                    requestsQueue.forEach(cb => cb(null));
                    requestsQueue = [];
                    localStorage.clear();
                    window.location.href = '/login';
                    return Promise.reject(err);
                } finally {
                    isRefreshing = false;
                }
            } else {
                // 如果当前别的请求正在刷新Token，把当前请求挂起放入队列
                return new Promise((resolve) => {
                    requestsQueue.push((newToken) => {
                        const userId = localStorage.getItem('userId') || getUserIdFromJwt(newToken);
                        config.headers['Authorization'] = `SUCCESS_TOKEN_FOR_${userId}`;
                        resolve(config);
                    });
                });
            }
        }

        // 🌟 降维打击式完美兼容：从本地拿出用户ID，组装成后端控制器绝对不会报错的经典格式
        let userId = localStorage.getItem('userId');
        if (!userId && accessToken) {
            // 保底防御：如果没有存过userId（比如上一个版本登录的），现场从JWT中现拆现用
            userId = getUserIdFromJwt(accessToken);
            if (userId) localStorage.setItem('userId', userId);
        }

        if (userId) {
            // 无论前端怎么折腾，发给业务接口的永远是老控制器认识的 SUCCESS_TOKEN_FOR_2
            config.headers['Authorization'] = `SUCCESS_TOKEN_FOR_${userId}`;
        } else {
            // 最后的纯保底
            const token = localStorage.getItem('token');
            if (token) config.headers['Authorization'] = token;
        }

        return config;
    },
    error => Promise.reject(error)
);

// 2. 响应拦截器
service.interceptors.response.use(
    response => response.data,
    error => {
        // 如果后端依然返回 401，直接清理并踢回登录
        if (error.response && error.response.status === 401) {
            localStorage.clear();
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default service;