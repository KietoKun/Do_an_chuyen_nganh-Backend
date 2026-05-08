package com.pizzastore.controller.integration;

import com.pizzastore.controller.UploadController;
import com.pizzastore.service.ImageService;
import com.pizzastore.config.WebSecurityConfig;
import com.pizzastore.security.JwtUtils;
import com.pizzastore.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UploadController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class UploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImageService imageService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @WithMockUser(roles = "MANAGER")
    void uploadImageShouldReturnUrlForAuthorizedUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pizza.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes()
        );

        when(imageService.uploadImage(any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn("https://res.cloudinary.com/demo/pizza.jpg");

        mockMvc.perform(multipart("/api/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/demo/pizza.jpg"));

        verify(imageService).uploadImage(any(org.springframework.web.multipart.MultipartFile.class));
    }
}
