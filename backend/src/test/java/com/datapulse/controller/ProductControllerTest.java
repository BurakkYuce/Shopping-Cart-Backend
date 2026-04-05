package com.datapulse.controller;

import com.datapulse.dto.response.ProductResponse;
import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.Product;
import com.datapulse.logging.LogEventPublisher;
import com.datapulse.security.JwtAuthenticationFilter;
import com.datapulse.security.JwtUtil;
import com.datapulse.security.UserDetailsServiceImpl;
import com.datapulse.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private ProductResponse sampleProduct(String id, String name, double price) {
        Product p = new Product();
        p.setId(id);
        p.setStoreId("store1");
        p.setCategoryId("cat1");
        p.setSku("SKU-" + id);
        p.setName(name);
        p.setUnitPrice(price);
        p.setDescription("A test product");
        return ProductResponse.from(p);
    }

    @Test
    void getProducts_returnsPagedList() throws Exception {
        List<ProductResponse> products = List.of(
                sampleProduct("p1", "Widget", 10.0),
                sampleProduct("p2", "Gadget", 20.0)
        );
        Page<ProductResponse> page = new PageImpl<>(products);

        when(productService.getProducts(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Widget"))
                .andExpect(jsonPath("$.content[1].name").value("Gadget"));
    }

    @Test
    void getProducts_withPagination_passesParams() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(sampleProduct("p1", "Widget", 10.0)));

        when(productService.getProducts(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getProductById_exists_returnsProduct() throws Exception {
        ProductResponse product = sampleProduct("p1", "Widget", 10.0);

        when(productService.getProductById("p1")).thenReturn(product);

        mockMvc.perform(get("/api/products/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.unitPrice").value(10.0));
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        when(productService.getProductById("nonexistent"))
                .thenThrow(new EntityNotFoundException("Product", "nonexistent"));

        mockMvc.perform(get("/api/products/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
