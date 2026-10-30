package app.owlcms.nui.admin;

import java.util.stream.Collectors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.ContentWrapping;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.OwlcmsLayoutAware;
import app.owlcms.simulation.SimulationControl;

/** Starts and stops the competition simulation. Requires a login: the page is not token-accessible. */
@SuppressWarnings("serial")
@RequiresRole({ Role.ADMIN_PAGES, Role.COMPETITION_SUPERVISOR })
@Route(value = "simulation", layout = OwlcmsLayout.class)
public class SimulationView extends Composite<VerticalLayout>
        implements OwlcmsLayoutAware, ContentWrapping, HasDynamicTitle {

	/** A simulation also ends on its own or from a script: the page follows its state while open. */
	private static final int STATUS_POLL_MS = 2000;

	private final Checkbox skipDone = new Checkbox(Translator.translate("Simulation.SkipDone"));
	private final Checkbox randomDeclarationJumps = new Checkbox(
	        Translator.translate("Simulation.RandomDeclarationJumps"));
	private final TextField skipBefore = new TextField(Translator.translate("Simulation.SkipBefore"));
	private final TextField platforms = new TextField(Translator.translate("Simulation.Platforms"));
	private final Paragraph status = new Paragraph();
	private final Paragraph message = new Paragraph();
	private final Button start = new Button(Translator.translate("Simulation.Start"));
	private final Button stop = new Button(Translator.translate("Simulation.Stop"));
	private OwlcmsLayout routerLayout;
	private Registration pollRegistration;

	public SimulationView() {
		String available = PlatformRepository.findAll().stream()
		        .map(platform -> platform.getName())
		        .filter(name -> name != null && !name.isBlank())
		        .sorted(String.CASE_INSENSITIVE_ORDER)
		        .collect(Collectors.joining(", "));
		if (!available.isBlank()) {
			this.platforms.setHelperText(Translator.translate("Simulation.AvailablePlatforms", available));
		}
		this.skipBefore.setWidth("20em");
		this.platforms.setWidth("20em");

		this.start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		this.start.addClickListener(event -> {
			SimulationControl.Result result = SimulationControl.start(this.skipDone.getValue(),
			        this.skipBefore.getValue(), this.platforms.getValue(), this.randomDeclarationJumps.getValue());
			this.message.setText(Translator.translate(result == SimulationControl.Result.STARTED ? "Simulation.Started"
			        : "Simulation.AlreadyRunning"));
			// the simulation thread marks itself running a moment later
			showState(true);
		});
		this.stop.addClickListener(event -> {
			SimulationControl.Result result = SimulationControl.stop();
			this.message.setText(Translator.translate(result == SimulationControl.Result.STOPPED
			        ? "Simulation.StopRequested"
			        : "Simulation.NotRunning"));
			showState(false);
		});

		getContent().add(new Paragraph(Translator.translate("Simulation.Description")), this.status, this.skipDone,
		        this.randomDeclarationJumps, this.skipBefore, this.platforms, new HorizontalLayout(this.start, this.stop),
		        this.message);
		showState(SimulationControl.isRunning());
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		UI ui = attachEvent.getUI();
		ui.setPollInterval(STATUS_POLL_MS);
		this.pollRegistration = ui.addPollListener(event -> showState(SimulationControl.isRunning()));
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		if (this.pollRegistration != null) {
			this.pollRegistration.remove();
			this.pollRegistration = null;
		}
		detachEvent.getUI().setPollInterval(-1);
		super.onDetach(detachEvent);
	}

	/** Start is offered only while stopped, Stop only while running; the options apply to the next start. */
	private void showState(boolean running) {
		this.status.setText(Translator.translate(running ? "Simulation.Running" : "Simulation.Stopped"));
		this.start.setEnabled(!running);
		this.stop.setEnabled(running);
		if (running) {
			this.stop.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		} else {
			this.stop.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		}
		this.skipDone.setEnabled(!running);
		this.randomDeclarationJumps.setEnabled(!running);
		this.skipBefore.setEnabled(!running);
		this.platforms.setEnabled(!running);
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Simulation.Title");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setHeaderContent() {
		this.routerLayout.setMenuTitle(getPageTitle());
		this.routerLayout.setMenuArea(createMenuArea());
		this.routerLayout.showLocaleDropdown(false);
		this.routerLayout.setDrawerOpened(false);
		this.routerLayout.updateHeader(true);
	}

	@Override
	public void setPadding(boolean b) {
		// not needed
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}
