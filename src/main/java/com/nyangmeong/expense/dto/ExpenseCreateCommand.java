package com.nyangmeong.expense.dto;

import java.time.LocalDate;

/**
 * [가계부 지출 생성 요청]
 * - expense 지출 생성(SJM)에 넘기는 값 : 카테고리 병원, 입력방식 OCR (FN-AIM-009-10)
 */
public record ExpenseCreateCommand(Long mbrSn, Long petSn, Long recptSn, LocalDate expndYmd, Long expndAmt,
        String dtlMttr) {
}
