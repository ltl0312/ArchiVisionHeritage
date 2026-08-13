<template>
  <div class="profile-page animate-fade-in">
    <!-- 用户信息卡片 -->
    <div class="profile-header glass-card">
      <div class="profile-cover">
        <div class="cover-gradient"></div>
      </div>
      <div class="profile-info">
        <div class="avatar-wrapper" @click="showAvatarDialog = true">
          <el-avatar :size="88" :src="profile.avatarUrl" :icon="UserFilled" />
          <div class="avatar-overlay">
            <el-icon size="20"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
        </div>
        <div class="profile-text">
          <h2 class="profile-name">{{ profile.nickname || userStore.username || '用户' }}</h2>
          <p class="profile-username">@{{ userStore.username }}</p>
          <p class="profile-bio">{{ profile.bio || '还没有填写文化签名' }}</p>
        </div>
        <div class="profile-actions">
          <el-button type="primary" @click="openEditDialog">
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
          <el-button @click="showPasswordDialog = true">
            <el-icon><Lock /></el-icon>
            修改密码
          </el-button>
        </div>
      </div>
    </div>

    <!-- Tab 区 -->
    <div class="profile-tabs glass-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="我的帖子" name="posts">
          <div v-if="myPosts.length" class="post-grid">
            <div v-for="p in myPosts" :key="p.postId" class="mini-card" @click="$router.push(`/post/${p.postId}`)">
              <div class="card-cover">
                <el-image v-if="p.preview2dPath" :src="p.preview2dPath" fit="cover">
                  <template #error><el-icon size="32"><PictureFilled /></el-icon></template>
                </el-image>
                <div v-else class="card-cover-placeholder"><el-icon size="32"><PictureFilled /></el-icon></div>
              </div>
              <div class="mini-card-body">
                <h4>{{ p.title }}</h4>
                <span class="mini-card-status" :class="p.status">{{ statusLabel(p.status) }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有发布帖子" />
        </el-tab-pane>

        <el-tab-pane label="我的点赞" name="likes">
          <div v-if="likedPosts.length" class="post-grid">
            <div v-for="p in likedPosts" :key="p.postId" class="mini-card" @click="$router.push(`/post/${p.postId}`)">
              <div class="card-cover">
                <el-image v-if="p.preview2dPath" :src="p.preview2dPath" fit="cover">
                  <template #error><el-icon size="32"><PictureFilled /></el-icon></template>
                </el-image>
                <div v-else class="card-cover-placeholder"><el-icon size="32"><PictureFilled /></el-icon></div>
              </div>
              <div class="mini-card-body">
                <h4>{{ p.title }}</h4>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有点赞的帖子" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 头像上传弹窗 -->
    <el-dialog v-model="showAvatarDialog" title="更换头像" width="420px" :close-on-click-modal="false">
      <div class="avatar-dialog-content">
        <div class="avatar-preview-large">
          <el-avatar :size="120" :src="avatarForm.avatarUrl" :icon="UserFilled" />
        </div>
        <ImageUploader
          v-model="avatarForm.avatarUrl"
          placeholder="上传新头像"
          :show-url-input="true"
          sub-dir="avatars"
        />
      </div>
      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingAvatar" @click="saveAvatar">保存头像</el-button>
      </template>
    </el-dialog>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="520px" :close-on-click-modal="false">
      <el-form :model="editForm" label-position="top" class="edit-form">
        <el-form-item label="昵称">
          <el-input
            v-model="editForm.nickname"
            placeholder="设置您的显示名称"
            maxlength="64"
            show-word-limit
            :prefix-icon="UserFilled"
          />
        </el-form-item>
        <el-form-item label="文化签名">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="4"
            placeholder="一句话介绍你的古建情怀..."
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form :model="passwordForm" label-position="top" class="password-form">
        <el-form-item label="当前密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="至少6位"
            show-password
            :prefix-icon="Key"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="再次输入新密码"
            show-password
            :prefix-icon="Key"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="changePassword">修改密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { UserFilled, PictureFilled, Camera, Edit, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import ImageUploader from '@/components/ImageUploader.vue'

const userStore = useUserStore()
const activeTab = ref('posts')
const profile = ref({ nickname: '', bio: '', avatarUrl: '' })
const myPosts = ref([])
const likedPosts = ref([])

// 弹窗状态
const showAvatarDialog = ref(false)
const showEditDialog = ref(false)
const showPasswordDialog = ref(false)

// 表单数据
const avatarForm = ref({ avatarUrl: '' })
const editForm = ref({ nickname: '', bio: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

// Loading 状态
const savingAvatar = ref(false)
const savingProfile = ref(false)
const changingPwd = ref(false)

function statusLabel(s) {
  return { PENDING: '审核中', APPROVED: '已发布', REJECTED: '已驳回' }[s] || s
}

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  try {
    const [profileRes, postsRes, likesRes] = await Promise.all([
      authApi.getCurrentUser(),
      authApi.getMyPosts(),
      authApi.getMyLikes()
    ])
    profile.value = profileRes.data || {}
    myPosts.value = postsRes.data?.records || postsRes.data || []
    likedPosts.value = likesRes.data?.records || likesRes.data || []
  } catch { /* ignore */ }
}

function openEditDialog() {
  editForm.value = {
    nickname: profile.value.nickname || '',
    bio: profile.value.bio || ''
  }
  showEditDialog.value = true
}

async function saveAvatar() {
  savingAvatar.value = true
  try {
    await authApi.updateProfile({ avatarUrl: avatarForm.value.avatarUrl })
    profile.value.avatarUrl = avatarForm.value.avatarUrl
    showAvatarDialog.value = false
    ElMessage.success('头像已更新')
  } catch {
    ElMessage.error('头像更新失败')
  } finally {
    savingAvatar.value = false
  }
}

async function saveProfile() {
  if (!editForm.value.nickname.trim()) {
    return ElMessage.warning('昵称不能为空')
  }
  savingProfile.value = true
  try {
    await authApi.updateProfile(editForm.value)
    profile.value.nickname = editForm.value.nickname
    profile.value.bio = editForm.value.bio
    showEditDialog.value = false
    ElMessage.success('资料已更新')
  } catch {
    ElMessage.error('更新失败')
  } finally {
    savingProfile.value = false
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
    showPasswordDialog.value = false
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.profile-page {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.profile-page::-webkit-scrollbar { width: 6px; }
.profile-page::-webkit-scrollbar-track { background: transparent; }
.profile-page::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }

/* ===== 用户信息卡片 ===== */
.profile-header {
  padding: 0;
  overflow: hidden;
  margin-bottom: var(--spacing-lg);
}

.profile-cover {
  height: 160px;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-rose) 100%);
  position: relative;
}

.cover-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 80px;
  background: linear-gradient(to top, var(--color-surface), transparent);
}

.profile-info {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-lg);
  padding: 0 var(--spacing-xl) var(--spacing-xl);
  margin-top: -48px;
  position: relative;
}

/* ===== 头像样式 ===== */
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}

