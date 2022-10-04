package app.owlcms.displays.scoreboard;

import java.util.List;
import java.util.function.BiPredicate;

import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;

@SuppressWarnings("serial")
@Route("displays/resultsLiftingOrder")
public class ResultsLiftingOrder extends Results {

    @Override
    public String getPageTitle() {
        return getTranslation("Scoreboard.LiftingOrder") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    protected List<Athlete> getOrder(FieldOfPlay fop) {
        return fop.getLiftingOrder();
    }

    /**
     * return true if change of lifts
     */
    @Override
    protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
        BiPredicate<Athlete, Athlete> separator = (cur, prev) -> (prev == null) ||
                cur.getActuallyAttemptedLifts() >= 3
                        && (prev.getActuallyAttemptedLifts() < 3);
        return separator;
    }

    @Override
    protected int countSubsets(List<Athlete> athlete) {
        return 1 + (athlete.get(0).getActuallyAttemptedLifts() >= 3 ? 2 : 1);
    }

}
