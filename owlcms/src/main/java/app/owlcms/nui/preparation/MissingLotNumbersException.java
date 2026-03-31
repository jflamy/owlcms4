package app.owlcms.nui.preparation;

/**
 * Exception thrown when a document requires lot numbers but athletes still have
 * missing or zero-valued lot numbers.
 */
public class MissingLotNumbersException extends ScopeException {
	private static final long serialVersionUID = 1L;

	public MissingLotNumbersException() {
		super("WeighIn.LotNumbersMissing");
	}

	public MissingLotNumbersException(String message) {
		super(message);
	}

	public MissingLotNumbersException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getTranslationKey() {
		return "WeighIn.LotNumbersMissing";
	}

	@Override
	public String getDisplayMessage() {
		return DocumentsPrecheckService.formatMissingLotNumbersMessage();
	}
}