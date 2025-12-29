package com.yuntian.chat_app.controller.usercontroller;


import com.yuntian.chat_app.entity.Character;
import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.service.userService.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/character")
public class CharacterController {

    @Autowired
    private  CharacterService characterService;

    @Autowired
    private FollowService followService;



    /**
     * 新增角色
     */
    @PostMapping("/add")
    public Result addCharacter(@RequestBody Character character){
        characterService.addCharacter(character);
        return Result.success(character.getId());
    }

    /**
     * 修改角色信息
     *
     */
    @PostMapping("/update")
    public Result updateCharacter(@RequestBody Character character){
        if (character.getId() == null) {
            return Result.error("角色ID不能为空");
        }
        characterService.updateCharacter(character);
        return Result.success(character.getId());
    }

    /**
     * 删除角色
     * @param characterId 角色ID
     * @return 操作结果
     */
    @GetMapping("/delete")
    public Result deleteCharacter(@RequestParam Long characterId){
        characterService.deleteCharacter(characterId);
        return Result.success(characterId);
    }

    /**
     * 检索角色
     * @param name 角色名称
     * @param personality 角色性格
     * @return 角色列表
     */
    @GetMapping("/search")
    public Result searchCharacter(@RequestParam(required = false) String name,
                                  @RequestParam(required = false) String personality){

        List<Character> characters = characterService.searchCharacter(name, personality);
        return Result.success(characters);

    }

    /**
     * 查询角色是否公开
     * @param characterId 角色ID
     * @return 角色公开状态
     */
    @GetMapping("/isPublicOrNot")
    public Result getCharacterIsPublic(@RequestParam Long characterId){
        Character characterById = characterService.getCharacterById(characterId);
        Integer isPublic = characterById.getIsPublic();
        return Result.success(isPublic);
    }

    /**
     * 公开角色或私密角色
     * @param characterId 角色ID
     * 0-不公开，1-公开
     * @return 操作结果
     */
    @PostMapping("/isPublic")
    public Result<Map<Integer,Long>> publicCharacter(@RequestParam Long characterId){

        Integer result =  characterService.publicOrNotCharacter(characterId);
        Map<Integer,Long> map = new HashMap<>();
        map.put(result,characterId);
        log.info("公开角色或私密角色，角色ID：{}，公开状态：{}", characterId, result);
        return Result.success(map);
    }


    /**
     * 我的模型角色列表
     * @return
     */
    @GetMapping("/my-list")
    public Result getCharacterList(){


        List<Character> characters = characterService.getCharacterList();

        // 构建符合要求的响应格式
        Map<String, Object> response = new HashMap<>();
        response.put("characters", characters);

        return Result.success(response);
    }

    /**角色广场
     *
     */
    @GetMapping("/square")
    public Result getCharacterSquare(){
        List<Character> characters = characterService.getPublicCharacter();
        return Result.success(characters);
    }

    /**
     * 获取角色详情
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    public Result getCharacterById(@PathVariable Long id) {
        Character character = characterService.getCharacterById(id);
        if (character == null) {
            return Result.error("角色不存在");
        }
        return Result.success(character);
    }

    /**
     * 关注角色
     * @param id 角色ID
     */
    @PostMapping("/follow")
    public Result followCharacter(@RequestParam Long id){
        Boolean followCharacter = followService.followCharacter(id);
        if (followCharacter){
            return Result.success("关注成功");
        }
        return Result.success("取消关注");
    }
    /**
     * 获取我的关注和创建的角色
     * @return
     */
    @GetMapping("/my-and-follow")
    public Result getMyCharacterAndFollow(){
        List<Character> characters = characterService.getMyCharacterAndFollow();
        return Result.success(characters);
    }

}
