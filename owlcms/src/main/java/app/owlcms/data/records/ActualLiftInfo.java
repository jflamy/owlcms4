package app.owlcms.data.records;

import java.time.LocalDateTime;

import app.owlcms.data.athlete.Athlete;

class ActualLiftInfo {
	private Athlete a;
	private Integer lift;
	private int liftNo;
	private LocalDateTime t;

	@Override
	public String toString() {
		return "ActualLiftInfo [a=" + getA().getAbbreviatedName() + ", lift=" + getLift() + ", liftNo=" + getLiftNo() + ", t=" + getT() + "]";
	}

	LocalDateTime getT() {
		return t;
	}

	void setT(LocalDateTime t) {
		this.t = t;
	}

	int getLiftNo() {
		return liftNo;
	}

	void setLiftNo(int liftNo) {
		this.liftNo = liftNo;
	}

	Integer getLift() {
		return lift;
	}

	void setLift(Integer lift) {
		this.lift = lift;
	}

	Athlete getA() {
		return a;
	}

	void setA(Athlete a) {
		this.a = a;
	}
}