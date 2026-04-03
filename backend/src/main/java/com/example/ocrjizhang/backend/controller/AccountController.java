package com.example.ocrjizhang.backend.controller;

import com.example.ocrjizhang.backend.api.AccountDto;
import com.example.ocrjizhang.backend.api.ApiResponse;
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
@RequestMapping("/api/accounts")
public class AccountController {

    private final DemoStore demoStore;

    public AccountController(DemoStore demoStore) {
        this.demoStore = demoStore;
    }

    @GetMapping
    public ApiResponse<List<AccountDto>> getAccounts(HttpServletRequest request) {
        return ApiResponse.success(demoStore.getAccounts(RequestUser.requireUserId(request)));
    }

    @PostMapping
    public ApiResponse<AccountDto> createAccount(
        HttpServletRequest request,
        @RequestBody AccountDto accountDto
    ) {
        return ApiResponse.success(demoStore.createOrUpdateAccount(
            RequestUser.requireUserId(request),
            accountDto,
            null
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountDto> updateAccount(
        HttpServletRequest request,
        @PathVariable("id") long accountId,
        @RequestBody AccountDto accountDto
    ) {
        return ApiResponse.success(demoStore.createOrUpdateAccount(
            RequestUser.requireUserId(request),
            accountDto,
            accountId
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAccount(
        HttpServletRequest request,
        @PathVariable("id") long accountId
    ) {
        demoStore.deleteAccount(RequestUser.requireUserId(request), accountId);
        return ApiResponse.success(null);
    }
}
