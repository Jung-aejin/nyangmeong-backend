package com.nyangmeong.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationTab;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.notification.domain.InappNotification;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.dto.NotificationSummary;
import com.nyangmeong.notification.dto.UnreadCountResponse;
import com.nyangmeong.notification.mapper.NotificationMapper;

import lombok.RequiredArgsConstructor;

/**
 * [인앱 알림 서비스]
 * - 저장은 NotificationEventListener 로만 (도메인은 EventPublisher 발행)
 * - 수신 OFF 유형은 숨김이 아니라 미생성 (FN-COM-004-13)
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int RECENT_LIMIT = 5;

    private final NotificationMapper notificationMapper;

    /**
     * 인앱 알림 생성
     */
    @Transactional
    public void create(NotificationEvent event) {
        NotificationReceiveSetting setting = notificationMapper.selectReceiveSetting(event.mbrSn());
        if (!canReceive(setting, event.type())) {
            return;
        }
        InappNotification notification = new InappNotification();
        notification.setMbrSn(event.mbrSn());
        notification.setNotiTypeCd(event.type().getCode());
        notification.setNotiTtl(event.notiTtl());
        notification.setNotiCn(event.notiCn());
        notification.setMvmnPath(event.mvmnPath());
        notificationMapper.insert(notification);
    }

    /**
     * 미읽음 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long mbrSn) {
        return new UnreadCountResponse(notificationMapper.countUnread(mbrSn));
    }

    /**
     * 탭별 최근 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationSummary> search(Long mbrSn, NotificationTab tab) {
        List<String> notiTypeCds = tab == null || tab == NotificationTab.ALL ? null : NotificationType.codesOf(tab);
        List<NotificationSummary> rows = notificationMapper.selectRecentList(mbrSn, notiTypeCds, RECENT_LIMIT);
        rows.forEach(row -> row.setTab(NotificationType.from(row.getNotiTypeCd()).getTab().name()));
        return rows;
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markRead(Long mbrSn, Long inappNotiSn) {
        if (!notificationMapper.existsOwn(inappNotiSn, mbrSn)) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notificationMapper.updateRead(inappNotiSn, mbrSn);
    }

    /**
     * 수신 여부 판단
     */
    private boolean canReceive(NotificationReceiveSetting setting, NotificationType type) {
        if (setting == null || MemberStatus.WITHDRAWN.is(setting.getMbrSttsCd())) {
            return false;
        }
        if (!"Y".equals(setting.getInappNotiRcptnYn())) {
            return false;
        }
        return switch (type) {
            case CARE_SCHEDULE -> "Y".equals(setting.getCareSchdlNotiRcptnYn());
            case CARE_MATCH -> "Y".equals(setting.getMtchDlngNotiRcptnYn());
            case COMMUNITY -> "Y".equals(setting.getCmntNotiRcptnYn());
            case LOST_EXPIRY -> "Y".equals(setting.getLostExpryNotiRcptnYn());
            case EXPENSE -> "Y".equals(setting.getExpndLimNotiRcptnYn());
            case CHARACTER -> "Y".equals(setting.getCharCrtNotiRcptnYn());
            /** 공동양육·1:1 문의 답변·공지는 설정 없음 */
            case CO_PARENTING, INQUIRY_ANSWER, NOTICE -> true;
        };
    }
}
