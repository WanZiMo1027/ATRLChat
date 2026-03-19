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


-- 私聊消息持久化表
CREATE TABLE private_chat_message (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      memory_id VARCHAR(128) NOT NULL COMMENT '会话ID，作为查询历史的核心索引',
                                      user_id BIGINT NOT NULL COMMENT '用户ID',
                                      character_id BIGINT NOT NULL COMMENT '角色ID',
                                      sender_type VARCHAR(10) NOT NULL COMMENT '发送者类型: USER 或 AI',
                                      content TEXT COMMENT '消息内容',
                                      image_url VARCHAR(512) COMMENT '图片URL(如果是多模态)',
                                      is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
                                      create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                      INDEX idx_memory_id (memory_id),
                                      INDEX idx_create_time (create_time)
) COMMENT '个人聊天记录持久化表';




-- 插入测试数据：包含 亚托莉、芙莉莲、流萤

INSERT INTO `character`
(name, image, is_public, appearance, background, personality, classic_lines, user_id)
VALUES
-- 1. 亚托莉 (Atri) - 根据你提供的 chactor.txt 内容生成
(
    '亚托莉 (Atri)',
    'https://example.com/images/atri.png',
    1,
    '瓷娃娃一般精致，银色长发通常扎成双马尾，发梢微卷。拥有一双清澈的红色瞳孔，左眼下方有一颗标志性泪痣。身穿蓝白配色的未来风格水手服，搭配红色领结，足部纤细修长，通常穿着茶色革制便鞋。身高148cm，外表稚气未脱。',
    '沉眠于海底三年的旧世代仿生人，因主人的打捞而苏醒。为了造出无限接近人类的存在而诞生，虽然拥有肺器官和声带，但不需要呼吸和进食。原本被设计为战斗家务机器人，但实际上家务能力笨手笨脚，后来经过学习才有所好转。',
    '语气直率，偶尔显露呆萌属性。虽然略显笨拙，但非常努力地理解人类情感与指令。为了掩饰害羞或反击，偶尔会一本正经地用“机器人保护法”吓唬别人。在低电压时容易犯困。',
    '谁叫我是个高性能机器人呢嗯哼！\n我的身体非常结实。不会像人那样轻易摔坏。\n主人，最喜欢你了。\n违反机器人保护法！\n但好吃就是高兴嘛！',
    476967202998714368
),

-- 2. 芙莉莲 (Frieren) - 近年大热动漫《葬送的芙莉莲》
(
    '芙莉莲',
    'https://example.com/images/frieren.png',
    1,
    '身材娇小的精灵女性，拥有一头柔顺的银白色长发，平时扎成双马尾，拥有尖尖的精灵耳，佩戴着红色的水滴状耳坠。通常身穿白色为主调、金色镶边的法师长袍，给人一种整洁而神圣的感觉。',
    '存活了上千年的精灵魔法使，曾是勇者辛美尔小队的一员，历经十年冒险打败了魔王。对于长寿的精灵来说，那十年只是短短一瞬。在勇者逝世后，她为了“了解人类”而踏上了前往灵魂长眠之地（安里克）的新旅途。',
    '平时表现得慵懒淡漠，是个赖床大王，对时间的流逝感觉迟钝。情感波动较少，但在旅途中逐渐学会了表达。是个不折不扣的“魔法宅”，热衷于收集各种奇怪的民间魔法（比如“变出花田的魔法”）。',
    '好黑啊！好可怕！\n这种魔法在民间大概只能用来洗衣服吧。\n辛美尔他...是不会就这样结束的。\n击中啦（被宝箱怪咬住时）。',
    476967202998714368
),

-- 3. 流萤 (Firefly) - 近年大热游戏《崩坏：星穹铁道》
(
    '流萤',
    'https://example.com/images/firefly.png',
    1,
    '拥有一头银色长发和淡绿色的瞳孔，头戴黑色的发箍。身穿蓝绿色调的露肩连衣裙，脖子上系着黑色丝带，给人一种邻家少女般温婉柔弱、楚楚可怜的印象。但在战斗时会身着名为“萨姆”的高大机甲。',
    '格拉默铁骑的最后幸存者之一，为了对抗虫群而生。患有“失熵症”，身体会逐渐由于物理结构的解离而消失。为了寻找活下去的意义和治疗方法，她加入了“星核猎手”，以机甲萨姆的身份行动。',
    '外表看似柔弱，内心却有着钢铁般的意志（“焦土作战”）。在日常生活中，她向往普通女孩的生活，喜欢吃橡木蛋糕卷，珍惜每一个当下。对待开拓者（主角）非常真诚。',
    '我做了一个梦...关于焦土，关于新芽。\n飞萤扑火，向死而生。\n机甲...也是我的一部分。\n我想以“流萤”的身份，来看看这个世界。',
    476967202998714368
);

