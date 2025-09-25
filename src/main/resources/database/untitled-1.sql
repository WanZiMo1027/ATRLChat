-- 用户表创建脚本
-- 创建时间: 2024年
-- 基于User实体类自动生成

-- 如果表已存在则删除
DROP TABLE IF EXISTS `user`;

-- 创建用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号码',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试数据（可选）
INSERT INTO `user` (username, password, email, phone) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'admin@example.com', '13800138000' ),
('testuser', 'e10adc3949ba59abbe56e057f20f883e', 'test@example.com', '13900139000');

-- 查看表结构
-- DESCRIBE `user`;

-- 用户角色创建表
-- 用于存储用户创建的虚拟角色信息

CREATE TABLE IF NOT EXISTS `character` (
                                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID，主键自增',
                                           `name` VARCHAR(100) NOT NULL COMMENT '角色名字',
                                           `appearance` TEXT COMMENT '角色外表描述',
                                           `background` TEXT COMMENT '角色经历背景故事',
                                           `personality` TEXT COMMENT '角色性格特征',
                                           `classic_lines` TEXT COMMENT '经典台词示例',
                                           `user_id` BIGINT NOT NULL COMMENT '创建该角色的用户ID，外键关联user表',
                                           `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除，0-未删除，1-已删除',
                                           INDEX `idx_user_id` (`user_id`),
                                           INDEX `idx_name` (`name`),
                                           CONSTRAINT `fk_character_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色创建表';

-- 插入示例数据
INSERT INTO `character` (name, appearance, background, personality, classic_lines, user_id) VALUES
                                                                                                ('勇敢的骑士',                                                                                             '身穿银白色盔甲，手持闪耀的长剑，金发碧眼，面容坚毅',
                                                                                                 '出生贵族世家，自幼接受骑士训练，誓死保卫王国和平',
                                                                                                 '勇敢无畏，忠诚正直，富有同情心',
                                                                                                 '为了荣耀和正义，我绝不退缩！',
                                                                                                 1),
                                                                                                ('神秘的法师',
                                                                                                 '身穿深蓝色长袍，手持镶嵌宝石的法杖，银发紫眸，气质超凡',
                                                                                                 '来自古老的魔法世家，掌握着禁忌的魔法知识',
                                                                                                 '睿智冷静，追求知识，偶尔显得高傲',
                                                                                                 '知识就是力量，而力量需要智慧来驾驭。',
                                                                                                 1);

-- 在character表中添加image字段
ALTER TABLE `character` ADD COLUMN `image` VARCHAR(255) COMMENT '角色头像URL' AFTER `name`;

-- 在character表中添加is_public字段
ALTER TABLE `character` ADD COLUMN `is_public` TINYINT(1) DEFAULT 0 COMMENT '是否公开，0-不公开，1-公开' AFTER `image`;


create table user_follow_character
(
    follow_id             bigint auto_increment comment '主键自增ID'
        primary key,

    user_id        bigint                              not null comment '关注者用户ID，外键关联 user(id)',
    character_id   bigint                              not null comment '被关注的角色ID，外键关联 character(id)',

    create_time    datetime default CURRENT_TIMESTAMP  not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '更新时间',

    is_deleted     tinyint(0) default 1                not null comment '是否删除（0-未删除，1-已删除）',

    -- 约束
    constraint fk_ufc_user
        foreign key (user_id) references user (id)
            on delete cascade
            on update cascade,

    constraint fk_ufc_character
        foreign key (character_id) references `character` (id)
            on delete cascade
            on update cascade,

    -- 一个用户对同一角色只能关注一次
    constraint uk_user_character unique (user_id, character_id)
)
    comment '用户关注角色关系表';

