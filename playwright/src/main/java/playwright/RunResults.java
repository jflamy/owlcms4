/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package playwright;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/**
 * Simulate several users using the public results scoreboard.
 * 
 * Each user is a different context in a Chromium browser (as if all starting incognito sessions).
 * Optionnally, poll the different browsers to retrieve a value, in order to confirm that Vaadin push is working.
 * 
 * @author Jean-François Lamy
 */
public class RunResults {
    private static final int NB_REMOTE_USERS = 15;
    private static final int POLLING_DELAY_SECONDS = 30;
    private static final int POLLING_DELAY_MILLISECONDS = POLLING_DELAY_SECONDS*1000;

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            
            // create a number of browsers to simulate independent users
            Browser browser = playwright.chromium().launch();
            for (int i = 0; i < NB_REMOTE_USERS; i++) {
                Page page = browser.newContext().newPage();
                page.navigate("http://localhost:8082/results?silent=true&lifting=false&fop=RED");
                System.out.println("creating context "+ (i+1));
            }

            if (POLLING_DELAY_MILLISECONDS > 0) {
                // periodically poll the browsers to check content.
                // loop forever, we must be killed externally.
                while (true) {    
                    // ask each browser for the name of the current athlete
                    // to check whether the Vaadin push has worked
                    List<BrowserContext> contexts = browser.contexts();
                    int i = 0;
                    for (BrowserContext context : contexts) {
                        for (Page page : context.pages()) {
                            String res = page.innerHTML("div.v-status-message span");
                            System.out.println((i+1) + " " + res);
                            i++;
                        }
                    }
                    System.out.println();
                    try {
                        Thread.sleep(POLLING_DELAY_MILLISECONDS);
                    } catch (InterruptedException e) {
                    }
                }
            } else {
                // run to create load. Just wait for external kill.  
                // There is a single thread, so the wait for 2 threads never terminates.
                try {
                    new CyclicBarrier(2).await();
                } catch (InterruptedException | BrokenBarrierException e) {
                }
            }
        }
    }
}
