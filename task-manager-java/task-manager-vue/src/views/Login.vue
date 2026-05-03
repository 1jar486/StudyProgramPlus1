<template>
  <div class="app-container">
    <div class="login-card">
      <h2 class="card-title">任务管理系统</h2>

      <div class="input-group">
        <input type="text" v-model="form.username" placeholder="请输入用户名" class="form-input" autofocus />
      </div>

      <div class="input-group">
        <input type="password" v-model="form.password" placeholder="请输入密码" class="form-input" />
      </div>

      <button id="addBtn" class="btn btn-primary" @click="handleLogin">登 录</button>
      <button class="btn btn-secondary" @click="handleRegister">注 册</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();
const form = ref({ username: '', password: '' });

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    alert("请填写完整！");
    return;
  }
  try {
    // 1. 发起登录请求，res 接收到的是后端返回的 JSON 对象 {"token": "SUCCESS..."}
    const res = await request.post('/auth/login', {
      username: form.value.username,
      password: form.value.password
    });

    // 2. 正规做法：先检查 res 是否存在，再从 res.token 中取值
    // 因为后端现在返回的是 Map，数据在 .token 字段里
    if (res && res.token && res.token.includes("SUCCESS_TOKEN_FOR_")) {
      // 3. 把真正的字符串存入本地缓存
      localStorage.setItem('token', res.token);

      // 4. 跳转到任务看板
      alert("登录成功！");
      router.push('/tasks');
    } else {
      // 如果后端返回了错误信息，通常在 res 中或者直接弹出
      alert(res || "用户名或密码错误");
    }
  } catch (error) {
    console.error("登录报错详情:", error);
    alert("登录失败：请检查用户名密码或后端服务");
  }
};

const handleRegister = async () => {
  if (!form.value.username || !form.value.password) {
    alert("请填写完整！");
    return;
  }
  try {
    // 配合后端 @RequestBody 的写法，直接发送 JSON 对象
    const res = await request.post('/auth/register', {
      username: form.value.username,
      password: form.value.password
    });

    if (res === "注册成功") {
      alert("注册成功，请登录！");
    } else {
      alert(res || "注册失败");
    }
  } catch (error) {
    console.error(error);
    alert("注册请求失败，请检查后端设置");
  }
};
</script>


<style scoped>
.app-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  padding: 20px;
  box-sizing: border-box;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 24px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.login-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 44px rgba(0, 0, 0, 0.09);
}

.card-title {
  text-align: center;
  font-size: 26px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  letter-spacing: 1px;
}

.input-group {
  position: relative;
}

.form-input {
  width: 100%;
  padding: 16px 18px;
  font-size: 15px;
  color: #334155;
  background: #f8fafc;
  border: 1px solid transparent;
  border-radius: 12px;
  outline: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
}

.form-input:focus {
  background: #ffffff;
  border-color: #6366f1;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.15);
}

.form-input::placeholder {
  color: #94a3b8;
}

.btn {
  width: 100%;
  padding: 15px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: none;
  outline: none;
  letter-spacing: 2px;
}

.btn-primary {
  background: #6366f1;
  color: #ffffff;
}

.btn-primary:hover {
  background: #4f46e5;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.35);
}

.btn-secondary {
  background: transparent;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.btn-secondary:hover {
  background: #f8fafc;
  color: #334155;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

.btn:active {
  transform: translateY(0);
}
</style>

