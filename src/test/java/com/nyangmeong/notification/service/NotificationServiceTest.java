package com.nyangmeong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.notification.NotificationEvent;
import com.nyangmeong.global.notification.NotificationTab;
import com.nyangmeong.global.notification.NotificationType;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.notification.domain.InappNotification;
import com.nyangmeong.notification.domain.NotificationReceiveSetting;
import com.nyangmeong.notification.dto.NotificationSummary;
import com.nyangmeong.notification.mapper.NotificationMapper;

/**
 * [검사 - 알림 생성·수신 설정·목록·읽음]
 */
class NotificationServiceTest {

    private final NotificationMapper notificationMapper = mock(NotificationMapper.class);
    private final NotificationService notificationService = new NotificationService(notificationMapper);

    private NotificationReceiveSetting allOn() {
        NotificationReceiveSetting setting = new NotificationReceiveSetting();
        setting.setMbrSn(1L);
        setting.setMbrSttsCd(MemberStatus.NORMAL.getCode());
        setting.setInappNotiRcptnYn("Y");
        setting.setCareSchdlNotiRcptnYn("Y");
        setting.setMtchDlngNotiRcptnYn("Y");
        setting.setCmntNotiRcptnYn("Y");
        setting.setLostExpryNotiRcptnYn("Y");
        setting.setExpndLimNotiRcptnYn("Y");
        setting.setCharCrtNotiRcptnYn("Y");
        return setting;
    }

    private NotificationEvent event(NotificationType type) {
        return new NotificationEvent(1L, type, "초코 심장사상충 예방일이 내일이에요", "본문", "/health/calendar");
    }

    private NotificationSummary row(NotificationType type) {
        NotificationSummary row = new NotificationSummary();
        row.setInappNotiSn(10L);
        row.setNotiTypeCd(type.getCode());
        return row;
    }

    @Test
    void 수신_ON이면_이벤트_값_그대로_저장한다() {
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(allOn());

        notificationService.create(event(NotificationType.CARE_SCHEDULE));

        ArgumentCaptor<InappNotification> captor = ArgumentCaptor.forClass(InappNotification.class);
        verify(notificationMapper).insert(captor.capture());
        InappNotification saved = captor.getValue();
        assertThat(saved.getMbrSn()).isEqualTo(1L);
        assertThat(saved.getNotiTypeCd()).isEqualTo("AIMC0008");
        assertThat(saved.getNotiTtl()).isEqualTo("초코 심장사상충 예방일이 내일이에요");
        assertThat(saved.getNotiCn()).isEqualTo("본문");
        assertThat(saved.getMvmnPath()).isEqualTo("/health/calendar");
    }

    @Test
    void 케어_일정을_끄면_케어_일정만_만들지_않고_공지는_만든다() {
        NotificationReceiveSetting setting = allOn();
        setting.setCareSchdlNotiRcptnYn("N");
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);

        notificationService.create(event(NotificationType.CARE_SCHEDULE));
        verify(notificationMapper, never()).insert(any());

