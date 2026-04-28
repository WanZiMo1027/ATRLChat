package com.yuntian.chat_app.service.userService.userServiceImpl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yuntian.chat_app.context.BaseContext;
import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.exception.CharacterException;
import com.yuntian.chat_app.exception.UserException;
import com.yuntian.chat_app.mapper.userMapper.CharacterMapper;
import com.yuntian.chat_app.mapper.userMapper.CharacterTagMapper;
import com.yuntian.chat_app.mapper.userMapper.UserFollowCharacterMapper;
import com.yuntian.chat_app.result.PageResult;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.service.userService.FollowService;
import com.yuntian.chat_app.vo.CharacterFollowVo;
import com.yuntian.chat_app.vo.CharacterSquareItemVo;
import com.yuntian.chat_app.vo.CharacterSquareOverviewVo;
import com.yuntian.chat_app.vo.CharacterTagVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CharacterServiceImpl implements CharacterService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private CharacterTagMapper characterTagMapper;

    @Autowired
    private FollowService followService;

    @Autowired
    private UserFollowCharacterMapper userFollowCharacterMapper;

    private static final String CHARACTER_DETAIL_KEY = "character:detail:";
    private static final String CHARACTER_LIST_KEY = "character:list:user:";
    private static final String CHARACTER_SQUARE_KEY = "character:list:public";
    private static final String CHARACTER_SQUARE_PAGE_KEY_PREFIX = "character:square:page:";
    private static final String CHARACTER_SQUARE_OVERVIEW_KEY_PREFIX = "character:square:overview:user:";
    private static final String CHARACTER_TAGS_KEY = "character:tags:all";
    private static final String FOLLOW_LIST_KEY = "follow:list:";
    private static final String FOLLOW_RANK_KEY_PREFIX = "follow:rank:";

    private static final long CACHE_TTL_DAYS = 7;
    private static final long TAG_CACHE_TTL_HOURS = 2;
    private static final long OVERVIEW_CACHE_TTL_MINUTES = 10;
    private static final int TAG_NAME_MAX_LENGTH = 50;
    private static final String[] TAG_COLOR_PALETTE = {
            "#2ec4b6", "#3b82f6", "#f97316", "#60a5fa", "#8b5cf6",
            "#ec4899", "#a78bfa", "#ef4444", "#14b8a6", "#6366f1"
    };

    @Override
    @Transactional
    public void addCharacter(Character character) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new UserException(UserException.SESSION_EXPIRED, "用户会话过期");
        }

        character.setUserId(currentUserId);
        character.setIsPublic(0);
        int result = characterMapper.insert(character);
        if (result <= 0) {
            throw new CharacterException(CharacterException.CHARACTER_CREATE_DATABASE_ERROR, "创建角色失败");
        }

        syncCharacterTags(character.getId(), character.getTagIds(), character.getTagNames());
        attachTagsToCharacter(character);
        updateCharacterDetailCache(character);
        evictUserCharacterListCache(currentUserId);
        evictSquareCaches();
    }

    @Override
    public void updateCharacterAvatar(Long characterId, String imageUrl) {
        Character patch = new Character();
        patch.setId(characterId);
        patch.setImage(imageUrl);
        characterMapper.updateById(patch);

        Character character = characterMapper.selectById(characterId);
        if (character != null) {
            character.setImage(imageUrl);
            attachTagsToCharacter(character);
            updateCharacterDetailCache(character);
            evictUserCharacterListCache(character.getUserId());
        }
        evictFollowCaches(characterId);
        evictSquareCaches();
    }

    @Override
    public List<Character> getCharacterList() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new UserException(UserException.SESSION_EXPIRED, "用户会话过期");
        }

        String key = CHARACTER_LIST_KEY + currentUserId;
        String characterListJson = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(characterListJson)) {
            try {
                JSONArray array = JSONUtil.parseArray(characterListJson);
                List<Character> cachedCharacters = JSONUtil.toList(array, Character.class);
                if (cachedCharacters.stream().anyMatch(character -> character != null && character.getTags() == null)) {
                    attachTagsToCharacters(cachedCharacters);
                }
                return cachedCharacters;
            } catch (JSONException ex) {
                log.warn("角色列表缓存无效, key={}", key, ex);
            }
        }

        List<Character> characters = characterMapper.selectByUserId(currentUserId);
        attachTagsToCharacters(characters);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(characters), CACHE_TTL_DAYS, TimeUnit.DAYS);
        return characters;
    }

    @Override
    public Character getCharacterById(Long id) {
        String characterKey = CHARACTER_DETAIL_KEY + id;
        String characterJson = stringRedisTemplate.opsForValue().get(characterKey);
        if (StrUtil.isNotBlank(characterJson)) {
            Character cachedCharacter = JSONUtil.toBean(characterJson, Character.class);
            if (cachedCharacter != null && cachedCharacter.getTags() == null) {
                attachTagsToCharacter(cachedCharacter);
                updateCharacterDetailCache(cachedCharacter);
            }
            return cachedCharacter;
        }

        Character character = characterMapper.selectById(id);
        if (character != null) {
            attachTagsToCharacter(character);
            stringRedisTemplate.opsForValue().set(characterKey, JSONUtil.toJsonStr(character), CACHE_TTL_DAYS, TimeUnit.DAYS);
        }
        return character;
    }

    @Override
    public List<Character> getPublicCharacter() {
        String characterSquareJson = stringRedisTemplate.opsForValue().get(CHARACTER_SQUARE_KEY);
        if (StrUtil.isNotBlank(characterSquareJson)) {
            try {
                JSONArray array = JSONUtil.parseArray(characterSquareJson);
                List<Character> cachedCharacters = JSONUtil.toList(array, Character.class);
                if (cachedCharacters.stream().anyMatch(character -> character != null && character.getTags() == null)) {
                    attachTagsToCharacters(cachedCharacters);
                }
                return cachedCharacters;
            } catch (JSONException ex) {
                log.warn("公开角色缓存无效, key={}", CHARACTER_SQUARE_KEY, ex);
            }
        }

        List<Character> characters = characterMapper.selectAll();
        attachTagsToCharacters(characters);
        stringRedisTemplate.opsForValue().set(CHARACTER_SQUARE_KEY, JSONUtil.toJsonStr(characters), CACHE_TTL_DAYS, TimeUnit.DAYS);
        return characters;
    }

    @Override
    public List<Character> searchCharacter(String name, String personality) {
        return characterMapper.selectByKeyword(name, personality);
    }

    @Override
    public Integer publicOrNotCharacter(Long characterId) {
        Character character = characterMapper.selectById(characterId);
        if (character == null) {
            throw new CharacterException(CharacterException.CHARACTER_ERROR, "角色不存在");
        }

        Integer newStatus = character.getIsPublic() == 0 ? 1 : 0;
        character.setIsPublic(newStatus);
        characterMapper.updateIsPublic(characterId, newStatus);

        attachTagsToCharacter(character);
        updateCharacterDetailCache(character);
        evictUserCharacterListCache(character.getUserId());
        evictSquareCaches();
        return newStatus;
    }

    @Override
    @Transactional
    public void updateCharacter(Character character) {
        Character existingCharacter = characterMapper.selectById(character.getId());
        if (existingCharacter == null) {
            throw new CharacterException(CharacterException.CHARACTER_ERROR, "角色不存在");
        }

        characterMapper.updateInfoById(character);
        syncCharacterTags(character.getId(), character.getTagIds(), character.getTagNames());

        Character updated = characterMapper.selectById(character.getId());
        attachTagsToCharacter(updated);
        updateCharacterDetailCache(updated);
        evictUserCharacterListCache(existingCharacter.getUserId());
        evictFollowCaches(character.getId());
        evictSquareCaches();
    }

    @Override
    public void deleteCharacter(Long characterId) {
        Character character = characterMapper.selectById(characterId);
        if (character == null) {
            throw new CharacterException(CharacterException.CHARACTER_ERROR, "角色不存在");
        }

        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null || !character.getUserId().equals(currentUserId)) {
            throw new RuntimeException("没有权限删除该角色");
        }

        characterMapper.deleteById(characterId);
        deleteCharacterCaches(character);
        evictSquareCaches();
    }

    @Override
    public List<Character> getMyCharacterAndFollow() {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            throw new UserException(UserException.SESSION_EXPIRED, "用户会话过期");
        }

        List<Character> myCharacters = getCharacterList();
        List<CharacterFollowVo> followList = followService.getFollowList(currentUserId);

        Map<Long, Character> merged = new LinkedHashMap<>();
        if (myCharacters != null) {
            for (Character c : myCharacters) {
                if (c != null && c.getId() != null) {
                    merged.put(c.getId(), c);
                }
            }
        }

        if (followList != null) {
            for (CharacterFollowVo vo : followList) {
                if (vo == null || vo.getId() == null) {
                    continue;
                }
                Character c = new Character();
                c.setId(vo.getId());
                c.setName(vo.getName());
                c.setImage(vo.getImage());
                c.setAppearance(vo.getAppearance());
                c.setBackground(vo.getBackground());
                c.setIsPublic(1);
                merged.putIfAbsent(c.getId(), c);
            }
        }

        return new ArrayList<>(merged.values());
    }

    @Override
    public PageResult getCharacterSquarePage(Integer page, Integer pageSize, String keyword, List<Long> tagIds, String tagKeyword, String tab) {
        int safePage = page == null || page <= 0 ? 1 : page;
        int safePageSize = pageSize == null || pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        String safeTab = normalizeTab(tab);
        List<Long> safeTagIds = normalizeTagIds(tagIds);
        Long currentUserId = BaseContext.getCurrentId();
        String safeKeyword = StrUtil.isBlank(keyword) ? null : keyword.trim();
        String safeTagKeyword = StrUtil.isBlank(tagKeyword) ? null : tagKeyword.trim();
        String pageCacheKey = buildSquarePageCacheKey(
                currentUserId,
                safePage,
                safePageSize,
                safeKeyword,
                safeTagIds,
                safeTagKeyword,
                safeTab
        );
        String pageJson = stringRedisTemplate.opsForValue().get(pageCacheKey);
        if (StrUtil.isNotBlank(pageJson)) {
            try {
                return JSONUtil.toBean(pageJson, PageResult.class);
            } catch (JSONException ex) {
                log.warn("公开角色缓存无效, key={}", pageCacheKey, ex);
                stringRedisTemplate.delete(pageCacheKey);
            }
        }

        PageHelper.startPage(safePage, safePageSize);
        List<CharacterSquareItemVo> items = characterMapper.selectSquarePage(
                safeKeyword,
                safeTagIds,
                safeTagIds.size(),
                safeTagKeyword,
                safeTab,
                currentUserId
        );
        Page<CharacterSquareItemVo> pageInfo = (Page<CharacterSquareItemVo>) items;
        attachTags(items);
        PageResult result = new PageResult(pageInfo.getTotal(), items);
        stringRedisTemplate.opsForValue().set(pageCacheKey, JSONUtil.toJsonStr(result), OVERVIEW_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public CharacterSquareOverviewVo getCharacterSquareOverview() {
        Long currentUserId = BaseContext.getCurrentId();
        String overviewKey = CHARACTER_SQUARE_OVERVIEW_KEY_PREFIX + (currentUserId == null ? "anonymous" : currentUserId);
        String overviewJson = stringRedisTemplate.opsForValue().get(overviewKey);
        if (StrUtil.isNotBlank(overviewJson)) {
            try {
                return JSONUtil.toBean(overviewJson, CharacterSquareOverviewVo.class);
            } catch (JSONException ex) {
                log.warn("公开角色缓存无效, key={}", overviewKey, ex);
                stringRedisTemplate.delete(overviewKey);
            }
        }

        CharacterSquareOverviewVo overview = new CharacterSquareOverviewVo();
        overview.setPublicCharacterTotal(nullToZero(characterMapper.countPublicCharacters()));
        overview.setTodayNewCount(nullToZero(characterMapper.countTodayPublicCharacters()));
        overview.setHotTags(characterTagMapper.selectHotTags(5));

        List<CharacterSquareItemVo> active = characterMapper.selectSquareTop(currentUserId, 5);
        attachTags(active);
        overview.setRecentActive(active);

        List<CharacterSquareItemVo> featured = characterMapper.selectSquareTop(currentUserId, 3);
        attachTags(featured);
        overview.setFeaturedCharacters(featured);

        stringRedisTemplate.opsForValue().set(
                overviewKey,
                JSONUtil.toJsonStr(overview),
                OVERVIEW_CACHE_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        return overview;
    }

    @Override
    public List<CharacterTagVo> getCharacterTags() {
        String tagJson = stringRedisTemplate.opsForValue().get(CHARACTER_TAGS_KEY);
        if (StrUtil.isNotBlank(tagJson)) {
            try {
                JSONArray array = JSONUtil.parseArray(tagJson);
                return JSONUtil.toList(array, CharacterTagVo.class);
            } catch (JSONException ex) {
                log.warn("公开角色缓存无效, key={}", CHARACTER_TAGS_KEY, ex);
            }
        }

        List<CharacterTagVo> tags = characterTagMapper.selectAll();
        stringRedisTemplate.opsForValue().set(CHARACTER_TAGS_KEY, JSONUtil.toJsonStr(tags), TAG_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return tags;
    }

    private void syncCharacterTags(Long characterId, List<Long> tagIds, List<String> tagNames) {
        if (tagIds == null && tagNames == null) {
            return;
        }

        List<Long> safeTagIds = resolveTagIds(tagIds, tagNames);
        characterTagMapper.deleteRelationsByCharacterId(characterId);
        if (!safeTagIds.isEmpty()) {
            characterTagMapper.insertRelations(characterId, safeTagIds);
        }
        evictTagCaches();
    }

    private void attachTags(List<CharacterSquareItemVo> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> characterIds = items.stream()
                .map(CharacterSquareItemVo::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (characterIds.isEmpty()) {
            return;
        }

        Map<Long, List<CharacterTagVo>> tagsByCharacterId = characterTagMapper.selectTagsByCharacterIds(characterIds)
                .stream()
                .collect(Collectors.groupingBy(CharacterTagVo::getCharacterId, LinkedHashMap::new, Collectors.toList()));

        for (CharacterSquareItemVo item : items) {
            List<CharacterTagVo> tags = tagsByCharacterId.get(item.getId());
            item.setTags(tags == null ? new ArrayList<>() : tags);
        }
    }

    private void attachTagsToCharacter(Character character) {
        if (character == null || character.getId() == null) {
            return;
        }
        List<CharacterTagVo> tags = characterTagMapper.selectTagsByCharacterIds(List.of(character.getId()));
        character.setTags(tags == null ? new ArrayList<>() : tags);
        character.setTagIds(character.getTags().stream().map(CharacterTagVo::getId).collect(Collectors.toList()));
    }

    private void attachTagsToCharacters(List<Character> characters) {
        if (characters == null || characters.isEmpty()) {
            return;
        }
        List<Long> characterIds = characters.stream()
                .filter(character -> character != null)
                .map(Character::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (characterIds.isEmpty()) {
            return;
        }
        Map<Long, List<CharacterTagVo>> tagsByCharacterId = characterTagMapper.selectTagsByCharacterIds(characterIds)
                .stream()
                .collect(Collectors.groupingBy(CharacterTagVo::getCharacterId, LinkedHashMap::new, Collectors.toList()));

        for (Character character : characters) {
            if (character == null) {
                continue;
            }
            List<CharacterTagVo> tags = tagsByCharacterId.get(character.getId());
            character.setTags(tags == null ? new ArrayList<>() : tags);
            character.setTagIds(character.getTags().stream().map(CharacterTagVo::getId).collect(Collectors.toList()));
        }
    }

    private List<Long> resolveTagIds(List<Long> tagIds, List<String> tagNames) {
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(normalizeTagIds(tagIds));
        List<String> safeTagNames = normalizeTagNames(tagNames);
        if (safeTagNames.isEmpty()) {
            return new ArrayList<>(uniqueIds);
        }

        Map<String, CharacterTagVo> existingByName = characterTagMapper.selectByNames(safeTagNames)
                .stream()
                .collect(Collectors.toMap(CharacterTagVo::getName, tag -> tag, (left, right) -> left, LinkedHashMap::new));

        Integer maxSortOrder = characterTagMapper.selectMaxSortOrder();
        int nextSortOrder = (maxSortOrder == null ? 0 : maxSortOrder) + 10;
        for (String tagName : safeTagNames) {
            CharacterTagVo existing = existingByName.get(tagName);
            if (existing != null && existing.getId() != null) {
                uniqueIds.add(existing.getId());
                continue;
            }
            Long tagId = characterTagMapper.upsertTag(tagName, pickTagColor(tagName), nextSortOrder);
            nextSortOrder += 10;
            if (tagId != null) {
                uniqueIds.add(tagId);
            }
        }

        return new ArrayList<>(uniqueIds);
    }

    private List<String> normalizeTagNames(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> uniqueNames = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (StrUtil.isBlank(tagName)) {
                continue;
            }
            String normalized = tagName.trim();
            if (normalized.length() > TAG_NAME_MAX_LENGTH) {
                throw new CharacterException(CharacterException.CHARACTER_CREATE_ERROR, "标签名称不能超过50个字符");
            }
            uniqueNames.add(normalized);
        }
        return new ArrayList<>(uniqueNames);
    }

    private String pickTagColor(String tagName) {
        return TAG_COLOR_PALETTE[Math.floorMod(tagName.hashCode(), TAG_COLOR_PALETTE.length)];
    }

    private List<Long> normalizeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId != null && tagId > 0) {
                uniqueIds.add(tagId);
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private String normalizeTab(String tab) {
        if (StrUtil.isBlank(tab)) {
            return "all";
        }
        String normalized = tab.trim();
        if (List.of("all", "recommend", "active", "mostFollowed", "followed").contains(normalized)) {
            return normalized;
        }
        return "all";
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String buildSquarePageCacheKey(Long userId,
                                           Integer page,
                                           Integer pageSize,
                                           String keyword,
                                           List<Long> tagIds,
                                           String tagKeyword,
                                           String tab) {
        String userPart = userId == null ? "anonymous" : String.valueOf(userId);
        String tagPart = tagIds == null || tagIds.isEmpty()
                ? "none"
                : tagIds.stream().map(String::valueOf).collect(Collectors.joining("-"));
        String raw = String.join("|",
                userPart,
                String.valueOf(page),
                String.valueOf(pageSize),
                keyword == null ? "" : keyword,
                tagPart,
                tagKeyword == null ? "" : tagKeyword,
                tab == null ? "all" : tab
        );
        return CHARACTER_SQUARE_PAGE_KEY_PREFIX + Integer.toHexString(raw.hashCode());
    }

    private void evictUserCharacterListCache(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.delete(CHARACTER_LIST_KEY + userId);
    }

    private void evictFollowCaches(Long characterId) {
        evictFollowListCaches(characterId);
        evictFollowRankCaches();
    }

    private void evictFollowListCaches(Long characterId) {
        try {
            List<Long> followerUserIds = userFollowCharacterMapper.selectFollowerUserIdsByCharacterId(characterId);
            if (followerUserIds == null || followerUserIds.isEmpty()) {
                return;
            }

            for (Long followerUserId : followerUserIds) {
                stringRedisTemplate.delete(FOLLOW_LIST_KEY + followerUserId);
            }
        } catch (Exception e) {
            log.warn("删除关注列表缓存失败, characterId={}", characterId, e);
        }
    }

    private void evictFollowRankCaches() {
        try {
            var rankKeys = stringRedisTemplate.keys(FOLLOW_RANK_KEY_PREFIX + "*");
            if (rankKeys != null && !rankKeys.isEmpty()) {
                stringRedisTemplate.delete(rankKeys);
            }
        } catch (Exception e) {
            log.warn("删除关注排名缓存失败", e);
        }
    }

    private void updateCharacterDetailCache(Character character) {
        if (character == null || character.getId() == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(CHARACTER_DETAIL_KEY + character.getId(), JSONUtil.toJsonStr(character), CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    private void deleteCharacterCaches(Character character) {
        if (character == null || character.getId() == null) {
            return;
        }
        stringRedisTemplate.delete(CHARACTER_DETAIL_KEY + character.getId());
        evictUserCharacterListCache(character.getUserId());
        evictFollowCaches(character.getId());
    }

    private void evictSquareCaches() {
        stringRedisTemplate.delete(CHARACTER_SQUARE_KEY);
        evictSquarePageCaches();
        evictSquareOverviewCaches();
    }

    private void evictTagCaches() {
        stringRedisTemplate.delete(CHARACTER_TAGS_KEY);
        evictSquarePageCaches();
        evictSquareOverviewCaches();
    }

    private void evictSquarePageCaches() {
        try {
            var pageKeys = stringRedisTemplate.keys(CHARACTER_SQUARE_PAGE_KEY_PREFIX + "*");
            if (pageKeys != null && !pageKeys.isEmpty()) {
                Long deletedCount = stringRedisTemplate.delete(pageKeys);
                log.info("删除公开角色缓存, count={}", deletedCount);
            }
        } catch (Exception e) {
            log.warn("删除公开角色缓存失败", e);
        }
    }

    private void evictSquareOverviewCaches() {
        try {
            var overviewKeys = stringRedisTemplate.keys(CHARACTER_SQUARE_OVERVIEW_KEY_PREFIX + "*");
            if (overviewKeys != null && !overviewKeys.isEmpty()) {
                Long deletedCount = stringRedisTemplate.delete(overviewKeys);
                log.info("删除公开角色缓存, count={}", deletedCount);
            }
        } catch (Exception e) {
            log.warn("删除公开角色缓存失败", e);
        }
    }
}
