package kgu.developers.globalutils.encryption;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import kgu.developers.globalutils.encryption.exception.DecryptionFailedException;
import kgu.developers.globalutils.encryption.exception.EncryptionFailedException;
import kgu.developers.globalutils.encryption.exception.InvalidSecretKeyException;

@Component
public class AesUtil {

	private static final String ALGORITHM = "AES";
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH_BIT = 128;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private static String SECRET_KEY = "";

	public AesUtil(@Value("${file.secret-key}") String secretKey) {
		int length = secretKey.getBytes(StandardCharsets.UTF_8).length;
		if (length != 16 && length != 24 && length != 32) {
			throw new InvalidSecretKeyException();
		}
		SECRET_KEY = secretKey;
	}

	public static String encrypt(String input) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			SECURE_RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
			byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));

			byte[] ivAndEncryptedBytes = new byte[IV_LENGTH + encryptedBytes.length];
			System.arraycopy(iv, 0, ivAndEncryptedBytes, 0, IV_LENGTH);
			System.arraycopy(encryptedBytes, 0, ivAndEncryptedBytes, IV_LENGTH, encryptedBytes.length);

			return Base64.getEncoder().encodeToString(ivAndEncryptedBytes);
		} catch (GeneralSecurityException e) {
			throw new EncryptionFailedException(e);
		}
	}

	public static String decrypt(String encryptedInput) {
		try {
			byte[] ivAndEncryptedBytes = Base64.getDecoder().decode(encryptedInput);

			byte[] iv = Arrays.copyOfRange(ivAndEncryptedBytes, 0, IV_LENGTH);
			byte[] encryptedBytes = Arrays.copyOfRange(ivAndEncryptedBytes, IV_LENGTH, ivAndEncryptedBytes.length);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(TAG_LENGTH_BIT, iv));

			return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new DecryptionFailedException(e);
		}
	}

	private static SecretKeySpec keySpec() {
		return new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
	}
}
