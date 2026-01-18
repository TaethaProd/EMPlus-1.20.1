package taethaprod.emplus.cases;

public final class CaseClientState {
	private static String pendingCaseId = "";
	private static int pendingEntryIndex = -1;

	private CaseClientState() {
	}

	public static void setPendingResult(String caseId, int entryIndex) {
		pendingCaseId = caseId != null ? caseId : "";
		pendingEntryIndex = entryIndex;
	}

	public static int consumePendingResult(String caseId) {
		if (caseId == null || caseId.isBlank()) {
			return -1;
		}
		if (!caseId.equals(pendingCaseId)) {
			return -1;
		}
		int result = pendingEntryIndex;
		pendingCaseId = "";
		pendingEntryIndex = -1;
		return result;
	}
}
