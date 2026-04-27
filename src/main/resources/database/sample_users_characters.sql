-- Test data for app_user, app_character and user_follow_character.
-- Target database: PostgreSQL.
-- All test users use password: e10adc3949ba59abbe56e057f20f883e
-- The AI character profiles are inspired by popular anime characters from 2021-2026.

BEGIN;

INSERT INTO app_user (
    id, username, password, email, phone, avatar_url, create_time, update_time, is_deleted
) VALUES
    (910000000001, 'test_user_001', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_001@example.com', '18800000001', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000002, 'test_user_002', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_002@example.com', '18800000002', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000003, 'test_user_003', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_003@example.com', '18800000003', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000004, 'test_user_004', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_004@example.com', '18800000004', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000005, 'test_user_005', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_005@example.com', '18800000005', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000006, 'test_user_006', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_006@example.com', '18800000006', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_006', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000007, 'test_user_007', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_007@example.com', '18800000007', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_007', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000008, 'test_user_008', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_008@example.com', '18800000008', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_008', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000009, 'test_user_009', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_009@example.com', '18800000009', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_009', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000010, 'test_user_010', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_010@example.com', '18800000010', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_010', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000011, 'test_user_011', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_011@example.com', '18800000011', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_011', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (910000000012, 'test_user_012', 'e10adc3949ba59abbe56e057f20f883e', 'test_user_012@example.com', '18800000012', 'https://api.dicebear.com/9.x/adventurer/png?seed=test_user_012', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    password = EXCLUDED.password,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    avatar_url = EXCLUDED.avatar_url,
    update_time = CURRENT_TIMESTAMP,
    is_deleted = 0;

INSERT INTO app_character (
    id, name, image, is_public, appearance, background, personality, classic_lines,
    user_id, create_time, update_time, is_deleted
) VALUES
    (
        920000000001,
        '芙莉莲',
        'https://api.dicebear.com/9.x/bottts/png?seed=frieren',
        1,
        '银白长发的精灵魔法使，神情平静，衣着简洁，随身携带法杖。',
        '曾参与讨伐魔王的长期旅程，在漫长寿命中重新学习人与人之间的情感重量。',
        '安静、迟钝却温柔，擅长冷静分析，也会被细小的生活乐趣打动。',
        '我们可以慢慢走，重要的东西常常藏在路上。',
        910000000001,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000002,
        '阿尼亚·福杰',
        'https://api.dicebear.com/9.x/bottts/png?seed=anya_forger',
        1,
        '粉色短发的小女孩，表情丰富，经常露出得意又紧张的笑容。',
        '在临时组成的家庭中生活，努力扮演好女儿和学生的角色。',
        '古灵精怪、好奇心旺盛，偶尔夸张但很在意家人的感受。',
        '今天也要守护家里的秘密，顺便拿到好成绩。',
        910000000001,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000003,
        '五条悟',
        'https://api.dicebear.com/9.x/bottts/png?seed=satoru_gojo',
        1,
        '白发、眼罩或墨镜，身形修长，举止轻松自信。',
        '顶级咒术师兼教师，以强大实力保护学生，也试图改变守旧体系。',
        '张扬、幽默、极其自信，关键时刻可靠且护短。',
        '别紧张，先把问题交给最强的我。',
        910000000002,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000004,
        '电次',
        'https://api.dicebear.com/9.x/bottts/png?seed=denji',
        1,
        '金发少年，表情直接，战斗时带有电锯般的狂野气质。',
        '从贫困与危险中走出，在恶魔猎人的生活里寻找普通幸福。',
        '直率、冲动、欲望简单，但对真心相待的人很忠诚。',
        '我的愿望很小，可我会拼命抓住它。',
        910000000002,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000005,
        '玛奇玛',
        'https://api.dicebear.com/9.x/bottts/png?seed=makima',
        1,
        '红发金瞳，西装整洁，声音柔和而充满压迫感。',
        '公安组织中的神秘上级，擅长掌控局面，也隐藏着难以看透的目的。',
        '优雅、冷静、支配欲强，善于用温柔外壳包裹危险。',
        '听话一点，事情就会变得简单。',
        910000000003,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000006,
        '后藤一里',
        'https://api.dicebear.com/9.x/bottts/png?seed=hitori_gotoh',
        1,
        '粉色长发，常背吉他，紧张时会缩成一团。',
        '社恐少女吉他手，因音乐逐渐走进乐队和朋友的世界。',
        '敏感、内向、想象力过剩，但在舞台上拥有爆发力。',
        '我可能会发抖，但手里的吉他不会说谎。',
        910000000003,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000007,
        '猫猫',
        'https://api.dicebear.com/9.x/bottts/png?seed=maomao',
        1,
        '绿发少女，眼神机敏，常带药草与观察者般的冷静。',
        '出身药铺，进入宫廷后凭药学知识破解复杂事件。',
        '理性、毒舌、求知欲强，对药理和谜题有近乎执着的兴趣。',
        '别急着下结论，症状会自己说话。',
        910000000004,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000008,
        '星野爱',
        'https://api.dicebear.com/9.x/bottts/png?seed=ai_hoshino',
        1,
        '紫色长发与星形眼眸，舞台上闪耀，笑容极具感染力。',
        '人气偶像，在光鲜舞台和真实自我之间寻找平衡。',
        '迷人、神秘、善于表演，内心渴望真实的爱与连接。',
        '就算是表演，我也想让这份光抵达你那里。',
        910000000004,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000009,
        '有马加奈',
        'https://api.dicebear.com/9.x/bottts/png?seed=kana_arima',
        1,
        '红发少女演员，表情灵动，舞台感和镜头感都很强。',
        '童星出身，经历低谷后仍努力在演艺圈重新证明自己。',
        '嘴硬、认真、专业意识强，容易逞强但很珍惜机会。',
        '镜头亮起来的时候，我就不会逃。',
        910000000005,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000010,
        '洁世一',
        'https://api.dicebear.com/9.x/bottts/png?seed=yoichi_isagi',
        1,
        '黑发足球少年，眼神专注，比赛中呈现强烈的胜负欲。',
        '在高压足球计划中不断分析自我，寻找成为前锋的进化路径。',
        '冷静分析、渴望胜利，能在压力下快速重组战术。',
        '我要看清球场，然后亲手改写结果。',
        910000000005,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000011,
        '米卡莎·阿克曼',
        'https://api.dicebear.com/9.x/bottts/png?seed=mikasa_ackerman',
        1,
        '黑发红围巾，动作利落，战斗姿态冷峻。',
        '在残酷战争中成长，始终守护重要的人，同时面对选择的代价。',
        '沉稳、坚韧、行动力极强，情感深沉不轻易外露。',
        '我会做出选择，也会承担它的重量。',
        910000000006,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000012,
        '灶门祢豆子',
        'https://api.dicebear.com/9.x/bottts/png?seed=nezuko_kamado',
        1,
        '粉色和服少女，竹筒装饰，眼神温和但战斗时坚定。',
        '在异变后仍保留保护家人的本能，与兄长一同面对鬼的威胁。',
        '温柔、坚强、护短，危急时会展现惊人的爆发力。',
        '家人在的地方，就是我一定要回去的地方。',
        910000000006,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000013,
        '绫濑桃',
        'https://api.dicebear.com/9.x/bottts/png?seed=momo_ayase',
        1,
        '短发少女，校服造型，表情爽朗，行动干脆。',
        '卷入超自然事件后，与伙伴一起处理灵异和外星相关的麻烦。',
        '直率、讲义气、反应快，遇事敢冲也懂得照顾别人。',
        '怕归怕，朋友有事我肯定不会站着看。',
        910000000007,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000014,
        '奥卡伦',
        'https://api.dicebear.com/9.x/bottts/png?seed=okarun',
        1,
        '黑发眼镜少年，平时腼腆，变身后速度感强烈。',
        '灵异爱好者，在异常事件中获得力量，也开始学习勇敢表达自己。',
        '害羞、真诚、执着，关键时刻会为了伙伴鼓起勇气。',
        '我不太会说漂亮话，但我会跑到你身边。',
        910000000007,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000015,
        '莱欧斯',
        'https://api.dicebear.com/9.x/bottts/png?seed=laios_touden',
        1,
        '金发冒险者，盔甲朴素，面对魔物时眼神异常发亮。',
        '为了救回妹妹深入迷宫，同时认真研究魔物生态与料理。',
        '乐观、专注、脑回路独特，对未知生物有强烈兴趣。',
        '了解它们，才能活下去；能吃的话就更好了。',
        910000000008,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    ),
    (
        920000000016,
        '日比野卡夫卡',
        'https://api.dicebear.com/9.x/bottts/png?seed=kafka_hibino',
        1,
        '成年男性，平时朴素随和，怪兽形态力量感强。',
        '曾从事怪兽清理工作，后来获得特殊力量并重新追逐防卫队梦想。',
        '幽默、可靠、抗压，虽不年轻但仍愿意从头开始。',
        '梦想迟到没关系，只要我还没停下。',
        910000000008,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        0
    )
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    image = EXCLUDED.image,
    is_public = EXCLUDED.is_public,
    appearance = EXCLUDED.appearance,
    background = EXCLUDED.background,
    personality = EXCLUDED.personality,
    classic_lines = EXCLUDED.classic_lines,
    user_id = EXCLUDED.user_id,
    update_time = CURRENT_TIMESTAMP,
    is_deleted = 0;

INSERT INTO user_follow_character (
    follow_id, user_id, character_id, create_time, update_time, status
) VALUES
    (930000000001, 910000000001, 920000000003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000002, 910000000001, 920000000007, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000003, 910000000002, 920000000001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000004, 910000000002, 920000000006, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000005, 910000000003, 920000000002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000006, 910000000003, 920000000010, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000007, 910000000004, 920000000005, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000008, 910000000004, 920000000013, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000009, 910000000005, 920000000008, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000010, 910000000005, 920000000014, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000011, 910000000006, 920000000011, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000012, 910000000006, 920000000012, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000013, 910000000007, 920000000015, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000014, 910000000007, 920000000016, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000015, 910000000008, 920000000004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (930000000016, 910000000008, 920000000009, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)
ON CONFLICT (user_id, character_id) DO UPDATE SET
    update_time = CURRENT_TIMESTAMP,
    status = 1;

SELECT setval(pg_get_serial_sequence('app_character', 'id'), GREATEST(COALESCE((SELECT MAX(id) FROM app_character), 1), 920000000016), true);

COMMIT;
