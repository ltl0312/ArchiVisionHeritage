<template>
  <div class="settings-page">
    <h2 class="page-title">个人设置</h2>

    <!-- 基础资料编辑 -->
    <el-card class="settings-card" shadow="never">
      <template #header>
        <span class="card-header-text">基础资料</span>
      </template>
      <el-form :model="profileForm" label-position="top" class="settings-form">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="显示名称" maxlength="64" />
        </el-form-item>
        <el-form-item label="文化签名">
          <el-input
            v-model="profileForm.bio"
            type="textarea"
            :rows="3"
            placeholder="一句话介绍你的古建情怀..."
            maxlength="255"
          />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="profileForm.avatarUrl" placeholder="输入头像图片URL" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="saveProfile">
          保存修改
        </el-button>
      </el-form>
    </el-card>

    <!-- 密码修改 -->
    <el-card class="settings-card" shadow="never">
      <template #header>
        <span class="card-header-text">修改密码</span>
      </template>
      <el-form :model="passwordForm" label-position="top" class="settings-form">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" />
        </el-form-item>
        <el-button type="primary" :loading="changingPwd" @click="changePassword">
          修改密码
        </el-button>
      </el-form>
    </el-card>

    <!-- 系统通知开关（静态骨架） -->
    <el-card class="settings-card" shadow="never">
      <template #header>
        <span class="card-header-text">通知设置</span>
      </template>
      <div class="notif-options">
        <div class="notif-item">
          <div class="notif-label">
            <span class="notif-title">幻筑完成通知</span>
            <span class="notif-desc">当您的一键幻筑任务生成完成时，发送站内信通知</span>
          </div>
          <el-switch v-model="notifSettings.huanzhuComplete" />
        </div>
        <el-divider />
        <div class="notif-item">
          <div class="notif-label">
            <span class="notif-title">评论与互动通知</span>
            <span class="notif-desc">有人评论或点赞您的帖子时，发送站内信通知</span>
          </div>
          <el-switch v-model="notifSettings.interaction" />
        </div>
        <el-divider />
        <div class="notif-item">
          <div class="notif-label">
            <span class="notif-title">审核结果通知</span>
            <span class="notif-desc">您的帖子审核通过或被驳回时，发送站内信通知</span>
          </div>
          <el-switch v-model="notifSettings.auditResult" />
        </div>
      </div>
      <p class="notif-hint">通知设置将在后续版本中接入后端，当前为界面预览。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const saving = ref(false)
const changingPwd = ref(false)

const profileForm = ref({ nickname: '', bio: '', avatarUrl: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const notifSettings = ref({
  huanzhuComplete: true,
  interaction: true,
  auditResult: true
})

onMounted(async () => {
  try {
    const res = await authApi.getCurrentUser()
    if (res.data) {
      profileForm.value.nickname = res.data.nickname || ''
      profileForm.value.bio = res.data.bio || ''
      profileForm.value.avatarUrl = res.data.avatarUrl || ''
    }
  } catch { /* ignore */ }
})

async function saveProfile() {
  saving.value = true
  try {
    await authApi.updateProfile(profileForm.value)
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword) {
    return ElMessage.warning('请填写完整密码信息')
  }
  if (newPassword.length < 6) {
    return ElMessage.warning('新密码至少6位')
  }
  if (newPassword !== confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  changingPwd.value = true
  try {
    await authApi.changePassword({ oldPassword, newPassword })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 24px;
  letter-spacing: 2px;
}

.settings-card {
  margin-bottom: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(0,0,0,0.04);
}

.card-header-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.settings-form {
  max-width: 480px;
}

.notif-options { padding: 4px 0; }

.notif-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
}

.notif-label {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notif-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.notif-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}

.notif-hint {
  font-size: 13px;
  color: var(--color-text-muted);
  font-style: italic;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #e0e0e0;
}
</style>
