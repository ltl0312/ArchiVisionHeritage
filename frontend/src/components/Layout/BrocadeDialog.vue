<template>
  <el-dialog
    v-model="notifStore.showBrocade"
    title="&#x1F381; 数字锦盒已送达"
    width="520px"
    :close-on-click-modal="false"
    custom-class="brocade-modal"
    @closed="notifStore.closeBrocade()"
  >
    <div class="brocade-content">
      <p style="font-size:16px; line-height:1.8; margin-bottom:20px;">
        您的一键幻筑已生成完成<br/>
        <span style="color:var(--color-accent)">点击下方按钮，开启您的数字珍藏</span>
      </p>
      <el-button type="primary" size="large" @click="openBrocadeAsset">
        打开数字锦盒
      </el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notifStore = useNotificationStore()

function openBrocadeAsset() {
  notifStore.showBrocade = false
  notifStore.fetchNotifications()
  router.push('/notifications')
}
</script>

<style scoped>
/* ═══ 锦盒弹窗内容（原 App.vue scoped 逐字搬移）═══ */
.brocade-content {
  text-align: center;
  padding: 10px 0;
}
</style>
