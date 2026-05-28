package com.ramsai.kitchen.services;

import com.ramsai.kitchen.mappers.ProductMapper;
import com.ramsai.kitchen.models.dtos.ProductResponse;
import com.ramsai.kitchen.models.entities.Product;
import com.ramsai.kitchen.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Pizza Margherita")
                .basePrice(BigDecimal.valueOf(10.0))
                .approvalStatus(Product.ApprovalStatus.APPROVED)
                .isActive(true)
                .build();

        productResponse = new ProductResponse(
                1L,
                "Pizza Margherita",
                null,
                BigDecimal.valueOf(10.0),
                false,
                false,
                null,
                0.0,
                "Category",
                1L,
                "APPROVED",
                true,
                null
        );
    }

    @Test
    void updateProductActiveStatus_ApprovedSuccess() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        ProductResponse response = productService.updateProductActiveStatus(1L, false);

        assertNotNull(response);
        assertFalse(product.isActive());
        verify(productRepository).save(product);
    }

    @Test
    void updateProductActiveStatus_RejectedAndActivateThrowsException() {
        product.setApprovalStatus(Product.ApprovalStatus.REJECTED);
        product.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
                productService.updateProductActiveStatus(1L, true));

        assertEquals("Cannot activate a rejected product. It must be approved first.", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProductActiveStatus_RejectedAndDeactivateSuccess() {
        product.setApprovalStatus(Product.ApprovalStatus.REJECTED);
        product.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        ProductResponse response = productService.updateProductActiveStatus(1L, false);

        assertNotNull(response);
        assertFalse(product.isActive());
        verify(productRepository).save(product);
    }

    @Test
    void approveProduct_Success() {
        product.setApprovalStatus(Product.ApprovalStatus.REJECTED);
        product.setActive(false);
        product.setRejectionFeedback("Not good");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        ProductResponse response = productService.approveProduct(1L);

        assertNotNull(response);
        assertEquals(Product.ApprovalStatus.APPROVED, product.getApprovalStatus());
        assertTrue(product.isActive());
        assertNull(product.getRejectionFeedback());
        verify(productRepository).save(product);
    }

    @Test
    void rejectProduct_Success() {
        product.setApprovalStatus(Product.ApprovalStatus.PENDING);
        product.setActive(true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(any(Product.class))).thenReturn(productResponse);

        ProductResponse response = productService.rejectProduct(1L, "Needs more ingredients");

        assertNotNull(response);
        assertEquals(Product.ApprovalStatus.REJECTED, product.getApprovalStatus());
        assertFalse(product.isActive());
        assertEquals("Needs more ingredients", product.getRejectionFeedback());
        verify(productRepository).save(product);
    }
}
