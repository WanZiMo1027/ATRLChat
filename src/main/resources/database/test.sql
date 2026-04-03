-- PostgreSQL verification snippets

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

SELECT COUNT(*) AS app_user_count FROM app_user;
SELECT COUNT(*) AS app_character_count FROM app_character;
SELECT COUNT(*) AS chat_group_count FROM chat_group;
SELECT COUNT(*) AS private_chat_message_count FROM private_chat_message;
SELECT COUNT(*) AS memory_index_count FROM memory_index;

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'conversation_memories'
ORDER BY ordinal_position;

SELECT embedding_id, text, metadata
FROM conversation_memories
LIMIT 3;
