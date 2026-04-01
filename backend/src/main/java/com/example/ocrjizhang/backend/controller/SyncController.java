package com.example.ocrjizhang.backend.controller;

import com.example.ocrjizhang.backend.api.ApiResponse;
import com.example.ocrjizhang.backend.api.SyncPullPayloadDto;
import com.example.ocrjizhang.backend.api.SyncPullRequest;
import com.example.ocrjizhang.backend.api.SyncPushRequest;
import com.example.ocrjizhang.backend.auth.RequestUser;
import com.example.ocrjizhang.backend.store.DemoStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final DemoStore demoStore;

    public SyncController(DemoStore demoStore) {
        this.demoStore = demoStore;
    }

    @PostMapping("/push")
    public ApiResponse<Void> pushChanges(
        HttpServletRequest request,
        @RequestBody SyncPushRequest syncPushRequest
    ) {
        demoStore.applySyncPush(RequestUser.requireUserId(request), syncPushRequest);
        return ApiResponse.success(null);
    }

    @PostMapping("/pull")
    public ApiResponse<SyncPullPayloadDto> pullChanges(
        HttpServletRequest request,
        @RequestBody SyncPullRequest syncPullRequest
    ) {
        return ApiResponse.success(demoStore.pullAll(RequestUser.requireUserId(request)));
    }
}
