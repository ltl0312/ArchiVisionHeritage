<template>
  <div class="settings-page animate-fade-in">
    <header class="page-title-bar">
      <h2 class="page-title">个人设置</h2>
      <p class="page-subtitle">管理您的视觉偏好与平台信息</p>
    </header>

    <!-- 卡片一：视觉偏好 -->
    <section class="settings-card">
      <div class="card-section-header">
        <el-icon class="section-icon"><BrushFilled /></el-icon>
        <span>视觉偏好</span>
      </div>

      <div class="setting-group">
        <!-- 深色/浅色模式 -->
        <div class="pref-item">
          <div class="pref-info">
            <div class="pref-title">
              <el-icon class="pref-title-icon" :class="{ 'is-dark': themeStore.isDark }">
                <Sunny v-if="!themeStore.isDark" />
                <Moon v-else />
              </el-icon>
              <span>深色 / 浅色模式</span>
            </div>
            <p class="pref-desc">
              {{ themeStore.isDark ? '当前为深色模式，适合夜间阅读古建文献' : '当前为浅色模式，如同宣纸画卷般清雅' }}
            </p>
          </div>
          <el-switch
            :model-value="themeStore.isDark"
            class="theme-switch"
            :active-icon="Moon"
            :inactive-icon="Sunny"
            inline-prompt
            @change="themeStore.toggleTheme"
          />
        </div>

        <hr class="settings-divider" />

        <!-- 通知设置 -->
        <div class="group-label">
          <el-icon class="label-icon"><Bell /></el-icon>
          <span>通知偏好</span>
        </div>

        <div class="pref-item">
          <div class="pref-info">
            <span class="pref-item-title">幻筑完成通知</span>
            <p class="pref-desc">当您的一键幻筑任务生成完成时，发送站内信通知</p>
          </div>
          <el-switch v-model="notifSettings.huanzhuComplete" />
        </div>

        <hr class="settings-divider" />

        <div class="pref-item">
          <div class="pref-info">
            <span class="pref-item-title">评论与互动通知</span>
            <p class="pref-desc">有人评论或点赞您的帖子时，发送站内信通知</p>
          </div>
          <el-switch v-model="notifSettings.interaction" />
        </div>

        <hr class="settings-divider" />

        <div class="pref-item">
          <div class="pref-info">
            <span class="pref-item-title">审核结果通知</span>
            <p class="pref-desc">您的帖子审核通过或被驳回时，发送站内信通知</p>
          </div>
          <el-switch v-model="notifSettings.auditResult" />
        </div>

        <p class="pref-hint">通知设置将在后续版本中接入后端，当前为界面预览。</p>
      </div>
    </section>

    <!-- 卡片二：关于平台 -->
    <section class="settings-card">
      <div class="card-section-header">
        <el-icon class="section-icon"><InfoFilled /></el-icon>
        <span>关于平台</span>
      </div>

      <div class="about-list">
        <div class="about-item">
          <el-icon class="about-icon"><Box /></el-icon>
          <span class="about-label">平台名称</span>
          <span class="about-value">智观·古建 — 数字孪生与文化传承</span>
        </div>
        <hr class="settings-divider" />
        <div class="about-item">
          <el-icon class="about-icon"><Clock /></el-icon>
          <span class="about-label">版本号</span>
          <span class="about-value">V1.3.0</span>
        </div>
        <hr class="settings-divider" />
        <div class="about-item">
          <el-icon class="about-icon"><Connection /></el-icon>
          <span class="about-label">技术架构</span>
          <span class="about-value">Vue 3 + Spring Boot 3.2 + VGGT + AI 3D</span>
        </div>
        <hr class="settings-divider" />
        <div class="about-item">
          <el-icon class="about-icon"><Stamp /></el-icon>
          <span class="about-label">设计语言</span>
          <span class="about-value">中国传统色彩体系 · 宣纸白 · 朱砂红 · 琉璃黄</span>
        </div>
      </div>
    </section>

    <!-- 提示卡片 -->
    <section class="settings-card tip-card">
      <el-icon class="tip-icon"><User /></el-icon>
      <div class="tip-content">
        <p class="tip-title">个人信息管理</p>
        <p class="tip-desc">头像、昵称、签名、密码等个人信息请前往「个人中心」页面进行管理。</p>
        <el-button type="primary" size="small" @click="$router.push('/profile')">
          前往个人中心
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  BrushFilled, InfoFilled, Bell,
  Sunny, Moon, Box, Clock, Connection, Stamp, User
} from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

const notifSettings = ref({
  huanzhuComplete: true,
  interaction: true,
  auditResult: true
})
</script>

<style scoped>
.settings-page {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);
}

/* ===== 页面标题 ===== */
.page-title-bar {
  text-align: center;
  padding-bottom: var(--spacing-lg);
}

.page-title {
  font-family: var(--font-family-serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--color-text-main);
  letter-spacing: 3px;
  margin-bottom: var(--spacing-xs);
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
  letter-spacing: 1px;
}

/* ===== 卡片 ===== */
.settings-card {
  background: var(--color-surface);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
}

[data-theme="dark"] .settings-card {
  border: 1px solid var(--color-border);
  box-shadow: none;
}

/* 卡片分区标题 */
.card-section-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-family: var(--font-family-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-main);
  padding-bottom: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 1px;
}

.section-icon {
  font-size: 20px;
  color: var(--color-primary);
}

/* ===== 设置分组 ===== */
.setting-group {
  padding: var(--spacing-sm) 0;
}

.group-label {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-md);
  margin-top: var(--spacing-xs);
}

.label-icon {
  font-size: 16px;
  color: var(--color-primary);
}

/* ===== 偏好设置项 ===== */
.pref-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) 0;
}

.pref-info {
  flex: 1;
  margin-right: var(--spacing-lg);
}

.pref-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-family: var(--font-family-serif);
  font-size: 17px;
  font-weight: 500;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-xs);
}

.pref-title-icon {
  font-size: 18px;
  color: var(--color-accent);
  transition: color var(--transition-theme);
}

.pref-title-icon.is-dark {
  color: var(--color-accent-light);
}

.pref-item-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-main);
}

.pref-desc {
  font-size: 13px;
  color: var(--color-text-muted);
  line-height: 1.5;
}

.pref-hint {
  font-size: 12px;
  color: var(--color-text-muted);
  font-style: italic;
  margin-top: var(--spacing-md);
  padding-top: var(--spacing-sm);
  border-top: 1px dashed var(--color-border);
}

/* ===== 关于平台 ===== */
.about-list {
  padding: var(--spacing-xs) 0;
}

.about-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) 0;
}

.about-icon {
  font-size: 18px;
  color: var(--color-primary);
  flex-shrink: 0;
}

.about-label {
  font-size: 14px;
  color: var(--color-text-sub);
  flex-shrink: 0;
  min-width: 72px;
}

.about-value {
  font-size: 14px;
  color: var(--color-text-main);
  font-weight: 500;
}

/* ===== 提示卡片 ===== */
.tip-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
}

.tip-icon {
  font-size: 32px;
  color: var(--color-accent);
  flex-shrink: 0;
}

.tip-content {
  flex: 1;
}

.tip-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.tip-desc {
  font-size: 13px;
  color: var(--color-text-sub);
  margin-bottom: var(--spacing-sm);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .settings-page {
    padding: var(--spacing-md);
  }

  .page-title {
    font-size: 22px;
  }

  .settings-card {
    padding: var(--spacing-md);
  }

  .about-item {
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }

  .tip-card {
    flex-direction: column;
    text-align: center;
  }
}
</style>
