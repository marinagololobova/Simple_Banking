package com.skypro.simplebanking.controller;

import com.skypro.simplebanking.component.ComponentTest;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.UserService;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static com.skypro.simplebanking.component.ComponentTest.createUser;
import static com.skypro.simplebanking.component.ComponentTest.getAuthenticationHeader;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withUsername("banking")
            .withPassword("super-safe-pass");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DataSource dataSource;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ComponentTest componentTest;

    @BeforeEach
    public void cleanData() {
        userRepository.deleteAll();
    }

    private void addUserToRepository() {
        userService.createUser("user1", "user1");
    }

    void addUsersToRepository() {
        userService.createUser("user1", "user1");
        userService.createUser("user2", "user2");
        userService.createUser("user3", "user3");
    }

    @Test
    @Transactional
    void createUserTest_thenItExistInDatabase(@Value("${app.security.admin-token}") String token) throws Exception {

        JSONObject user = componentTest.getUser();

        mockMvc.perform(post("/user/")
                        .header("X-SECURITY-ADMIN-KEY", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(user.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("userName"))
                .andExpect(jsonPath("$.accounts").isNotEmpty());

    }

    @Test
    @WithMockUser(roles = "USER")
    public void createUserTest_ForUser() throws Exception {
        mockMvc.perform(post("/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUser().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createUserTest_UserExists() throws Exception {

        addUserToRepository();

        mockMvc.perform(post("/user/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUser().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_Test() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        addUsersToRepository();

        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsersTest_ForAdmin() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMyProfile_Test() throws Exception {

        addUsersToRepository();

        mockMvc.perform(get("/user/me")
                        .header(HttpHeaders.AUTHORIZATION,
                                getAuthenticationHeader("user1", "user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"));
    }
}
