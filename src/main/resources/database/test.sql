\d langchain4j_embedding
-- 或者
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'langchain4j_embedding';

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'conversation_memories'
ORDER BY ordinal_position;

SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

SELECT embedding_id, text, metadata
FROM conversation_memories
LIMIT 3;