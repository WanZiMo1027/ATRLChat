package com.yuntian.chat_app.controller.usercontroller;

import com.yuntian.chat_app.result.Result;
import com.yuntian.chat_app.service.userService.CharacterService;
import com.yuntian.chat_app.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/user/common")
@Slf4j
public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private CharacterService characterService;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}", file.getOriginalFilename());
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            log.info("文件上传成功：{}", filePath);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }
        return Result.error("文件上传失败");
    }

    /**
     * 角色头像上传（专门接口）
     * @param file 头像文件
     * @param characterId 角色ID
     * @return 上传结果
     */
    @PostMapping("/uploadCharacterAvatar")
    public Result<String> uploadCharacterAvatar(@RequestParam("file") MultipartFile file,
                                                @RequestParam("characterId") Long characterId) {
        log.info("角色头像上传，角色ID：{}，文件名：{}", characterId, file.getOriginalFilename());

        try {
            // 验证文件类型
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!".jpg".equals(extension) && !".jpeg".equals(extension) &&
                    !".png".equals(extension) && !".webp".equals(extension)) {
                return Result.error("只支持jpg、jpeg、png、webp格式的图片");
            }

            // 验证文件大小（最大2MB）
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.error("图片大小不能超过2MB");
            }

            // 生成OSS对象名：character_avatars/{角色ID}_{时间戳}{后缀}
            String objectName = String.format("character_avatars/%d_%d%s",
                    characterId, System.currentTimeMillis(), extension);

            // 上传到OSS
            String imageUrl = aliOssUtil.upload(file.getBytes(), objectName);

            // 新增：更新数据库中的角色头像URL
            characterService.updateCharacterAvatar(characterId, imageUrl);

            log.info("角色头像上传成功，角色ID：{}，URL：{}", characterId, imageUrl);
            return Result.success(imageUrl);

        } catch (IOException e) {
            log.error("角色头像上传失败：{}", e.getMessage(), e);
            return Result.error("头像上传失败");
        } catch (Exception e) {
            log.error("更新角色头像URL失败：{}", e.getMessage(), e);
            return Result.error("头像上传成功但保存失败");
        }
    }
}