<template>
  <div class="app-container">
    <div class="toast" :class="[toastVisible ? 'show' : '', toastType]" id="toast">
      {{ toastMessage }}
    </div>

    <div class="main-container">
      <div class="login-card">
        <div class="card-header">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="3" fill="white" stroke="none" />
              <circle cx="12" cy="12" r="9" stroke-dasharray="4 3" />
              <circle cx="12" cy="12" r="6" stroke-dasharray="2 5" opacity="0.6" />
            </svg>
          </div>
          <h1>欢迎回来</h1>
          <p class="subtitle">登录您的 Nebula 账户</p >
        </div>

        <form class="login-form" @submit.prevent="handleLogin" autocomplete="off" novalidate>
          <div class="input-group">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg viewBox="0 0 24 24">
                  <rect x="2" y="4" width="20" height="16" rx="3" />
                  <path d="M2 7l10 7 10-7" />
                </svg>
              </span>
              <input
                  type="text"
                  id="username"
                  placeholder="请输入邮箱地址"
                  required
                  v-model="form.username"
                  ref="usernameInputRef"
              >
              <div class="input-underline"></div>
            </div>
          </div>

          <div class="input-group">
            <div class="input-wrapper">
              <span class="input-icon">
                <svg viewBox="0 0 24 24">
                  <rect x="3" y="10" width="18" height="11" rx="2" />
                  <circle cx="12" cy="16" r="1.5" fill="currentColor" stroke="none" opacity="0.8" />
                  <path d="M12 14v-1.5" stroke-linecap="round" />
                  <path d="M7 10V7a5 5 0 0110 0v3" />
                </svg>
              </span>
              <input
                  :type="passwordVisible ? 'text' : 'password'"
                  id="password"
                  placeholder="请输入密码"
                  required
                  v-model="form.password"
                  ref="passwordInputRef"
                  autocomplete="current-password"
              >
              <button
                  type="button"
                  class="toggle-password"
                  @click="togglePasswordVisibility"
                  ref="toggleBtnRef"
                  aria-label="切换密码可见性"
                  tabindex="-1"
              >
                <svg v-if="passwordVisible" class="eye-open" viewBox="0 0 24 24">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg v-else class="eye-closed" viewBox="0 0 24 24">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94" />
                  <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
              <div class="input-underline"></div>
            </div>
          </div>

          <div class="options-row">
            <label class="custom-checkbox">
              <input type="checkbox" v-model="rememberMe">
              <span class="checkmark">
                <svg viewBox="0 0 24 24">
                  <polyline points="4 12 10 18 20 6" />
                </svg>
              </span>
              <span>记住我</span>
            </label>
            <a href=" " class="forgot-link" tabindex="0">忘记密码？</a >
          </div>

          <button type="submit" class="btn-login" :class="{ 'loading': loading, 'success': btnSuccess }">
            <span class="btn-text">登 录</span>
            <span class="btn-spinner"></span>
          </button>
        </form>

        <div class="divider">或通过以下方式</div>

        <div class="social-row">
          <button type="button" class="social-btn wechat" aria-label="WeChat登录" tabindex="0" @click="handleSocialClick('微信')">
            <svg viewBox="0 0 24 24" fill="white" opacity="0.85">
              <path d="M8.2 13.5c-.6 0-1.1-.5-1.1-1.1s.5-1.1 1.1-1.1c.6 0 1.1.5 1.1 1.1s-.5 1.1-1.1 1.1zm6 0c-.6 0-1.1-.5-1.1-1.1s.5-1.1 1.1-1.1c.6 0 1.1.5 1.1 1.1s-.5 1.1-1.1 1.1zm3.8 2.3c-.4 1-1.1 1.8-2.1 2.3l.5 1.4-1.7-.8c-.6.1-1.2.2-1.9.2-3.1 0-5.6-2-5.6-4.5 0-2.5 2.5-4.5 5.6-4.5 3.1 0 5.6 2 5.6 4.5 0 1.1-.5 2.2-1.4 3.1zM17 6c-3.6 0-6.5 2.4-6.5 5.3 0 .3 0 .7.1 1-3.9-.1-7-2.7-7-5.9 0-3.3 3.2-6 7.1-6s7.1 2.7 7.1 6c0 .3 0 .7-.1 1-.2-.1-.4-.1-.7-.1z"/>
            </svg>
          </button>
          <button type="button" class="social-btn qq" aria-label="QQ登录" tabindex="0" @click="handleSocialClick('QQ')">
            <svg viewBox="0 0 24 24" fill="white" opacity="0.85">
              <path d="M12 2.5c-3 0-5.5 2.5-5.5 5.5v3.8c-1.5.6-2.5 2-2.5 3.7 0 1 .5 2 1.3 2.6l1 .7c-.2.5-.3 1-.3 1.4 0 1.8 1.4 3.3 3.3 3.3h.2c1 .7 2 1 3.2 1s2.2-.3 3.2-1h.2c1.8 0 3.3-1.5 3.3-3.3 0-.5-.1-.9-.3-1.4l1-.7c.8-.6 1.3-1.6 1.3-2.6 0-1.7-1-3.1-2.5-3.7V8c0-3-2.5-5.5-5.5-5.5z"/>
            </svg>
          </button>
          <button type="button" class="social-btn phone" aria-label="手机号登录" tabindex="0" @click="handleSocialClick('手机号')">
            <svg viewBox="0 0 24 24" fill="white" opacity="0.85">
              <path d="M17 1H7c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-2-2-2zm0 18H7V5h10v14zm-6 2c-.6 0-1-.4-1-1s.4-1 1-1 1 .4 1 1-.4 1-1 1z"/>
            </svg>
          </button>
        </div>

        <div class="card-footer">
          还没有账户？ <a href=" " tabindex="0" @click.prevent="handleRegister">立即注册</a >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const form = ref({ username: '', password: '' });

