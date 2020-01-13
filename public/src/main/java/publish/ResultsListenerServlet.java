package publish;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import ch.qos.logback.classic.Logger;

@WebServlet("/results")
public class ResultsListenerServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(ResultsListenerServlet.class);
    static EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());
    
    public static EventBus getEventBus() {
        return eventBus;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        PrintWriter osw = new PrintWriter(resp.getOutputStream());
        osw.print("Hello!");
        osw.flush();
        osw.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
        for (Entry<String, String[]> pair : pairs) {
            logger.warn("{} = {}", pair.getKey(), pair.getValue()[0]);
        }
        
        eventBus.post(new UpdateEvent(req.getParameter("leaders")));
    }
    
    

}