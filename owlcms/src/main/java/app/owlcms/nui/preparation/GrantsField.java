package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.i18n.Translator;

/** Rows of role and platform scope; a scope is only offered for roles that can be limited to a platform. */
@SuppressWarnings("serial")
class GrantsField extends CustomField<List<RoleGrant>> {

	private static final String ALL_PLATFORMS = "";

	private record Row(HorizontalLayout layout, ComboBox<Role> role, ComboBox<String> scope) {
	}

	private final VerticalLayout rows = new VerticalLayout();
	private final List<Row> rowList = new ArrayList<>();
	private final List<String> platforms;
	private boolean rebuilding;

	GrantsField(List<String> platforms) {
		super(new ArrayList<>());
		this.platforms = platforms;
		this.rows.setPadding(false);
		this.rows.setSpacing(false);
		Button add = new Button(Translator.translate("Access.Grant.Add"), VaadinIcon.PLUS.create());
		add.setEnabled(false);
		add.setVisible(false);
		add(this.rows, new HorizontalLayout(add));
		setPresentationValue(List.of());
	}

	@Override
	protected List<RoleGrant> generateModelValue() {
		List<RoleGrant> result = new ArrayList<>();
		for (Row row : this.rowList) {
			Role role = row.role().getValue();
			if (role == null) {
				continue;
			}
			String scope = row.scope().getValue();
			RoleGrant grant = new RoleGrant(role, scope == null || ALL_PLATFORMS.equals(scope) ? null : scope);
			if (!result.contains(grant)) {
				result.add(grant);
			}
		}
		return result;
	}

	@Override
	protected void setPresentationValue(List<RoleGrant> grants) {
		this.rebuilding = true;
		try {
			this.rows.removeAll();
			this.rowList.clear();
			if (grants == null || grants.isEmpty()) {
				addRow(new RoleGrant(null, null));
			} else {
				grants.forEach(this::addRow);
			}
		} finally {
			this.rebuilding = false;
		}
	}

	private void addRow(RoleGrant grant) {
		ComboBox<Role> role = new ComboBox<>();
		role.setItems(grantableRoles());
		role.setItemLabelGenerator(r -> Translator.translate("Access.Role." + r.name()));
		role.setAllowCustomValue(false);
		role.setPlaceholder(Translator.translate("Access.Grant.Role"));

		List<String> scopes = new ArrayList<>();
		scopes.add(ALL_PLATFORMS);
		scopes.addAll(this.platforms);
		String current = grant.getPlatformName() == null ? ALL_PLATFORMS : grant.getPlatformName();
		if (!scopes.contains(current)) {
			scopes.add(current);
		}
		ComboBox<String> scope = new ComboBox<>();
		scope.setItems(scopes);
		scope.setItemLabelGenerator(this::scopeLabel);
		scope.setAllowCustomValue(false);
		scope.setPlaceholder(Translator.translate("Access.Grant.Scope"));

		HorizontalLayout layout = new HorizontalLayout();
		layout.setAlignItems(Alignment.CENTER);
		Row row = new Row(layout, role, scope);
		Button remove = new Button(VaadinIcon.TRASH.create(), e -> {
			this.rows.remove(layout);
			this.rowList.remove(row);
			if (this.rowList.isEmpty()) {
				setPresentationValue(List.of());
			}
			changed();
		});
		layout.add(role, scope, remove);
		this.rows.add(layout);
		this.rowList.add(row);

		role.setValue(grant.getRole());
		scope.setValue(current);
		limitScopeToScopableRoles(row);
		role.addValueChangeListener(e -> {
			limitScopeToScopableRoles(row);
			changed();
		});
		scope.addValueChangeListener(e -> changed());
		if (!this.rebuilding) {
			changed();
		}
	}

	private void limitScopeToScopableRoles(Row row) {
		Role role = row.role().getValue();
		boolean scopable = role != null && role.isPlatformScopable();
		row.scope().setEnabled(scopable);
		if (!scopable && !ALL_PLATFORMS.equals(row.scope().getValue())) {
			row.scope().setValue(ALL_PLATFORMS);
		}
	}

	private void changed() {
		if (!this.rebuilding) {
			updateValue();
		}
	}

	private String scopeLabel(String scope) {
		if (ALL_PLATFORMS.equals(scope)) {
			return Translator.translate("Access.Grant.AllPlatforms");
		}
		return this.platforms.contains(scope) ? scope
		        : scope + " (" + Translator.translate("Access.Grant.UnknownPlatform") + ")";
	}

	private static List<Role> grantableRoles() {
		List<Role> result = new ArrayList<>();
		for (Role role : Role.values()) {
			if (role.isGrantable()) {
				result.add(role);
			}
		}
		return result;
	}
}
