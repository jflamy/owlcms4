package app.owlcms.access;

/** A navigation refused because of its token. Carries only the HTTP status, never the token. */
@SuppressWarnings("serial")
public class TokenRefusedException extends RuntimeException {

	private final int status;

	public TokenRefusedException(int status) {
		super(null, null, false, false);
		this.status = status;
	}

	/** 401, 403 or 429. */
	public int status() {
		return this.status;
	}
}
