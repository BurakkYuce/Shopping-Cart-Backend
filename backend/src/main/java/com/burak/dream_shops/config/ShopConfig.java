package com.burak.dream_shops.config;

import com.burak.dream_shops.dto.ProductDto;
import com.burak.dream_shops.model.Product;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShopConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.typeMap(Product.class, ProductDto.class)
                .addMappings(mapper -> mapper.skip(ProductDto::setImages));
        return modelMapper;
    }
}
