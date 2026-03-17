/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.LoginView;
import ch.qos.logback.classic.Logger;

public interface AuthorizationDispatch extends BeforeEnterObserver {

	Logger logger = (Logger) LoggerFactory.getLogger(AuthorizationDispatch.class);

	@Override
	public default void beforeEnter(BeforeEnterEvent event) {
		OwlcmsFactory.waitDBInitialized();
		String path = event.getLocation().getPath();
		QueryParameters queryParameters = event.getLocation().getQueryParameters();

		boolean recordsOnly = Config.getCurrent().isRecordRepository();

		// Dispatch root URL based on mode
		if (path.isEmpty()) {
			if (recordsOnly) {
				event.forwardTo("publicRecords");
				return;
			} else if (isMobileRefereeMode()) {
				// future: forward to mobile refereeing page
			}
			// default: fall through to normal HomeNavigationContent handling
		}

		if (recordsOnly) {
			if (isRecordsOnlyPublicPath(path)) {
				return;
			}

			if (isRecordsOnlyAdminPath(path)) {
				if (AccessUtils.isBackdoorAccess()) {
					return;
				}
				event.forwardTo("recordsPreparation");
				return;
			}

			if (!isRecordsOnlySecretaryPath(path)) {
				event.forwardTo("publicRecords");
				return;
			}
		}

		boolean isAuthenticated = OwlcmsSession.isAuthenticated();
		if (isAuthenticated) {
			// no check required
			OwlcmsSession.setAuthenticated(true);
			return;
		}

		if (recordsOnly) {
			OwlcmsSession.setRequestedUrl(path);
			OwlcmsSession.setRequestedQueryParameters(queryParameters);
		} else if (!path.equals(LoginView.LOGIN)) {
			OwlcmsSession.setRequestedUrl(path);
			OwlcmsSession.setRequestedQueryParameters(queryParameters);
		}

		if (recordsOnly && !isRecordsOnlySecretaryPath(path)) {
			return;
		}

		String paramPin = Config.getCurrent().getParamPin();
		String dbPin = Config.getCurrent().getPin();
		String backdoorList = Config.getCurrent().getParamBackdoorList();

		boolean pinOverride = paramPin != null && paramPin.isBlank();
		boolean backdoor = backdoorList != null && !backdoorList.isBlank();
		if (pinOverride) {
			// no check required
			logger.debug("noPin {}", paramPin == null ? null : paramPin.length());
			OwlcmsSession.setAuthenticated(true);
		} else {
			boolean pinExpected = (paramPin != null && !paramPin.isBlank()) || (dbPin != null && !dbPin.isBlank());
			// non-whitelisted addresses will get the login page, but will stubbornly be
			// denied login.
			// we don't return 403
			if (pinExpected) {
				logger.debug("paramPin {} pin {}", paramPin, dbPin);
				String clientIp = AccessUtils.getClientIp();
				if (backdoor && AccessUtils.checkBackdoor(clientIp)) {
					// explicit backdoor access allowed (e.g. for video capture of browser screens)
					logger.info("Backdoor access from {}", clientIp);
					OwlcmsSession.setAuthenticated(true);
				} else if (!path.equals(LoginView.LOGIN)) {
					// prompt user for PIN
					event.forwardTo(LoginView.LOGIN);
				} else {
					// already on login view, do nothing.
					// login will send to home.
				}
			} else {
				logger.debug("no pin expected {} {}", paramPin, dbPin);
			}
		}
	}

	private static boolean isRecordsOnlyPublicPath(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		return path.equals(LoginView.LOGIN)
		        || path.equals("publicRecords")
		        || path.equals("recordsPreparation");
	}

	private static boolean isRecordsOnlySecretaryPath(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		return path.equals("preparation/recordsConfig")
		        || path.equals("preparation/recordsConfig")
		        || path.equals("preparation/records");
	}

	private static boolean isRecordsOnlyAdminPath(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		return path.equals("preparation/config");
	}

	private static boolean isMobileRefereeMode() {
		return false;
	}

}
