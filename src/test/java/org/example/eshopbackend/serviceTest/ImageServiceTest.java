package org.example.eshopbackend.serviceTest;
// src/test/java/org/example/zeniqbackend/service/ImageServiceTest.java

import org.example.eshopbackend.dto.image.CreateImageRequestDTO;
import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.dto.image.UpdateImageRequestDTO;
import org.example.eshopbackend.entity.Image;
import org.example.eshopbackend.entity.Product;
import org.example.eshopbackend.exception.NotFoundException;
import org.example.eshopbackend.mapper.ImageMapper;
import org.example.eshopbackend.repository.ImageRepository;
import org.example.eshopbackend.repository.ProductRepository;
import org.example.eshopbackend.service.ImageService;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageServiceTest {

    @Mock private ImageRepository imageRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ImageMapper imageMapper;

    @InjectMocks
    private ImageService imageService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // --- helpers ---
    private static Product product(long id) {
        Product p = new Product();
        p.setProductId(id);
        p.setProductName("P"+id);
        return p;
    }

    private static Image img(long id, Product p, String url, int sort, boolean primary) {
        Image i = Image.builder()
                .imageId(id)
                .product(p)
                .url(url)
                .altText("alt-"+id)
                .sortOrder(sort)
                .primary(primary)
                .build();
        return i;
    }

    private static ImageResponseDTO dtoOf(Image i) {
        ImageResponseDTO d = new ImageResponseDTO();
        d.setImageId(i.getImageId());
        d.setProductId(i.getProduct().getProductId());
        d.setUrl(i.getUrl());
        d.setAltText(i.getAltText());
        d.setSortOrder(i.getSortOrder());
        d.setPrimary(i.isPrimary());
        return d;
    }

    // ============ listByProduct ============

    @Test
    @DisplayName("listByProduct: vrátí obrázky seřazené mapperem do DTO")
    void listByProduct_ok() {
        Product p = product(1L);
        Image i1 = img(10L, p, "u1", 0, true);
        Image i2 = img(11L, p, "u2", 1, false);

        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p))
                .thenReturn(List.of(i1, i2));
        when(imageMapper.toDto(i1)).thenReturn(dtoOf(i1));
        when(imageMapper.toDto(i2)).thenReturn(dtoOf(i2));

        List<ImageResponseDTO> out = imageService.listByProduct(1L);

        assertEquals(2, out.size());
        assertEquals(10L, out.get(0).getImageId());
        verify(imageRepository).findByProductOrderBySortOrderAscImageIdAsc(p);
    }

    @Test
    @DisplayName("listByProduct: product nenalezen → NotFoundException")
    void listByProduct_notFound() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> imageService.listByProduct(9L));
        verify(imageRepository, never()).findByProductOrderBySortOrderAscImageIdAsc(any());
    }

    // ============ addToProduct ============

    @Test
    @DisplayName("addToProduct: první obrázek se stává primary, sortOrder=0")
    void addToProduct_firstBecomesPrimary() {
        Product p = product(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(imageRepository.countByProduct(p)).thenReturn(0); // první
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p)).thenReturn(List.of());

        CreateImageRequestDTO req = new CreateImageRequestDTO();
        req.setUrl("first");
        req.setAltText("a");
        req.setSortOrder(null); // nech na defaultu
        req.setPrimary(null);   // default

        ArgumentCaptor<Image> savedCap = ArgumentCaptor.forClass(Image.class);
        when(imageRepository.save(savedCap.capture())).thenAnswer(inv -> {
            Image in = inv.getArgument(0);
            in.setImageId(100L);
            return in;
        });
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        ImageResponseDTO out = imageService.addToProduct(1L, req);

        Image saved = savedCap.getValue();
        assertEquals(0, saved.getSortOrder());
        assertTrue(saved.isPrimary());
        assertEquals(100L, out.getImageId());
    }

    @Test
    @DisplayName("addToProduct: vložení doprostřed → reindex sortOrder ostatním, primary=true zruší ostatním")
    void addToProduct_insertMiddle_reindexAndPrimary() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, true);
        Image b = img(2L, p, "b", 1, false);
        Image c = img(3L, p, "c", 2, false);

        // před vložením v repu existují a,b,c
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(imageRepository.countByProduct(p)).thenReturn(3);
        // voláno 2× v metodě (nejdřív kvůli zrušení primary, pak kvůli přečíslování)
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p))
                .thenReturn(new ArrayList<>(List.of(a, b, c)))
                .thenReturn(new ArrayList<>(List.of(a, b, c)));

        CreateImageRequestDTO req = new CreateImageRequestDTO();
        req.setUrl("mid");
        req.setAltText("mid");
        req.setSortOrder(1);      // vlož na pozici 1
        req.setPrimary(true);     // vynutit primary

        ArgumentCaptor<Image> savedCap = ArgumentCaptor.forClass(Image.class);
        when(imageRepository.save(savedCap.capture())).thenAnswer(inv -> {
            Image in = inv.getArgument(0);
            in.setImageId(99L);
            return in;
        });
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        ImageResponseDTO out = imageService.addToProduct(1L, req);

        // a,b,c byly upraveny in-place:
        assertFalse(a.isPrimary(), "a už nesmí být primary");
        assertEquals(0, a.getSortOrder(), "a zůstává 0");
        assertEquals(2, b.getSortOrder(), "b se posunul z 1 na 2");
        assertEquals(3, c.getSortOrder(), "c se posunul z 2 na 3");

        Image saved = savedCap.getValue();
        assertEquals(1, saved.getSortOrder(), "nový je na pozici 1");
        assertTrue(saved.isPrimary(), "nový je primary");
        assertEquals(99L, out.getImageId());
    }

    // ============ update ============

    @Test
    @DisplayName("update: změní url/altText, přesun sortOrder nahoru (dopředu), korektně posune ostatní")
    void update_moveEarlier() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, false);
        Image b = img(2L, p, "b", 1, true);
        Image c = img(3L, p, "c", 2, false);

        when(imageRepository.findById(2L)).thenReturn(Optional.of(b));
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p)).thenReturn(List.of(a, b, c));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        UpdateImageRequestDTO dto = new UpdateImageRequestDTO();
        dto.setUrl("b2");
        dto.setAltText("B2");
        dto.setSortOrder(0);  // z 1 na 0
        dto.setPrimary(null); // bez primárnosti

        ImageResponseDTO out = imageService.update(2L, dto);

        assertEquals("b2", out.getUrl());
        assertEquals("B2", out.getAltText());
        assertEquals(0, out.getSortOrder());

        // a (0) se posunul na 1, protože b jde na jeho místo
        assertEquals(1, a.getSortOrder());
        assertEquals(0, b.getSortOrder());
        assertEquals(2, c.getSortOrder());
    }

    @Test
    @DisplayName("update: přesun sortOrder dolů (dozadu), korektně posune ostatní")
    void update_moveLater() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, false);
        Image b = img(2L, p, "b", 1, false);
        Image c = img(3L, p, "c", 2, true);

        when(imageRepository.findById(2L)).thenReturn(Optional.of(b));
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p)).thenReturn(List.of(a, b, c));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        UpdateImageRequestDTO dto = new UpdateImageRequestDTO();
        dto.setSortOrder(2);  // b z 1 na 2

        ImageResponseDTO out = imageService.update(2L, dto);

        assertEquals(2, out.getSortOrder());
        // mezi (old=1, new=2] → c se posune z 2 na 1
        assertEquals(0, a.getSortOrder());
        assertEquals(1, c.getSortOrder());
    }

    @Test
    @DisplayName("update: primary=true zruší primary ostatním a nastaví u aktuálního")
    void update_makePrimary() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, true);
        Image b = img(2L, p, "b", 1, false);

        when(imageRepository.findById(2L)).thenReturn(Optional.of(b));
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p)).thenReturn(List.of(a, b));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        UpdateImageRequestDTO dto = new UpdateImageRequestDTO();
        dto.setPrimary(true);

        ImageResponseDTO out = imageService.update(2L, dto);

        assertTrue(out.isPrimary());
        assertFalse(a.isPrimary());
        assertTrue(b.isPrimary());
    }

    @Test
    @DisplayName("update: primary=false u aktuálního, vybere se první jiný jako primary")
    void update_unsetPrimary_picksFirstOther() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, false);
        Image b = img(2L, p, "b", 1, true); // b je původně primary

        when(imageRepository.findById(2L)).thenReturn(Optional.of(b));
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p)).thenReturn(List.of(a, b));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        UpdateImageRequestDTO dto = new UpdateImageRequestDTO();
        dto.setPrimary(false); // odeber primárnost z b

        ImageResponseDTO out = imageService.update(2L, dto);

        // out je DTO obrázku b → ten už primary NENÍ
        assertFalse(out.isPrimary());

        // první jiný (a) se stal primary
        assertTrue(a.isPrimary(), "první jiný se stal primary");

        // volitelně: ověř, že se ukládal právě b (ten, který update vrací)
        verify(imageRepository).save(same(b));
    }


    @Test
    @DisplayName("update: image nenalezen → NotFoundException")
    void update_notFound() {
        when(imageRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> imageService.update(404L, new UpdateImageRequestDTO()));
    }

    // ============ delete ============

    @Test
    @DisplayName("delete: zkompaktuje sortOrder a ponechá stávající primary (pokud smazaný nebyl primary)")
    void delete_compactOrder_noPrimaryChange() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, true);
        Image b = img(2L, p, "b", 1, false);
        Image c = img(3L, p, "c", 2, false);

        when(imageRepository.findById(2L)).thenReturn(Optional.of(b));
        doNothing().when(imageRepository).delete(b);
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p))
                .thenReturn(new ArrayList<>(List.of(a, c)));

        imageService.delete(2L);

        // b smazán, c se posune z 2 na 1
        assertEquals(0, a.getSortOrder());
        assertEquals(1, c.getSortOrder());
        assertTrue(a.isPrimary());
        assertFalse(c.isPrimary());
    }

    @Test
    @DisplayName("delete: smaže primární a nastaví primary prvnímu zbývajícímu, pokud žádný jiný primary není")
    void delete_deletedWasPrimary_pickFirstAsPrimary() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, true);  // smažeme
        Image b = img(2L, p, "b", 1, false);
        Image c = img(3L, p, "c", 2, false);

        when(imageRepository.findById(1L)).thenReturn(Optional.of(a));
        doNothing().when(imageRepository).delete(a);
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p))
                .thenReturn(new ArrayList<>(List.of(b, c)));

        imageService.delete(1L);

        // kompakce: b(0), c(1)
        assertEquals(0, b.getSortOrder());
        assertEquals(1, c.getSortOrder());
        // žádný primary nebyl → první zbývající se stal primary
        assertTrue(b.isPrimary());
        assertFalse(c.isPrimary());
    }

    // ============ setPrimary ============

    @Test
    @DisplayName("setPrimary: zruší primary všem a nastaví ho cílovému obrazku, uloží a vrátí DTO")
    void setPrimary_ok() {
        Product p = product(1L);
        Image a = img(1L, p, "a", 0, true);
        Image b = img(2L, p, "b", 1, false);

        when(imageRepository.findById(2L)).thenReturn(Optional.of(b));
        when(imageRepository.findByProductOrderBySortOrderAscImageIdAsc(p)).thenReturn(List.of(a, b));
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageMapper.toDto(any(Image.class))).thenAnswer(inv -> dtoOf(inv.getArgument(0)));

        ImageResponseDTO out = imageService.setPrimary(2L);

        assertTrue(out.isPrimary());
        assertFalse(a.isPrimary());
        assertTrue(b.isPrimary());
        verify(imageRepository).save(b);
    }

    @Test
    @DisplayName("setPrimary: image nenalezen → NotFoundException")
    void setPrimary_notFound() {
        when(imageRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> imageService.setPrimary(999L));
    }
}

