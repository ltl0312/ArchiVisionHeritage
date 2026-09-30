<template>
  <el-dialog
    v-model="visible"
    title="&#x1F381; 数字锦盒已送达"
    width="520px"
    :close-on-click-modal="false"
    custom-class="brocade-modal"
    @closed="notifStore.closeBrocade()"
  >
    <div class="brocade-content">
      <p class="brocade-lead">
        您的一键幻筑已生成完成<br />
        <span class="brocade-accent">点击下方按钮，开启您的数字珍藏</span>
      </p>
      <button type="button" class="v4-btn v4-btn--gold v4-btn--lg" @click="openBrocadeAsset">
        打开数字锦盒
      </button>
      <p class="brocade-note">
        可在「个人设置 → 通知策略」中关闭此弹窗提醒。
      </p>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { useNotifyPrefs } from '@/composables/useNotifyPrefs'

/**
 * 数字锦盒弹窗（幻筑完成时的「刚送达」提示）。
 *
 * 与「通知策略」中的「数字锦盒送达提醒」开关联动：关闭后不再弹窗，
 * 但站内信仍会写入通知中心 —— 这是真实的偏好消费点，
 * 而不是一个只被记录、无人读取的装饰开关。
 */
const router = useRouter()
const notifStore = useNotificationStore()
const { isOn } = useNotifyPrefs()

const visible = computed({
  get: () => notifStore.showBrocade && isOn('box'),
  set: (v) => { if (!v) notifStore.closeBrocade() }
})

function openBrocadeAsset() {
  notifStore.closeBrocade()
  notifStore.fetchNotifications()
  router.push('/notifications')
}
</script>

<style scoped>
.brocade-content {
  text-align: center;
  padding: 10px 0;
}

.brocade-lead {
  font-size: 16px;
  line-height: 1.8;
  margin-bottom: 20px;
  color: var(--color-text-main);
}

.brocade-accent {
  color: var(--color-accent-text);
}

.brocade-note {
  margin-top: 16px;
  font-size: 11px;
  color: var(--color-text-faint);
}
</style>