.avatar-wrapper :deep(.el-avatar) {
  border: 4px solid var(--color-surface);
  box-shadow: var(--shadow-card);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  opacity: 0;
  transition: opacity var(--transition-fast);
  color: #fff;
  font-size: 12px;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

/* ===== 用户信息 ===== */
.profile-text {
  flex: 1;
  min-width: 0;
  padding-top: 56px;
}

.profile-name {
  font-family: var(--font-family-serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.profile-username {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 8px;
}

.profile-bio {
  font-size: 14px;
  color: var(--color-text-sub);
  line-height: 1.6;
}

.profile-actions {
  display: flex;
  gap: var(--spacing-sm);
  padding-top: 56px;
  flex-shrink: 0;
}

/* ===== Tabs ===== */
.profile-tabs {
  padding: var(--spacing-lg);
}

.post-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--spacing-md);
  margin-top: var(--spacing-md);
}

.mini-card {
  cursor: pointer;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  transition: all var(--transition-fast);
}

.mini-card:hover {
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}

.card-cover {
  width: 100%;
  min-height: 140px;
  background: var(--color-bg-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.card-cover-placeholder {
  width: 100%;
  min-height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.mini-card-body {
  padding: 12px;
}

.mini-card-body h4 {
  font-size: 14px;
  color: var(--color-text-main);
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mini-card-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--radius-full);
}

.mini-card-status.APPROVED { color: #16A34A; background: rgba(34,197,94,0.1); }
.mini-card-status.PENDING { color: var(--color-accent); background: var(--color-accent-soft); }
.mini-card-status.REJECTED { color: var(--color-rose); background: var(--color-rose-soft); }

/* ===== 弹窗样式 ===== */
.avatar-dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
}

.avatar-preview-large {
  margin-bottom: var(--spacing-md);
}

.avatar-preview-large :deep(.el-avatar) {
  border: 3px solid var(--color-border);
  box-shadow: var(--shadow-card);
}

.edit-form,
.password-form {
  padding: var(--spacing-sm) 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .profile-page {
    padding: var(--spacing-md);
  }

  .profile-info {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 var(--spacing-md) var(--spacing-md);
  }

  .profile-text {
    padding-top: var(--spacing-md);
  }

  .profile-actions {
    padding-top: 0;
    width: 100%;
    justify-content: center;
  }

  .post-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
