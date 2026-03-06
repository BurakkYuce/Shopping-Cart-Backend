package com.burak.dream_shops.controller;

import com.burak.dream_shops.dto.ImageDto;
import com.burak.dream_shops.exceptions.ResourcesNotFoundException;
import com.burak.dream_shops.model.Image;
import com.burak.dream_shops.request.ApiResponse;
import com.burak.dream_shops.service.image.IImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/images")
public class ImageController {
private final IImageService imageService;
@PostMapping("/upload")
public ResponseEntity<ApiResponse> saveImages(List<MultipartFile> files , @RequestParam Long productId){
    try {
        List<ImageDto> imageDtos=imageService.saveImages(files,productId);
        return ResponseEntity.ok(new ApiResponse("Upload Succesful",imageDtos));
    } catch (Exception e) {
    return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Upload Failed!",e.getMessage()));
    }
}
    @GetMapping("/image/download/{imageId}")
    public ResponseEntity<Resource> downloadImage(@PathVariable long imageId) throws SQLException {
        try {
            Image image = imageService.getImageById(imageId);
            ByteArrayResource resource = new ByteArrayResource(image.getImage());
            String fileType = image.getFileType() != null ? image.getFileType() : "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(fileType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + image.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @PutMapping("image/{imageId}/update")
    public ResponseEntity<ApiResponse>upadateImage(@PathVariable Long imageId,@RequestBody MultipartFile file){
        try {
            Image image = imageService.getImageById(imageId);
            if(image != null){
                imageService.updateImage(file,imageId);
                return ResponseEntity.ok(new ApiResponse("Update success!",null));
            }
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Update failed!",INTERNAL_SERVER_ERROR));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Update Failed!",INTERNAL_SERVER_ERROR));
    }
    @DeleteMapping("image/{imageId}/delete")
    public ResponseEntity<ApiResponse>deleteImage(@PathVariable Long imageId){
        try {
            Image image = imageService.getImageById(imageId);
            if(image != null){
                imageService.deleteImageById(imageId);
                return ResponseEntity.ok(new ApiResponse("Delete success!",null));
            }
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse(e.getMessage(),INTERNAL_SERVER_ERROR));
        }
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new ApiResponse("Delete Failed!",INTERNAL_SERVER_ERROR));
    }


}

