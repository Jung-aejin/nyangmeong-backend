package com.nyangmeong.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.mapper.PetMapper;

/**
 * [검사 - 내 반려동물 목록]
 */
class PetServiceTest {

    private static final Long MBR = 1L;

    private final PetMapper mapper = mock(PetMapper.class);
    private final PetService service = new PetService(mapper);

    @Test
    void 목록은_공동양육자_코드로_인원을_센_결과를_그대로_돌려준다() {
        PetSummary primary = pet(10L, BrupRole.PRIMARY.getCode(), true, 2, "410000000000001");
        PetSummary coParent = pet(11L, BrupRole.CO_PARENT.getCode(), false, 1, null);
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of(primary, coParent));

        List<PetSummary> result = service.searchMyPets(MBR);

        assertThat(result).extracting(PetSummary::getPetSn).containsExactly(10L, 11L);
        assertThat(result.get(0).isRprsPetYn()).isTrue();
        assertThat(result.get(0).getCaregiverCount()).isEqualTo(2);
        assertThat(result.get(1).getBrupRoleCd()).isEqualTo(BrupRole.CO_PARENT.getCode());
    }

    @Test
    void 칩이_없는_반려동물의_chipNo는_null() {
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode()))
                .thenReturn(List.of(pet(10L, BrupRole.PRIMARY.getCode(), true, 0, null)));

        assertThat(service.searchMyPets(MBR).get(0).getChipNo()).isNull();
    }

    @Test
    void 양육_반려동물이_없으면_빈_목록() {
        when(mapper.selectList(MBR, BrupRole.CO_PARENT.getCode())).thenReturn(List.of());

        assertThat(service.searchMyPets(MBR)).isEmpty();
    }

    private PetSummary pet(Long petSn, String roleCd, boolean rprs, int caregivers, String chipNo) {
        PetSummary pet = new PetSummary();
        pet.setPetSn(petSn);
        pet.setBrupRoleCd(roleCd);
        pet.setRprsPetYn(rprs);
        pet.setCaregiverCount(caregivers);
        pet.setChipNo(chipNo);
        return pet;
    }
}
