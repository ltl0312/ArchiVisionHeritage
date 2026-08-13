-- ============================================================================
-- 智观·古建 (ArchiVision Heritage) — 数据库初始化脚本 v1.2
-- ============================================================================
-- 使用方法: mysql -u root -p < init.sql
-- 字符集: utf8mb4 (完整支持中文古建术语与emoji)
-- 引擎: InnoDB (事务支持与外键约束)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `zhiguan_gujian`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `zhiguan_gujian`;

-- ============================================================================
-- 1. 用户表 (User) — 支持 RBAC 角色权限
-- ============================================================================
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '登录用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密密码',
    `nickname` VARCHAR(64) NOT NULL COMMENT '用户显示昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像路径',
    `bio` VARCHAR(255) DEFAULT NULL COMMENT '个人文化签名',
    `role` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色: USER(普通用户) / ADMIN(管理员)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息与授权状态';

-- ============================================================================
-- 2. 关注关系表 (Follow Record)
-- ============================================================================
CREATE TABLE `follow_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `follower_id` BIGINT NOT NULL COMMENT '关注者用户ID',
    `following_id` BIGINT NOT NULL COMMENT '被关注者用户ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`following_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- ============================================================================
-- 3. AI 任务状态机表 (AI Task)
-- ============================================================================
CREATE TABLE `ai_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '提交任务的用户ID',
    `task_type` ENUM('HUANZHU_3D', 'ZHIXI_VGGT') NOT NULL COMMENT '任务类型: 3D生成 或 VGGT解析',
    `original_prompt` VARCHAR(512) NOT NULL COMMENT '用户原始输入描述',
    `enhanced_prompt` TEXT COMMENT '经文化词库优化后的提示词',
    `status` ENUM('PENDING', 'RUNNING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '任务状态机',
    `error_message` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_user_status` (`user_id`, `status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步幻筑与智析任务状态机';

-- ============================================================================
-- 4. 模型与图片资产表 (Model Asset)
-- ============================================================================
CREATE TABLE `model_asset` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `task_id` BIGINT NOT NULL COMMENT '关联的AI生成任务ID',
    `preview_2d_path` VARCHAR(512) NOT NULL COMMENT '2D预览图本地路径',
    `glb_3d_path` VARCHAR(512) DEFAULT NULL COMMENT '3D GLB模型文件本地路径',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`task_id`) REFERENCES `ai_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生成的数字资产文件路径';

-- ============================================================================
-- 5. 社区动态表 (Post) — 含内容审核状态机
-- ============================================================================
CREATE TABLE `post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '发布者用户ID',
    `model_asset_id` BIGINT DEFAULT NULL COMMENT '关联的古建3D资产ID',
    `title` VARCHAR(128) NOT NULL COMMENT '动态标题',
    `content` TEXT COMMENT '文化内涵与文字描述',
    `tags` VARCHAR(512) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `cover_image_url` VARCHAR(1024) DEFAULT NULL COMMENT '封面图URL',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING(待审核) / APPROVED(已发布) / REJECTED(已驳回)',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '审核驳回原因',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_created_at` (`created_at`),
    KEY `idx_status` (`status`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`model_asset_id`) REFERENCES `model_asset`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='古建文化社区图文动态';

-- ============================================================================
-- 6. 评论表 (Comment)
-- ============================================================================
CREATE TABLE `comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `post_id` BIGINT NOT NULL COMMENT '被评论的帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '评论者用户ID',
    `content` VARCHAR(1024) NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID (用于楼中楼回复)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态帖子的互动交流';

-- ============================================================================
-- 7. 点赞记录表 (Like Record)
-- ============================================================================
CREATE TABLE `like_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '点赞用户ID',
    `target_id` BIGINT NOT NULL COMMENT '点赞目标ID (帖子ID 或 评论ID)',
    `target_type` ENUM('POST', 'COMMENT') NOT NULL COMMENT '目标类型: 帖子 或 评论',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子与评论的点赞行为记录';

-- ============================================================================
-- 8. 站内信通知表 (Notification)
-- ============================================================================
CREATE TABLE `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
    `task_id` BIGINT DEFAULT NULL COMMENT '关联的AI任务ID (幻筑完成时)',
    `message` VARCHAR(255) NOT NULL COMMENT '通知内容',
    `is_read` BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`task_id`) REFERENCES `ai_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信系统与数字锦盒提醒';

-- ============================================================================
-- 9. 智析演示数据表 (Analysis Demo)
-- ============================================================================
CREATE TABLE `analysis_demo` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(128) NOT NULL COMMENT '解析示例标题',
    `mock_json_data` JSON NOT NULL COMMENT 'VGGT智析引擎的离线JSON模拟数据',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储VGGT古建结构解析的演示数据';

-- ============================================================================
-- 初始化管理员账号
-- 用户名: admin  密码: admin123  (BCrypt 加密)
-- ============================================================================
INSERT IGNORE INTO `user` (`username`, `password_hash`, `nickname`, `role`, `bio`)
VALUES ('admin',
        '$2b$12$CEWyJAgaE7kjNj.WJqnp8uaSeV79e5G13vjev9D7Gxsx9MHh2bRTm',
        '古建守门人',
        'ADMIN',
        '守护每一座古建筑的数字孪生');

-- ============================================================================
-- 初始化演示用户
-- 用户名: demo_user  密码: demo123  (BCrypt 加密，独立于 admin)
-- ============================================================================
INSERT IGNORE INTO `user` (`username`, `password_hash`, `nickname`, `role`, `bio`)
VALUES ('demo_user',
        '$2a$12$XStzN1IempNxEjtZIy3Oke/gFaPEZ3e9ZQP/ipaDIxnpRlFG5TGeG',
        '古建爱好者',
        'USER',
        '热爱中国古建筑文化，致力于数字化保护');

-- ============================================================================
-- 初始化 VGGT 演示解析数据
-- ============================================================================

-- 演示案例1: 唐代斗栱结构解析
INSERT INTO `analysis_demo` (`title`, `mock_json_data`) VALUES (
    '唐代斗栱结构解析 (VGGT 演示)',
    '{
      "scene_id": "dougong_tang_01",
      "camera_extrinsics": [
        [0.866, -0.5, 0.0, 1.2],
        [0.5, 0.866, 0.0, 0.5],
        [0.0, 0.0, 1.0, 3.0],
        [0.0, 0.0, 0.0, 1.0]
      ],
      "camera_intrinsics": [
        [800.0, 0.0, 256.0],
        [0.0, 800.0, 256.0],
        [0.0, 0.0, 1.0]
      ],
      "structural_elements": [
        {
          "ontology": "Dou (斗)",
          "confidence": 0.85,
          "bounding_box": [50, 60, 100, 100],
          "cultural_insight": "斗是承托其上方栱或昂的方形木块，是力的汇聚点。宋代《营造法式》将斗分为栌斗、交互斗、齐心斗、散斗四种，各司其职。"
        },
        {
          "ontology": "Gong (栱)",
          "confidence": 0.91,
          "bounding_box": [120, 100, 200, 150],
          "cultural_insight": "栱为弓形短木，通过层层出跳，将屋檐的重量均匀传递至柱子。唐代斗栱雄大，出檐深远，展现了唐风建筑的恢弘气度。"
        },
        {
          "ontology": "Ang (昂)",
          "confidence": 0.88,
          "bounding_box": [220, 150, 300, 200],
          "cultural_insight": "昂起杠杆作用，利用内部屋架的重量挑起沉重的外部出檐。它是中国古代木构建筑中独有的结构智慧，体现了力与美的统一。"
        }
      ]
    }'
);

