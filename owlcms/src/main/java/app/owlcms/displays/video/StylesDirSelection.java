package app.owlcms.displays.video;

import java.io.FileNotFoundException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

import app.owlcms.data.config.Config;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public interface StylesDirSelection {

	public boolean isVideo();
	public void setVideo(boolean video);

	public default void checkVideo(String cssPath, Component component) {
		Logger logger = (Logger) LoggerFactory.getLogger(StylesDirSelection.class);
		Element element = component.getElement();
		if (isVideo()) {
			try {
				logger.warn("{} setting video styles {}", this.getClass(), cssPath);
				// use video override if /video is in the URL and the override stylesheet exists.
				ResourceWalker.getFileOrResourcePath(cssPath);
				element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
				element.setProperty("video", "video/");
			} catch (FileNotFoundException e) {
				element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
				element.setProperty("video", "");
				logger.error("missing video override {}", cssPath);
			}
		} else {
			element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
			element.setProperty("video", "");
			logger.warn("no video override requested");
		}

	}
}
