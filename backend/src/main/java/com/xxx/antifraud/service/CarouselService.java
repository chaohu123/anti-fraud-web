package com.xxx.antifraud.service;

import com.xxx.antifraud.entity.Carousel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 轮播图服务
 */
public interface CarouselService {

    /**
     * 前台：获取已启用的轮播列表（按 sort_order 升序）
     */
    List<Carousel> listEnabled();

    /**
     * 后台：获取全部轮播列表（按 sort_order 升序）
     */
    List<Carousel> listAll();

    /**
     * 新增轮播（图片地址为 URL 或本地上传返回的路径）
     */
    Long create(String imageUrl, String title, String linkUrl, Integer sortOrder);

    /**
     * 更新轮播
     */
    void update(Long id, String imageUrl, String title, String linkUrl, Integer sortOrder, Integer enableFlag);

    /**
     * 删除轮播
     */
    void delete(Long id);

    /**
     * 本地上传图片，保存到 uploads/carousel/，返回可访问路径如 /uploads/carousel/xxx.jpg
     */
    String uploadImage(MultipartFile file);

    /**
     * 批量本地上传图片，返回可访问路径列表
     */
    List<String> uploadImages(List<MultipartFile> files);
}
