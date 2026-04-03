package com.example.ocrjizhang.backend.store;

import com.example.ocrjizhang.backend.api.AuthPayloadDto;
import com.example.ocrjizhang.backend.api.AccountDto;
import com.example.ocrjizhang.backend.api.CategoryDto;
import com.example.ocrjizhang.backend.api.LoginRequest;
import com.example.ocrjizhang.backend.api.RegisterRequest;
import com.example.ocrjizhang.backend.api.SyncPullPayloadDto;
import com.example.ocrjizhang.backend.api.SyncPushRequest;
import com.example.ocrjizhang.backend.api.TransactionDto;
import com.example.ocrjizhang.backend.api.UpdateCurrentUserRequest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoStore {

    private static final long DEMO_USER_ID = 9_001_001L;
    private static final String LEGACY_DEMO_TOKEN = "local-demo-token";

    private final Map<Long, UserRecord> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> userIdsByUsername = new ConcurrentHashMap<>();
    private final Map<String, Long> userIdsByToken = new ConcurrentHashMap<>();
    private final Map<Long, AccountDto> accountsById = new ConcurrentHashMap<>();
    private final Map<Long, CategoryDto> categoriesById = new ConcurrentHashMap<>();
    private final Map<Long, TransactionDto> transactionsById = new ConcurrentHashMap<>();
    private final AtomicLong userIdGenerator = new AtomicLong(DEMO_USER_ID + 1);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DemoStore() {
        long now = System.currentTimeMillis();
        UserRecord demoUser = new UserRecord(
            DEMO_USER_ID,
            "demo",
            passwordEncoder.encode("123456"),
            "本机演示用户",
            null,
            null,
            now
        );
        usersById.put(demoUser.id(), demoUser);
        userIdsByUsername.put(normalizeUsername(demoUser.username()), demoUser.id());
        userIdsByToken.put(LEGACY_DEMO_TOKEN, demoUser.id());
    }

    public synchronized AuthPayloadDto register(RegisterRequest request) {
        String username = requireText(request.username(), "用户名不能为空").trim();
        String password = requireText(request.password(), "密码不能为空");
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码至少需要 6 位");
        }

        String key = normalizeUsername(username);
        if (userIdsByUsername.containsKey(key)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        long now = System.currentTimeMillis();
        UserRecord user = new UserRecord(
            userIdGenerator.getAndIncrement(),
            username,
            passwordEncoder.encode(password),
            blankToNull(request.nickname()),
            blankToNull(request.email()),
            blankToNull(request.phone()),
            now
        );
        usersById.put(user.id(), user);
        userIdsByUsername.put(key, user.id());
        return createSession(user);
    }

    public synchronized AuthPayloadDto login(LoginRequest request) {
        String username = requireText(request.username(), "用户名不能为空").trim();
        String password = requireText(request.password(), "密码不能为空");
        Long userId = userIdsByUsername.get(normalizeUsername(username));
        if (userId == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        UserRecord user = usersById.get(userId);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return createSession(user);
    }

    public Long findUserIdByToken(String token) {
        return userIdsByToken.get(token);
    }

    public AuthPayloadDto getCurrentUser(long userId) {
        return toAuthPayload(requireUser(userId), null);
    }

    public synchronized AuthPayloadDto updateCurrentUser(long userId, UpdateCurrentUserRequest request) {
        UserRecord existing = requireUser(userId);
        String nextPasswordHash = null;
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6) {
                throw new IllegalArgumentException("密码至少需要 6 位");
            }
            nextPasswordHash = passwordEncoder.encode(request.password());
        }

        long now = System.currentTimeMillis();
        UserRecord updated = existing.withProfile(
            blankToNull(request.nickname()),
            blankToNull(request.email()),
            blankToNull(request.phone()),
            nextPasswordHash,
            now
        );
        usersById.put(userId, updated);
        return toAuthPayload(updated, null);
    }

    public List<AccountDto> getAccounts(long userId) {
        return accountsById.values().stream()
            .filter(account -> account.userId() == userId)
            .sorted(Comparator
                .comparing(AccountDto::isDefault).reversed()
                .thenComparingLong(AccountDto::updatedAt).reversed()
                .thenComparing(AccountDto::name))
            .toList();
    }

    public synchronized AccountDto createOrUpdateAccount(long userId, AccountDto request, Long pathAccountId) {
        if (request == null) {
            throw new IllegalArgumentException("账户数据不能为空");
        }
        if (pathAccountId != null && request.id() != pathAccountId) {
            throw new IllegalArgumentException("账户 ID 不一致");
        }

        String normalizedName = requireText(request.name(), "账户名称不能为空").trim();
        AccountDto existingByName = findAccountByName(userId, normalizedName);
        long canonicalId = existingByName != null ? existingByName.id() : request.id();
        AccountDto account = new AccountDto(
            canonicalId,
            userId,
            normalizedName,
            blankToNull(request.symbol()),
            request.balanceFen(),
            request.isDefault(),
            existingByName != null ? existingByName.createdAt() : request.createdAt(),
            normalizeUpdatedAt(request.updatedAt())
        );
        accountsById.put(canonicalId, account);
        if (existingByName != null && existingByName.id() != canonicalId) {
            accountsById.remove(existingByName.id());
            remapTransactionAccount(userId, existingByName.id(), account.id(), account.name());
        }
        return account;
    }

    public synchronized void deleteAccount(long userId, long accountId) {
        AccountDto account = accountsById.get(accountId);
        if (account == null || account.userId() != userId) {
            throw new IllegalArgumentException("账户不存在");
        }
        accountsById.remove(accountId);
        clearTransactionAccount(userId, accountId);
    }

    public List<CategoryDto> getCategories(long userId) {
        compactCategories(userId);
        return categoriesById.values().stream()
            .filter(category -> category.userId() == userId)
            .sorted(Comparator.comparing(CategoryDto::isDefault).reversed().thenComparing(CategoryDto::name))
            .toList();
    }

    public synchronized CategoryDto createOrUpdateCategory(long userId, CategoryDto request, Long pathCategoryId) {
        if (request == null) {
            throw new IllegalArgumentException("分类数据不能为空");
        }
        if (pathCategoryId != null && request.id() != pathCategoryId) {
            throw new IllegalArgumentException("分类 ID 不一致");
        }

        String normalizedName = requireText(request.name(), "分类名称不能为空").trim();
        String normalizedType = requireText(request.type(), "分类类型不能为空").trim().toUpperCase(Locale.ROOT);
        CategoryDto existingByName = findCategoryByNameAndType(userId, normalizedName, normalizedType);
        long canonicalId = existingByName != null ? existingByName.id() : request.id();

        CategoryDto category = new CategoryDto(
            canonicalId,
            userId,
            normalizedName,
            normalizedType,
            blankToNull(request.icon()),
            blankToNull(request.color()),
            request.isDefault(),
            existingByName != null ? existingByName.createdAt() : request.createdAt(),
            normalizeUpdatedAt(request.updatedAt())
        );
        categoriesById.put(canonicalId, category);
        if (existingByName != null && existingByName.id() != canonicalId) {
            categoriesById.remove(existingByName.id());
            remapTransactionCategory(userId, existingByName.id(), canonicalId, normalizedName);
        }
        return category;
    }

    public synchronized void deleteCategory(long userId, long categoryId) {
        CategoryDto category = categoriesById.get(categoryId);
        if (category == null || category.userId() != userId) {
            throw new IllegalArgumentException("分类不存在");
        }
        categoriesById.remove(categoryId);
    }

    public List<TransactionDto> getTransactions(long userId, Long startTime, Long endTime, String type) {
        compactCategories(userId);
        return transactionsById.values().stream()
            .filter(transaction -> transaction.userId() == userId)
            .filter(transaction -> startTime == null || transaction.transactionTime() >= startTime)
            .filter(transaction -> endTime == null || transaction.transactionTime() <= endTime)
            .filter(transaction -> type == null || type.isBlank()
                || transaction.type().equalsIgnoreCase(type.trim()))
            .sorted(Comparator.comparingLong(TransactionDto::transactionTime).reversed())
            .toList();
    }

    public synchronized TransactionDto createOrUpdateTransaction(
        long userId,
        TransactionDto request,
        Long pathTransactionId
    ) {
        if (request == null) {
            throw new IllegalArgumentException("交易数据不能为空");
        }
        if (pathTransactionId != null && request.id() != pathTransactionId) {
            throw new IllegalArgumentException("交易 ID 不一致");
        }

        String normalizedType = requireText(request.type(), "交易类型不能为空").trim().toUpperCase(Locale.ROOT);
        String normalizedCategoryName = requireText(request.categoryName(), "分类名称不能为空").trim();
        CategoryDto canonicalCategory = findCategoryByNameAndType(userId, normalizedCategoryName, normalizedType);
        long categoryId = canonicalCategory != null ? canonicalCategory.id() : request.categoryId();
        String categoryName = canonicalCategory != null ? canonicalCategory.name() : normalizedCategoryName;

        TransactionDto transaction = new TransactionDto(
            request.id(),
            userId,
            normalizedType,
            request.amountFen(),
            request.accountId(),
            blankToNull(request.accountName()),
            categoryId,
            categoryName,
            blankToNull(request.remark()),
            blankToNull(request.merchantName()),
            request.transactionTime(),
            requireText(request.source(), "来源不能为空").trim().toUpperCase(Locale.ROOT),
            request.createdAt(),
            normalizeUpdatedAt(request.updatedAt())
        );
        transactionsById.put(transaction.id(), transaction);
        return transaction;
    }

    public synchronized void deleteTransaction(long userId, long transactionId) {
        TransactionDto transaction = transactionsById.get(transactionId);
        if (transaction == null || transaction.userId() != userId) {
            throw new IllegalArgumentException("交易不存在");
        }
        transactionsById.remove(transactionId);
    }

    public synchronized void applySyncPush(long userId, SyncPushRequest request) {
        if (request == null) {
            return;
        }

        safeList(request.createAccounts()).forEach(account -> createOrUpdateAccount(userId, account, null));
        safeList(request.updateAccounts()).forEach(account -> createOrUpdateAccount(userId, account, account.id()));
        safeList(request.deleteAccountIds()).forEach(accountId -> deleteAccount(userId, accountId));
        safeList(request.createCategories()).forEach(category -> createOrUpdateCategory(userId, category, null));
        safeList(request.updateCategories()).forEach(category -> createOrUpdateCategory(userId, category, category.id()));
        safeList(request.deleteCategoryIds()).forEach(categoryId -> deleteCategory(userId, categoryId));
        safeList(request.createTransactions()).forEach(transaction -> createOrUpdateTransaction(userId, transaction, null));
        safeList(request.updateTransactions()).forEach(transaction -> createOrUpdateTransaction(userId, transaction, transaction.id()));
        safeList(request.deleteTransactionIds()).forEach(transactionId -> deleteTransaction(userId, transactionId));
    }

    public SyncPullPayloadDto pullAll(long userId) {
        compactCategories(userId);
        return new SyncPullPayloadDto(
            getAccounts(userId),
            getCategories(userId),
            getTransactions(userId, null, null, null),
            System.currentTimeMillis()
        );
    }

    private AuthPayloadDto createSession(UserRecord user) {
        String token = "demo-" + UUID.randomUUID();
        userIdsByToken.put(token, user.id());
        return toAuthPayload(user, token);
    }

    private AuthPayloadDto toAuthPayload(UserRecord user, String tokenOverride) {
        String token = tokenOverride;
        if (token == null) {
            token = userIdsByToken.entrySet().stream()
                .filter(entry -> entry.getValue().equals(user.id()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
        }
        return new AuthPayloadDto(token, user.id(), user.username(), user.nickname());
    }

    private UserRecord requireUser(long userId) {
        UserRecord user = usersById.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static long normalizeUpdatedAt(long updatedAt) {
        return updatedAt > 0 ? updatedAt : System.currentTimeMillis();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values.stream().filter(item -> item != null).toList();
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private AccountDto findAccountByName(long userId, String name) {
        return accountsById.values().stream()
            .filter(account -> account.userId() == userId)
            .filter(account -> account.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    private CategoryDto findCategoryByNameAndType(long userId, String name, String type) {
        return categoriesById.values().stream()
            .filter(category -> category.userId() == userId)
            .filter(category -> category.type().equalsIgnoreCase(type))
            .filter(category -> category.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    private void remapTransactionAccount(long userId, long fromAccountId, long toAccountId, String accountName) {
        if (fromAccountId == toAccountId) {
            return;
        }
        transactionsById.replaceAll((transactionId, transaction) -> {
            if (transaction.userId() != userId || !Long.valueOf(fromAccountId).equals(transaction.accountId())) {
                return transaction;
            }
            return new TransactionDto(
                transaction.id(),
                transaction.userId(),
                transaction.type(),
                transaction.amountFen(),
                toAccountId,
                accountName,
                transaction.categoryId(),
                transaction.categoryName(),
                transaction.remark(),
                transaction.merchantName(),
                transaction.transactionTime(),
                transaction.source(),
                transaction.createdAt(),
                transaction.updatedAt()
            );
        });
    }

    private void clearTransactionAccount(long userId, long accountId) {
        transactionsById.replaceAll((transactionId, transaction) -> {
            if (transaction.userId() != userId || !Long.valueOf(accountId).equals(transaction.accountId())) {
                return transaction;
            }
            return new TransactionDto(
                transaction.id(),
                transaction.userId(),
                transaction.type(),
                transaction.amountFen(),
                null,
                null,
                transaction.categoryId(),
                transaction.categoryName(),
                transaction.remark(),
                transaction.merchantName(),
                transaction.transactionTime(),
                transaction.source(),
                transaction.createdAt(),
                transaction.updatedAt()
            );
        });
    }

    private void compactCategories(long userId) {
        Map<String, CategoryDto> canonicalByKey = new LinkedHashMap<>();
        categoriesById.values().stream()
            .filter(category -> category.userId() == userId)
            .sorted(Comparator
                .comparing(CategoryDto::isDefault).reversed()
                .thenComparingLong(CategoryDto::updatedAt).reversed()
                .thenComparingLong(CategoryDto::createdAt))
            .forEach(category -> {
                String key = category.userId() + "|" + category.type().toUpperCase(Locale.ROOT) + "|" + category.name().toLowerCase(Locale.ROOT);
                CategoryDto canonical = canonicalByKey.get(key);
                if (canonical == null) {
                    canonicalByKey.put(key, category);
                } else if (canonical.id() != category.id()) {
                    remapTransactionCategory(userId, category.id(), canonical.id(), canonical.name());
                    categoriesById.remove(category.id());
                }
            });
    }

    private void remapTransactionCategory(long userId, long fromCategoryId, long toCategoryId, String categoryName) {
        if (fromCategoryId == toCategoryId) {
            return;
        }
        transactionsById.replaceAll((transactionId, transaction) -> {
            if (transaction.userId() != userId || transaction.categoryId() != fromCategoryId) {
                return transaction;
            }
            return new TransactionDto(
                transaction.id(),
                transaction.userId(),
                transaction.type(),
                transaction.amountFen(),
                transaction.accountId(),
                transaction.accountName(),
                toCategoryId,
                categoryName,
                transaction.remark(),
                transaction.merchantName(),
                transaction.transactionTime(),
                transaction.source(),
                transaction.createdAt(),
                transaction.updatedAt()
            );
        });
    }
}
