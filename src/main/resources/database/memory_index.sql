CREATE TABLE IF NOT EXISTS `memory_index` (
    `memory_id` varchar(128) NOT NULL COMMENT 'session id',
    `memory_key` varchar(128) NOT NULL COMMENT 'memory key',
    `embedding_id` varchar(255) NOT NULL COMMENT 'embedding record id',
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT 'last update time',
    PRIMARY KEY (`memory_id`, `memory_key`),
    KEY `idx_embedding_id` (`embedding_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='memory index table';
