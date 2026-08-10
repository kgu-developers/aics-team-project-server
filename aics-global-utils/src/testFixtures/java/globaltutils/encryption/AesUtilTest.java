package globaltutils.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import javax.crypto.AEADBadTagException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import kgu.developers.globalutils.encryption.AesUtil;
import kgu.developers.globalutils.encryption.exception.DecryptionFailedException;
import kgu.developers.globalutils.encryption.exception.InvalidSecretKeyException;

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
  @DisplayName("변조된 암호문은 복호화에 실패해 예외를 던진다")
  void decryptRejectsTamperedInput() {
    byte[] encrypted = Base64.getDecoder().decode(AesUtil.encrypt("tamper-me"));
    encrypted[encrypted.length - 1] ^= 1;
    String tampered = Base64.getEncoder().encodeToString(encrypted);

    assertThatThrownBy(() -> AesUtil.decrypt(tampered))
        .isInstanceOf(DecryptionFailedException.class)
        .hasMessage("복호화에 실패했습니다.")
        .hasCauseInstanceOf(AEADBadTagException.class);
  }

  @Test
  @DisplayName("암호문이 아닌 값은 복호화에 실패한다")
  void decryptRejectsGarbage() {
    assertThatThrownBy(() -> AesUtil.decrypt("not-base64!!"))
        .isInstanceOf(DecryptionFailedException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("키 길이가 16/24/32바이트가 아니면 생성 시점에 거부한다")
  void rejectsInvalidKeyLength() {
    assertThatThrownBy(() -> new AesUtil("too-short"))
        .isInstanceOf(InvalidSecretKeyException.class)
        .hasMessageContaining("16/24/32바이트");

    assertThatThrownBy(() -> new AesUtil("한글은세바이트라열여섯이아님"))
        .isInstanceOf(InvalidSecretKeyException.class);
  }

  @Test
  @DisplayName("24바이트, 32바이트 키도 허용한다")
  void acceptsLongerKeys() {
    for (String key : new String[] {"local-dev-key-24-bytes!!", "local-dev-key-32-bytes-padding!!"}) {
      new AesUtil(key);

      assertThat(AesUtil.decrypt(AesUtil.encrypt("ok"))).isEqualTo("ok");
    }
  }
}
