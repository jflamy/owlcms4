package publish;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(HelloServlet.class);

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
    }

}