package com.example.ocrjizhang.backend.manage;

import com.example.ocrjizhang.backend.api.AccountDto;
import com.example.ocrjizhang.backend.api.AuthPayloadDto;
import com.example.ocrjizhang.backend.api.CategoryDto;
import com.example.ocrjizhang.backend.api.LoginRequest;
import com.example.ocrjizhang.backend.api.TransactionDto;
import com.example.ocrjizhang.backend.store.DemoStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/manage")
public class ManageController {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter INPUT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final DemoStore demoStore;
    private final ManageIdGenerator idGenerator;

    public ManageController(DemoStore demoStore, ManageIdGenerator idGenerator) {
        this.demoStore = demoStore;
        this.idGenerator = idGenerator;
    }

    @GetMapping
    public String root() {
        return "redirect:/manage/dashboard";
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (ManageSession.isSignedIn(session)) {
            return "redirect:/manage/dashboard";
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        return "manage/login";
    }

    @PostMapping("/login")
    public String login(
        @ModelAttribute("loginForm") LoginForm form,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes
    ) {
        try {
            AuthPayloadDto payload = demoStore.login(new LoginRequest(form.getUsername(), form.getPassword()));
            HttpSession session = request.getSession(true);
            ManageSession.signIn(session, payload);
            redirectAttributes.addFlashAttribute("successMessage", "已进入演示后台管理面板。");
            return "redirect:/manage/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("loginForm", form);
            return "redirect:/manage/login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("successMessage", "已退出后台管理面板。");
        return "redirect:/manage/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        long userId = ManageSession.requireUserId(session);
        List<AccountDto> accounts = demoStore.getAccounts(userId);
        List<CategoryDto> categories = demoStore.getCategories(userId);
        List<TransactionDto> transactions = demoStore.getTransactions(userId, null, null, null);

        model.addAttribute("accountCount", accounts.size());
        model.addAttribute("categoryCount", categories.size());
        model.addAttribute("transactionCount", transactions.size());
        model.addAttribute("totalBalanceFen", accounts.stream().mapToLong(AccountDto::balanceFen).sum());
        model.addAttribute("recentTransactions", transactions.stream().limit(6).toList());
        model.addAttribute("latestUpdatedAt", latestUpdatedAt(accounts, categories, transactions));
        populateShell(model, session, "dashboard", "管理总览");
        return "manage/dashboard";
    }

    @GetMapping("/accounts")
    public String accounts(
        HttpSession session,
        Model model,
        @RequestParam(value = "editId", required = false) Long editId
    ) {
        long userId = ManageSession.requireUserId(session);
        List<AccountDto> accounts = demoStore.getAccounts(userId);
        model.addAttribute("accounts", accounts);
        if (!model.containsAttribute("accountForm")) {
            model.addAttribute("accountForm", buildAccountForm(accounts, editId));
        }
        model.addAttribute("editingAccountId", editId);
        populateShell(model, session, "accounts", "账户管理");
        return "manage/accounts";
    }

    @PostMapping("/accounts/save")
    public String saveAccount(
        HttpSession session,
        @ModelAttribute AccountForm form,
        RedirectAttributes redirectAttributes
    ) {
        long userId = ManageSession.requireUserId(session);
        Long accountId = form.getId();
        boolean isEdit = accountId != null;
        try {
            AccountDto existing = isEdit ? requireAccount(userId, accountId) : null;
            long now = System.currentTimeMillis();
            long targetId = isEdit ? accountId : idGenerator.nextId();
            demoStore.createOrUpdateAccount(
                userId,
                new AccountDto(
                    targetId,
                    userId,
                    form.getName(),
                    blankToNull(form.getSymbol()),
                    parseMoneyToFen(form.getBalanceYuan()),
                    form.isDefaultAccount(),
                    existing != null ? existing.createdAt() : now,
                    now
                ),
                isEdit ? targetId : null
            );
            redirectAttributes.addFlashAttribute("successMessage", isEdit ? "账户已更新。" : "账户已创建。");
            return "redirect:/manage/accounts";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("accountForm", form);
            return "redirect:/manage/accounts" + (isEdit ? "?editId=" + accountId : "");
        }
    }

    @PostMapping("/accounts/delete")
    public String deleteAccount(
        HttpSession session,
        @RequestParam("id") long accountId,
        RedirectAttributes redirectAttributes
    ) {
        long userId = ManageSession.requireUserId(session);
        try {
            demoStore.deleteAccount(userId, accountId);
            redirectAttributes.addFlashAttribute("successMessage", "账户已删除。");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manage/accounts";
    }

    @GetMapping("/categories")
    public String categories(
        HttpSession session,
        Model model,
        @RequestParam(value = "editId", required = false) Long editId
    ) {
        long userId = ManageSession.requireUserId(session);
        List<CategoryDto> categories = demoStore.getCategories(userId);
        model.addAttribute("categories", categories);
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", buildCategoryForm(categories, editId));
        }
        model.addAttribute("editingCategoryId", editId);
        populateShell(model, session, "categories", "分类管理");
        return "manage/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(
        HttpSession session,
        @ModelAttribute CategoryForm form,
        RedirectAttributes redirectAttributes
    ) {
        long userId = ManageSession.requireUserId(session);
        Long categoryId = form.getId();
        boolean isEdit = categoryId != null;
        try {
            CategoryDto existing = isEdit ? requireCategory(userId, categoryId) : null;
            long now = System.currentTimeMillis();
            long targetId = isEdit ? categoryId : idGenerator.nextId();
            demoStore.createOrUpdateCategory(
                userId,
                new CategoryDto(
                    targetId,
                    userId,
                    form.getName(),
                    upperRequired(form.getType(), "分类类型不能为空"),
                    blankToNull(form.getIcon()),
                    blankToNull(form.getColor()),
                    form.isDefaultCategory(),
                    existing != null ? existing.createdAt() : now,
                    now
                ),
                isEdit ? targetId : null
            );
            redirectAttributes.addFlashAttribute("successMessage", isEdit ? "分类已更新。" : "分类已创建。");
            return "redirect:/manage/categories";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("categoryForm", form);
            return "redirect:/manage/categories" + (isEdit ? "?editId=" + categoryId : "");
        }
    }

    @PostMapping("/categories/delete")
    public String deleteCategory(
        HttpSession session,
        @RequestParam("id") long categoryId,
        RedirectAttributes redirectAttributes
    ) {
        long userId = ManageSession.requireUserId(session);
        try {
            demoStore.deleteCategory(userId, categoryId);
            redirectAttributes.addFlashAttribute("successMessage", "分类已删除。");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manage/categories";
    }

    @GetMapping("/transactions")
    public String transactions(
        HttpSession session,
        Model model,
        @RequestParam(value = "editId", required = false) Long editId
    ) {
        long userId = ManageSession.requireUserId(session);
        List<AccountDto> accounts = demoStore.getAccounts(userId);
        List<CategoryDto> categories = demoStore.getCategories(userId);
        List<TransactionDto> transactions = demoStore.getTransactions(userId, null, null, null);
        model.addAttribute("accounts", accounts);
        model.addAttribute("categories", categories);
        model.addAttribute("transactions", transactions);
        model.addAttribute(
            "expenseCategories",
            categories.stream().filter(category -> "EXPENSE".equalsIgnoreCase(category.type())).toList()
        );
        model.addAttribute(
            "incomeCategories",
            categories.stream().filter(category -> "INCOME".equalsIgnoreCase(category.type())).toList()
        );
        if (!model.containsAttribute("transactionForm")) {
            model.addAttribute("transactionForm", buildTransactionForm(transactions, editId));
        }
        model.addAttribute("editingTransactionId", editId);
        populateShell(model, session, "transactions", "交易管理");
        return "manage/transactions";
    }

    @PostMapping("/transactions/save")
    public String saveTransaction(
        HttpSession session,
        @ModelAttribute TransactionForm form,
        RedirectAttributes redirectAttributes
    ) {
        long userId = ManageSession.requireUserId(session);
        Long transactionId = form.getId();
        boolean isEdit = transactionId != null;
        try {
            if (demoStore.getCategories(userId).isEmpty()) {
                throw new IllegalArgumentException("请先创建至少一个分类，再录入交易。");
            }
            TransactionDto existing = isEdit ? requireTransaction(userId, transactionId) : null;
            CategoryDto category = requireCategory(userId, form.getCategoryId());
            String type = upperRequired(form.getType(), "交易类型不能为空");
            if (!type.equalsIgnoreCase(category.type())) {
                throw new IllegalArgumentException("交易类型与分类类型不一致，请重新选择。");
            }
            AccountDto account = null;
            if (form.getAccountId() != null) {
                account = requireAccount(userId, form.getAccountId());
            }

            long now = System.currentTimeMillis();
            long targetId = isEdit ? transactionId : idGenerator.nextId();
            demoStore.createOrUpdateTransaction(
                userId,
                new TransactionDto(
                    targetId,
                    userId,
                    type,
                    parseMoneyToFen(form.getAmountYuan()),
                    account != null ? account.id() : null,
                    account != null ? account.name() : null,
                    category.id(),
                    category.name(),
                    blankToNull(form.getRemark()),
                    blankToNull(form.getMerchantName()),
                    parseDateTime(form.getTransactionAt()),
                    upperRequired(form.getSource(), "交易来源不能为空"),
                    existing != null ? existing.createdAt() : now,
                    now
                ),
                isEdit ? targetId : null
            );
            redirectAttributes.addFlashAttribute("successMessage", isEdit ? "交易已更新。" : "交易已创建。");
            return "redirect:/manage/transactions";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            redirectAttributes.addFlashAttribute("transactionForm", form);
            return "redirect:/manage/transactions" + (isEdit ? "?editId=" + transactionId : "");
        }
    }

    @PostMapping("/transactions/delete")
    public String deleteTransaction(
        HttpSession session,
        @RequestParam("id") long transactionId,
        RedirectAttributes redirectAttributes
    ) {
        long userId = ManageSession.requireUserId(session);
        try {
            demoStore.deleteTransaction(userId, transactionId);
            redirectAttributes.addFlashAttribute("successMessage", "交易已删除。");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/manage/transactions";
    }

    @GetMapping("/sync")
    public String syncStatus(HttpSession session, Model model) {
        long userId = ManageSession.requireUserId(session);
        var payload = demoStore.pullAll(userId);
        List<AccountDto> accounts = payload.accounts();
        List<CategoryDto> categories = payload.categories();
        List<TransactionDto> transactions = payload.transactions();

        model.addAttribute("syncPayload", payload);
        model.addAttribute("accountCount", accounts.size());
        model.addAttribute("categoryCount", categories.size());
        model.addAttribute("transactionCount", transactions.size());
        model.addAttribute("totalBalanceFen", accounts.stream().mapToLong(AccountDto::balanceFen).sum());
        model.addAttribute("latestTransaction", transactions.stream().findFirst().orElse(null));
        model.addAttribute("latestUpdatedAt", latestUpdatedAt(accounts, categories, transactions));
        model.addAttribute("recentAccounts", accounts.stream().limit(5).toList());
        model.addAttribute("recentCategories", categories.stream().limit(6).toList());
        model.addAttribute("recentTransactions", transactions.stream().limit(8).toList());
        populateShell(model, session, "sync", "同步状态");
        return "manage/sync";
    }

    private void populateShell(Model model, HttpSession session, String activeNav, String pageTitle) {
        model.addAttribute("activeNav", activeNav);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("currentUserName", ManageSession.displayName(session));
        model.addAttribute("currentUsername", ManageSession.username(session));
        model.addAttribute("currentToken", ManageSession.token(session));
    }

    private AccountForm buildAccountForm(List<AccountDto> accounts, Long editId) {
        if (editId == null) {
            return new AccountForm();
        }
        return accounts.stream()
            .filter(account -> account.id() == editId)
            .findFirst()
            .map(account -> {
                AccountForm form = new AccountForm();
                form.setId(account.id());
                form.setName(account.name());
                form.setSymbol(account.symbol());
                form.setBalanceYuan(toMoneyInput(account.balanceFen()));
                form.setDefaultAccount(account.isDefault());
                return form;
            })
            .orElseGet(AccountForm::new);
    }

    private CategoryForm buildCategoryForm(List<CategoryDto> categories, Long editId) {
        if (editId == null) {
            return new CategoryForm();
        }
        return categories.stream()
            .filter(category -> category.id() == editId)
            .findFirst()
            .map(category -> {
                CategoryForm form = new CategoryForm();
                form.setId(category.id());
                form.setName(category.name());
                form.setType(category.type());
                form.setIcon(category.icon());
                form.setColor(category.color());
                form.setDefaultCategory(category.isDefault());
                return form;
            })
            .orElseGet(CategoryForm::new);
    }

    private TransactionForm buildTransactionForm(List<TransactionDto> transactions, Long editId) {
        if (editId == null) {
            TransactionForm form = new TransactionForm();
            form.setTransactionAt(formatDateTimeInput(System.currentTimeMillis()));
            return form;
        }
        return transactions.stream()
            .filter(transaction -> transaction.id() == editId)
            .findFirst()
            .map(transaction -> {
                TransactionForm form = new TransactionForm();
                form.setId(transaction.id());
                form.setType(transaction.type());
                form.setAmountYuan(toMoneyInput(transaction.amountFen()));
                form.setAccountId(transaction.accountId());
                form.setCategoryId(transaction.categoryId());
                form.setRemark(transaction.remark());
                form.setMerchantName(transaction.merchantName());
                form.setTransactionAt(formatDateTimeInput(transaction.transactionTime()));
                form.setSource(transaction.source());
                return form;
            })
            .orElseGet(TransactionForm::new);
    }

    private AccountDto requireAccount(long userId, Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("请选择有效的账户。");
        }
        return demoStore.getAccounts(userId).stream()
            .filter(account -> account.id() == accountId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("账户不存在。"));
    }

    private CategoryDto requireCategory(long userId, Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("请选择有效的分类。");
        }
        return demoStore.getCategories(userId).stream()
            .filter(category -> category.id() == categoryId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("分类不存在。"));
    }

    private TransactionDto requireTransaction(long userId, Long transactionId) {
        if (transactionId == null) {
            throw new IllegalArgumentException("交易不存在。");
        }
        return demoStore.getTransactions(userId, null, null, null).stream()
            .filter(transaction -> transaction.id() == transactionId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("交易不存在。"));
    }

    private static long parseMoneyToFen(String amountText) {
        String value = required(amountText, "金额不能为空");
        try {
            return new BigDecimal(value.trim())
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("金额格式不正确");
        }
    }

    private static long parseDateTime(String value) {
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(required(value, "交易时间不能为空"), INPUT_DATE_TIME_FORMATTER);
            return localDateTime.atZone(ZONE_ID).toInstant().toEpochMilli();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("交易时间格式不正确");
        }
    }

    private static String formatDateTimeInput(long timestamp) {
        return INPUT_DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZONE_ID));
    }

    private static String toMoneyInput(long amountFen) {
        return BigDecimal.valueOf(amountFen, 2).stripTrailingZeros().toPlainString();
    }

    private static long latestUpdatedAt(
        List<AccountDto> accounts,
        List<CategoryDto> categories,
        List<TransactionDto> transactions
    ) {
        return List.of(
                accounts.stream().map(AccountDto::updatedAt).max(Long::compareTo).orElse(0L),
                categories.stream().map(CategoryDto::updatedAt).max(Long::compareTo).orElse(0L),
                transactions.stream().map(TransactionDto::updatedAt).max(Long::compareTo).orElse(0L)
            ).stream()
            .max(Comparator.naturalOrder())
            .orElse(0L);
    }

    private static String upperRequired(String value, String message) {
        return required(value, message).trim().toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static class LoginForm {

        private String username = "demo";
        private String password = "123456";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class AccountForm {

        private Long id;
        private String name;
        private String symbol;
        private String balanceYuan = "0";
        private boolean defaultAccount;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getBalanceYuan() {
            return balanceYuan;
        }

        public void setBalanceYuan(String balanceYuan) {
            this.balanceYuan = balanceYuan;
        }

        public boolean isDefaultAccount() {
            return defaultAccount;
        }

        public void setDefaultAccount(boolean defaultAccount) {
            this.defaultAccount = defaultAccount;
        }
    }

    public static class CategoryForm {

        private Long id;
        private String name;
        private String type = "EXPENSE";
        private String icon;
        private String color = "#E86F51";
        private boolean defaultCategory;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public boolean isDefaultCategory() {
            return defaultCategory;
        }

        public void setDefaultCategory(boolean defaultCategory) {
            this.defaultCategory = defaultCategory;
        }
    }

    public static class TransactionForm {

        private Long id;
        private String type = "EXPENSE";
        private String amountYuan;
        private Long accountId;
        private Long categoryId;
        private String remark;
        private String merchantName;
        private String transactionAt;
        private String source = "MANUAL";

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAmountYuan() {
            return amountYuan;
        }

        public void setAmountYuan(String amountYuan) {
            this.amountYuan = amountYuan;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getMerchantName() {
            return merchantName;
        }

        public void setMerchantName(String merchantName) {
            this.merchantName = merchantName;
        }

        public String getTransactionAt() {
            return transactionAt;
        }

        public void setTransactionAt(String transactionAt) {
            this.transactionAt = transactionAt;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
