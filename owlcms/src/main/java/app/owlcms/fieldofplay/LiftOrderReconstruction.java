/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Logger;

public class LiftOrderReconstruction {

    final static String LINESEPARATOR = System.getProperty("line.separator");

    private TreeSet<LiftOrderInfo> pastOrder;

    public LiftOrderReconstruction(FieldOfPlay fop) {
        computePastOrder(fop);
    }

    public LiftOrderInfo getLastLift() {
        if (this.pastOrder == null || this.pastOrder.isEmpty()) {
            return null;
        }
        return this.pastOrder.last();

    }

    public TreeSet<LiftOrderInfo> getPastOrder() {
        return pastOrder;
    }

    /**
     * @param liftList
     * @return ordered printout of lifters, one per line.
     */
    public String shortDump() {
        StringBuffer sb = new StringBuffer();
        for (LiftOrderInfo ali : this.pastOrder) {
            sb.append(ali.toString());
            sb.append(LINESEPARATOR);
        }
        return sb.toString();
    }

    public void shortDump(String string, Logger logger) {
//        logger.trace("{}{}", OwlcmsSession.getFopLoggingName(), string);
//        for (LiftOrderInfo ali : this.pastOrder) {
//            logger.trace("{}    {}", OwlcmsSession.getFopLoggingName(), ali.toString());
//        }
    }

    /**
     * Reconstructed lifting order according to rules
     *
     * @param fop
     */
    private Collection<LiftOrderInfo> computePastOrder(FieldOfPlay fop) {
        this.pastOrder = new TreeSet<>();
        List<Athlete> athletes = fop.getLiftingOrder();

        for (Athlete a : athletes) {
            int prevweight = 0;
            for (int liftNo = 0; liftNo < 6; liftNo++) {
                LiftOrderInfo ali = new LiftOrderInfo();
                int w;
                switch (liftNo) {
                case 0:
                    w = Math.abs(Athlete.zeroIfInvalid(a.getSnatch1ActualLift()));
                    prevweight = w;
                    ali.setWeight(w);
                    break;
                case 1:
                    ali.setWeight(Math.abs(Athlete.zeroIfInvalid(a.getSnatch2ActualLift())));
                    break;
                case 2:
                    ali.setWeight(Math.abs(Athlete.zeroIfInvalid(a.getSnatch3ActualLift())));
                    break;
                case 3:
                    w = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk1ActualLift()));
                    prevweight = w;
                    ali.setWeight(w);
                    break;
                case 4:
                    ali.setWeight(Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk2ActualLift())));
                    break;
                case 5:
                    ali.setWeight(Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk3ActualLift())));
                    break;
                }
                ali.setAthlete(a);
                ali.setAttemptNo(liftNo + 1);
                ali.setProgression(ali.getWeight() - prevweight);
                prevweight = Math.abs(ali.getWeight());
                ali.setStartNumber(a.getStartNumber());
                ali.setLotNumber(a.getLotNumber());

                if (ali.getWeight() > 0) {
                    this.pastOrder.add(ali);
                }

            }
        }
        return this.pastOrder;
    }

}
