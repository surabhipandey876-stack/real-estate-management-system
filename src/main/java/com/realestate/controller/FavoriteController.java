package com.realestate.controller;

import com.realestate.dto.PropertyResponse;
import com.realestate.service.FavoriteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse add(@PathVariable Long propertyId, Authentication authentication) {
        return favoriteService.add(authentication.getName(), propertyId);
    }

    @GetMapping
    public List<PropertyResponse> getMine(Authentication authentication) {
        return favoriteService.getMine(authentication.getName());
    }

    @DeleteMapping("/{propertyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long propertyId, Authentication authentication) {
        favoriteService.remove(authentication.getName(), propertyId);
    }
}
