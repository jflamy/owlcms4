package playwright;

import java.util.List;

import com.microsoft.playwright.*;

public class RunResults {
    private static final int NB_PAGES = 50;

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page[] pages = new Page[100];

            for (int i = 0; i < NB_PAGES; i++) {
                pages[i] = browser.newContext().newPage();
                Page page = pages[i];
                page.navigate("http://localhost:8082/displays/scoreleader?fop=A");
                System.out.println(i);
            }

            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
                System.out.println("---");
                List<BrowserContext> contexts = browser.contexts();
                int i = 0;
                for (BrowserContext context : contexts) {
                    for (Page page : context.pages()) {
                        page.bringToFront();
                        String content = page.content();
//                        System.out.println(content);
                        int pushIdIx = content.indexOf("fullName");
                        int endIx = content.indexOf("}", pushIdIx);
                        if (endIx >= pushIdIx) {
                            String idString = content.substring(pushIdIx - 1, endIx);
                            System.out.println(i++ + " " + idString);
                        }
                        page.reload();
                    }
                }
            }
        }
    }
}
