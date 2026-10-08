package com.realestate.service;

import com.realestate.exception.PropertyImageStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class PropertyImageStorage {

    private static final String PUBLIC_PATH = "/uploads/property-images/";
    private final Path directory;

    public PropertyImageStorage(
            @Value("${app.property-images.directory:uploads/property-images}") String directory
    ) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(byte[] contents, String extension) {
        String filename = UUID.randomUUID() + extension;
        Path file = directory.resolve(filename).normalize();
        if (!file.startsWith(directory)) {
            throw new IllegalStateException("Generated image path is outside the configured directory.");
        }
        try {
            Files.createDirectories(directory);
            Files.write(file, contents, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return PUBLIC_PATH + filename;
        } catch (IOException exception) {
            throw new PropertyImageStorageException(exception);
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(PUBLIC_PATH)) {
            throw new IllegalStateException("Stored property image URL is invalid.");
        }
        String filename = imageUrl.substring(PUBLIC_PATH.length());
        if (!filename.matches("[0-9a-f-]{36}\\.(jpg|png)")) {
            throw new IllegalStateException("Stored property image name is invalid.");
        }
        try {
            Files.deleteIfExists(directory.resolve(filename).normalize());
        } catch (IOException exception) {
            throw new PropertyImageStorageException(exception);
        }
    }
}
