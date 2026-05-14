CREATE DATABASE IF NOT EXISTS `zhiguan_gujian`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `zhiguan_gujian`;

-- 1. 用户表 (User)
CREATE TABLE `user` (
                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
                        `password_hash` VARCHAR(255) NOT NULL COMMENT '加密密码',
                        `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
                        `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像路径',
                        `bio` VARCHAR(255) DEFAULT NULL COMMENT '个人文化签名',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息与授权状态';

-- 2. 关注关系表 (Follow Record)
CREATE TABLE `follow_record` (
                                 `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 `follower_id` BIGINT NOT NULL COMMENT '关注者ID',
                                 `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
                                 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
                                 FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                 FOREIGN KEY (`following_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- 3. AI 任务状态机表 (AI Task)
CREATE TABLE `ai_task` (
                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                           `user_id` BIGINT NOT NULL,
                           `task_type` ENUM('HUANZHU_3D', 'ZHIXI_VGGT') NOT NULL COMMENT '任务类型：3D生成或VGGT解析',
                           `original_prompt` VARCHAR(512) NOT NULL COMMENT '用户原始输入',
                           `enhanced_prompt` TEXT COMMENT '文化词库优化后的提示词',
                           `status` ENUM('PENDING', 'RUNNING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '核心状态机',
                           `error_message` VARCHAR(512) DEFAULT NULL,
                           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           KEY `idx_user_status` (`user_id`, `status`),
                           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步幻筑与智析任务状态机';

-- 4. 模型与图片资产表 (Model Asset)
CREATE TABLE `model_asset` (
                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                               `task_id` BIGINT NOT NULL COMMENT '关联的AI生成任务',
                               `preview_2d_path` VARCHAR(512) NOT NULL COMMENT '2D降维预览图本地路径',
                               `glb_3d_path` VARCHAR(512) DEFAULT NULL COMMENT '3D GLB文件本地路径',
                               `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (`task_id`) REFERENCES `ai_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI生成的数字资产路径';

-- 5. 社区动态表 (Post)
CREATE TABLE `post` (
                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `user_id` BIGINT NOT NULL,
                        `model_asset_id` BIGINT DEFAULT NULL COMMENT '关联的古建3D资产',
                        `title` VARCHAR(128) NOT NULL COMMENT '动态标题',
                        `content` TEXT COMMENT '文化内涵与文字描述',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        KEY `idx_created_at` (`created_at`),
                        FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY (`model_asset_id`) REFERENCES `model_asset`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='古建文化社区图文动态';

-- 6. 评论表 (Comment)
CREATE TABLE `comment` (
                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                           `post_id` BIGINT NOT NULL,
                           `user_id` BIGINT NOT NULL,
                           `content` VARCHAR(1024) NOT NULL,
                           `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(用于楼中楼)',
                           `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           FOREIGN KEY (`post_id`) REFERENCES `post`(`id`) ON DELETE CASCADE,
                           FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态帖子的互动交流';

-- 7. 点赞记录表 (Like Record)
CREATE TABLE `like_record` (
                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                               `user_id` BIGINT NOT NULL,
                               `target_id` BIGINT NOT NULL COMMENT '帖子ID或评论ID',
                               `target_type` ENUM('POST', 'COMMENT') NOT NULL,
                               `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
                               FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子与评论的点赞行为记录';

-- 8. 站内信通知表 (Notification)
CREATE TABLE `notification` (
                                `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                `user_id` BIGINT NOT NULL,
                                `task_id` BIGINT DEFAULT NULL COMMENT '当ai_task成功时关联',
                                `message` VARCHAR(255) NOT NULL COMMENT '例如：您的数字锦盒已送达',
                                `is_read` BOOLEAN DEFAULT FALSE,
                                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
                                FOREIGN KEY (`task_id`) REFERENCES `ai_task`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信系统与数字锦盒提醒';

-- 9. 智析沙盒离线数据表 (Analysis Demo)
CREATE TABLE `analysis_demo` (
                                 `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 `title` VARCHAR(128) NOT NULL COMMENT '解析示例标题',
                                 `mock_json_data` JSON NOT NULL COMMENT 'VGGT智析引擎的离线JSON模拟数据',
                                 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='存储VGGT古建结构解析的演示数据';

-- 初始化 VGGT 斗栱解析的模拟数据 (Mock JSON)
INSERT INTO `analysis_demo` (`title`, `mock_json_data`) VALUES (
                                                                   '唐代斗栱结构解析 (VGGT 演示)',
                                                                   '{
                                                                     "scene_id": "dougong_tang_01",
                                                                     "camera_extrinsics": [[0.866, -0.5, 0.0, 1.2], [0.5, 0.866, 0.0, 0.5], [0.0, 0.0, 1.0, 3.0], [0.0, 0.0, 0.0, 1.0]],
                                                                     "camera_intrinsics": [[800.0, 0.0, 256.0], [0.0, 800.0, 256.0], [0.0, 0.0, 1.0]],
                                                                     "structural_elements": [
                                                                       {"ontology": "Dou (斗)", "confidence": 0.85, "bounding_box": [50, 60, 100, 100], "cultural_insight": "斗是承托其上方栱或昂的方形木块，是力的汇聚点。"},
                                                                       {"ontology": "Gong (栱)", "confidence": 0.91, "bounding_box": [120, 100, 200, 150], "cultural_insight": "栱为弓形短木，通过层层出跳，将屋檐的重量均匀传递至柱子。"},
                                                                       {"ontology": "Ang (昂)", "confidence": 0.88, "bounding_box": [220, 150, 300, 200], "cultural_insight": "昂起杠杆作用，利用内部屋架的重量挑起沉重的外部出檐。"}
                                                                     ]
                                                                   }'
                                                               );

