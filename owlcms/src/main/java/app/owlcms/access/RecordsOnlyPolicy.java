package app.owlcms.access;

/** Routing decisions that preserve the legacy records-only behavior. */
final class RecordsOnlyPolicy {

	enum DisplayDestination {
		EDIT_RECORDS, PUBLIC_RECORDS
	}

	private RecordsOnlyPolicy() {
	}

	static DisplayDestination displayDestination(boolean officialsAuthenticated) {
		return officialsAuthenticated ? DisplayDestination.EDIT_RECORDS : DisplayDestination.PUBLIC_RECORDS;
	}
}