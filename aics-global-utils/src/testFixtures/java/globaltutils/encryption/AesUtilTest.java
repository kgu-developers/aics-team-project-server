package globaltutils.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.globalutils.encryption.AesUtil;

class AesUtilTest {

  @BeforeEach
  void setUp() {
    new AesUtil("local-dev-key-16");
  }

  @Test
  @DisplayName("암호화한 값을 복호화하면 원본과 같다")
  void encryptAndDecrypt() {
    String encrypted = AesUtil.encrypt("객체지향프로그래밍.pdf");

    assertThat(encrypted).isNotNull();
    assertThat(AesUtil.decrypt(encrypted)).isEqualTo("객체지향프로그래밍.pdf");
  }

  @Test
  @DisplayName("같은 값을 암호화해도 IV가 매번 달라 결과가 다르다")
  void encryptUsesRandomIv() {
    String first = AesUtil.encrypt("same-input");
    String second = AesUtil.encrypt("same-input");

    assertThat(first).isNotEqualTo(second);
    assertThat(AesUtil.decrypt(first)).isEqualTo(AesUtil.decrypt(second));
  }

  @Test
  @DisplayName("변조된 암호문은 복호화에 실패해 null을 반환한다")
  void decryptRejectsTamperedInput() {
    byte[] encrypted = Base64.getDecoder().decode(AesUtil.encrypt("tamper-me"));
    encrypted[encrypted.length - 1] ^= 1;

    assertThat(AesUtil.decrypt(Base64.getEncoder().encodeToString(encrypted))).isNull();
  }
}
