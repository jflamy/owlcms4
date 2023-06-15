package app.owlcms.displays;

import java.io.FileNotFoundException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

import app.owlcms.utils.ResourceWalker;

public interface VideoOverride {
	
	public void setVideo(boolean b);
	
	public boolean isVideo();
	
	public default void checkVideo(String cssPath, String routeParameter, Component component) {
		try {
			setVideo(routeParameter != null && routeParameter.contentEquals("video"));
			// use video override if /video is in the URL and the override stylesheet exists.
			ResourceWalker.getFileOrResourcePath(cssPath);
			Element element = component.getElement();
			element.setProperty("video", routeParameter != null ? routeParameter + "/" : "");	
		} catch (FileNotFoundException e) {
		}
	}
}
