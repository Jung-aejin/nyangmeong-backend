package com.nyangmeong.member.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.member.domain.Member;

/**
 * [회원 Mapper]
 */
@Mapper
public interface MemberMapper {

    /**
     * 로그인 아이디 HMAC로 회원 조회 (탈퇴·정지 포함)
     */
    Member selectByEmailHmac(String acntEmlHmac);

    /**
     * 최종 로그인 일시 갱신
     */
    int updateLastLogin(@Param("mbrSn") Long mbrSn, @Param("actorId") String actorId);

    /**
     * 탈퇴 보관일수가 지난 회원 번호 목록 조회
     */
    List<Long> selectExpiredWithdrawnMbrSns(@Param("mbrSttsCd") String mbrSttsCd,
            @Param("keepDayCnt") int keepDayCnt);
}
