package importcommon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import kgu.developers.admin.importcommon.Phones;

class PhonesTest {

    @ParameterizedTest
    @CsvSource({"010-1234-5678,010-1234-5678", "01012345678,010-1234-5678", "010-12345678,010-1234-5678",
        "0101234-5678,010-1234-5678", "02-123-4567,02-123-4567", "0212345678,02-1234-5678",
        "03112345678,031-1234-5678", "011-123-4567,011-123-4567", "07012345678,070-1234-5678"})
    @DisplayName("normalize는 대시 위치가 제각각인 정상 번호를 한 가지 표기로 모은다")
    void normalize_UnifiesValidNotations(String phone, String expected) {
        assertThat(Phones.normalize(phone)).isEqualTo(expected);
    }

    // isValid 를 빠뜨린 호출자가 500을 받는 대신 입력이 그대로 흘러가 행 단위 검증에 걸리도록
    @ParameterizedTest
    @ValueSource(strings = {"", "02", "123", "0101234567", "010123456789", "099-1234-5678", "+82-10-1234-5678"})
    @DisplayName("normalize는 형식이 깨진 값을 터지지 않고 입력 그대로 돌려준다")
    void normalize_ReturnsInvalidInputUnchanged(String phone) {
        assertThat(Phones.isValid(phone)).isFalse();
        assertThat(Phones.normalize(phone)).isEqualTo(phone);
    }

    @ParameterizedTest
    @CsvSource({"1012345678,01012345678", "212345678,0212345678", "3112345678,03112345678"})
    @DisplayName("restoreLeadingZero는 Excel이 먹은 앞자리 0을 되살린다")
    void restoreLeadingZero_RecoversDroppedZero(String phone, String expected) {
        assertThat(Phones.restoreLeadingZero(phone)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "010-1234-5678", "9912345678", "12345"})
    @DisplayName("restoreLeadingZero는 0을 붙여도 유효해지지 않는 값은 건드리지 않는다")
    void restoreLeadingZero_LeavesOthersUntouched(String phone) {
        assertThat(Phones.restoreLeadingZero(phone)).isEqualTo(phone);
    }
}
