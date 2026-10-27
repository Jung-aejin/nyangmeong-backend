package com.nyangmeong.member.domain;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * [회원 VO]
 * - MBR 1:1 중 로그인에 쓰는 컬럼만
 * - 조회는 mapper/member/MemberMapper.xml
 */
@Data
public class Member {
    private Long mbrSn;
    private String mbrSttsCd;
    private String mbrAuthrtCd;
    private String joinPathCd;
    private String pswdHash;
    private String mbrNcnm;
    private LocalDateTime lastLgnDt;
    private String ncnmStngCmptnYn;
}
