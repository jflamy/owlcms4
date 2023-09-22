package app.owlcms.displays.video;

import java.io.FileNotFoundException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

import app.owlcms.data.config.Config;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

public interface VideoCSSOverride {
	
	public void setVideo(boolean b);
	
	public boolean isVideo();
	
	public default void checkVideo(String cssPath, String routeParameter, Component component) {
		Logger logger = (Logger)LoggerFactory.getLogger(VideoCSSOverride.class);
		try {
			boolean b = routeParameter != null && routeParameter.contentEquals("video");
			setVideo(b);
			logger.warn("checking video {} {}", routeParameter, b);
			// use video override if /video is in the URL and the override stylesheet exists.
			ResourceWalker.getFileOrResourcePath(cssPath);
			Element element = component.getElement();
			element.setProperty("stylesDir", Config.getCurrent().getParamStylesDir());
			element.setProperty("video", routeParameter != null ? routeParameter + "/" : "");	
		} catch (FileNotFoundException e) {
			logger.warn("not video!");
		}
	}
}
