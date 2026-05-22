package com.securebank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.dto.LoginRequest;
import com.securebank.dto.RegisterRequest;
import com.securebank.dto.TransferRequest;
import com.securebank.dto.FundAccountRequest;
import com.securebank.event.TransferCompletedEvent;
import jakarta.servlet.http.Cookie;
import com.securebank.model.Account;
import com.securebank.model.Role;
import com.securebank.model.User;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.AuditLogRepository;
import com.securebank.repository.RefreshTokenRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.repository.UserRepository;
import com.securebank.service.TransferEventPublisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurebankBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TransferEventPublisher transferEventPublisher;

    @BeforeEach
    void cleanDatabase() {
        reset(transferEventPublisher);
        auditLogRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void registerLoginAndAccessProtectedEndpoint() throws Exception {
        register("Ada Lovelace", "ada@example.com", "secret123");

        String token = login("ada@example.com", "secret123");

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ada@example.com"));
    }

    @Test
    void refreshTokenCanIssueNewAccessTokenAndLogoutRevokesIt() throws Exception {
        register("Token User", "token-user@example.com", "secret123");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("token-user@example.com");
        loginRequest.setPassword("secret123");

        Cookie refreshCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("securebank.refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
                .andReturn()
                .getResponse()
                .getCookie("securebank.refresh");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("securebank.refresh=")));

        Cookie rotatedRefreshCookie = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getCookie("securebank.refresh");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(rotatedRefreshCookie))
                .andExpect(status().isUnauthorized());

        Cookie currentRefreshCookie = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("securebank.refresh");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(currentRefreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logout completed")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(currentRefreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenIsRejectedWhenUserNoLongerExists() throws Exception {
        register("Grace Hopper", "grace@example.com", "secret123");
        String token = login("grace@example.com", "secret123");

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegisterReturnsGenericMessage() throws Exception {
        register("Alan Turing", "alan@example.com", "secret123");

        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alan Turing");
        request.setEmail("alan@example.com");
        request.setPassword("secret123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Registration request received")));
    }

    @Test
    void userCannotAccessAdminDashboard() throws Exception {
        register("Katherine Johnson", "katherine@example.com", "secret123");
        String token = login("katherine@example.com", "secret123");

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/admin/dashboard")));
    }

    @Test
    void adminCanAccessDashboardSummary() throws Exception {
        createAdmin("admin@securebank.com", "secret123");
        String adminToken = login("admin@securebank.com", "secret123");

        register("Sophie Wilson", "sophie@example.com", "secret123");
        String sophieToken = login("sophie@example.com", "secret123");
        Long fromAccountId = createAccount(sophieToken);
        setBalance(fromAccountId, "250.00");

        register("Radia Perlman", "radia@example.com", "secret123");
        String radiaToken = login("radia@example.com", "secret123");
        Long toAccountId = createAccount(radiaToken);

        transfer(sophieToken, fromAccountId, toAccountId, "50.00", "Dashboard test")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", is(3)))
                .andExpect(jsonPath("$.totalAccounts", is(2)))
                .andExpect(jsonPath("$.totalTransactions", is(1)))
                .andExpect(jsonPath("$.totalSystemBalance", is(250.00)))
                .andExpect(jsonPath("$.currency", is("TRY")));
    }

    @Test
    void adminCanFundAccount() throws Exception {
        createAdmin("fund-admin@securebank.com", "secret123");
        String adminToken = login("fund-admin@securebank.com", "secret123");

        register("Adele Goldberg", "adele@example.com", "secret123");
        String userToken = login("adele@example.com", "secret123");
        Long accountId = createAccount(userToken);

        fundAccount(adminToken, accountId, "500.00", "Initial test balance")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(accountId.intValue())))
                .andExpect(jsonPath("$.balance", is(500.00)))
                .andExpect(jsonPath("$.currency", is("TRY")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance", is(500.00)));
    }

    @Test
    void adminCanFundAccountByAccountNumber() throws Exception {
        createAdmin("fund-number-admin@securebank.com", "secret123");
        String adminToken = login("fund-number-admin@securebank.com", "secret123");

        register("Grace Hopper", "grace-funding@example.com", "secret123");
        String userToken = login("grace-funding@example.com", "secret123");
        Long accountId = createAccount(userToken);
        String accountNumber = getAccountNumber(accountId);

        fundAccountByAccountNumber(adminToken, accountNumber, "250.00", "Demo top-up")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(accountId.intValue())))
                .andExpect(jsonPath("$.accountNumber", is(accountNumber)))
                .andExpect(jsonPath("$.balance", is(250.00)));
    }

    @Test
    void regularUserCannotFundAccount() throws Exception {
        register("Marissa Mayer", "marissa@example.com", "secret123");
        String token = login("marissa@example.com", "secret123");
        Long accountId = createAccount(token);

        fundAccount(token, accountId, "100.00", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.path", is("/api/admin/accounts/" + accountId + "/fund")));
    }

    @Test
    void fundAccountValidatesAmountAndMissingAccount() throws Exception {
        createAdmin("validation-admin@securebank.com", "secret123");
        String adminToken = login("validation-admin@securebank.com", "secret123");

        fundAccount(adminToken, 999999L, "100.00", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Account not found")));

        fundAccount(adminToken, 999999L, "0.00", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }

    @Test
    void authenticatedUserCanCreateAndListOwnAccounts() throws Exception {
        register("Dorothy Vaughan", "dorothy@example.com", "secret123");
        String token = login("dorothy@example.com", "secret123");

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").exists())
                .andExpect(jsonPath("$.iban").exists())
                .andExpect(jsonPath("$.balance").exists())
                .andExpect(jsonPath("$.currency", is("TRY")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").exists())
                .andExpect(jsonPath("$[0].iban").exists())
                .andExpect(jsonPath("$[0].balance").exists())
                .andExpect(jsonPath("$[0].currency", is("TRY")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    void accountEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/accounts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanTransferMoneyAndViewTransactionHistory() throws Exception {
        register("Mary Jackson", "mary@example.com", "secret123");
        String maryToken = login("mary@example.com", "secret123");
        Long fromAccountId = createAccount(maryToken);
        setBalance(fromAccountId, "100.00");

        register("Annie Easley", "annie@example.com", "secret123");
        String annieToken = login("annie@example.com", "secret123");
        Long toAccountId = createAccount(annieToken);
        String toAccountNumber = getAccountNumber(toAccountId);

        transferToAccountNumber(maryToken, fromAccountId, toAccountNumber, "35.50", "Rent payment")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccountId", is(fromAccountId.intValue())))
                .andExpect(jsonPath("$.toAccountId", is(toAccountId.intValue())))
                .andExpect(jsonPath("$.toAccountNumber", is(toAccountNumber)))
                .andExpect(jsonPath("$.amount", is(35.50)))
                .andExpect(jsonPath("$.currency", is("TRY")))
                .andExpect(jsonPath("$.type", is("TRANSFER")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.description", is("Rent payment")));

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + maryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance", is(64.50)));

        mockMvc.perform(get("/api/accounts/me")
                        .header("Authorization", "Bearer " + annieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance", is(35.50)));

        mockMvc.perform(get("/api/transactions/me")
                        .header("Authorization", "Bearer " + maryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromAccountId", is(fromAccountId.intValue())))
                .andExpect(jsonPath("$[0].toAccountId", is(toAccountId.intValue())))
                .andExpect(jsonPath("$[0].amount", is(35.50)));

        ArgumentCaptor<TransferCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(TransferCompletedEvent.class);
        verify(transferEventPublisher).publishTransferCompleted(eventCaptor.capture());

        TransferCompletedEvent event = eventCaptor.getValue();
        Assertions.assertEquals(fromAccountId, event.fromAccountId());
        Assertions.assertEquals(toAccountId, event.toAccountId());
        Assertions.assertEquals("mary@example.com", event.senderEmail());
        Assertions.assertEquals("annie@example.com", event.recipientEmail());
        Assertions.assertEquals(new BigDecimal("35.50"), event.amount());
        Assertions.assertEquals("TRY", event.currency());
        Assertions.assertEquals("Rent payment", event.description());
        Assertions.assertNotNull(event.transactionId());
        Assertions.assertNotNull(event.occurredAt());
    }

    @Test
    void authenticatedUserCanTransferMoneyToIban() throws Exception {
        register("Ada Lovelace", "ada@example.com", "secret123");
        String adaToken = login("ada@example.com", "secret123");
        Long fromAccountId = createAccount(adaToken);
        setBalance(fromAccountId, "75.00");

        register("Grace Hopper", "grace@example.com", "secret123");
        String graceToken = login("grace@example.com", "secret123");
        Long toAccountId = createAccount(graceToken);
        String toIban = getIban(toAccountId);

        transferToIban(adaToken, fromAccountId, toIban, "20.00", "IBAN transfer")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromAccountId", is(fromAccountId.intValue())))
                .andExpect(jsonPath("$.toAccountId", is(toAccountId.intValue())))
                .andExpect(jsonPath("$.amount", is(20.00)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void transferRejectsAccountOwnedByAnotherUser() throws Exception {
        register("Christine Darden", "christine@example.com", "secret123");
        String christineToken = login("christine@example.com", "secret123");
        Long christineAccountId = createAccount(christineToken);

        register("Evelyn Boyd Granville", "evelyn@example.com", "secret123");
        String evelynToken = login("evelyn@example.com", "secret123");
        Long evelynAccountId = createAccount(evelynToken);
        setBalance(evelynAccountId, "100.00");

        transfer(christineToken, evelynAccountId, christineAccountId, "25.00", null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("You can transfer only from your own accounts")))
                .andExpect(jsonPath("$.path", is("/api/transfers")))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(transferEventPublisher, never()).publishTransferCompleted(any());
    }

    @Test
    void transferRejectsInsufficientBalance() throws Exception {
        register("Mae Jemison", "mae@example.com", "secret123");
        String maeToken = login("mae@example.com", "secret123");
        Long fromAccountId = createAccount(maeToken);

        register("Valerie Thomas", "valerie@example.com", "secret123");
        String valerieToken = login("valerie@example.com", "secret123");
        Long toAccountId = createAccount(valerieToken);

        transfer(maeToken, fromAccountId, toAccountId, "10.00", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Insufficient balance")))
                .andExpect(jsonPath("$.path", is("/api/transfers")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void transferRejectsSameAccount() throws Exception {
        register("Joan Clarke", "joan@example.com", "secret123");
        String token = login("joan@example.com", "secret123");
        Long accountId = createAccount(token);
        setBalance(accountId, "100.00");

        transfer(token, accountId, accountId, "10.00", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Cannot transfer to the same account")))
                .andExpect(jsonPath("$.path", is("/api/transfers")));
    }

    @Test
    void transferValidationReturnsStandardErrorResponse() throws Exception {
        register("Hedy Lamarr", "hedy@example.com", "secret123");
        String token = login("hedy@example.com", "secret123");

        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("0.00"));

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.path", is("/api/transfers")))
                .andExpect(jsonPath("$.validationErrors.amount").exists());
    }

    @Test
    void transferEndpointsRequireAuthentication() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(new BigDecimal("10.00"));

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/transactions/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanViewTransactionsForOwnAccount() throws Exception {
        register("Elizabeth Feinler", "elizabeth@example.com", "secret123");
        String elizabethToken = login("elizabeth@example.com", "secret123");
        Long fromAccountId = createAccount(elizabethToken);
        setBalance(fromAccountId, "80.00");

        register("Karen Sparck Jones", "karen@example.com", "secret123");
        String karenToken = login("karen@example.com", "secret123");
        Long toAccountId = createAccount(karenToken);

        transfer(elizabethToken, fromAccountId, toAccountId, "25.00", "Account history")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/" + fromAccountId + "/transactions")
                        .header("Authorization", "Bearer " + elizabethToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromAccountId", is(fromAccountId.intValue())))
                .andExpect(jsonPath("$[0].toAccountId", is(toAccountId.intValue())))
                .andExpect(jsonPath("$[0].amount", is(25.00)));
    }

    @Test
    void userCannotViewTransactionsForAnotherUsersAccount() throws Exception {
        register("Frances Allen", "frances@example.com", "secret123");
        String francesToken = login("frances@example.com", "secret123");
        Long francesAccountId = createAccount(francesToken);

        register("Jean Bartik", "jean@example.com", "secret123");
        String jeanToken = login("jean@example.com", "secret123");
        Long jeanAccountId = createAccount(jeanToken);

        mockMvc.perform(get("/api/accounts/" + jeanAccountId + "/transactions")
                        .header("Authorization", "Bearer " + francesToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", is("You can view transactions only for your own accounts")))
                .andExpect(jsonPath("$.path", is("/api/accounts/" + jeanAccountId + "/transactions")));

        mockMvc.perform(get("/api/accounts/" + francesAccountId + "/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountTransactionHistoryReturnsNotFoundForMissingAccount() throws Exception {
        register("Barbara Liskov", "barbara@example.com", "secret123");
        String token = login("barbara@example.com", "secret123");

        mockMvc.perform(get("/api/accounts/999999/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Account not found")))
                .andExpect(jsonPath("$.path", is("/api/accounts/999999/transactions")));
    }

    private void register(String fullName, String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPassword(password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Registration request received")));
    }

    private void createAdmin(String email, String password) {
        User admin = User.builder()
                .fullName("SecureBank Admin")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
    }

    private String login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private Long createAccount(String token) throws Exception {
        String response = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asLong();
    }

    private void setBalance(Long accountId, String balance) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.setBalance(new BigDecimal(balance));
        accountRepository.save(account);
    }

    private String getAccountNumber(Long accountId) {
        return accountRepository.findById(accountId).orElseThrow().getAccountNumber();
    }

    private String getIban(Long accountId) {
        return accountRepository.findById(accountId).orElseThrow().getIban();
    }

    private org.springframework.test.web.servlet.ResultActions transfer(
            String token,
            Long fromAccountId,
            Long toAccountId,
            String amount,
            String description
    ) throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountId(toAccountId);
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);

        return mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions transferToAccountNumber(
            String token,
            Long fromAccountId,
            String toAccountNumber,
            String amount,
            String description
    ) throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToAccountNumber(toAccountNumber);
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);

        return mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions transferToIban(
            String token,
            Long fromAccountId,
            String toIban,
            String amount,
            String description
    ) throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(fromAccountId);
        request.setToIban(toIban);
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);

        return mockMvc.perform(post("/api/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions fundAccount(
            String token,
            Long accountId,
            String amount,
            String description
    ) throws Exception {
        FundAccountRequest request = new FundAccountRequest();
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);

        return mockMvc.perform(post("/api/admin/accounts/" + accountId + "/fund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions fundAccountByAccountNumber(
            String token,
            String accountNumber,
            String amount,
            String description
    ) throws Exception {
        FundAccountRequest request = new FundAccountRequest();
        request.setAmount(new BigDecimal(amount));
        request.setDescription(description);

        return mockMvc.perform(post("/api/admin/accounts/by-account-number/" + accountNumber + "/fund")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
