package com.nyangmeong.seed;

import java.util.List;

/**
 * [시드 대상 테이블]
 * - SEEDED : 생성기가 채우는 테이블. 하나라도 행이 있으면 넣지 않는다
 * - 넣지 않는 테이블 : COM_CD(기준 SQL), RFRSH_TKN·LGN_ATMPT_LMT·EML_CERT·NOTI_SNDNG_LOG(실행 중 쌓이는 기록),
 *   NOTI_TMPLT(없으면 코드의 기본 문구를 쓴다)
 * - RUNTIME : 로컬 -Preset이 SEEDED와 함께 비우는 기록 테이블(지운 회원을 가리키지 않게)
 */
public final class SeedTables {

    public static final List<String> SEEDED = List.of(
            "SYS_STNG", "TRMS", "RGN", "VRTY", "ATCH_FILE", "CARE_MSN", "ITEM", "CMNTY_AI_CMTRY",
            "MBR", "MBR_STTS_HSTRY", "SNS_ACNT_LINK", "MBR_TRMS_AGRE", "MBR_DSHBD_WDGT",
            "PET", "BRUP_REL", "BRUP_LINK_STTS_HSTRY", "JNT_BRUP_INVTN", "INVTN_TRGT_PET", "JNT_BRUP_INVTN_STTS_HSTRY",
            "PLC", "PLC_BKMK",
            "RECPT", "RECPT_STTS_HSTRY", "EXPND",
            "DAILY_HLTH_RCD", "OBSRV_ARTCL", "OBSRV_ARTCL_STTS_HSTRY", "OBSRV_CYCL", "SCHDL",
            "CKUP_RCD", "CKUP_RCD_STTS_HSTRY", "CKUP_RCD_ATCH", "INSP_NVL", "ETC_INSP", "PRSC", "PRSC_STTS_HSTRY",
            "REPT_PRVNT_MNG", "CARE_ARTCL", "CARE_CMPTN_HSTRY",
            "PIXEL_CHAR", "CHAR_CRT_JOB", "CHAR_CRT_JOB_STTS_HSTRY", "CHAR_PHOTO", "HLD_ITEM", "DAY_MSN", "PNT_GNLGR",
            "PST", "PST_STTS_HSTRY", "PST_IMG", "PST_RCMDTN", "CMNT", "DCLR", "DCLR_STTS_HSTRY",
            "LOST_TPOF", "LOST_TPOF_STTS_HSTRY",
            "PET_PRTC_RCRT", "PET_PRTC_RCRT_STTS_HSTRY", "RCRT_PET_KND", "RCRT_PRTC_SHP",
            "OTO_CHAT_ROOM", "OTO_CHAT_ROOM_STTS_HSTRY", "CHAT_PRTPNT", "CHAT_PRTPNT_STTS_HSTRY", "CHAT_MSG",
            "CHAT_MSG_PHOTO", "PET_PRTC_MTCH", "PET_PRTC_MTCH_STTS_HSTRY", "MTCH_CND_VER", "PRTC_CMPTN_DMND_CYCL",
            "PRTC_CMPTN_PHOTO",
            "NEWS", "NTC_MTTR", "FAQ", "OTO_INQRY", "OTO_INQRY_STTS_HSTRY", "INQRY_ATCH_PHOTO",
            "INAPP_NOTI", "AI_CNVRS_ROOM", "AI_CNVRS_MSG");

    public static final List<String> RUNTIME = List.of("RFRSH_TKN", "LGN_ATMPT_LMT", "EML_CERT", "NOTI_SNDNG_LOG");

    private SeedTables() {
    }
}
