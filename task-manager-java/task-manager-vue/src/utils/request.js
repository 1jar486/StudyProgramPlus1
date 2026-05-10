import axios from 'axios';

// 创建 axios 实例
const service = axios.create({
    baseURL: 'http://localhost:9090/start/', // 你的后端基础地址
    timeout: 60000
});

// 请求拦截器：发请求前，如果本地有 Token，就带上
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers['Authorization'] = token.startsWith('SUCCESS_TOKEN')? token:`SUCCESS_TOKEN_FOR_${token}`; // 后端通过这个 Header 识别用户
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器：如果有通用错误可以在这里统一处理
service.interceptors.response.use(
    response => response.data,
    error => {
        if (error.response && error.response.status === 401) {
            // 如果后端说没登录，就清空并跳回登录页
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default service;