-- 演示案例2: 歇山顶屋顶结构解析
INSERT INTO `analysis_demo` (`title`, `mock_json_data`) VALUES (
    '宋代歇山顶屋顶结构解析 (VGGT 演示)',
    '{
      "scene_id": "xieshan_song_01",
      "camera_extrinsics": [
        [0.9, -0.4, 0.0, 1.5],
        [0.4, 0.9, 0.0, 0.8],
        [0.0, 0.0, 1.0, 4.0],
        [0.0, 0.0, 0.0, 1.0]
      ],
      "camera_intrinsics": [
        [900.0, 0.0, 300.0],
        [0.0, 900.0, 300.0],
        [0.0, 0.0, 1.0]
      ],
      "structural_elements": [
        {
          "ontology": "Zhengwen (正吻)",
          "confidence": 0.92,
          "bounding_box": [180, 30, 280, 90],
          "cultural_insight": "正吻位于正脊两端，宋代称为鸱吻，形如龙首吞脊。它不仅具有装饰意义，更起到了加固屋脊、防止雨水渗漏的实际作用。"
        },
        {
          "ontology": "Chuisha Ji (垂脊)",
          "confidence": 0.87,
          "bounding_box": [120, 100, 200, 200],
          "cultural_insight": "垂脊自正吻沿屋面坡下延伸，是歇山顶的标志性结构线。其上排列的脊兽数量与等级直接相关，体现了古代建筑的礼制秩序。"
        },
        {
          "ontology": "Shanhua Mian (山花面)",
          "confidence": 0.83,
          "bounding_box": [300, 120, 380, 200],
          "cultural_insight": "歇山顶两侧的三角形山花面，是区分歇山顶与庑殿顶的关键特征。宋代山花面较小，清代逐渐增大，反映了不同朝代的审美演变。"
        }
      ]
    }'
);

