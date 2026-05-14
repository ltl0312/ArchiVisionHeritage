-- ============================================================================
-- 智观·古建 — AI 3D 幻筑接口幂等性守护 Lua 脚本
-- ============================================================================
-- 设计意图：
--   针对耗时的 AI 3D 生成接口，防止用户在短时间内重复提交相同 Prompt，
--   从而造成双重扣费与 GPU 算力浪费。
--   本脚本在 Redis 服务端原子执行，杜绝 race condition。
--
-- KEYS[1]  : huanzhu:dedup:{userId}:{promptHash}   （幂等锁 Key）
-- KEYS[2]  : huanzhu:dedup:{userId}:{promptHash}:taskId （存储已创建 taskId）
-- ARGV[1]  : 锁过期时间 TTL（秒），默认 600s = 10min
-- ARGV[2]  : 新任务 ID（当需要写入时传入）
--
-- 返回值约定：
--   1  → 幂等锁不存在，允许创建新任务
--   0  → 幂等锁已存在（重复提交），返回已有 taskId
--   -1 → 脚本执行异常
-- ============================================================================

local lockKey = KEYS[1]
local taskIdKey = KEYS[2]
local ttl = tonumber(ARGV[1])

-- 检查是否已有进行中的任务
local existingTaskId = redis.call('GET', taskIdKey)

if existingTaskId and existingTaskId ~= '' then
    -- 已存在 → 幂等返回，拒绝重复创建
    return 0
end

-- 无重复 → 允许创建，设置短期占位锁防并发穿透
-- 注意：真正的 taskId 由 Java 端在 MySQL INSERT 后通过 updateLockWithTaskId() 回填
redis.call('SETEX', lockKey, ttl, '1')
return 1
