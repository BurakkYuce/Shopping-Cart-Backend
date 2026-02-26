package com.burak.dream_shops.service.image;

import com.burak.dream_shops.dto.ImageDto;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Image;
import com.burak.dream_shops.model.Product;
import com.burak.dream_shops.repository.ImageRepository.ImageRepository;
import com.burak.dream_shops.service.product.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService  implements  IImageService{
    private  final ImageRepository imageRepository;
    private  final IProductService productService;


    @Override
    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(()->new ResourcesNotFoundException("No image found with id : "+id))
                ;
    }

    @Override
    public void deleteImageById(Long id) {
        imageRepository.findById(id).ifPresentOrElse(imageRepository::delete,()->{
            throw new ResourcesNotFoundException("No image found with id :"+id);
        });
    }

    @Override
    public List<ImageDto> saveImages(List<MultipartFile> files, Long productId) {
        Product product = productService.getProductById(productId);
        List<ImageDto> savedImageDto = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Image image = new Image();
                image.setFileType(file.getContentType());
                image.setFileName(file.getOriginalFilename());
                image.setImage(file.getBytes());
                image.setProduct(product);
                String buildDownloadUrl = "/api/v1/images/image/download/";
                image.setDownloadUrl(buildDownloadUrl + image.getId());
                Image savedImage = imageRepository.save(image);
                savedImage.setDownloadUrl(buildDownloadUrl + savedImage.getId());
                imageRepository.save(savedImage);
                ImageDto imageDto = new ImageDto();
                imageDto.setId(savedImage.getId());
                imageDto.setFileName(savedImage.getFileName());
                imageDto.setDownloadUrl(savedImage.getDownloadUrl());
                savedImageDto.add(imageDto);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return savedImageDto;
    }

    @Override
    public void updateImage(MultipartFile file, Long imageId) {
        Image image = getImageById(imageId);
        try{
            image.setFileType(file.getContentType());      // image/jpeg gibi
            image.setFileName(file.getOriginalFilename());
            image.setImage(file.getBytes());
            imageRepository.save(image);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
