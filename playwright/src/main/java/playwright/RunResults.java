/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package playwright;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
    private static final String DEFAULT_URL = "http://192.168.1.174:8096/start-order?fop=A";
    private static final int DEFAULT_NB_REMOTE_USERS = 30;
    private static final int POLLING_DELAY_SECONDS = 0; // set to 0 to disable polling
    private static final int POLLING_DELAY_MILLISECONDS = POLLING_DELAY_SECONDS*1000;
    private static Map<BrowserContext,Page> activePages = new HashMap<>();

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        // Parse command-line arguments: url number-of-users
        String url = DEFAULT_URL;
        int nbRemoteUsers = DEFAULT_NB_REMOTE_USERS;
        
        if (args.length >= 2) {
            url = args[0];
            try {
                nbRemoteUsers = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid number of users: " + args[1]);
                printUsage();
                System.exit(1);
            }
        } else if (args.length == 1) {
            System.err.println("Error: Both URL and number of users arguments are required");
            printUsage();
            System.exit(1);
        }
        System.out.println("Target URL: " + url);
        System.out.println("Simulating " + nbRemoteUsers + " remote users");
        
        try (Playwright playwright = Playwright.create()) {
            
            // create a single browser; we will create independent sessions.
            Browser browser = playwright.chromium().launch();
            for (int i = 0; i < nbRemoteUsers; i++) {
                // create two tabs in each incognito session, switch to the publicresults one
                // this will allow closing the tab, or switching to the other one.
                BrowserContext newContext = browser.newContext();
                Page emptyPage = newContext.newPage();
                Page page = newContext.newPage();
                page.navigate(url);
                page.bringToFront();
                activePages.put(newContext,page);
                System.out.println("creating context "+ (i+1));
            }

            if (POLLING_DELAY_MILLISECONDS > 0) {
                // periodically poll the browsers to check content.
                // loop forever, we must be killed externally.
                while (true) {
                    System.err.println("polling");
                    // ask each active publicresults tab for the name of the current athlete
                    // to check whether the Vaadin push has worked
                    int i = 0;
                    for (Entry<BrowserContext, Page> entry : activePages.entrySet()) {
                        Page page = entry.getValue();
                        String res = page.innerHTML("body > div > div.scoreboard > header > div.lifter-info > div.name-and-team > span.lifter-name");
                        System.out.println((i+1) + " " + res);
                        i++;
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
    
    private static void printUsage() {
        System.err.println("Usage: java -jar playwright-4.22.0-jar-with-dependencies.jar <url> <users>");
        System.err.println("");
        System.err.println("Arguments:");
        System.err.println("  url   - Full URL to the scoreboard (e.g., http://localhost:8096/start-order?fop=A)");
        System.err.println("  users - Number of concurrent browser sessions to simulate (e.g., 50)");
        System.err.println("");
        System.err.println("Examples:");
        System.err.println("  java -jar playwright-4.22.0-jar-with-dependencies.jar http://localhost:8096/start-order?fop=A 50");
        System.err.println("  java -jar playwright-4.22.0-jar-with-dependencies.jar http://192.168.1.174:8080/lifting-order?fop=B 100");
        System.err.println("");
        System.err.println("Defaults (if no arguments):");
        System.err.println("  url: " + DEFAULT_URL);
        System.err.println("  users: " + DEFAULT_NB_REMOTE_USERS);
    }
}