        notificationService.create(event(NotificationType.NOTICE));
        verify(notificationMapper).insert(any());
    }

    @Test
    void 유형별_수신_컬럼을_따른다() {
        assertOffBlocks(NotificationType.CARE_MATCH, s -> s.setMtchDlngNotiRcptnYn("N"));
        assertOffBlocks(NotificationType.COMMUNITY, s -> s.setCmntNotiRcptnYn("N"));
        assertOffBlocks(NotificationType.LOST_EXPIRY, s -> s.setLostExpryNotiRcptnYn("N"));
        assertOffBlocks(NotificationType.EXPENSE, s -> s.setExpndLimNotiRcptnYn("N"));
        assertOffBlocks(NotificationType.CHARACTER, s -> s.setCharCrtNotiRcptnYn("N"));
    }

    private void assertOffBlocks(NotificationType type, Consumer<NotificationReceiveSetting> off) {
        NotificationMapper mapper = mock(NotificationMapper.class);
        NotificationService service = new NotificationService(mapper);
        NotificationReceiveSetting setting = allOn();
        off.accept(setting);
        when(mapper.selectReceiveSetting(1L)).thenReturn(setting);

        service.create(event(type));

        verify(mapper, never()).insert(any());
    }

    @Test
    void 설정이_없는_유형은_다른_설정을_모두_꺼도_만든다() {
        NotificationReceiveSetting setting = allOn();
        setting.setCareSchdlNotiRcptnYn("N");
        setting.setMtchDlngNotiRcptnYn("N");
        setting.setCmntNotiRcptnYn("N");
        setting.setLostExpryNotiRcptnYn("N");
        setting.setExpndLimNotiRcptnYn("N");
        setting.setCharCrtNotiRcptnYn("N");
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);

        notificationService.create(event(NotificationType.CO_PARENTING));
        notificationService.create(event(NotificationType.INQUIRY_ANSWER));
        notificationService.create(event(NotificationType.NOTICE));

        verify(notificationMapper, times(3)).insert(any());
    }

    @Test
    void 인앱_채널을_끄면_어떤_유형도_만들지_않는다() {
        NotificationReceiveSetting setting = allOn();
        setting.setInappNotiRcptnYn("N");
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);

        notificationService.create(event(NotificationType.NOTICE));

        verify(notificationMapper, never()).insert(any());
    }

    @Test
    void 탈퇴_회원과_없는_회원에게는_만들지_않는다() {
        NotificationReceiveSetting setting = allOn();
        setting.setMbrSttsCd(MemberStatus.WITHDRAWN.getCode());
        when(notificationMapper.selectReceiveSetting(1L)).thenReturn(setting);
        when(notificationMapper.selectReceiveSetting(2L)).thenReturn(null);

        notificationService.create(event(NotificationType.NOTICE));
        notificationService.create(new NotificationEvent(2L, NotificationType.NOTICE, "공지", null, null));

        verify(notificationMapper, never()).insert(any());
    }

    @Test
    void 전체_탭은_유형_조건_없이_5건을_조회한다() {
        when(notificationMapper.selectRecentList(eq(1L), isNull(), eq(5))).thenReturn(new ArrayList<>());

        notificationService.search(1L, NotificationTab.ALL);
        notificationService.search(1L, null);

        verify(notificationMapper, times(2)).selectRecentList(eq(1L), isNull(), eq(5));
    }

    @Test
    void 케어_탭은_케어_일정과_가계부_유형만_조회하고_행마다_탭을_채운다() {
        List<String> careCodes = List.of("AIMC0008", "AIMC0013");
        when(notificationMapper.selectRecentList(1L, careCodes, 5))
                .thenReturn(new ArrayList<>(List.of(row(NotificationType.CARE_SCHEDULE), row(NotificationType.EXPENSE))));

        List<NotificationSummary> rows = notificationService.search(1L, NotificationTab.CARE);

        assertThat(rows).extracting(NotificationSummary::getTab).containsExactly("CARE", "CARE");
    }

    @Test
    void 커뮤니티와_안내_탭의_유형_묶음() {
        assertThat(NotificationType.codesOf(NotificationTab.COMMUNITY))
                .containsExactlyInAnyOrder("AIMC0009", "AIMC0010", "AIMC0012");
        assertThat(NotificationType.codesOf(NotificationTab.INFO))
                .containsExactlyInAnyOrder("AIMC0011", "AIMC0014", "AIMC0015", "AIMC0030");
    }

    @Test
    void 본인_알림은_읽음_처리한다() {
        when(notificationMapper.existsOwn(10L, 1L)).thenReturn(true);

        notificationService.markRead(1L, 10L);

        verify(notificationMapper).updateRead(10L, 1L);
    }

    @Test
    void 남의_알림이나_없는_알림은_NOTIFICATION_NOT_FOUND() {
        when(notificationMapper.existsOwn(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.markRead(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
        verify(notificationMapper, never()).updateRead(anyLong(), anyLong());
    }
}