const rememberMe = ref(false);
const passwordVisible = ref(false);
const loading = ref(false);
const btnSuccess = ref(false);
const toastMessage = ref('');
const toastType = ref('');
const toastVisible = ref(false);

const usernameInputRef = ref(null);
const passwordInputRef = ref(null);
const toggleBtnRef = ref(null);


let toastTimer;
const showToast = (message, type = '') => {
  clearTimeout(toastTimer);
  toastMessage.value = message;
  toastType.value = type;
  toastVisible.value = true;
  toastTimer = setTimeout(() => {
    toastVisible.value = false;
  }, 2200);
};

const shakeElement = (el) => {
  if (!el) return;
  el.style.animation = 'none';
  el.offsetHeight;
  el.style.animation = 'shake 0.5s ease';
  el.addEventListener('animationend', () => {
    el.style.animation = '';
  }, { once: true });
};

const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value;
  if (toggleBtnRef.value) {
    toggleBtnRef.value.style.transform = 'scale(0.85)';
    setTimeout(() => {
      if (toggleBtnRef.value) toggleBtnRef.value.style.transform = 'scale(1)';
    }, 150);
  }
};

const handleLogin = async () => {
  // 1. 表单校验与 UI 动画反馈（保持原样）
  if (!form.value.username || !form.value.password) {
    showToast("请填写完整！", 'error');
    if (!form.value.username) shakeElement(usernameInputRef.value);
    else shakeElement(passwordInputRef.value);
    return;
  }

  loading.value = true;
  btnSuccess.value = false;

  try {
    const res = await request.post('/auth/login', {
      username: form.value.username,
      password: form.value.password
    });

    // 为了平滑过渡，兼容后端返回新字段 accessToken 或老字段 token
    const tokenStr = res.accessToken || res.token;

    // 校验令牌是否存在且包含有效标识
    if (tokenStr && (tokenStr.includes("SUCCESS_TOKEN_FOR_") || tokenStr.startsWith("SUCCESS_TOKEN"))) {

      // 1. 存储短效访问令牌 (同时保留 token 键，防止系统中其他地方硬编码读取)
      localStorage.setItem('accessToken', tokenStr);
      localStorage.setItem('token', tokenStr);

      // 2. 如果后端返回了长效刷新令牌，将其存入本地
      if (res.refreshToken) {
        localStorage.setItem('refreshToken', res.refreshToken);
      }

      // 🌟 3. 核心新增：将后端随登录接口一同传回的用户唯一 ID 存入浏览器本地缓存
      // 这行代码是整个“前端静默降维翻译”魔法的关键火种！
      if (res.userId) {
        localStorage.setItem('userId', res.userId);
      }

      // UI 成功反馈与跳转
      btnSuccess.value = true;
      showToast("登录成功！", 'success');
      setTimeout(async () => { await router.push('/tasks'); }, 800);

    } else {
      // 处理后端返回错误信息的情况
      showToast(res || "用户名或密码错误", 'error');
    }
  } catch (error) {
    console.error("登录报错详情:", error);
    showToast("登录失败：请检查用户名密码或后端服务", 'error');
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) {
    showToast("请填写完整！", 'error');
    return;
  }
  try {
    const res = await request.post('/auth/register', {
      username: form.value.username,
      password: form.value.password
    });
    if (res === "注册成功") {
      showToast("注册成功，请登录！", 'success');
    } else {
      showToast(res || "注册失败", 'error');
    }
  } catch (error) {
    console.error(error);
    showToast("注册请求失败，请检查后端设置", 'error');
  }
};

