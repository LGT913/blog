<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const adminLinks = [
  {
    title: '分类管理',
    desc: '创建、编辑、删除文章分类',
    path: '/admin/categories',
    icon: 'folder'
  },
  {
    title: '站点配置',
    desc: '修改站点名称、Logo、版权等信息',
    path: '/admin/config',
    icon: 'settings'
  }
]

const iconMap = {
  folder: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z',
  settings: 'M12 20h9M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z'
}
</script>

<template>
  <main class="admin-page">
    <div class="page-header">
      <h1 class="page-title">管理面板</h1>
      <p class="page-desc">网站管理功能入口</p>
    </div>

    <div class="admin-grid">
      <div
        v-for="link in adminLinks"
        :key="link.path"
        class="admin-card"
        @click="router.push(link.path)"
      >
        <span class="card-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path :d="iconMap[link.icon]"></path>
          </svg>
        </span>
        <h3 class="card-title">{{ link.title }}</h3>
        <p class="card-desc">{{ link.desc }}</p>
      </div>
    </div>
  </main>
</template>

<style scoped>
.admin-page {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: var(--space-8) var(--space-6);
}

.page-header {
  text-align: center;
  margin-bottom: var(--space-8);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  font-family: 'Noto Serif SC', var(--font-serif);
}

.page-desc {
  color: var(--color-text-secondary);
  margin-top: var(--space-2);
}

.admin-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-6);
  max-width: 720px;
  margin: 0 auto;
}

.admin-card {
  padding: var(--space-6);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.admin-card:hover {
  border-color: var(--color-primary, #7ec8a0);
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.card-icon {
  font-size: 32px;
  display: block;
  margin-bottom: var(--space-3);
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
  font-family: 'Noto Serif SC', var(--font-serif);
}

.card-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>