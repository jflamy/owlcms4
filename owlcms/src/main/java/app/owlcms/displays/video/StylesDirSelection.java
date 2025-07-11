/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.video;

import java.io.FileNotFoundException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

import app.owlcms.data.config.Config;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public interface StylesDirSelection {
	
	Logger logger = (Logger) LoggerFactory.getLogger(StylesDirSelection.class);

	public default void computeStylesDir(Component component) {
		logger.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%% {}\n",LoggerUtils.stackTrace());
		Logger logger = (Logger) LoggerFactory.getLogger(StylesDirSelection.class);
		Element element = component.getElement();
		if (isVideo()) {
			try {
				// use video override if /video is in the URL and the override stylesheet exists.
				String paramVideoStylesDir = Config.getCurrent().getParamVideoStylesDir();
				ResourceWalker.getFileOrResourcePath(paramVideoStylesDir);
				element.setProperty("stylesDir", paramVideoStylesDir);
				logger.warn("{} setting video styles {}", this.getClass(), paramVideoStylesDir);
				element.setProperty("video", true);
				element.setProperty("public", false);
			} catch (FileNotFoundException e) {
				// should not happen, fall back to normal style
				element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
				element.setProperty("video", true);
				element.setProperty("public", false);
				String paramVideoStylesDir4 = Config.getCurrent().getParamVideoStylesDir();
				logger.error("missing video styles {}", paramVideoStylesDir4);
			}
		} else if (isPublicDisplay()) {
			try {
				// use video override if /video is in the URL and the override stylesheet exists.
				String paramPublicStylesDir = Config.getCurrent().getParamPublicStylesDir();
				ResourceWalker.getFileOrResourcePath(paramPublicStylesDir);
				logger.warn("{} setting public styles {}", this.getClass(), paramPublicStylesDir);
				element.setProperty("stylesDir", paramPublicStylesDir);
				element.setProperty("video", false);
				element.setProperty("public", true);
			} catch (FileNotFoundException e) {
				// should not happen, fall back to normal style
				element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
				element.setProperty("video", false);
				element.setProperty("public", true);
				String paramVideoStylesDir5 = Config.getCurrent().getParamVideoStylesDir();
				logger.error("missing public styles {}", paramVideoStylesDir5);
			}
		} else {
			element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
			element.setProperty("video", false);
			element.setProperty("public", false);
			// logger.debug("no video override requested");
		}
	}

	public boolean isVideo();

	public void setVideo(boolean video);
	
	public boolean isPublicDisplay();
	
	public void setPublicDisplay(boolean publicDisplay);
	
	public default void overrideColors(Element element) {
		Logger logger = (Logger) LoggerFactory.getLogger(StylesDirSelection.class);
		boolean overrideColors = Config.getCurrent().getEnableColorOverrides();
		String videoColorOverrides = Config.getCurrent().getVideoColorOverrides();
		logger.debug("overrideColors {} videoColorOverrides {}", overrideColors, videoColorOverrides);
		if (overrideColors && videoColorOverrides != null && !videoColorOverrides.isBlank()) {
			element.setProperty("colorOverride", videoColorOverrides);			
		}
	}
}


