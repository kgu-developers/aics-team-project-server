package kgu.developers.admin.importcommon;

import java.util.regex.Pattern;

/** 임포트 시트의 연락처 열 — 형식 검증과 표기 통일을 한곳에서 본다. */
public final class Phones {
    public static final String FORMAT_MESSAGE =
        "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678, 01012345678)";

    // 하이픈은 있어도 없어도 된다(-?). 하이픈 없는 표기는 이 분기들이 그대로 커버한다.
    // 010은 11자리만 쓰이므로 뒷자리를 4+4로 못 박고, 지역번호는 실제 쓰이는 값만 받는다.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:010-?[0-9]{4}|01[16789]-?[0-9]{3,4}"
            + "|(?:02|031|032|033|041|042|043|044|051|052|053|054|055|061|062|063|064)-?[0-9]{3,4}"
            + "|070-?[0-9]{4})-?[0-9]{4}$");

    private Phones() {
    }

    public static boolean isValid(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /** 초기 비밀번호처럼 숫자만 필요한 자리에 쓴다. */
    public static String digits(String phone) {
        return phone.replace("-", "");
    }

    // 시트마다 제각각인 표기(01012345678, 010-12345678 ...)를 010-1234-5678 한 가지로 맞춘다.
    // 형식 검증을 통과한 값만 들어오므로 자릿수는 믿고 잘라도 된다
    public static String normalize(String phone) {
        String digits = digits(phone);
        if (digits.isEmpty()) {
            return phone;
        }
        int head = digits.startsWith("02") ? 2 : 3;
        int tail = digits.length() - 4;
        return digits.substring(0, head) + "-" + digits.substring(head, tail) + "-" + digits.substring(tail);
    }
}
