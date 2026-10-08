package com.realestate.service;

import com.realestate.exception.InvalidPropertyRequestException;
import com.realestate.exception.PropertyAccessDeniedException;
import com.realestate.model.Property;
import com.realestate.model.Role;
import com.realestate.model.RoleName;
import com.realestate.model.User;
import com.realestate.repository.PropertyImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PropertyImageServiceTest {

    @TempDir
    Path imageDirectory;

    private PropertyImageRepository propertyImageRepository;
    private PropertyService propertyService;
    private PropertyImageStorage imageStorage;
    private PropertyImageService imageService;
    private Property property;

    @BeforeEach
    void setUp() {
        propertyImageRepository = mock(PropertyImageRepository.class);
        propertyService = mock(PropertyService.class);
        imageStorage = new PropertyImageStorage(imageDirectory.toString());
        imageService = new PropertyImageService(propertyImageRepository, propertyService, imageStorage);
        property = new Property(
                "Example home",
                null,
                "HOUSE",
                java.math.BigDecimal.valueOf(250000),
                3,
                2,
                1400,
                "Sample City",
                "10 Main Street",
                "AVAILABLE",
                new User("Pat", "Example", "agent@example.com", "not-used", null, new Role(RoleName.AGENT))
        );
        when(propertyService.findManageableProperty(4L, "agent@example.com", RoleName.AGENT))
                .thenReturn(property);
    }

    @Test
    void uploadsValidatedImageToLocalStorageAndCreatesPrimaryMetadata() throws Exception {
        when(propertyImageRepository.countByProperty_Id(4L)).thenReturn(0L);
        when(propertyImageRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = imageService.upload(
                4L,
                new MockMultipartFile("file", "home.png", "image/png", pngImage()),
                "agent@example.com",
                RoleName.AGENT
        );

        assertTrue(response.primary());
        assertTrue(response.imageUrl().startsWith("/uploads/property-images/"));
        assertTrue(Files.exists(imageDirectory.resolve(response.imageUrl().substring(response.imageUrl().lastIndexOf('/') + 1))));
        verify(propertyImageRepository).saveAndFlush(any());
    }

    @Test
    void rejectsNonImageContents() {
        when(propertyImageRepository.countByProperty_Id(4L)).thenReturn(0L);

        assertThrows(InvalidPropertyRequestException.class, () -> imageService.upload(
                4L,
                new MockMultipartFile("file", "home.png", "image/png", "not an image".getBytes()),
                "agent@example.com",
                RoleName.AGENT
        ));
    }

    @Test
    void rejectsImagesLargerThanFiveMegabytes() {
        when(propertyImageRepository.countByProperty_Id(4L)).thenReturn(0L);

        assertThrows(InvalidPropertyRequestException.class, () -> imageService.upload(
                4L,
                new MockMultipartFile("file", "home.png", "image/png", new byte[5 * 1024 * 1024 + 1]),
                "agent@example.com",
                RoleName.AGENT
        ));
    }

    @Test
    void rejectsUploadingToAPropertyTheAgentDoesNotManage() {
        when(propertyService.findManageableProperty(4L, "agent@example.com", RoleName.AGENT))
                .thenThrow(new PropertyAccessDeniedException());

        assertThrows(PropertyAccessDeniedException.class, () -> imageService.upload(
                4L,
                new MockMultipartFile("file", "home.png", "image/png", pngImage()),
                "agent@example.com",
                RoleName.AGENT
        ));
    }

    @Test
    void removesTheStoredFileWhenItsImageMetadataIsDeleted() throws Exception {
        String imageUrl = imageStorage.store(pngImage(), ".png");
        var image = new com.realestate.model.PropertyImage(property, imageUrl, false);
        when(propertyImageRepository.findByIdAndProperty_Id(9L, 4L)).thenReturn(java.util.Optional.of(image));
        when(propertyImageRepository.findAllByProperty_IdOrderByPrimaryDescCreatedAtAsc(4L))
                .thenReturn(List.of(image));

        imageService.delete(4L, 9L, "agent@example.com", RoleName.AGENT);

        assertFalse(Files.exists(imageDirectory.resolve(imageUrl.substring(imageUrl.lastIndexOf('/') + 1))));
        verify(propertyImageRepository).delete(image);
        verify(propertyImageRepository).flush();
    }

    private byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
