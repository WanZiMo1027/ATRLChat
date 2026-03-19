-- ============================================================
-- Database: Character Chat Platform
-- Description: AI角色对话与社交系统数据库
-- Charset: utf8mb4
-- ============================================================

-- 1. 创建数据库并设置字符集
CREATE DATABASE IF NOT EXISTS chatapp
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chatapp;

-- 2. 用户表 (基础表，最先创建)
CREATE TABLE IF NOT EXISTS `user` (
                                      `id` bigint AUTO_INCREMENT COMMENT '用户ID，主键',
                                      `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(255) NOT NULL COMMENT '密码（加密存储）',
    `email` varchar(100) NULL COMMENT '邮箱地址',
    `phone` varchar(20) NULL COMMENT '手机号码',
    `avatar_url` varchar(255) DEFAULT 'https://sky-wanzimo.oss-cn-beijing.aliyuncs.com/character_avatars/49_1757839782483.jpg' NULL COMMENT '头像URL',
    `create_time` datetime NULL COMMENT '创建时间',
    `update_time` datetime NULL COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 NULL COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_phone` (`phone`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 3. 管理员表 (独立表)
CREATE TABLE IF NOT EXISTS `admin` (
                                       `id` bigint AUTO_INCREMENT,
                                       `admin_name` varchar(64) NOT NULL,
    `password` varchar(255) NOT NULL,
    `email` varchar(128) NULL,
    `phone` varchar(32) NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` tinyint DEFAULT 0 NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_name` (`admin_name`),
    UNIQUE KEY `uk_email` (`email`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 4. 角色表 (依赖user表)
CREATE TABLE IF NOT EXISTS `character` (
                                           `id` bigint AUTO_INCREMENT COMMENT '角色ID，主键自增',
                                           `name` varchar(100) NOT NULL COMMENT '角色名字',
    `image` varchar(255) NULL COMMENT '角色头像URL',
    `is_public` tinyint(1) DEFAULT 0 NULL COMMENT '是否公开，0-不公开，1-公开',
    `appearance` text NULL COMMENT '角色外表描述',
    `background` text NULL COMMENT '角色经历背景故事',
    `personality` text NULL COMMENT '角色性格特征',
    `classic_lines` text NULL COMMENT '经典台词示例',
    `user_id` bigint NOT NULL COMMENT '创建该角色的用户ID，外键关联user表',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 NULL COMMENT '是否删除，0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_public` (`is_public`),
    CONSTRAINT `fk_character_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色创建表';

-- 5. 群组表 (依赖user表和character表)
CREATE TABLE IF NOT EXISTS `chat_group` (
                                            `id` bigint AUTO_INCREMENT COMMENT '群组ID，主键',
                                            `name` varchar(100) NOT NULL COMMENT '群组名称',
    `avatar_url` varchar(255) DEFAULT 'https://sky-wanzimo.oss-cn-beijing.aliyuncs.com/group_default.jpg' NULL COMMENT '群组头像URL',
    `creator_id` bigint NOT NULL COMMENT '创建者用户ID，外键关联user表',
    `character_id` bigint NULL COMMENT '绑定的AI角色ID，外键关联character表（可选）',
    `description` varchar(500) NULL COMMENT '群组简介',
    `max_members` int DEFAULT 500 NULL COMMENT '最大成员数',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 NULL COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_character_id` (`character_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_group_creator`
    FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_character`
    FOREIGN KEY (`character_id`) REFERENCES `character` (`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 6. 群成员表 (依赖chat_group表和user表)
CREATE TABLE IF NOT EXISTS `chat_group_member` (
                                                   `id` bigint AUTO_INCREMENT COMMENT '成员记录ID，主键',
                                                   `group_id` bigint NOT NULL COMMENT '群组ID，外键关联chat_group表',
                                                   `user_id` bigint NOT NULL COMMENT '用户ID，外键关联user表',
                                                   `role` enum('OWNER','ADMIN','MEMBER') DEFAULT 'MEMBER' NULL COMMENT '成员角色（OWNER-群主，ADMIN-管理员，MEMBER-普通成员）',
    `nickname` varchar(50) NULL COMMENT '群内昵称',
    `join_time` datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '加入时间',
    `last_read_time` datetime NULL COMMENT '最后阅读时间（用于未读消息统计）',
    `is_muted` tinyint(1) DEFAULT 0 NULL COMMENT '是否禁言（0-未禁言，1-已禁言）',
    `is_deleted` tinyint(1) DEFAULT 0 NULL COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_member_group`
    FOREIGN KEY (`group_id`) REFERENCES `chat_group` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_member_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

-- 7. 群消息表 (依赖chat_group表、user表和character表)
CREATE TABLE IF NOT EXISTS `chat_group_message` (
                                                    `id` bigint AUTO_INCREMENT COMMENT '消息ID，主键',
                                                    `group_id` bigint NOT NULL COMMENT '群组ID，外键关联chat_group表',
                                                    `sender_id` bigint NULL COMMENT '发送者用户ID（AI消息时为NULL或0）',
                                                    `sender_type` enum('USER','AI') DEFAULT 'USER' NULL COMMENT '发送者类型（USER-用户，AI-AI角色）',
    `character_id` bigint NULL COMMENT 'AI角色ID（当sender_type=AI时有效）',
    `content` text NOT NULL COMMENT '消息内容',
    `content_type` varchar(20) DEFAULT 'text' NULL COMMENT '消息类型（text-文本，image-图片，file-文件等）',
    `image_url` varchar(255) NULL COMMENT '图片URL（当content_type=image时）',
    `reply_to_id` bigint NULL COMMENT '回复的消息ID（用于引用回复）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '发送时间',
    `is_deleted` tinyint(1) DEFAULT 0 NULL COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_group_time` (`group_id`, `create_time`),
    KEY `idx_sender_id` (`sender_id`),
    KEY `idx_character_id` (`character_id`),
    CONSTRAINT `fk_message_group`
    FOREIGN KEY (`group_id`) REFERENCES `chat_group` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_sender`
    FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_message_character`
    FOREIGN KEY (`character_id`) REFERENCES `character` (`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群消息表';

-- 8. 用户关注角色关系表 (依赖user表和character表)
CREATE TABLE IF NOT EXISTS `user_follow_character` (
                                                       `follow_id` bigint AUTO_INCREMENT COMMENT '主键自增ID',
                                                       `user_id` bigint NOT NULL COMMENT '关注者用户ID，外键关联 user(id)',
                                                       `character_id` bigint NOT NULL COMMENT '被关注的角色ID，外键关联 character(id)',
                                                       `create_time` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
                                                       `update_time` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                       `status` tinyint DEFAULT 1 NOT NULL COMMENT '是否关注（0-未关注，1-已关注）',
                                                       PRIMARY KEY (`follow_id`),
    UNIQUE KEY `uk_user_character` (`user_id`, `character_id`),
    CONSTRAINT `fk_ufc_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT `fk_ufc_character`
    FOREIGN KEY (`character_id`) REFERENCES `character` (`id`) ON UPDATE CASCADE ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注角色关系表';

-- 9. AI对话记录表 (独立表，用于统计和分析)
CREATE TABLE IF NOT EXISTS `ai_call_log` (
                                             `id` bigint AUTO_INCREMENT,
                                             `user_id` varchar(64) NOT NULL COMMENT '用户ID',
    `character_id` varchar(64) NOT NULL COMMENT '角色ID',
    `model_name` varchar(128) NOT NULL COMMENT '模型名称',
    `memory_id` varchar(128) NOT NULL COMMENT '会话ID',
    `status` varchar(16) NOT NULL COMMENT '状态: success/error',
    `input_tokens` int DEFAULT 0 NULL COMMENT '输入token数',
    `output_tokens` int DEFAULT 0 NULL COMMENT '输出token数',
    `total_tokens` int DEFAULT 0 NULL COMMENT '总token数',
    `duration_ms` int DEFAULT 0 NULL COMMENT '响应时间(毫秒)',
    `request_ts` datetime NOT NULL COMMENT '请求开始时间',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_status_time_tokens` (`user_id`, `status`, `request_ts`, `total_tokens`),
    KEY `idx_character_time` (`character_id`, `request_ts`),
    KEY `idx_memory` (`memory_id`),
    KEY `idx_created_at` (`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话记录表';

-- ============================================================
-- 初始化数据（可选）
-- ============================================================

-- 插入默认管理员账号（密码需替换为实际加密后的值）
-- INSERT INTO `admin` (`admin_name`, `password`, `email`)
-- VALUES ('admin', '$2a$10$...', 'admin@example.com');

-- ============================================================
-- 完成
-- ============================================================