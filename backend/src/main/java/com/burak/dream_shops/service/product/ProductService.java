package com.burak.dream_shops.service.product;

import com.burak.dream_shops.dto.ImageDto;
import com.burak.dream_shops.dto.ProductDto;
import com.burak.dream_shops.exceptions.ProductNotFoundException;
import com.burak.dream_shops.model.Category;
import com.burak.dream_shops.model.Image;
import com.burak.dream_shops.model.Product;
import com.burak.dream_shops.repository.CategoryRepository.CategoryRepository;
import com.burak.dream_shops.repository.ImageRepository.ImageRepository;
import com.burak.dream_shops.repository.ProductRepository.ProductRepository;
import com.burak.dream_shops.request.AddProductRequest;
import com.burak.dream_shops.request.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Ürün (Product) işlemlerini yöneten servis sınıfı.
 * Ürün ekleme, güncelleme, silme, listeleme ve DTO dönüşümü işlemlerini gerçekleştirir.
 */
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final ModelMapper modelMapper;

    /**
     * Yeni ürün ekler.
     * Önce istekteki kategori adını DB'de arar:
     *   - Kategori varsa onu kullanır.
     *   - Yoksa otomatik yeni kategori oluşturur.
     * Ürünü oluşturup kaydeder. @Transactional: "Detached entity" hatasını önler.
     */
    @Override
    @Transactional
    public Product addProduct(AddProductRequest request) {
        Category category = Optional.ofNullable(categoryRepository.findByName(request.getCategory().getName()))
                .orElseGet(() -> {
                    Category newCategory = new Category(request.getCategory().getName());
                    return categoryRepository.save(newCategory);
                });
        request.setCategory(category);
        return productRepository.save(createProduct(request, category));
    }

    /**
     * AddProductRequest ve Category'den yeni bir Product nesnesi oluşturur (sadece nesne, kaydetmez).
     */
    private Product createProduct(AddProductRequest request, Category category) {
        return new Product(
                request.getName(),
                request.getBrand(),
                request.getPrice(),
                request.getInventory(),
                request.getDescription(),
                category
        );
    }

    /**
     * ID'ye göre ürün getirir. Bulunamazsa ProductNotFoundException fırlatır.
     */
    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }

    /**
     * ID'ye göre ürün siler. Bulunamazsa ProductNotFoundException fırlatır.
     */
    @Override
    public void deleteProductById(Long id) {
        productRepository.findById(id)
                .ifPresentOrElse(productRepository::delete,
                        () -> { throw new ProductNotFoundException("Product not found!"); });
    }

    /**
     * Var olan ürünü günceller.
     * Ürünü DB'den alır, updateExistingProduct ile alanları günceller, kaydeder.
     */
    @Override
    public Product updateProduct(ProductUpdateRequest request, Long productId) {
        return productRepository.findById(productId)
                .map(existingProduct -> updateExistingProduct(existingProduct, request))
                .map(productRepository::save)
                .orElseThrow(() -> new ProductNotFoundException("Product not found!"));
    }

    /**
     * Mevcut Product nesnesinin alanlarını günceller.
     * Kategori adına göre DB'den kategoriyi çekip set eder.
     */
    private Product updateExistingProduct(Product existingProduct, ProductUpdateRequest request) {
        existingProduct.setName(request.getName());
        existingProduct.setBrand(request.getBrand());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setInventory(request.getInventory());
        existingProduct.setDescription(request.getDescription());
        Category category = categoryRepository.findByName(request.getCategory().getName());
        existingProduct.setCategory(category);
        return existingProduct;
    }

    /** Tüm ürünleri listeler. */
    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /** Kategori adına göre ürünleri filtreler. */
    @Override
    public List<Product> getProductByCategory(String category) {
        return productRepository.findByCategoryName(category);
    }

    /** Kategori adı ve marka'ya göre ürünleri filtreler. */
    @Override
    public List<Product> getProductByCategoryAndBrand(String category, String brand) {
        return productRepository.findByCategoryNameAndBrand(category, brand);
    }

    /** Marka ve isme göre ürünleri filtreler. */
    @Override
    public List<Product> getProductByBrandAndName(String brand, String name) {
        return productRepository.findByBrandAndName(brand, name);
    }

    /** Belirli marka ve isme sahip ürünlerin toplam sayısını döner. */
    @Override
    public Long countProductsByBrandAndName(String brand, String name) {
        return productRepository.countByBrandAndName(brand, name);
    }

    /** Markaya göre ürünleri filtreler. */
    @Override
    public List<Product> getProductByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    /** İsme göre ürünleri filtreler. */
    @Override
    public List<Product> getProductByName(String name) {
        return productRepository.findByName(name);
    }

    /**
     * Product listesini ProductDto listesine dönüştürür.
     * Her ürün için convertToDto çağrılır.
     */
    @Override
    public List<ProductDto> getConvertedProducts(List<Product> products) {
        return products.stream().map(this::convertToDto).toList();
    }

    /**
     * Tek bir Product'ı ProductDto'ya dönüştürür.
     * ModelMapper ile temel alanları kopyalar, sonra imageRepository'den
     * ürüne ait görselleri çekip DTO'ya ekler.
     * (Lazy loading sorununu önlemek için görseller ayrıca sorgulanır.)
     */
    @Override
    public ProductDto convertToDto(Product product) {
        ProductDto productDto = modelMapper.map(product, ProductDto.class);
        List<Image> images = imageRepository.findByProductId(product.getId());
        List<ImageDto> imageDtos = images.stream()
                .map(image -> modelMapper.map(image, ImageDto.class))
                .toList();
        productDto.setImages(imageDtos);
        return productDto;
    }
}
