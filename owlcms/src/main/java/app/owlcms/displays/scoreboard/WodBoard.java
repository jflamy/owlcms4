package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import com.vaadin.flow.component.template.Id;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.CeremonyStarted;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;

@SuppressWarnings({ "serial", "deprecation" })
@Tag("wod-board")
@JsModule("./components/WodBoard.js")

public class WodBoard extends LitTemplate implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay,
        RequireDisplayLogin {
    private FieldOfPlay fop;
    private Group group;
    private String routeParameter;

    @Override
    public FieldOfPlay getFop() {
        return this.fop;
    }

    @Override
    public Group getGroup() {
        return this.group;
    }

    @Override
    public String getRouteParameter() {
        return this.routeParameter;
    }

    @Override
    public void setFop(FieldOfPlay fop) {
        this.fop = fop;
    }

    @Override
    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    public void setRouteParameter(String routeParameter) {
        this.routeParameter = routeParameter;
    }

    @Override
    public void doBreak(UIEvent e) {
    }

    @Override
    public void doCeremony(CeremonyStarted e) {
    }

    protected final Logger logger = (Logger) LoggerFactory.getLogger(WodBoard.class);
    @Id("breakTimer")
    private BreakTimerElement breakTimer;
    @SuppressWarnings("unused")
    private boolean silenced;

    public WodBoard() {
        super();
        // Set the 'athletes' property to the first 4 athletes in FOP display order
        OwlcmsSession.withFop(fop -> {
            var displayOrder = fop.getDisplayOrder();
            var jath = Json.createArray();
            for (int i = 0; i < 4; i++) {
                var ja = Json.createObject();
                if (displayOrder != null && i < displayOrder.size()) {
                    var a = displayOrder.get(i);
                    ja.put("name", a.getFullName());
                    ja.put("club", a.getTeam());
                } else {
                    ja.put("name", "");
                    ja.put("club", "");
                }
                jath.set(i, ja);
            }
            this.getElement().setPropertyJson("athletes", jath);
        });
    }

    public BreakTimerElement getBreakTimer() {
        return breakTimer;
    }

    public void setBreakTimer(BreakTimerElement breakTimer) {
        this.breakTimer = breakTimer;
    }

    @Override
    public boolean isDarkMode() {
        return true;
    }

    @Override
    public boolean isPublicDisplay() {
        return false;
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public void setAbbreviatedName(boolean b) {
    }

    @Override
    public void setDarkMode(boolean dark) {
    }

    @Override
    public void setEmFontSize(Double emFontSize) {
    }

    @Override
    public void setLeadersDisplay(boolean showLeaders) {
    }

    @Override
    public void setPublicDisplay(boolean publicDisplay) {
    }

    @Override
    public void setRecordsDisplay(boolean showRecords) {
    }

    @Override
    public void setTeamWidth(Double tw) {
    }

    @Override
    public void setVideo(boolean b) {
    }

    @Override
    public void setSilenced(boolean silent) {
        this.silenced = true;
    }
}
