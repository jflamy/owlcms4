import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.awt.Desktop;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) throws Exception {

        // Pick a free port automatically
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);

        // Serve embedded HTML
        server.createContext("/", (HttpExchange exchange) -> {
            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Update Required</title>
                    <style>
                        body {
                            font-family: sans-serif;
                            max-width: 600px;
                            margin: 40px auto;
                            line-height: 1.6;
                        }
                        h1 {
                            color: #b00;
                        }
                    </style>
                </head>
                <body>
                    <h1>Update Required</h1>
                    <p>This version of MyApp requires <strong>Java 21</strong>.</p>
                    <p>Your system is currently running an older Java version.</p>
                    <p>Please update your launcher to continue.</p>
                    <p>This page is served locally — no internet connection is required.</p>
                </body>
                </html>
            """;

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(null);
        server.start();

        int port = server.getAddress().getPort();
        String url = "http://localhost:" + port + "/";

        // Open browser
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception e) {
                System.out.println("Please open this URL in your browser: " + url);
            }
        } else {
            System.out.println("Please open this URL in your browser: " + url);
        }

        // Keep server alive
        System.out.println("Shim server running at " + url);
        System.out.println("Press Ctrl+C to exit.");
    }
}