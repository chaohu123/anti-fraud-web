package com.xxx.antifraud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xxx.antifraud.common.BusinessException;
import com.xxx.antifraud.entity.Carousel;
import com.xxx.antifraud.mapper.CarouselMapper;
import com.xxx.antifraud.service.CarouselService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarouselServiceImpl implements CarouselService {

    private final CarouselMapper carouselMapper;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Override
    public List<Carousel> listEnabled() {
        LambdaQueryWrapper<Carousel> q = new LambdaQueryWrapper<>();
        q.eq(Carousel::getEnableFlag, 1).orderByAsc(Carousel::getSortOrder);
        return carouselMapper.selectList(q);
    }

    @Override
    public List<Carousel> listAll() {
        LambdaQueryWrapper<Carousel> q = new LambdaQueryWrapper<>();
        q.orderByAsc(Carousel::getSortOrder);
        return carouselMapper.selectList(q);
    }

    @Override
    public Long create(String imageUrl, String title, String linkUrl, Integer sortOrder) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BusinessException(400, "图片地址不能为空");
        }
        Carousel c = new Carousel();
        c.setImageUrl(imageUrl.trim());
        c.setTitle(title != null ? title.trim() : null);
        c.setLinkUrl(linkUrl != null ? linkUrl.trim() : null);
        c.setSortOrder(sortOrder != null ? sortOrder : 0);
        c.setEnableFlag(1);
        carouselMapper.insert(c);
        return c.getId();
    }

    @Override
    public void update(Long id, String imageUrl, String title, String linkUrl, Integer sortOrder, Integer enableFlag) {
        Carousel c = carouselMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(404, "轮播项不存在");
        }
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            c.setImageUrl(imageUrl.trim());
        }
        if (title != null) {
            c.setTitle(title.trim().isEmpty() ? null : title.trim());
        }
        if (linkUrl != null) {
            c.setLinkUrl(linkUrl.trim().isEmpty() ? null : linkUrl.trim());
        }
        if (sortOrder != null) {
            c.setSortOrder(sortOrder);
        }
        if (enableFlag != null) {
            c.setEnableFlag(enableFlag);
        }
        carouselMapper.updateById(c);
    }

    @Override
    public void delete(Long id) {
        carouselMapper.deleteById(id);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new BusinessException(400, "仅支持 jpg、png、gif、webp 格式图片");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String filename = "carousel_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path root = Paths.get(uploadPath.trim()).toAbsolutePath().normalize();
        Path dir = root.resolve("carousel");
        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(filename);
            file.transferTo(dest.toFile());
            return "/uploads/carousel/" + filename;
        } catch (IOException e) {
            log.warn("carousel upload failed", e);
            throw new BusinessException(500, "图片保存失败：" + e.getMessage());
        }
    }

    @Override
    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        List<String> urls = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                urls.add(uploadImage(file));
            }
        }
        return urls;
    }
}
