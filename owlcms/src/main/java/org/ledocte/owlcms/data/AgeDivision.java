package org.ledocte.owlcms.data;

import java.util.Arrays;
import java.util.Collection;

public enum AgeDivision {
    DEFAULT, SENIOR, JUNIOR, YOUTH, KIDS, MASTERS, TRADITIONAL, A, B, C, D;

//    @Override
//    public String toString() {
//        return (isDefault() ? "" : name().charAt(0) + name().substring(1).toLowerCase());
//    }

    public String getCode() {
        return (isDefault() ? "" : name().substring(0,1).toLowerCase());
    }

    public boolean isDefault() {
        return this == DEFAULT;
    }

    static public AgeDivision getAgeDivisionFromCode(String code) {
        for (AgeDivision curAD : AgeDivision.values()) {
            if (code.equals(curAD.getCode())) {
                return curAD;
            }
        }
        return AgeDivision.DEFAULT;
    }

	public static Collection<AgeDivision> findAll() {
		return Arrays.asList(AgeDivision.values());
	}
}
