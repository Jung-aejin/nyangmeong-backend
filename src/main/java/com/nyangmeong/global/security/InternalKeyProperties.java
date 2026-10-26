package com.nyangmeong.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * [서버 간 내부 호출 키]
 * - app.internal.inbound-key : Python -> Spring 호출 검증, 비면 /internal/** 전부 거부
 * - app.internal.outbound-key : Spring -> Python 호출에 붙일 키
 * - 방향마다 다른 32바이트 난수, git 에 올리지 않는다
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.internal")
public class InternalKeyProperties {

    /** 받는 키 */
    private String inboundKey = "";

    /** 보내는 키 */
    private String outboundKey = "";
}
