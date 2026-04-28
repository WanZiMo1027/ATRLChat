package com.yuntian.chat_app.mapper.userMapper;

import com.yuntian.chat_app.vo.CharacterTagVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CharacterTagMapper {

    List<CharacterTagVo> selectAll();

    List<CharacterTagVo> selectHotTags(@Param("limit") Integer limit);

    List<CharacterTagVo> selectTagsByCharacterIds(@Param("characterIds") List<Long> characterIds);

    List<CharacterTagVo> selectByNames(@Param("names") List<String> names);

    Long upsertTag(@Param("name") String name, @Param("color") String color, @Param("sortOrder") Integer sortOrder);

    Integer selectMaxSortOrder();

    void deleteRelationsByCharacterId(@Param("characterId") Long characterId);

    void insertRelations(@Param("characterId") Long characterId, @Param("tagIds") List<Long> tagIds);
}
