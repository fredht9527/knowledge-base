<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>页面未找到</p>
      <button class="back-btn" @click="router.push({ name: 'Home' })">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #faf8f5;
}
.content {
  text-align: center;
}
h1 {
  font-size: 72px;
  font-weight: 800;
  color: #c2410c;
  margin: 0;
  line-height: 1;
}
p {
  font-size: 16px;
  color: #8c847c;
  margin: 12px 0 24px;
}
.back-btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #d97706, #b45309);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity .2s;
}
.back-btn:hover {
  opacity: 0.9;
}
</style>
