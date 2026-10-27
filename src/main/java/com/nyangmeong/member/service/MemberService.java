package com.nyangmeong.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.member.domain.Member;
import com.nyangmeong.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

/**
 * [회원 서비스]
 * - 다른 도메인(auth 등)은 mapper 대신 이 서비스를 부른다
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    /**
     * 로그인용 회원 조회 : 없으면 null (없는 아이디도 불일치와 같게 다루기 위해)
     */
    @Transactional(readOnly = true)
    public Member findForLogin(String acntEmlHmac) {
        return memberMapper.selectByEmailHmac(acntEmlHmac);
    }

    /**
     * 로그인 성공 기록 (최종 로그인 일시)
     */
    @Transactional
    public void recordLogin(Long mbrSn) {
        memberMapper.updateLastLogin(mbrSn, String.valueOf(mbrSn));
    }
}
