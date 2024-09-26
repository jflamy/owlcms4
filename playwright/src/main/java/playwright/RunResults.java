/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package playwright;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
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
    private static final int POLLING_DELAY_SECONDS = 0;
    private static final int POLLING_DELAY_MILLISECONDS = POLLING_DELAY_SECONDS*1000;
    private static Map<BrowserContext,Page> activePages = new TreeMap<>();

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            
            // create a single browser; we will create independent sessions.
            Browser browser = playwright.chromium().launch();
            for (int i = 0; i < NB_REMOTE_USERS; i++) {
                // create two tabs in each incognito session, switch to the publicresults one
                // this will allow closing the tab, or switching to the other one.
                BrowserContext newContext = browser.newContext();
                Page emptyPage = newContext.newPage();
                Page page = newContext.newPage();
                page.navigate("http://192.168.1.174:8082/results?silent=true&lifting=false&fop=A");
                page.bringToFront();
               // activePages.put(newContext,page);
                System.out.println("creating context "+ (i+1));
            }

            if (POLLING_DELAY_MILLISECONDS > 0) {
                // periodically poll the browsers to check content.
                // loop forever, we must be killed externally.
                while (true) {    
                    // ask each active publicresults tab for the name of the current athlete
                    // to check whether the Vaadin push has worked
                    int i = 0;
                    for (Entry<BrowserContext, Page> entry : activePages.entrySet()) {
                        Page page = entry.getValue();
                        String res = page.innerHTML("div.v-status-message span");
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
}
