/**
 * 乐观点赞回滚：先翻转本地点赞状态，请求失败时还原。
 * 登录校验由调用方负责（视图各自保留 isLoggedIn 检查 + 跳转登录）。
 * getTarget 用函数而非裸对象：统一处理 props.post（PostCard）与 post.value（PostDetailView）。
 * @param {Object} options
 * @param {() => Object} options.getTarget 返回当前帖子对象
 * @param {(post: Object) => Promise<any>} options.api 点赞请求函数
 */
export function useOptimisticLike({ getTarget, api }) {
  async function toggle() {
    const post = getTarget()
    const prevLiked = post.likedByMe
    const prevCount = post.likeCount || 0
    post.likedByMe = !post.likedByMe
    post.likeCount = prevCount + (post.likedByMe ? 1 : -1)
    try {
      await api(post)
    } catch {
      post.likedByMe = prevLiked
      post.likeCount = prevCount
    }
  }
  return { toggle }
}
