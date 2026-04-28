package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.PageResult;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.service.userService.FollowService;
import com.yuntian.chat_app.vo.CharacterSquareOverviewVo;
import com.yuntian.chat_app.vo.CharacterTagVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/character")
@Tag(name = "Character API", description = "角色相关接口，包括角色创建、更新、分页查询、关注、详情查询、公开角色列表查询、公开角色标签查询")
public class CharacterController {

    @Autowired
    private CharacterService characterService;

    @Autowired
    private FollowService followService;

    @PostMapping("/add")
    @Operation(summary = "Create character", description = "新增角色")
    public Result<Long> addCharacter(@RequestBody Character character) {
        characterService.addCharacter(character);
        return Result.success(character.getId());
    }

    @PostMapping("/update")
    @Operation(summary = "Update character", description = "更新角色信息")
    public Result<Long> updateCharacter(@RequestBody Character character) {
        if (character.getId() == null) {
            return Result.error("角色ID不能为空");
        }
        characterService.updateCharacter(character);
        return Result.success(character.getId());
    }

    @GetMapping("/delete")
    @Operation(summary = "Delete character", description = "删除角色信息")
    public Result<Long> deleteCharacter(@RequestParam Long characterId) {
        characterService.deleteCharacter(characterId);
        return Result.success(characterId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search characters", description = "检索角色信息")
    public Result<List<Character>> searchCharacter(@RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String personality) {
        List<Character> characters = characterService.searchCharacter(name, personality);
        return Result.success(characters);
    }

    @GetMapping("/isPublicOrNot")
    @Operation(summary = "获取角色公开状态", description = "获取角色是否公开状态")
    public Result<Integer> getCharacterIsPublic(@RequestParam Long characterId) {
        Character characterById = characterService.getCharacterById(characterId);
        if (characterById == null) {
            return Result.error("角色不存在");
        }
        return Result.success(characterById.getIsPublic());
    }

    @PostMapping("/isPublic")
    @Operation(summary = "切换角色公开状态", description = "切换角色公开状态")
    public Result<Map<Integer, Long>> publicCharacter(@RequestParam Long characterId) {
        Integer result = characterService.publicOrNotCharacter(characterId);
        Map<Integer, Long> map = new HashMap<>();
        map.put(result, characterId);
        log.info("切换角色公开状态, characterId={}, status={}", characterId, result);
        return Result.success(map);
    }

    @GetMapping("/my-list")
    @Operation(summary = "我的角色列表", description = "获取当前用户创建的角色列表，包含公开状态")
    public Result<Map<String, Object>> getCharacterList() {
        List<Character> characters = characterService.getCharacterList();
        Map<String, Object> response = new HashMap<>();
        response.put("characters", characters);
        return Result.success(response);
    }

    @GetMapping("/square")
    @Operation(summary = "公开角色列表", description = "获取公开角色列表，包含公开状态")
    public Result<List<Character>> getCharacterSquare() {
        List<Character> characters = characterService.getPublicCharacter();
        return Result.success(characters);
    }

    @GetMapping("/square/page")
    @Operation(summary = "公开角色列表分页查询", description = "分页查询公开角色列表，包含关键词、标签、分页标签筛选")
    public Result<PageResult> getCharacterSquarePage(@RequestParam(required = false, defaultValue = "1") Integer page,
                                                     @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String tagIds,
                                                     @RequestParam(required = false) String tagKeyword,
                                                     @RequestParam(required = false, defaultValue = "all") String tab) {
        PageResult result = characterService.getCharacterSquarePage(page, pageSize, keyword, parseTagIds(tagIds), tagKeyword, tab);
        return Result.success(result);
    }

    @GetMapping("/square/overview")
    @Operation(summary = "公开角色列表总览", description = "获取公开角色列表总览，包含角色数量、热门标签、排名角色块")
    public Result<CharacterSquareOverviewVo> getCharacterSquareOverview() {
        return Result.success(characterService.getCharacterSquareOverview());
    }

    @GetMapping("/tags")
    @Operation(summary = "公开角色标签列表", description = "获取公开角色标签列表")
    public Result<List<CharacterTagVo>> getCharacterTags() {
        return Result.success(characterService.getCharacterTags());
    }

    @GetMapping("/{id}")
    @Operation(summary = "角色详情", description = "根据角色ID获取角色详情")
    public Result<Character> getCharacterById(@PathVariable Long id) {
        Character character = characterService.getCharacterById(id);
        if (character == null) {
            return Result.error("角色不存在");
        }
        return Result.success(character);
    }

    @PostMapping("/follow")
    @Operation(summary = "关注角色", description = "切换当前用户关注角色状态")
    public Result<String> followCharacter(@RequestParam Long id) {
        Boolean followCharacter = followService.followCharacter(id);
        if (followCharacter) {
            return Result.success("Followed");
        }
        return Result.success("Unfollowed");
    }

    @GetMapping("/my-and-follow")
    @Operation(summary = "我的关注和创建的角色列表", description = "获取当前用户关注和创建的角色列表，包含公开状态")
    public Result<List<Character>> getMyCharacterAndFollow() {
        List<Character> characters = characterService.getMyCharacterAndFollow();
        return Result.success(characters);
    }

    private List<Long> parseTagIds(String tagIds) {
        if (tagIds == null || tagIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
