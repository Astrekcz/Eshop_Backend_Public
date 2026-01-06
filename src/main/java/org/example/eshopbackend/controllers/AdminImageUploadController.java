// src/main/java/org/example/zeniqbackend/controllers/AdminImageUploadController.java
package org.example.eshopbackend.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eshopbackend.dto.image.CreateImageRequestDTO;
import org.example.eshopbackend.dto.image.ImageResponseDTO;
import org.example.eshopbackend.service.ImageService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminImageUploadController {

    private final ImageService imageService;

    private static final Set<String> ALLOWED_EXT  = Set.of("png","jpg","jpeg","webp","gif");
    private static final Set<String> ALLOWED_MIME = Set.of("image/png","image/jpeg","image/webp","image/gif");

    // volitelný limit (ochrana)
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    @PostMapping(
            value = "/products/{productId}/images/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ImageResponseDTO uploadAndCreate(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "primary", required = false) Boolean primary,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Soubor je prázdný.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Soubor je příliš velký (limit 10 MB).");
        }

        // MIME
        String contentType = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME.contains(contentType)) {
            throw new IllegalArgumentException("Nepovolený typ souboru: " + contentType);
        }

        // Původní název a přípona
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("");
        original = StringUtils.cleanPath(original); // očistí případné ../
        String ext = Optional.ofNullable(StringUtils.getFilenameExtension(original))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .orElse("");

        // Když z názvu nezjistíme příponu, určete podle MIME
        if (ext.isBlank()) {
            if ("image/png".equals(contentType)) {
                ext = "png";
            } else if ("image/webp".equals(contentType)) {
                ext = "webp";
            } else if ("image/gif".equals(contentType)) {
                ext = "gif";
            } else if ("image/jpeg".equals(contentType)) {
                ext = "jpg"; // sjednotíme na .jpg
            }
        }

        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Nepovolená přípona: " + ext);
        }

        // Destinace na disku
        Path dir = Paths.get("./static/images").toAbsolutePath().normalize();
        Files.createDirectories(dir);

        // Výsledný název souboru
        String filename = productId + "-" + UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename);

        // Uložení
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Veřejná ABSOLUTNÍ URL (aby prošla @URL v entitě)
        String publicUrl = "/static/images/" + filename;


        log.info("Uložen obrázek produktu {} → {}", productId, publicUrl);

        // Založení záznamu
        CreateImageRequestDTO dto = CreateImageRequestDTO.builder()
                .url(publicUrl)
                .altText(altText)
                .primary(primary)
                .sortOrder(sortOrder)
                .build();

        return imageService.addToProduct(productId, dto);
    }
}
