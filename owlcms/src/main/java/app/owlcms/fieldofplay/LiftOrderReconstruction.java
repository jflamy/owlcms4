package app.owlcms.fieldofplay;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import app.owlcms.data.athlete.Athlete;

public class LiftOrderReconstruction {

    final static String LINESEPARATOR = System.getProperty("line.separator");

    private TreeSet<LiftOrderInfo> pastOrder;

    public LiftOrderReconstruction(FieldOfPlay fop) {
        computePastOrder(fop);
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
                switch (liftNo) {
                case 0:
                    prevweight = 0;
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getSnatch1ActualLift()));
                    break;
                case 1:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getSnatch2ActualLift()));
                    break;
                case 2:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getSnatch3ActualLift()));
                    break;
                case 3:
                    prevweight = 0;
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk1ActualLift()));
                    break;
                case 4:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk2ActualLift()));
                    break;
                case 5:
                    ali.weight = Math.abs(Athlete.zeroIfInvalid(a.getCleanJerk3ActualLift()));
                    break;
                }
                ali.athlete = a;
                ali.attemptNo = liftNo;
                ali.progression = ali.weight - prevweight;
                prevweight = ali.weight;
                ali.startNumber = a.getStartNumber();
                ali.lotNumber = a.getLotNumber();
                
                if (ali.weight > 0) {
                    this.pastOrder.add(ali);
                }

            }
        }
        return this.pastOrder;
    }
    
    public LiftOrderInfo getLastLift() {
        if (this.pastOrder == null || this.pastOrder.isEmpty()) {
            return null;
        }
        return this.pastOrder.last();
        
    }

}
