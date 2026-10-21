package com.nyangmeong.notification.domain;

/**
 * [케어 일정 사전 알림 시점]
 * - MBR 의 THDY·D1·D3·D7_BFHD_NOTI_YN 컬럼과 1:1 (FN-COM-008-06)
 */
public enum NotificationLeadTime {
    TODAY,
    D1,
    D3,
    D7
}
