package com.nyangmeong.pet.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.WithMockLoginMember;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

/**
 * [검사 - 내 반려동물 목록 API]
 */
@WebMvcTest(controllers = PetController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class PetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PetService petService;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    @Test
    void 쿠키가_없으면_401() throws Exception {
        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.httpCode").value(401));
    }

    @Test
    @WithMockLoginMember
    void 목록은_역할_대표_인원과_상세_항목을_돌려준다() throws Exception {
        PetSummary pet = new PetSummary();
        pet.setPetSn(10L);
        pet.setPetNm("뭉치");
        pet.setPetSeCd("PETC0001");
        pet.setVrtyDrctInptNm("믹스");
        pet.setPetGndrCd("PETC0003");
        pet.setPetBrdt(LocalDate.of(2023, 5, 1));
        pet.setPetBdwg(new BigDecimal("4.250"));
        pet.setColrCd("PETC0099");
        pet.setColrDrctInptNm("연갈색");
        pet.setAlgyCn("닭고기,연어");
        pet.setDssCn("슬개골 탈구");
        pet.setBrupRoleCd("PETC0018");
        pet.setRprsPetYn(true);
        pet.setCaregiverCount(2);
        pet.setBscCharSbstYn(true);
        when(petService.searchMyPets(1L)).thenReturn(List.of(pet));

        mockMvc.perform(get("/api/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.httpCode").value(200))
                .andExpect(jsonPath("$.data[0].petSn").value(10))
                .andExpect(jsonPath("$.data[0].vrtySn").doesNotExist())
                .andExpect(jsonPath("$.data[0].vrtyDrctInptNm").value("믹스"))
                .andExpect(jsonPath("$.data[0].colrDrctInptNm").value("연갈색"))
                .andExpect(jsonPath("$.data[0].algyCn").value("닭고기,연어"))
                .andExpect(jsonPath("$.data[0].dssCn").value("슬개골 탈구"))
                .andExpect(jsonPath("$.data[0].brupRoleCd").value("PETC0018"))
                .andExpect(jsonPath("$.data[0].rprsPetYn").value(true))
                .andExpect(jsonPath("$.data[0].caregiverCount").value(2))
                .andExpect(jsonPath("$.data[0].bscCharSbstYn").value(true));
    }
}
