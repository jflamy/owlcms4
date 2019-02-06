package org.ledocte.owlcms;

import com.vaadin.flow.server.startup.ServletContextListeners;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.*;

import java.net.URI;
import java.net.URL;

public class Main {

    public static void main(String... args) throws Exception {
    	// read server.port parameter from -D"server.port"=9999 on java command line
    	Integer serverPort = Integer.getInteger("server.port",8080);
        new Main().run(serverPort, "/");
    }

    public void run(int port, String contextPath) throws Exception {
        URL webRootLocation = this.getClass().getResource("/META-INF/resources/");
        URI webRootUri = webRootLocation.toURI();

        WebAppContext context = new WebAppContext();
        context.setBaseResource(Resource.newResource(webRootUri));
        context.setContextPath(contextPath);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*");
        context.setConfigurationDiscovered(true);
        context.setConfigurations(new Configuration[]{
                new AnnotationConfiguration(),
                new WebInfConfiguration(),
                new WebXmlConfiguration(),
                new MetaInfConfiguration(),
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new PlusConfiguration(),
                new JettyWebXmlConfiguration()
        });
        context.getServletContext().setExtendedListenerTypes(true);
        context.addEventListener(new ServletContextListeners());

        Server server = new Server(port);
        server.setHandler(context);

        server.start();
        server.join();
    }
}