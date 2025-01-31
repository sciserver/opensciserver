package org.sciserver.sso;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.subtle.AesGcmJce;

@Service
public class EncryptedMessageService {
	private final ObjectMapper mapper = new ObjectMapper();
	private final Aead aead;

	public EncryptedMessageService(AppConfig appConfig) throws GeneralSecurityException {
		TinkConfig.register();

		try {
			aead = new AesGcmJce(appConfig.getAppSettings().getSecretKey());
		} catch (GeneralSecurityException e) {
			throw new GeneralSecurityException(
					"Unable to load the secret key. "
					+ "Is a 32-byte base64-encoded string set for validation_code.secret_key?",
					e);
		}
		mapper.registerModule(new JavaTimeModule());
	}

	public String generateEncryptedString(Object user) throws GeneralSecurityException, IOException {
		byte[] ciphertext = aead.encrypt(mapper.writeValueAsBytes(user), null);
		return new String(
				Base64.getUrlEncoder().withoutPadding().encode(ciphertext),
				StandardCharsets.UTF_8);
	}

	public <T> T decryptString(String cipher, Class<T> clazz) throws GeneralSecurityException, IOException {
		byte[] ciphertext = Base64.getUrlDecoder().decode(cipher.getBytes(StandardCharsets.UTF_8));
		byte[] plaintext = aead.decrypt(ciphertext, null);
		return mapper.readValue(plaintext, clazz);
	}
}
