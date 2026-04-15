package app.owlcms.nui.preparation;

/**
 * Advisory variant of missing lot numbers used for document generation paths
 * where users may proceed after being warned.
 */
public class WarningMissingLotNumbersException extends MissingLotNumbersException {
    private static final long serialVersionUID = 1L;

    public WarningMissingLotNumbersException() {
        super();
    }

    public WarningMissingLotNumbersException(String message) {
        super(message);
    }

    public WarningMissingLotNumbersException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}