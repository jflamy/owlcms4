package publish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

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
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter osw = new PrintWriter(new OutputStreamWriter(resp.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            StringBuilder content = new StringBuilder();

            while ((line = br.readLine()) != null) {
                content.append(line);
                logger.warn("{}", line);
                content.append(System.lineSeparator());
            }
            osw.print("parameter length="+content.length());
        }
    }

}