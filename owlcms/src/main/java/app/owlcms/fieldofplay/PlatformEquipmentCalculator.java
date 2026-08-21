package app.owlcms.fieldofplay;

import app.owlcms.data.athlete.Gender;

public final class PlatformEquipmentCalculator {

	private static final int[] BAR_WEIGHTS = { 20, 15, 10, 5 };

	private PlatformEquipmentCalculator() {
	}

	static int maximumAllowedBarWeight(Gender gender, Integer ageGroupMaximumAge, boolean lightBarU13,
			boolean lightBarU15) {
		boolean lightBarRequired = gender == Gender.M && ageGroupMaximumAge != null
				&& (lightBarU13 && ageGroupMaximumAge <= 13 || lightBarU15 && ageGroupMaximumAge <= 15);
		return lightBarRequired || gender != Gender.M ? 15 : 20;
	}

	public static Selection select(Request request) {
		if (request.useUsawCollars()) {
			return selection(Math.min(request.standardBarWeight(), request.maximumAllowedBarWeight()), true, request);
		}

		if (!request.useNonStandardBar()) {
			Selection bumperSelection = findBumperSelection(request);
			if (bumperSelection != null) {
				return bumperSelection;
			}
		}

		if (request.useNonStandardBar()) {
			int barWeight = request.nonStandardBarWeight();
			return selection(barWeight, collarsPreservingBumpers(request, barWeight), request);
		}

		Inventory inventory = request.inventory();
		if (request.targetWeight() <= 14 && inventory.bar5()) {
			return selection(5, shouldUseCollars(request, 5), request);
		}
		if (request.targetWeight() <= 19 && inventory.bar10()) {
			return selection(10, shouldUseCollars(request, 10), request);
		}
		if (!shouldUseCollars(request, 15)
				&& (!inventory.bar20() || request.maximumAllowedBarWeight() == 15)
				&& inventory.bar15()) {
			return selection(15, false, request);
		}
		if (shouldUseCollars(request, 15)
				&& (!inventory.bar20() || request.maximumAllowedBarWeight() == 15)
				&& inventory.bar15()) {
			return selection(15, true, request);
		}

		int standardBar = Math.min(request.standardBarWeight(), request.maximumAllowedBarWeight());
		int actualBar = heaviestAvailableBarAtOrBelow(standardBar, inventory);
		return selection(actualBar, shouldUseCollars(request, actualBar), request);
	}

	private static Selection findBumperSelection(Request request) {
		for (int barWeight : BAR_WEIGHTS) {
			if (barWeight > request.maximumAllowedBarWeight() || !hasBar(request.inventory(), barWeight)) {
				continue;
			}
			if (shouldUseCollars(request, barWeight) && request.inventory().collars()
					&& bumperPairFits(request, request.targetWeight() - barWeight - 5)) {
				return selection(barWeight, true, request);
			}
			if (bumperPairFits(request, request.targetWeight() - barWeight)) {
				return selection(barWeight, false, request);
			}
		}
		return null;
	}

	private static boolean collarsPreservingBumpers(Request request, int barWeight) {
		boolean collarsWanted = shouldUseCollars(request, barWeight);
		if (collarsWanted && request.inventory().collars()
				&& !bumperPairFits(request, request.targetWeight() - barWeight - 5)
				&& bumperPairFits(request, request.targetWeight() - barWeight)) {
			return false;
		}
		return collarsWanted;
	}

	private static int heaviestAvailableBarAtOrBelow(int maximumWeight, Inventory inventory) {
		for (int barWeight : BAR_WEIGHTS) {
			if (barWeight <= maximumWeight && hasBar(inventory, barWeight)) {
				return barWeight;
			}
		}
		return maximumWeight;
	}

	private static boolean hasBar(Inventory inventory, int barWeight) {
		return switch (barWeight) {
			case 20 -> inventory.bar20();
			case 15 -> inventory.bar15();
			case 10 -> inventory.bar10();
			case 5 -> inventory.bar5();
			default -> false;
		};
	}

	private static boolean bumperPairFits(Request request, int remainingWeight) {
		int smallestPair = request.inventory().smallestBumperPairWeight();
		return smallestPair > 0 && smallestPair <= remainingWeight;
	}

	private static boolean shouldUseCollars(Request request, int barWeight) {
		if (barWeight == 5 && request.noCollars5kgBar()) {
			return false;
		}
		int threshold = request.collarThreshold() - (20 - barWeight);
		return request.targetWeight() >= threshold;
	}

	private static Selection selection(int barWeight, boolean useCollars, Request request) {
		return new Selection(barWeight, useCollars, barWeight != request.standardBarWeight());
	}

	public record Inventory(boolean bar20, boolean bar15, boolean bar10, boolean bar5,
			boolean collars, int smallestBumperPairWeight) {
	}

	public record Request(int targetWeight, int standardBarWeight, int maximumAllowedBarWeight,
			boolean useUsawCollars, boolean noCollars5kgBar, boolean useNonStandardBar, int nonStandardBarWeight,
			int collarThreshold, Inventory inventory) {
	}

	public record Selection(int barWeight, boolean useCollars, boolean lightBarInUse) {
	}
}