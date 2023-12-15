package app.owlcms.displays.video;

import java.io.FileNotFoundException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

import app.owlcms.data.config.Config;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public interface StylesDirSelection {

	public default void checkVideo(Component component) {
		Logger logger = (Logger) LoggerFactory.getLogger(StylesDirSelection.class);
		Element element = component.getElement();
		if (isVideo()) {
			try {
				//logger.debug("{} setting video styles {}", this.getClass(), cssPath);
				// use video override if /video is in the URL and the override stylesheet exists.
				ResourceWalker.getFileOrResourcePath(Config.getCurrent().getParamVideoStylesDir());
				element.setProperty("stylesDir", Config.getCurrent().getParamVideoStylesDir());
				element.setProperty("video", true);
			} catch (FileNotFoundException e) {
				// should not happen, fall back to normal style
				element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
				element.setProperty("video", true);
				logger.error("missing video styles {}", Config.getCurrent().getParamVideoStylesDir());
			}
		} else {
			element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
			element.setProperty("video", "");
			//logger.debug("no video override requested");
		}

	}
	public boolean isVideo();

	public void setVideo(boolean video);
}
