-- ============================================================================
-- 智观·古建 — VGGT 接口限流 Lua 脚本（分布式令牌桶精简版）
-- ============================================================================
-- 设计意图：
--   对高成本的 VGGT 深度解析接口进行日粒度限流，替代原有的 JVM 内存计数器。
--   在 Redis 服务端原子执行 INCR + TTL 设置，多实例部署时计数一致。
--
-- KEYS[1]  : rate:zhixi:{userId}:{date}    （限流计数 Key，如 rate:zhixi:42:2026-05-14）
-- ARGV[1]  : 当日最大调用次数（默认 5）
-- ARGV[2]  : Key 过期时间（秒），设为当日剩余秒数，次日自动清零
--
-- 返回值：
--   1   → 未超限，计数已加 1，允许调用
--   0   → 已达上限，拒绝调用（HTTP 429）
-- ============================================================================

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

local current = redis.call('GET', key)
if current and tonumber(current) >= limit then
    return 0
end

local count = redis.call('INCR', key)
-- 首次设置时绑定过期时间（次日自动清零）
if count == 1 then
    redis.call('EXPIRE', key, ttl)
end

return 1
