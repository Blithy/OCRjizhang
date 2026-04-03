package com.example.ocrjizhang.backend.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.example.ocrjizhang.backend.api.CategoryDto;
import com.example.ocrjizhang.backend.api.LoginRequest;
import com.example.ocrjizhang.backend.api.RegisterRequest;
import com.example.ocrjizhang.backend.api.SyncPushRequest;
import com.example.ocrjizhang.backend.api.TransactionDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoStoreTest {

    private final DemoStore demoStore = new DemoStore();

    @Test
    void loginWithSeededDemoAccountShouldSucceed() {
        var payload = demoStore.login(new LoginRequest("demo", "123456"));
        assertEquals("demo", payload.username());
        assertFalse(payload.token().isBlank());
    }

    @Test
    void pushAndPullShouldRoundTripCurrentUserData() {
        var registered = demoStore.register(
            new RegisterRequest("local-user", "123456", null, null, "本地用户")
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(new CategoryDto(101L, registered.userId(), "餐饮", "EXPENSE", null, null, false, 1L, 2L)),
                List.of(),
                List.of(),
                List.of(new TransactionDto(
                    201L,
                    registered.userId(),
                    "EXPENSE",
                    3250L,
                    9001L,
                    "微信",
                    101L,
                    "餐饮",
                    "奶茶",
                    "鹿角巷",
                    10L,
                    "MANUAL",
                    1L,
                    2L
                )),
                List.of(),
                List.of()
            )
        );

        var payload = demoStore.pullAll(registered.userId());
        assertEquals(1, payload.categories().size());
        assertEquals(1, payload.transactions().size());
        assertEquals("鹿角巷", payload.transactions().get(0).merchantName());
    }

    @Test
    void duplicateCategoriesShouldCompactAndRemapTransactions() {
        var registered = demoStore.register(
            new RegisterRequest("dedupe-user", "123456", null, null, "去重用户")
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(
                    new CategoryDto(101L, registered.userId(), "餐饮", "EXPENSE", null, null, true, 1L, 10L),
                    new CategoryDto(202L, registered.userId(), "餐饮", "EXPENSE", null, null, false, 2L, 20L)
                ),
                List.of(),
                List.of(),
                List.of(
                    new TransactionDto(
                        301L,
                        registered.userId(),
                        "EXPENSE",
                        3294L,
                        9001L,
                        "微信",
                        202L,
                        "餐饮",
                        "外卖",
                        "鹿角巷",
                        30L,
                        "MANUAL",
                        3L,
                        30L
                    )
                ),
                List.of(),
                List.of()
            )
        );

        var payload = demoStore.pullAll(registered.userId());
        assertEquals(1, payload.categories().size());
        assertEquals(101L, payload.categories().get(0).id());
        assertEquals(1, payload.transactions().size());
        assertEquals(101L, payload.transactions().get(0).categoryId());
        assertEquals("餐饮", payload.transactions().get(0).categoryName());
    }
}
