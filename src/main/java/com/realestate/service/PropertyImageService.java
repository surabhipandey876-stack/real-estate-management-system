package com.realestate.service;

import com.realestate.dto.PropertyImageResponse;
import com.realestate.exception.InvalidPropertyRequestException;
import com.realestate.exception.PropertyImageNotFoundException;
import com.realestate.model.Property;
import com.realestate.model.PropertyImage;
import com.realestate.model.RoleName;
import com.realestate.repository.PropertyImageRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

@Service
public class PropertyImageService {

    private static final int MAX_IMAGES_PER_PROPERTY = 10;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 12_000_000L;

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyService propertyService;
    private final PropertyImageStorage propertyImageStorage;

    public PropertyImageService(
            PropertyImageRepository propertyImageRepository,
            PropertyService propertyService,
            PropertyImageStorage propertyImageStorage
    ) {
        this.propertyImageRepository = propertyImageRepository;
        this.propertyService = propertyService;
        this.propertyImageStorage = propertyImageStorage;
    }

    @Transactional
    public PropertyImageResponse upload(
            Long propertyId,
            MultipartFile file,
            String requesterEmail,
            RoleName requesterRole
    ) {
        Property property = propertyService.findManageableProperty(propertyId, requesterEmail, requesterRole);
        if (propertyImageRepository.countByProperty_Id(propertyId) >= MAX_IMAGES_PER_PROPERTY) {
            throw new InvalidPropertyRequestException("A property can have at most 10 images.");
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidPropertyRequestException("Choose a non-empty image no larger than 5 MB.");
        }

        byte[] contents;
        try {
            contents = file.getBytes();
        } catch (IOException exception) {
            throw new InvalidPropertyRequestException("The uploaded image could not be read.");
        }
        String extension = validateImage(contents);
        String imageUrl = propertyImageStorage.store(contents, extension);
        try {
            PropertyImage image = new PropertyImage(
                    property,
                    imageUrl,
                    propertyImageRepository.countByProperty_Id(propertyId) == 0
            );
            return toResponse(propertyImageRepository.saveAndFlush(image));
        } catch (DataAccessException exception) {
            propertyImageStorage.delete(imageUrl);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<PropertyImageResponse> getForProperty(Long propertyId, String requesterEmail, RoleName requesterRole) {
        propertyService.getById(propertyId, requesterEmail, requesterRole);
        return propertyImageRepository.findAllByProperty_IdOrderByPrimaryDescCreatedAtAsc(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(
            Long propertyId,
            Long imageId,
            String requesterEmail,
            RoleName requesterRole
    ) {
        propertyService.findManageableProperty(propertyId, requesterEmail, requesterRole);
        PropertyImage image = propertyImageRepository.findByIdAndProperty_Id(imageId, propertyId)
                .orElseThrow(() -> new PropertyImageNotFoundException(imageId, propertyId));
        List<PropertyImage> images = propertyImageRepository
                .findAllByProperty_IdOrderByPrimaryDescCreatedAtAsc(propertyId);
        propertyImageRepository.delete(image);
        if (image.isPrimary()) {
            images.stream()
                    .filter(candidate -> !candidate.getId().equals(imageId))
                    .findFirst()
                    .ifPresent(candidate -> {
                        candidate.markPrimary();
                        propertyImageRepository.save(candidate);
                    });
        }
        propertyImageRepository.flush();
        propertyImageStorage.delete(image.getImageUrl());
    }

    private String validateImage(byte[] contents) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(contents))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String extension = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg" -> ".jpg";
                    case "png" -> ".png";
                    default -> throw invalidImage();
                };
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_IMAGE_PIXELS) {
                    throw new InvalidPropertyRequestException("Image dimensions must not exceed 12 megapixels.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw invalidImage();
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalidImage();
        }
    }

    private InvalidPropertyRequestException invalidImage() {
        return new InvalidPropertyRequestException("Only valid JPEG and PNG images are supported.");
    }

    private PropertyImageResponse toResponse(PropertyImage image) {
        return new PropertyImageResponse(
                image.getId(),
                image.getProperty().getId(),
                image.getImageUrl(),
                image.isPrimary(),
                image.getCreatedAt()
        );
    }
}
