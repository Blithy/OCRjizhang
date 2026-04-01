package com.example.ocrjizhang.backend.controller;

import com.example.ocrjizhang.backend.api.ApiResponse;
import com.example.ocrjizhang.backend.api.TransactionDto;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final DemoStore demoStore;

    public TransactionController(DemoStore demoStore) {
        this.demoStore = demoStore;
    }

    @GetMapping
    public ApiResponse<List<TransactionDto>> getTransactions(
        HttpServletRequest request,
        @RequestParam(value = "startTime", required = false) Long startTime,
        @RequestParam(value = "endTime", required = false) Long endTime,
        @RequestParam(value = "type", required = false) String type
    ) {
        return ApiResponse.success(demoStore.getTransactions(
            RequestUser.requireUserId(request),
            startTime,
            endTime,
            type
        ));
    }

    @PostMapping
    public ApiResponse<TransactionDto> createTransaction(
        HttpServletRequest request,
        @RequestBody TransactionDto transactionDto
    ) {
        return ApiResponse.success(demoStore.createOrUpdateTransaction(
            RequestUser.requireUserId(request),
            transactionDto,
            null
        ));
    }

    @PutMapping("/{id}")
    public ApiResponse<TransactionDto> updateTransaction(
        HttpServletRequest request,
        @PathVariable("id") long transactionId,
        @RequestBody TransactionDto transactionDto
    ) {
        return ApiResponse.success(demoStore.createOrUpdateTransaction(
            RequestUser.requireUserId(request),
            transactionDto,
            transactionId
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTransaction(
        HttpServletRequest request,
        @PathVariable("id") long transactionId
    ) {
        demoStore.deleteTransaction(RequestUser.requireUserId(request), transactionId);
        return ApiResponse.success(null);
    }
}
