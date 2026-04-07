package com.example.ocrjizhang.backend.manage;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.example.ocrjizhang.backend.store.DemoStore;

@SpringBootTest
@AutoConfigureMockMvc
class ManageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoStore demoStore;

    @Test
    void manageDashboardShouldRedirectToLoginWhenNoSession() throws Exception {
        mockMvc.perform(get("/manage/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "/manage/login"));
    }

    @Test
    void loginShouldOpenDashboardAndAllowAccountCrudPage() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                post("/manage/login")
                    .param("username", "demo")
                    .param("password", "123456")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/manage/dashboard"))
            .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/manage/accounts").session((MockHttpSession) session))
            .andExpect(status().isOk())
            .andExpect(view().name("manage/accounts"));

        mockMvc.perform(
                post("/manage/accounts/save")
                    .session((MockHttpSession) session)
                    .param("name", "答辩备用账户")
                    .param("symbol", "备")
                    .param("balanceYuan", "88.80")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/manage/accounts"));

        mockMvc.perform(get("/manage/accounts").session((MockHttpSession) session))
            .andExpect(status().isOk())
            .andExpect(view().name("manage/accounts"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("答辩备用账户")));
    }

    @Test
    void managePanelShouldCreateCategoryAndTransactionForSyncView() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                post("/manage/login")
                    .param("username", "demo")
                    .param("password", "123456")
            )
            .andExpect(status().is3xxRedirection())
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(
                post("/manage/categories/save")
                    .session(session)
                    .param("name", "答辩餐饮")
                    .param("type", "EXPENSE")
                    .param("icon", "restaurant")
                    .param("color", "#E86F51")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/manage/categories"));

        long userId = ManageSession.requireUserId(session);
        String categoryId = String.valueOf(
            demoStore.getCategories(userId).stream()
                .filter(category -> "答辩餐饮".equals(category.name()))
                .findFirst()
                .orElseThrow()
                .id()
        );

        mockMvc.perform(
                post("/manage/transactions/save")
                    .session(session)
                    .param("type", "EXPENSE")
                    .param("amountYuan", "12.50")
                    .param("categoryId", categoryId)
                    .param("merchantName", "答辩奶茶店")
                    .param("transactionAt", "2026-04-07T18:30")
                    .param("source", "MANUAL")
                    .param("remark", "后台录入")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/manage/transactions"));

        mockMvc.perform(get("/manage/sync").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("manage/sync"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(containsString("后台录入")));
    }
}
