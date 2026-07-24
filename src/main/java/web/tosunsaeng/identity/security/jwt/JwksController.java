package web.tosunsaeng.identity.security.jwt;

import java.util.Map;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

	private final RSAKey rsaKey;

	public JwksController(RSAKey rsaKey) {
		this.rsaKey = rsaKey;
	}

	@GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, Object> jwks() {
		return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
	}
}
