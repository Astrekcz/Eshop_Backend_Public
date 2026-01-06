// src/test/java/org/example/zeniqbackend/service/CatalogServiceTest.java
package org.example.eshopbackend.serviceTest;
import org.example.eshopbackend.dto.ProductResponseDTO;
import org.example.eshopbackend.entity.Category;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.mapper.ProductMapper;
import org.example.eshopbackend.repository.CategoryRepository;
import org.example.eshopbackend.repository.ImageRepository;
import org.example.eshopbackend.repository.ProductRepository;
import org.example.eshopbackend.service.CatalogService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private CatalogService catalogService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    private static Product makeProduct(Long id, String slug, String name) {
        Product p = new Product();
        p.setProductId(id);
        p.setSlug(slug);
        p.setProductName(name);
        p.setPrice(new BigDecimal("123.45"));
        return p;
    }

    @Nested
    @DisplayName("list / getBy* – entity")
    class EntityReadTests {

        @Test
        @DisplayName("list vrátí stránkované produkty")
        void list_ok() {
            Pageable pageable = PageRequest.of(1, 10, Sort.by("productId").descending());
            List<Product> items = List.of(makeProduct(1L, "a", "A"), makeProduct(2L, "b", "B"));
            Page<Product> page = new PageImpl<>(items, pageable, 25);

            when(productRepository.findAll(pageable)).thenReturn(page);

            Page<Product> result = catalogService.list(pageable);

            assertEquals(25, result.getTotalElements());
            assertEquals(3, result.getTotalPages()); // 25 / 10 => 3
            assertEquals(2, result.getContent().size());
            assertEquals("B", result.getContent().get(1).getProductName());
            verify(productRepository).findAll(pageable);
        }

        @Test
        @DisplayName("getBySlug – nalezen")
        void getBySlug_found() {
            Product p = makeProduct(10L, "foo", "Foo");
            when(productRepository.findBySlug("foo")).thenReturn(Optional.of(p));

            Product result = catalogService.getBySlug("foo");
            assertEquals(10L, result.getProductId());
        }

        @Test
        @DisplayName("getBySlug – nenalezen → NoSuchElementException")
        void getBySlug_notFound() {
            when(productRepository.findBySlug("missing")).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> catalogService.getBySlug("missing"));
        }

        @Test
        @DisplayName("getById – nalezen")
        void getById_found() {
            Product p = makeProduct(5L, "x", "X");
            when(productRepository.findById(5L)).thenReturn(Optional.of(p));
            Product result = catalogService.getById(5L);
            assertEquals("X", result.getProductName());
        }

        @Test
        @DisplayName("getById – nenalezen → NoSuchElementException")
        void getById_notFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(NoSuchElementException.class, () -> catalogService.getById(99L));
        }

        @Test
        @DisplayName("listCategories – vrátí seřazené kategorie")
        void listCategories_ok() {
            Category c1 = new Category(); c1.setCategoryId(1L); c1.setCategoryName("A");
            Category c2 = new Category(); c2.setCategoryId(2L); c2.setCategoryName("B");
            when(categoryRepository.findAllOrdered()).thenReturn(List.of(c1, c2));

            List<Category> out = catalogService.listCategories();
            assertEquals(2, out.size());
            assertEquals("A", out.get(0).getCategoryName());
            verify(categoryRepository).findAllOrdered();
        }

        @Test
        @DisplayName("listByCategorySlug – vrátí stránkované produkty dle kategorie")
        void listByCategorySlug_ok() {
            Pageable pageable = PageRequest.of(0, 2);
            Page<Product> page = new PageImpl<>(List.of(makeProduct(1L, "a","A")), pageable, 1);
            when(productRepository.findByCategorySlug("vapky", pageable)).thenReturn(page);

            Page<Product> out = catalogService.listByCategorySlug("vapky", pageable);
            assertEquals(1, out.getTotalElements());
            verify(productRepository).findByCategorySlug("vapky", pageable);
        }
    }

    @Nested
    @DisplayName("DTO metody + primaryImageUrl")
    class DtoTests {

        @Test
        @DisplayName("listDto – mapuje na DTO a doplňuje primaryImageUrl (nalezen)")
        void listDto_withPrimaryImage() {
            Pageable pageable = PageRequest.of(0, 3);
            Product p1 = makeProduct(1L, "p-1", "P1");
            Product p2 = makeProduct(2L, "p-2", "P2");
            Page<Product> entityPage = new PageImpl<>(List.of(p1, p2), pageable, 10);

            ProductResponseDTO dto1 = new ProductResponseDTO(); dto1.setProductId(1L); dto1.setSlug("p-1");
            ProductResponseDTO dto2 = new ProductResponseDTO(); dto2.setProductId(2L); dto2.setSlug("p-2");

            when(productRepository.findAll(pageable)).thenReturn(entityPage);
            when(productMapper.toDto(p1)).thenReturn(dto1);
            when(productMapper.toDto(p2)).thenReturn(dto2);

            // imageRepo: vrátí první URL pro každý produkt
            when(imageRepository.findTopUrlsByProductId(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of("https://cdn/1.jpg"));
            when(imageRepository.findTopUrlsByProductId(eq(2L), any(Pageable.class)))
                    .thenReturn(List.of("https://cdn/2.png"));

            Page<ProductResponseDTO> out = catalogService.listDto(pageable);

            assertEquals(10, out.getTotalElements());
            assertEquals(2, out.getContent().size());
            assertEquals("https://cdn/1.jpg", out.getContent().get(0).getPrimaryImageUrl());
            assertEquals("https://cdn/2.png", out.getContent().get(1).getPrimaryImageUrl());

            // ověř, že se používá PageRequest.of(0,1)
            ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
            verify(imageRepository, times(2)).findTopUrlsByProductId(anyLong(), cap.capture());
            cap.getAllValues().forEach(pg -> {
                assertEquals(0, pg.getPageNumber());
                assertEquals(1, pg.getPageSize());
            });
        }

        @Test
        @DisplayName("listDto – primaryImageUrl neexistuje → null")
        void listDto_withoutImage() {
            Pageable pageable = PageRequest.of(0, 1);
            Product p = makeProduct(7L, "p-7", "P7");
            Page<Product> entityPage = new PageImpl<>(List.of(p), pageable, 1);
            ProductResponseDTO dto = new ProductResponseDTO(); dto.setProductId(7L); dto.setSlug("p-7");

            when(productRepository.findAll(pageable)).thenReturn(entityPage);
            when(productMapper.toDto(p)).thenReturn(dto);
            when(imageRepository.findTopUrlsByProductId(eq(7L), any(Pageable.class)))
                    .thenReturn(List.of()); // žádná URL

            Page<ProductResponseDTO> out = catalogService.listDto(pageable);
            assertNull(out.getContent().get(0).getPrimaryImageUrl());
        }

        @Test
        @DisplayName("getDtoBySlug – mapuje a doplní primaryImageUrl")
        void getDtoBySlug_ok() {
            Product p = makeProduct(11L, "slug-11", "S11");
            ProductResponseDTO dto = new ProductResponseDTO(); dto.setProductId(11L); dto.setSlug("slug-11");

            when(productRepository.findBySlug("slug-11")).thenReturn(Optional.of(p));
            when(productMapper.toDto(p)).thenReturn(dto);
            when(imageRepository.findTopUrlsByProductId(eq(11L), any(Pageable.class)))
                    .thenReturn(List.of("u11"));

            ProductResponseDTO out = catalogService.getDtoBySlug("slug-11");
            assertEquals("u11", out.getPrimaryImageUrl());
        }

        @Test
        @DisplayName("getDtoById – mapuje a doplní primaryImageUrl")
        void getDtoById_ok() {
            Product p = makeProduct(99L, "s-99", "S99");
            ProductResponseDTO dto = new ProductResponseDTO(); dto.setProductId(99L); dto.setSlug("s-99");

            when(productRepository.findById(99L)).thenReturn(Optional.of(p));
            when(productMapper.toDto(p)).thenReturn(dto);
            when(imageRepository.findTopUrlsByProductId(eq(99L), any(Pageable.class)))
                    .thenReturn(List.of("u99"));

            ProductResponseDTO out = catalogService.getDtoById(99L);
            assertEquals("u99", out.getPrimaryImageUrl());
        }

        @Test
        @DisplayName("listDtoByCategorySlug – mapuje a doplní primaryImageUrl + propagační stránkování")
        void listDtoByCategorySlug_ok() {
            Pageable pageable = PageRequest.of(2, 2, Sort.by("productId").ascending());
            Product p1 = makeProduct(1L, "a", "A");
            Product p2 = makeProduct(2L, "b", "B");
            Page<Product> page = new PageImpl<>(List.of(p1, p2), pageable, 9);

            ProductResponseDTO d1 = new ProductResponseDTO(); d1.setProductId(1L); d1.setSlug("a");
            ProductResponseDTO d2 = new ProductResponseDTO(); d2.setProductId(2L); d2.setSlug("b");

            when(productRepository.findByCategorySlug("cat", pageable)).thenReturn(page);
            when(productMapper.toDto(p1)).thenReturn(d1);
            when(productMapper.toDto(p2)).thenReturn(d2);
            when(imageRepository.findTopUrlsByProductId(eq(1L), any(Pageable.class))).thenReturn(List.of("i1"));
            when(imageRepository.findTopUrlsByProductId(eq(2L), any(Pageable.class))).thenReturn(List.of("i2"));

            Page<ProductResponseDTO> out = catalogService.listDtoByCategorySlug("cat", pageable);

            assertEquals(9, out.getTotalElements());
            assertEquals(2, out.getContent().size());
            assertEquals("i1", out.getContent().get(0).getPrimaryImageUrl());
            assertEquals("i2", out.getContent().get(1).getPrimaryImageUrl());
            verify(productRepository).findByCategorySlug("cat", pageable);
        }

        // ==== PŘIDEJ do `@Nested class DtoTests` ====

        @Test
        @DisplayName("listDto – volá imageRepo přesně s PageRequest.of(0,1)")
        void listDto_exactPageRequestForImages() {
            Pageable pageable = PageRequest.of(0, 2);
            Product p1 = makeProduct(1L, "p1", "P1");
            Product p2 = makeProduct(2L, "p2", "P2");
            Page<Product> entityPage = new PageImpl<>(List.of(p1, p2), pageable, 2);

            ProductResponseDTO d1 = new ProductResponseDTO(); d1.setProductId(1L);
            ProductResponseDTO d2 = new ProductResponseDTO(); d2.setProductId(2L);

            when(productRepository.findAll(pageable)).thenReturn(entityPage);
            when(productMapper.toDto(p1)).thenReturn(d1);
            when(productMapper.toDto(p2)).thenReturn(d2);

            when(imageRepository.findTopUrlsByProductId(eq(1L), eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of("u1"));
            when(imageRepository.findTopUrlsByProductId(eq(2L), eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of("u2"));

            Page<ProductResponseDTO> out = catalogService.listDto(pageable);

            assertEquals("u1", out.getContent().get(0).getPrimaryImageUrl());
            assertEquals("u2", out.getContent().get(1).getPrimaryImageUrl());

            // přesná verifikace s eq(PageRequest.of(0,1))
            verify(imageRepository).findTopUrlsByProductId(eq(1L), eq(PageRequest.of(0, 1)));
            verify(imageRepository).findTopUrlsByProductId(eq(2L), eq(PageRequest.of(0, 1)));
        }

        @Test
        @DisplayName("getDtoBySlug – nenalezen → NoSuchElementException a mapper/imageRepo se nevolají")
        void getDtoBySlug_notFound_propagates() {
            when(productRepository.findBySlug("missing")).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> catalogService.getDtoBySlug("missing"));

            verify(productMapper, never()).toDto(any());
            verify(imageRepository, never()).findTopUrlsByProductId(anyLong(), any());
        }

        @Test
        @DisplayName("getDtoById – nenalezen → NoSuchElementException a mapper/imageRepo se nevolají")
        void getDtoById_notFound_propagates() {
            when(productRepository.findById(404L)).thenReturn(Optional.empty());

            assertThrows(NoSuchElementException.class, () -> catalogService.getDtoById(404L));

            verify(productMapper, never()).toDto(any());
            verify(imageRepository, never()).findTopUrlsByProductId(anyLong(), any());
        }

        @Test
        @DisplayName("getDtoBySlug – přesně PageRequest.of(0,1) při načtení primaryImageUrl")
        void getDtoBySlug_exactPageRequest() {
            Product p = makeProduct(77L, "slug-77", "S77");
            ProductResponseDTO dto = new ProductResponseDTO(); dto.setProductId(77L);

            when(productRepository.findBySlug("slug-77")).thenReturn(Optional.of(p));
            when(productMapper.toDto(p)).thenReturn(dto);
            when(imageRepository.findTopUrlsByProductId(eq(77L), eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of("img77"));

            ProductResponseDTO out = catalogService.getDtoBySlug("slug-77");
            assertEquals("img77", out.getPrimaryImageUrl());

            verify(imageRepository).findTopUrlsByProductId(eq(77L), eq(PageRequest.of(0, 1)));
        }

        @Test
        @DisplayName("getDtoById – přesně PageRequest.of(0,1) při načtení primaryImageUrl")
        void getDtoById_exactPageRequest() {
            Product p = makeProduct(88L, "slug-88", "S88");
            ProductResponseDTO dto = new ProductResponseDTO(); dto.setProductId(88L);

            when(productRepository.findById(88L)).thenReturn(Optional.of(p));
            when(productMapper.toDto(p)).thenReturn(dto);
            when(imageRepository.findTopUrlsByProductId(eq(88L), eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of("img88"));

            ProductResponseDTO out = catalogService.getDtoById(88L);
            assertEquals("img88", out.getPrimaryImageUrl());

            verify(imageRepository).findTopUrlsByProductId(eq(88L), eq(PageRequest.of(0, 1)));
        }

        @Test
        @DisplayName("listDtoByCategorySlug – přesně PageRequest.of(0,1) pro každý produkt")
        void listDtoByCategorySlug_exactPageRequest() {
            Pageable pageable = PageRequest.of(1, 2);
            Product p1 = makeProduct(1L, "a", "A");
            Product p2 = makeProduct(2L, "b", "B");
            Page<Product> page = new PageImpl<>(List.of(p1, p2), pageable, 5);

            ProductResponseDTO d1 = new ProductResponseDTO(); d1.setProductId(1L);
            ProductResponseDTO d2 = new ProductResponseDTO(); d2.setProductId(2L);

            when(productRepository.findByCategorySlug("cat", pageable)).thenReturn(page);
            when(productMapper.toDto(p1)).thenReturn(d1);
            when(productMapper.toDto(p2)).thenReturn(d2);

            when(imageRepository.findTopUrlsByProductId(eq(1L), eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of("i1"));
            when(imageRepository.findTopUrlsByProductId(eq(2L), eq(PageRequest.of(0, 1))))
                    .thenReturn(List.of("i2"));

            Page<ProductResponseDTO> out = catalogService.listDtoByCategorySlug("cat", pageable);

            assertEquals("i1", out.getContent().get(0).getPrimaryImageUrl());
            assertEquals("i2", out.getContent().get(1).getPrimaryImageUrl());

            verify(imageRepository).findTopUrlsByProductId(eq(1L), eq(PageRequest.of(0, 1)));
            verify(imageRepository).findTopUrlsByProductId(eq(2L), eq(PageRequest.of(0, 1)));
        }


    }
}
