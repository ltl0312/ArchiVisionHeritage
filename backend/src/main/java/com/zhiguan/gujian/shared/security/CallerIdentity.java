package com.zhiguan.gujian.shared.security;

import com.zhiguan.gujian.shared.common.CulturalApiException;
import org.springframework.security.core.Authentication;

/**
 * 从 Spring Security 的 {@link Authentication} 读取调用方身份。
 *
 * 为什么要有这个类：控制器里原先散落 15 处 {@code (Long) auth.getPrincipal()}，
 * 既重复、又隐含"principal 一定是 Long"的假设（一旦上游改动就会 ClassCastException）。
 * 更重要的是，**可见性与归属校验**需要同时拿到 userId 与"是否管理员"，
 * 把这段判断收敛到一处，才能保证各处规则一致。
 *
 * 注意：{@code JwtAuthenticationFilter} 对无效/过期 token 只是"不设置认证"，
 * 因此匿名请求的 {@code auth} 可能是 null —— 本类的所有方法都必须容忍 null。
 */
public final class CallerIdentity {

    private CallerIdentity() {
    }

    /** 已登录返回 userId；匿名或 principal 类型不符返回 null */
    public static Long userId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return id;
        }
        return null;
    }

    /** 是否具备 ADMIN 角色（依据 GrantedAuthority，与 @PreAuthorize("hasRole('ADMIN')") 同源） */
    public static boolean isAdmin(Authentication auth) {
        return auth != null
                && auth.getAuthorities() != null
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * 供「必须登录」的端点使用：取不到身份即抛 401。
     * 注意 SecurityConfig 的 URL 规则已先拦一层，这里是第二层防护，
     * 也避免了把 null 当 userId 传进业务层（原先会产生 userId=null 的脏数据）。
     */
    public static Long requireUserId(Authentication auth) {
        Long id = userId(auth);
        if (id == null) {
            throw new CulturalApiException(401, "请先登录");
        }
        return id;
    }
}
