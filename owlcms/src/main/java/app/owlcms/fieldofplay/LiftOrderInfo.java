/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.Objects;

import app.owlcms.data.athlete.Athlete;

public class LiftOrderInfo implements Comparable<LiftOrderInfo> {
	private Athlete athlete;
	private int attemptNo;
	private int lotNumber;
	private int progression;
	private int startNumber;
	private int weight;
	private int cumulativeProgression;

	@Override
	public int compareTo(LiftOrderInfo actualLiftInfo) {
		int compare = 0;

		// all snatches come before all clean and jerks
		if (this.getAttemptNo() <= 3 && actualLiftInfo.getAttemptNo() > 3) {
			return -1;
		}
		if (this.getAttemptNo() > 3 && actualLiftInfo.getAttemptNo() <= 3) {
			return 1;
		}

		// same kind of lift.
		
		compare = Integer.compare(this.getWeight(), actualLiftInfo.getWeight());
		if (compare != 0) {
			// same attempt, bigger weight goes later
			return compare;
		}

		compare = Integer.compare(this.getAttemptNo(), actualLiftInfo.getAttemptNo());
		if (compare != 0) {
			// smaller attempt first
			return compare;
		}

		compare = Integer.compare(this.getProgression(), actualLiftInfo.getProgression());
		if (compare != 0) {
			// same weight, same attempt, bigger progression first
			return -compare;
		}
		
		// when same progression between 2 and 3 must also look at progression between 1 and 2
		compare = Integer.compare(this.getCumulativeProgression(), actualLiftInfo.getCumulativeProgression());
		if (compare != 0) {
			return -compare;
		}

		compare = Integer.compare(this.getStartNumber(), actualLiftInfo.getStartNumber());
		if (compare != 0) {
			// smaller start number first
			return compare;
		}

		compare = Integer.compare(this.getLotNumber(), actualLiftInfo.getLotNumber());
		if (compare != 0) {
			// should not happen. smaller lot number first.
			return compare;
		}

		return compare;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		LiftOrderInfo other = (LiftOrderInfo) obj;
		return getAttemptNo()==(other.getAttemptNo())
		        && getLotNumber()==(other.getLotNumber()) && getProgression()==(other.getProgression()) && getCumulativeProgression()==(other.getCumulativeProgression())
		        && getStartNumber()==(other.getStartNumber()) && getWeight()==(other.getWeight());
	}

	public Athlete getAthlete() {
		return this.athlete;
	}

	public int getAttemptNo() {
		return this.attemptNo;
	}

	public int getLotNumber() {
		return this.lotNumber;
	}

	public int getProgression() {
		return this.progression;
	}

	public int getStartNumber() {
		return this.startNumber;
	}

	public int getWeight() {
		return this.weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		        + Objects.hash(getAttemptNo(), getLotNumber(), getProgression(), getCumulativeProgression(), getStartNumber(), getWeight());
		return result;
	}

	public void setAthlete(Athlete athlete) {
		this.athlete = athlete;
	}

	public void setAttemptNo(int attemptNo) {
		// 1-based !!!
		this.attemptNo = attemptNo;
	}

	public void setLotNumber(int lotNumber) {
		this.lotNumber = lotNumber;
	}

	public void setProgression(int progression) {
		this.progression = progression;
	}

	public void setStartNumber(int startNumber) {
		this.startNumber = startNumber;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	@Override
	public String toString() {
		return "LiftOrderInfo [athlete=" + getAthlete().getLastName() + ", weight=" + getWeight() + ", attemptNo="
		        + getAttemptNo()
		        + ", progression=" + getProgression() + ", startNumber=" + getStartNumber() + ", lotNumber="
		        + getLotNumber()
		        + "]";
	}

	public void setCumulativeProgression(int cumulativeProgression) {
		this.cumulativeProgression = cumulativeProgression;
	}

	public int getCumulativeProgression() {
		return cumulativeProgression;
	}

}