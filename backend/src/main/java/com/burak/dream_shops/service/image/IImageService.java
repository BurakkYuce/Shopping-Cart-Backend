package com.burak.dream_shops.service.image;
import com.burak.dream_shops.dto.ImageDto;
import com.burak.dream_shops.model.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IImageService {
    Image getImageById(Long id);
    void deleteImageById(Long id);
    List<ImageDto> saveImages(List<MultipartFile> files , Long product);
    void updateImage(MultipartFile file , Long imageId);

}
