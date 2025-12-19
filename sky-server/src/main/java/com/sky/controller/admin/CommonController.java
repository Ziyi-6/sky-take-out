package com.sky.controller.admin;
import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil; // 注入搬运工具

    /**
     * 文件上传
     * @param file 前端发来的文件包裹 (MultipartFile 是 Spring 专门接文件的类型)
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);

        try {
            // 1. 获取原始文件名 (比如: chicken.jpg)
            String originalFilename = file.getOriginalFilename();
            // 2. 截取后缀名 (比如: .jpg)
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 3. 构造新文件名 (使用 UUID，防止文件重名被覆盖)
            String objectName = UUID.randomUUID().toString() + extension;

            // 4. 调用工具类上传到 OSS，拿到图片的网络路径
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);

            return Result.success(filePath); // 把图片路径给前端，前端以后就能显示这张图了
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }

        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}