-- ============================================================================
-- 智观·古建 — H2 测试库 schema（仅测试用，与 init.sql 手工同步）
-- 注意: JDBC URL 必须带 NON_KEYWORDS=USER（H2 保留字）
--
-- 同步规则: 表结构/索引与 init.sql 保持一致; ENUM→VARCHAR + CHECK, TEXT→CLOB, JSON→VARCHAR(4000)
--
-- ⚠️ 关于 CHECK 约束（测试保真度）：init.sql 用 MySQL ENUM 约束取值，
--    而 H2 侧原先只是裸 VARCHAR —— 于是"写入非法状态"在生产会被 DB 拒绝、
--    在测试里却照单全收，状态机不变量在测试中**从未被验证**。
--    这里用 CHECK 把 ENUM 的约束力补回来，让两边行为一致。
-- ============================================================================

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512),
    bio VARCHAR(255),
    role VARCHAR(16) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE follow_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_follow UNIQUE (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE ai_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_type VARCHAR(20) NOT NULL CHECK (task_type IN ('HUANZHU_3D', 'ZHIXI_VGGT')),
    original_prompt VARCHAR(512) NOT NULL,
    enhanced_prompt CLOB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    error_message VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE model_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    preview_2d_path VARCHAR(512) NOT NULL,
    glb_3d_path VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES ai_task(id) ON DELETE CASCADE
);

CREATE TABLE post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    model_asset_id BIGINT,
    title VARCHAR(128) NOT NULL,
    content CLOB,
    tags VARCHAR(512),
    cover_image_url VARCHAR(1024),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reject_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (model_asset_id) REFERENCES model_asset(id) ON DELETE SET NULL
);

CREATE TABLE comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1024) NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE like_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_target UNIQUE (user_id, target_id, target_type),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES ai_task(id) ON DELETE CASCADE
);

CREATE TABLE analysis_demo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    mock_json_data VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引（与 init.sql 对齐：既有 2 个 + 本阶段新增 3 个）
CREATE INDEX idx_user_status ON ai_task (user_id, status);
CREATE INDEX idx_updated_at ON ai_task (updated_at);
CREATE INDEX idx_created_at ON post (created_at);
CREATE INDEX idx_status ON post (status);
CREATE INDEX idx_target ON like_record (target_id, target_type);
CREATE INDEX idx_user_read ON notification (user_id, is_read);

-- ============================================================================
-- 种子数据（与 init.sql 对齐）
--
-- ⚠️ 原先 H2 里一条 INSERT 都没有，导致两个后果：
--    1. 管理员账号在测试中**从不存在** → admin 登录、ADMIN 角色分支、
--       /api/v1/admin/** 的 RBAC 规则在测试里从未被真实执行过；
--    2. analysis_demo 恒为空 → JSON 列的读取与反序列化从未跑过。
--    这里补齐，让集成测试能覆盖真实角色与真实数据路径。
-- ============================================================================

-- 管理员：admin / admin123
INSERT INTO user (username, password_hash, nickname, role, bio)
VALUES ('admin',
        '$2b$12$CEWyJAgaE7kjNj.WJqnp8uaSeV79e5G13vjev9D7Gxsx9MHh2bRTm',
        '古建守门人',
        'ADMIN',
        '守护每一座古建筑的数字孪生');

-- 演示用户：demo_user / demo123
INSERT INTO user (username, password_hash, nickname, role, bio)
VALUES ('demo_user',
        '$2a$12$XStzN1IempNxEjtZIy3Oke/gFaPEZ3e9ZQP/ipaDIxnpRlFG5TGeG',
        '古建爱好者',
        'USER',
        '热爱中国古建筑文化，致力于数字化保护');

-- VGGT 演示数据（取 init.sql 的第一条，字段结构一致）
INSERT INTO analysis_demo (title, mock_json_data)
VALUES ('唐代斗栱结构解析 (VGGT 演示)',
        '{"scene_id":"dougong_tang_01","camera_extrinsics":[[0.866,-0.5,0.0,1.2],[0.5,0.866,0.0,0.5],[0.0,0.0,1.0,3.0],[0.0,0.0,0.0,1.0]],"camera_intrinsics":[[800.0,0.0,256.0],[0.0,800.0,256.0],[0.0,0.0,1.0]],"structural_elements":[{"ontology":"Dou (斗)","confidence":0.85,"bounding_box":[50,60,100,100],"cultural_insight":"斗是承托其上方栱或昂的方形木块，是力的汇聚点。"}]}');

