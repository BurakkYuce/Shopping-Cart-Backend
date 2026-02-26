package com.burak.dream_shops.service.product;

import com.burak.dream_shops.dto.ProductDto;
import com.burak.dream_shops.model.Product;
import com.burak.dream_shops.request.AddProductRequest;
import com.burak.dream_shops.request.ProductUpdateRequest;

import java.util.List;

public interface IProductService {
Product addProduct(AddProductRequest request);
Product getProductById(Long id);
void deleteProductById(Long id);
Product updateProduct(ProductUpdateRequest product, Long productId);
List<Product> getAllProducts();
List<Product>getProductByCategory(String category);
List<Product>getProductByCategoryAndBrand(String category,String brand);
List<Product>getProductByBrandAndName(String brand,String name);
Long countProductsByBrandAndName(String brand,String name);
List<Product> getProductByBrand(String brand);
List<Product>getProductByName(String name);

    List<ProductDto>getConvertedProducts(List<Product> products);

    ProductDto convertToDto(Product product);
}