-- 演示案例3: 佛光寺东大殿整体结构
INSERT INTO `analysis_demo` (`title`, `mock_json_data`) VALUES (
    '佛光寺东大殿 — 唐代木构整体解析 (VGGT 演示)',
    '{
      "scene_id": "foguangsi_tang_01",
      "camera_extrinsics": [
        [0.95, -0.3, 0.0, 2.0],
        [0.3, 0.95, 0.0, 1.0],
        [0.0, 0.0, 1.0, 5.0],
        [0.0, 0.0, 0.0, 1.0]
      ],
      "camera_intrinsics": [
        [1000.0, 0.0, 320.0],
        [0.0, 1000.0, 320.0],
        [0.0, 0.0, 1.0]
      ],
      "structural_elements": [
        {
          "ontology": "Dunge (檐柱)",
          "confidence": 0.94,
          "bounding_box": [50, 180, 120, 320],
          "cultural_insight": "佛光寺东大殿的檐柱采用『侧脚』做法，柱子略向内侧倾斜，增强了整体结构的稳定性。这一设计巧思使大殿历经千年而屹立不倒。"
        },
        {
          "ontology": "Dougong Ceng (斗栱层)",
          "confidence": 0.96,
          "bounding_box": [100, 100, 350, 180],
          "cultural_insight": "东大殿的斗栱层高达柱高的1/2，出跳达四跳之多，是中国现存唐代木构中斗栱最为雄大者。梁思成先生称之为『中国第一国宝』。"
        },
        {
          "ontology": "Wudian Roof (庑殿顶)",
          "confidence": 0.90,
          "bounding_box": [50, 20, 350, 100],
          "cultural_insight": "单檐庑殿顶是唐代高等级建筑的标志。屋面曲线平缓优雅，出檐深远，体现了大唐盛世的宏大气象与精湛工艺。"
        }
      ]
    }'
);

-- ============================================================================
-- 数据库初始化完成
-- ============================================================================
-- 启动后端后访问: http://localhost:8080
-- 前端开发地址: http://localhost:5173
-- 管理员账号: admin / admin123
-- 演示账号: demo_user / demo123
-- ============================================================================
