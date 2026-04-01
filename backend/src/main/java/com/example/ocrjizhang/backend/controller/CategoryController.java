package com.example.ocrjizhang.backend.controller;

import com.example.ocrjizhang.backend.api.ApiResponse;
import com.example.ocrjizhang.backend.api.CategoryDto;
import com.example.ocrjizhang.backend.auth.RequestUser;
import com.example.ocrjizhang.backend.store.DemoStore;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final DemoStore demoStore;

    public CategoryController(DemoStore demoStore) {
        this.demoStore = demoStore;
    }

    @GetMapping
    public ApiResponse<List<CategoryDto>> getCategories(HttpServletRequest request) {
        return ApiResponse.success(demoStore.getCategories(RequestUser.requireUserId(request)));
    }

    @PostMapping
    public ApiResponse<CategoryDto> createCategory(
        HttpServletRequest request,
        @RequestBody CategoryDto categoryDto
    ) {
        return ApiResponse.success(demoStore.createOrUpdateCategory(RequestUser.requireUserId(request), categoryDto, null));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryDto> updateCategory(
        HttpServletRequest request,
        @PathVariable("id") long categoryId,
        @RequestBody CategoryDto categoryDto
    ) {
        return ApiResponse.success(demoStore.createOrUpdateCategory(
            RequestUser.requireUserId(request),
            categoryDto,
            categoryId
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(
        HttpServletRequest request,
        @PathVariable("id") long categoryId
    ) {
        demoStore.deleteCategory(RequestUser.requireUserId(request), categoryId);
        return ApiResponse.success(null);
    }
}
