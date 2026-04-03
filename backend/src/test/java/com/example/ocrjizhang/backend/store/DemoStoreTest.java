package com.example.ocrjizhang.backend.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.ocrjizhang.backend.api.AccountDto;
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
                List.of(),
                List.of(),
                List.of(),
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
                List.of(),
                List.of(),
                List.of(),
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

    @Test
    void defaultAccountShouldNotBeDowngradedBySameNameUpdate() {
        var registered = demoStore.register(
            new RegisterRequest("default-acc-user", "123456", null, null, "账户用户")
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(new AccountDto(101L, registered.userId(), "微信", "微", 0L, true, 1L, 1L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(),
                List.of(new AccountDto(202L, registered.userId(), "微信", "W", 123L, false, 2L, 2L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );

        var payload = demoStore.pullAll(registered.userId());
        assertEquals(1, payload.accounts().size());
        assertEquals("微信", payload.accounts().get(0).name());
        assertEquals(true, payload.accounts().get(0).isDefault());
    }

    @Test
    void mergedAccountShouldRemapExistingTransactionBinding() {
        var registered = demoStore.register(
            new RegisterRequest("merge-acc-user", "123456", null, null, "合并账户用户")
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(
                    new AccountDto(1001L, registered.userId(), "微信", "微", 0L, true, 1L, 1L),
                    new AccountDto(2002L, registered.userId(), "备用账户", "备", 0L, false, 1L, 1L)
                ),
                List.of(),
                List.of(),
                List.of(new CategoryDto(301L, registered.userId(), "餐饮", "EXPENSE", null, null, true, 1L, 1L)),
                List.of(),
                List.of(),
                List.of(new TransactionDto(
                    401L,
                    registered.userId(),
                    "EXPENSE",
                    1880L,
                    2002L,
                    "备用账户",
                    301L,
                    "餐饮",
                    null,
                    "测试商户",
                    10L,
                    "MANUAL",
                    1L,
                    1L
                )),
                List.of(),
                List.of()
            )
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(),
                List.of(new AccountDto(2002L, registered.userId(), "微信", "W", 66L, false, 2L, 2L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );

        var payload = demoStore.pullAll(registered.userId());
        assertEquals(1, payload.accounts().size());
        assertEquals(1001L, payload.accounts().get(0).id());
        assertEquals(1, payload.transactions().size());
        assertEquals(1001L, payload.transactions().get(0).accountId());
        assertEquals("微信", payload.transactions().get(0).accountName());
    }

    @Test
    void transactionAccountShouldBindToCanonicalAccountByName() {
        var registered = demoStore.register(
            new RegisterRequest("bind-acc-user", "123456", null, null, "绑定账户用户")
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(new AccountDto(5001L, registered.userId(), "支付宝", "支", 0L, true, 1L, 1L)),
                List.of(),
                List.of(),
                List.of(new CategoryDto(601L, registered.userId(), "餐饮", "EXPENSE", null, null, true, 1L, 1L)),
                List.of(),
                List.of(),
                List.of(new TransactionDto(
                    701L,
                    registered.userId(),
                    "EXPENSE",
                    2600L,
                    999999L,
                    "支付宝",
                    601L,
                    "餐饮",
                    null,
                    "测试商户",
                    20L,
                    "MANUAL",
                    1L,
                    1L
                )),
                List.of(),
                List.of()
            )
        );

        var payload = demoStore.pullAll(registered.userId());
        assertEquals(1, payload.transactions().size());
        assertEquals(5001L, payload.transactions().get(0).accountId());
        assertEquals("支付宝", payload.transactions().get(0).accountName());
    }

    @Test
    void accountIdShouldRejectNonPositiveValue() {
        var registered = demoStore.register(
            new RegisterRequest("invalid-acc-user", "123456", null, null, "非法账户用户")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> demoStore.applySyncPush(
                registered.userId(),
                new SyncPushRequest(
                    List.of(new AccountDto(0L, registered.userId(), "现金", "现", 0L, true, 1L, 1L)),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
                )
            )
        );
    }

    @Test
    void transactionSyncShouldUpdateAccountBalanceAndRollbackOnUpdateDelete() {
        var registered = demoStore.register(
            new RegisterRequest("tx-balance-user", "123456", null, null, "交易余额用户")
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(new AccountDto(8001L, registered.userId(), "SYNC1", "S", 10_000L, true, 1L, 1L)),
                List.of(),
                List.of(),
                List.of(new CategoryDto(8101L, registered.userId(), "餐饮", "EXPENSE", null, null, true, 1L, 1L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
            )
        );

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TransactionDto(
                    8201L,
                    registered.userId(),
                    "EXPENSE",
                    1_250L,
                    8001L,
                    "SYNC1",
                    8101L,
                    "餐饮",
                    null,
                    "测试商户",
                    10L,
                    "MANUAL",
                    2L,
                    2L
                )),
                List.of(),
                List.of()
            )
        );

        var afterCreate = demoStore.pullAll(registered.userId());
        assertEquals(8_750L, afterCreate.accounts().get(0).balanceFen());

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new TransactionDto(
                    8201L,
                    registered.userId(),
                    "EXPENSE",
                    2_500L,
                    8001L,
                    "SYNC1",
                    8101L,
                    "餐饮",
                    null,
                    "测试商户",
                    10L,
                    "MANUAL",
                    2L,
                    3L
                )),
                List.of()
            )
        );

        var afterUpdate = demoStore.pullAll(registered.userId());
        assertEquals(7_500L, afterUpdate.accounts().get(0).balanceFen());

        demoStore.applySyncPush(
            registered.userId(),
            new SyncPushRequest(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(8201L)
            )
        );

        var afterDelete = demoStore.pullAll(registered.userId());
        assertEquals(10_000L, afterDelete.accounts().get(0).balanceFen());
    }
}
