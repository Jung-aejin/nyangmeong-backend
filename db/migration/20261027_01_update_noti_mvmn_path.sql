-- =====================================================================
-- 냥멍케어 MySQL 알림 이동 경로 변경  |  v0.9 위에 실행 (20261026_01 다음)
-- INAPP_NOTI.MVMN_PATH 를 프론트 라우트(docs/routes.md COM-004-07)에 맞춘다
--   (1) 캐릭터 완성 알림 /mypage/pixel-characters → /mypage/characters
--   (2) 1:1 문의 답변 알림 /support/inquiries/{sn} → /inquiries/{sn}
--   (3) 시드 케어 일정 알림 /care → /health/calendar
--   (4) 시드 실종 만료 알림 /lost-reports → /community/missing
--   (5) 시드 공동양육 초대 결과 알림 /mypage/caregivers → /mypage/pets
-- 몇 번을 실행해도 결과가 같다
-- 되돌리기:
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/care' WHERE `MVMN_PATH`='/health/calendar';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/caregivers' WHERE `MVMN_PATH`='/mypage/pets';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/lost-reports' WHERE `MVMN_PATH`='/community/missing';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/pixel-characters' WHERE `MVMN_PATH`='/mypage/characters';
--   UPDATE `INAPP_NOTI` SET `MVMN_PATH`=CONCAT('/support', `MVMN_PATH`) WHERE `MVMN_PATH` LIKE '/inquiries/%';
-- =====================================================================

UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/characters' WHERE `MVMN_PATH`='/mypage/pixel-characters';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`=REPLACE(`MVMN_PATH`, '/support/inquiries/', '/inquiries/') WHERE `MVMN_PATH` LIKE '/support/inquiries/%';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/health/calendar' WHERE `MVMN_PATH`='/care';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/community/missing' WHERE `MVMN_PATH`='/lost-reports';
UPDATE `INAPP_NOTI` SET `MVMN_PATH`='/mypage/pets' WHERE `MVMN_PATH`='/mypage/caregivers';
