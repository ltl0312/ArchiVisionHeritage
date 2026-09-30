import { ref } from 'vue'

/**
 * 通知策略（本机偏好）— 单一 owner
 *
 * 后端没有「通知偏好」接口，因此这 4 项只能作为**本机偏好**存在
 * （localStorage），并且在 UI 上如实说明「暂未同步到账号」。
 *
 * 为什么要有这个 composable：设置页要**写**它，BrocadeDialog 要**读**它。
 * 若两边各持一份 localStorage 读写逻辑，就会出现两个 owner 各写同一个 key，
 * 迟早不一致。这里用模块级 ref 做单例，读 / 写 / 默认值只有一处定义。
 */

export const PREF_KEY = 'notify-prefs'

/** 文案照设计稿；`def` 是首次使用的默认值 */
export const PREF_ITEMS = [
  { key: 'box', label: '数字锦盒送达提醒', hint: '幻筑完成时弹窗并推送站内信', def: true },
  { key: 'audit', label: '审核结果通知', hint: '内容通过或被驳回时提醒', def: true },
  { key: 'social', label: '互动通知', hint: '被关注、被点赞、被评论', def: false },
  { key: 'digest', label: '邮件摘要', hint: '每周一封，汇总你的数字档案动态', def: false }
]

function defaults() {
  return PREF_ITEMS.reduce((acc, p) => ({ ...acc, [p.key]: p.def }), {})
}

function read() {
  const base = defaults()
  try {
    const raw = localStorage.getItem(PREF_KEY)
    if (!raw) return base
    const saved = JSON.parse(raw)
    PREF_ITEMS.forEach(p => {
      if (typeof saved?.[p.key] === 'boolean') base[p.key] = saved[p.key]
    })
  } catch {
    /* 读失败（隐私模式 / 脏数据）回退默认值 */
  }
  return base
}

/** 模块级单例：所有消费者共享同一份响应式偏好 */
const prefs = ref(read())

export function useNotifyPrefs() {
  function persist() {
    try {
      localStorage.setItem(PREF_KEY, JSON.stringify(prefs.value))
    } catch {
      /* 写失败不阻断交互：本次会话内仍然生效 */
    }
  }

  function set(key, value) {
    if (!(key in prefs.value)) return
    prefs.value = { ...prefs.value, [key]: !!value }
    persist()
  }

  function toggle(key) {
    set(key, !prefs.value[key])
  }

  /** 供非响应式场景（如事件回调）读取当前值 */
  function isOn(key) {
    return !!prefs.value[key]
  }

  return { prefs, items: PREF_ITEMS, set, toggle, isOn }
}