const handleSocialClick = (type) => {
  showToast(`正在跳转至 ${type} 登录...`, '');
};



onMounted(() => {
  // 仅保留登录页面的自动聚焦逻辑
  setTimeout(() => { usernameInputRef.value?.focus({ preventScroll: true }); }, 400);
});

onUnmounted(() => {
  // 仅保留清理 Toast 提示定时器的逻辑，防止内存泄漏
  clearTimeout(toastTimer);
});
</script>

<style scoped>
.app-container {--bg-deep: #06060f;--bg-card: rgba(18, 18, 36, 0.65);--border-card: rgba(255, 255, 255, 0.08);--text-primary: #e8e8f0;--text-secondary: #9898b4;--text-muted: #6b6b85;--accent: #7c6ff7;--accent-glow: rgba(124, 111, 247, 0.4);--accent-light: #a99df9;--input-bg: rgba(255, 255, 255, 0.04);--input-border: rgba(255, 255, 255, 0.1);--input-focus-border: #7c6ff7;--danger: #f56c6c;--radius-lg: 20px;--radius-md: 12px;--radius-sm: 8px;--transition-smooth: 0.3s cubic-bezier(0.4, 0, 0.2, 1);--transition-bounce: 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);font-family: 'Inter', 'SF Pro Display', 'Segoe UI', system-ui, -apple-system, sans-serif;background: transparent;min-height: 100vh;display: flex;align-items: center;justify-content: center;overflow: hidden;position: relative;-webkit-font-smoothing: antialiased;-moz-osx-font-smoothing: grayscale;user-select: none;-webkit-tap-highlight-color: transparent;width: 100%;}
.main-container {position: relative;z-index: 1;width: 100%;max-width: 420px;padding: 20px;animation: cardEntry 0.7s cubic-bezier(0.22, 0.61, 0.36, 1) both;}
@keyframes cardEntry {0% { opacity: 0; transform: translateY(30px) scale(0.96); } 100% { opacity: 1; transform: translateY(0) scale(1); }}
.login-card {background: var(--bg-card);backdrop-filter: blur(40px) saturate(140%);-webkit-backdrop-filter: blur(40px) saturate(140%);border: 1px solid var(--border-card);border-radius: var(--radius-lg);padding: 50px 40px 40px;box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 1px 0 rgba(255, 255, 255, 0.04) inset, 0 0 0 1px rgba(255, 255, 255, 0.02) inset, 0 0 80px -20px var(--accent-glow);position: relative;overflow: hidden;transition: box-shadow 0.5s ease, border-color 0.5s ease;}
.login-card::before {content: '';position: absolute;top: 0;left: 0;right: 0;height: 1px;background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.15) 30%, rgba(255, 255, 255, 0.25) 50%, rgba(255, 255, 255, 0.15) 70%, transparent 100%);opacity: 0.7;pointer-events: none;}
.login-card::after {content: '';position: absolute;top: -120px;right: -80px;width: 280px;height: 280px;background: radial-gradient(circle, rgba(124, 111, 247, 0.08) 0%, transparent 70%);pointer-events: none;border-radius: 50%;transition: transform 0.6s ease;}
.login-card:hover::after {transform: scale(1.2);}
.login-card:hover {box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5), 0 1px 0 rgba(255, 255, 255, 0.05) inset, 0 0 0 1px rgba(255, 255, 255, 0.03) inset, 0 0 100px -15px var(--accent-glow);border-color: rgba(255, 255, 255, 0.13);}
.card-header {text-align: center;margin-bottom: 36px;position: relative;z-index: 1; display: flex; flex-direction: column; align-items: center;}
.logo-icon {display: inline-flex;align-items: center;justify-content: center;width: 52px;height: 52px;border-radius: 16px;background: linear-gradient(135deg, #7c6ff7 0%, #5b4fcf 40%, #3b82f6 100%);margin-bottom: 20px;box-shadow: 0 8px 24px rgba(124, 111, 247, 0.35);position: relative;animation: logoPulse 3s ease-in-out infinite;}
@keyframes logoPulse {0%, 100% { box-shadow: 0 8px 24px rgba(124, 111, 247, 0.35); } 50% { box-shadow: 0 8px 36px rgba(124, 111, 247, 0.55), 0 0 60px rgba(124, 111, 247, 0.2); }}
.logo-icon svg {width: 26px;height: 26px;fill: none;stroke: #fff;stroke-width: 2;stroke-linecap: round;stroke-linejoin: round;}
.card-header h1 {font-size: 1.65rem;font-weight: 700;color: var(--text-primary);letter-spacing: -0.02em;margin: 0 0 6px;line-height: 1.2;}
.card-header .subtitle {font-size: 0.9rem;color: var(--text-secondary);font-weight: 400;letter-spacing: 0.01em;}
.login-form {position: relative;z-index: 1;display: flex;flex-direction: column;gap: 24px;}
.input-group {position: relative;display: flex;flex-direction: column;}
.input-wrapper {position: relative;display: flex;align-items: center;}
.input-icon {position: absolute;left: 16px;z-index: 2;pointer-events: none;display: flex;align-items: center;justify-content: center;transition: opacity var(--transition-smooth);}
.input-icon svg {width: 18px;height: 18px;stroke: var(--text-muted);fill: none;stroke-width: 1.8;stroke-linecap: round;stroke-linejoin: round;transition: stroke var(--transition-smooth);}
.input-group input {width: 100%;padding: 15px 44px 15px 46px;background: var(--input-bg);border: 1.5px solid var(--input-border);border-radius: var(--radius-md);color: var(--text-primary);font-size: 0.95rem;font-family: inherit;letter-spacing: 0.01em;outline: none;transition: all var(--transition-smooth);position: relative;z-index: 1;line-height: 1.4;}
.input-group input::placeholder {color: var(--text-muted);opacity: 0.7;transition: opacity var(--transition-smooth), transform var(--transition-smooth);}
.input-group input:focus::placeholder {opacity: 0.4;transform: translateX(4px);}
.input-group input:focus {border-color: var(--input-focus-border);background: rgba(255, 255, 255, 0.06);box-shadow: 0 0 0 4px rgba(124, 111, 247, 0.1), 0 0 20px -5px rgba(124, 111, 247, 0.2);outline: none;}
.input-wrapper:focus-within .input-icon svg {stroke: var(--accent-light);}
.input-group input:hover {border-color: rgba(255, 255, 255, 0.2);background: rgba(255, 255, 255, 0.05);}
.input-group input:focus:hover {border-color: var(--input-focus-border);background: rgba(255, 255, 255, 0.06);}
.input-underline {position: absolute;bottom: 0;left: 50%;width: 0;height: 2px;background: linear-gradient(90deg, #7c6ff7, #a99df9, #7c6ff7);border-radius: 0 0 2px 2px;transition: all var(--transition-bounce);z-index: 3;pointer-events: none;opacity: 0;}
.input-wrapper:focus-within .input-underline {width: calc(100% - 4px);left: 2px;opacity: 1;}
.toggle-password {position: absolute;right: 12px;z-index: 3;background: none;border: none;cursor: pointer;padding: 8px;display: flex;align-items: center;justify-content: center;border-radius: 50%;transition: all var(--transition-smooth);color: var(--text-muted);outline: none;-webkit-tap-highlight-color: transparent;}
.toggle-password:hover {color: var(--text-secondary);background: rgba(255, 255, 255, 0.06);}
.toggle-password:active {transform: scale(0.9);background: rgba(255, 255, 255, 0.1);}
.toggle-password svg {width: 20px;height: 20px;stroke: currentColor;fill: none;stroke-width: 1.8;stroke-linecap: round;stroke-linejoin: round;transition: all var(--transition-smooth);}
.options-row {display: flex;align-items: center;justify-content: space-between;font-size: 0.85rem;margin-top: -4px;}
.custom-checkbox {display: flex;align-items: center;gap: 9px;cursor: pointer;color: var(--text-secondary);transition: color var(--transition-smooth);position: relative;-webkit-tap-highlight-color: transparent;user-select: none; white-space: nowrap}
.custom-checkbox:hover {color: var(--text-primary);}
.custom-checkbox input[type="checkbox"] {position: absolute;opacity: 0;width: 0;height: 0;pointer-events: none;}
.checkmark {width: 20px;height: 20px;border-radius: 6px;border: 1.8px solid rgba(255, 255, 255, 0.2);background: rgba(255, 255, 255, 0.03);display: flex;align-items: center;justify-content: center;transition: all var(--transition-smooth);flex-shrink: 0;position: relative;}
.checkmark svg {width: 12px;height: 12px;stroke: #fff;fill: none;stroke-width: 2.5;stroke-linecap: round;stroke-linejoin: round;opacity: 0;transform: scale(0.5) rotate(-20deg);transition: all var(--transition-bounce);}
.custom-checkbox input:checked+.checkmark {background: var(--accent);border-color: var(--accent);box-shadow: 0 0 14px rgba(124, 111, 247, 0.4);}
.custom-checkbox input:checked+.checkmark svg {opacity: 1;transform: scale(1) rotate(0deg);}
.custom-checkbox input:focus-visible+.checkmark {outline: 2px solid var(--accent-light);outline-offset: 3px;border-radius: 8px;}
.forgot-link {color: var(--text-secondary);text-decoration: none;font-weight: 500;transition: all var(--transition-smooth);position: relative;padding: 2px 0;}
.forgot-link::after {content: '';position: absolute;bottom: 0;left: 0;width: 100%;height: 1px;background: var(--accent-light);transform: scaleX(0);transform-origin: right;transition: transform var(--transition-smooth);}
.forgot-link:hover {color: var(--accent-light);}
.forgot-link:hover::after {transform: scaleX(1);transform-origin: left;}
.btn-login {width: 100%;padding: 15px 24px;background: linear-gradient(135deg, #7c6ff7 0%, #5b4fcf 50%, #4c3fc4 100%);border: none;border-radius: var(--radius-md);color: #fff;font-size: 1rem;font-weight: 600;font-family: inherit;letter-spacing: 0.02em;cursor: pointer;position: relative;overflow: hidden;transition: all var(--transition-smooth);box-shadow: 0 6px 24px rgba(124, 111, 247, 0.35);margin-top: 6px;-webkit-tap-highlight-color: transparent;outline: none;}
.btn-login::before {content: '';position: absolute;top: 0;left: -100%;width: 100%;height: 100%;background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.15) 50%, transparent 100%);transition: left 0.6s ease;}
.btn-login:hover::before {left: 100%;}
.btn-login:hover {transform: translateY(-2px);box-shadow: 0 10px 32px rgba(124, 111, 247, 0.5);background: linear-gradient(135deg, #8b7df8 0%, #6b5fdf 50%, #5c4fd4 100%);}
.btn-login:active {transform: translateY(0) scale(0.98);box-shadow: 0 4px 16px rgba(124, 111, 247, 0.35);transition: all 0.1s ease;}
.btn-login:focus-visible {outline: 2px solid var(--accent-light);outline-offset: 3px;border-radius: var(--radius-md);}
.btn-login.loading {pointer-events: none;color: transparent;background: linear-gradient(135deg, #6b5fdf 0%, #4c3fc4 100%);}
.btn-login .btn-text {position: relative;z-index: 1;transition: opacity var(--transition-smooth);}
.btn-login.loading .btn-text {opacity: 0;}
.btn-spinner {position: absolute;top: 50%;left: 50%;transform: translate(-50%, -50%);width: 22px;height: 22px;border: 2.5px solid rgba(255, 255, 255, 0.3);border-top-color: #fff;border-radius: 50%;animation: spin 0.7s linear infinite;opacity: 0;transition: opacity var(--transition-smooth);pointer-events: none;}
.btn-login.loading .btn-spinner {opacity: 1;}
@keyframes spin {to { transform: translate(-50%, -50%) rotate(360deg); }}
.btn-login.success {background: linear-gradient(135deg, #4ade80 0%, #22c55e 100%) !important;box-shadow: 0 6px 24px rgba(74, 222, 128, 0.4) !important;}
.divider {display: flex;align-items: center;gap: 14px;color: var(--text-muted);font-size: 0.8rem;letter-spacing: 0.03em;margin: 6px 0 2px;user-select: none;}
.divider::before, .divider::after {content: '';flex: 1;height: 1px;background: rgba(255, 255, 255, 0.08);}
.social-row {display: flex;gap: 14px;justify-content: center;}
.social-btn {flex: 1;max-width: 90px;aspect-ratio: 1 / 1;border-radius: var(--radius-md);border: 1.5px solid rgba(255, 255, 255, 0.1);background: rgba(255, 255, 255, 0.03);cursor: pointer;display: flex;align-items: center;justify-content: center;transition: all var(--transition-smooth);position: relative;overflow: hidden;-webkit-tap-highlight-color: transparent;outline: none;}
.social-btn::before {content: '';position: absolute;inset: 0;border-radius: inherit;opacity: 0;transition: opacity var(--transition-smooth);pointer-events: none;}
.social-btn.wechat::before {background: radial-gradient(circle at center, rgba(7, 193, 96, 0.2) 0%, transparent 70%);}
.social-btn.qq::before {background: radial-gradient(circle at center, rgba(18, 183, 245, 0.25) 0%, transparent 70%);}
.social-btn.phone::before {background: radial-gradient(circle at center, rgba(124, 111, 247, 0.2) 0%, transparent 70%);}
.social-btn:hover::before {opacity: 1;}
.social-btn:hover {border-color: rgba(255, 255, 255, 0.25);transform: translateY(-3px);box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);background: rgba(255, 255, 255, 0.06);}
.social-btn:active {transform: translateY(0) scale(0.95);transition: all 0.1s ease;}
.social-btn:focus-visible {outline: 2px solid var(--accent-light);outline-offset: 3px;border-radius: var(--radius-md);}
.social-btn svg {width: 22px;height: 22px;transition: transform var(--transition-smooth);position: relative;z-index: 1;}
.social-btn:hover svg {transform: scale(1.1);}
.card-footer {text-align: center;margin-top: 28px;font-size: 0.88rem;color: var(--text-secondary);position: relative;z-index: 1; display: flex; justify-content: center; align-items: center; width: 100%;}
.card-footer a {color: var(--accent-light);text-decoration: none;font-weight: 600;transition: all var(--transition-smooth);position: relative;padding: 2px 0;}
.card-footer a::after {content: '';position: absolute;bottom: 0;left: 0;width: 100%;height: 1px;background: var(--accent-light);transform: scaleX(0);transform-origin: right;transition: transform var(--transition-smooth);}
.card-footer a:hover::after {transform: scaleX(1);transform-origin: left;}
.card-footer a:hover {color: #c4bdfb;}
.toast {position: absolute;top: 28px;left: 50%;transform: translateX(-50%) translateY(-120px);background: rgba(30, 30, 50, 0.9);backdrop-filter: blur(20px);-webkit-backdrop-filter: blur(20px);border: 1px solid rgba(255, 255, 255, 0.12);color: var(--text-primary);padding: 14px 24px;border-radius: 50px;font-size: 0.9rem;font-weight: 500;letter-spacing: 0.01em;z-index: 100;pointer-events: none;transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);white-space: nowrap;}
.toast.show {transform: translateX(-50%) translateY(0);}
.toast.success {border-color: rgba(74, 222, 128, 0.4);}
.toast.error {border-color: rgba(248, 113, 113, 0.4);}
@keyframes shake {0%, 100% { transform: translateX(0); } 15% { transform: translateX(-6px); } 30% { transform: translateX(6px); } 45% { transform: translateX(-4px); } 60% { transform: translateX(4px); } 75% { transform: translateX(-2px); } 90% { transform: translateX(2px); }}
@media (max-width: 480px) {.main-container { max-width: 100%; padding: 14px; } .login-card { padding: 32px 22px 28px; border-radius: 18px; } .card-header h1 { font-size: 1.45rem; } .card-header .subtitle { font-size: 0.82rem; } .input-group input { padding: 14px 40px 14px 42px; font-size: 0.9rem; border-radius: 10px; } .input-icon { left: 13px; } .input-icon svg { width: 16px; height: 16px; } .toggle-password { right: 8px; } .btn-login { padding: 14px 20px; font-size: 0.95rem; border-radius: 10px; } .options-row { font-size: 0.8rem; flex-wrap: wrap; gap: 8px; } .logo-icon { width: 44px; height: 44px; border-radius: 14px; margin-bottom: 16px; } .logo-icon svg { width: 22px; height: 22px; }}
@media (max-width: 360px) {.login-card { padding: 24px 16px 22px; }}
</style>