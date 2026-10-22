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

    // 일반 서식 셀에 01012345678을 입력하면 Excel이 숫자로 저장해 앞자리 0을 잃는다(→ 1012345678).
    // 국내 번호는 모두 0으로 시작하므로, 0을 붙여 유효해지는 값만 되살린다.
    public static String restoreLeadingZero(String phone) {
        if (phone.isEmpty() || phone.startsWith("0")) {
            return phone;
        }
        String restored = "0" + phone;
        return isValid(restored) ? restored : phone;
    }

    /** 초기 비밀번호처럼 숫자만 필요한 자리에 쓴다. */
    public static String digits(String phone) {
        return phone.replace("-", "");
    }

    // 시트마다 제각각인 표기(01012345678, 010-12345678 ...)를 010-1234-5678 한 가지로 맞춘다.
    // 자릿수를 믿고 자르기 전에 직접 검증한다 — 호출자가 isValid 를 빠뜨려도 행 단위 오류로 남고,
    // 빈 값이나 형식이 깨진 값에 substring 이 터지지 않는다.
    public static String normalize(String phone) {
        if (!isValid(phone)) {
            return phone;
        }
        String digits = digits(phone);
        int head = digits.startsWith("02") ? 2 : 3;
        int tail = digits.length() - 4;
        return digits.substring(0, head) + "-" + digits.substring(head, tail) + "-" + digits.substring(tail);
    }
}
