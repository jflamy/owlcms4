/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package playwright;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * Each user is a different context in a Chromium browser (as if all starting
 * incognito sessions).
 * Optionnally, poll the different browsers to retrieve a value, in order to
 * confirm that Vaadin push is working.
 * 
 * @author Jean-François Lamy
 */
public class RunResults {
    private static final int NB_REMOTE_USERS = 50;
    private static List<Page> activePages = new ArrayList<>(NB_REMOTE_USERS);

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        String users = System.getProperty("users");
        int nbUsers = users != null ? Integer.parseInt(users) : NB_REMOTE_USERS;
        String polling = System.getProperty("polling");
        int pollingInterval = polling != null ? Integer.parseInt(polling) : 0;
        String url = System.getProperty("url");
        url = url != null ? url : "http://192.168.1.174:8082/results?silent=true&lifting=false&fop=A";
        
        try (Playwright playwright = Playwright.create()) {

            // create a single browser; we will create independent sessions.
            Browser browser = playwright.chromium().launch();
            for (int i = 0; i < nbUsers; i++) {
                // create two tabs in each incognito session, switch to the publicresults one
                // this will allow closing the tab, or switching to the other one.
                BrowserContext newContext = browser.newContext();
                Page emptyPage = newContext.newPage();
                Page page = newContext.newPage();

                page.navigate(url);
                page.bringToFront();
                activePages.add(page);
                System.out.println("creating page " + (i + 1));
            }

            if (pollingInterval > 0) {
                // periodically poll the browsers to check content.
                // loop forever, we must be killed externally.
                while (true) {
                    // ask each active publicresults tab for the name of the current athlete
                    // to check whether the Vaadin push has worked
                    System.out.println();
                    System.out.println(LocalDateTime.now());
                    int i = 0;
                    for (Page page: activePages) {
                        String res = page.innerHTML("div.v-status-message span");
                        
                        String innerHTML = "";
                        try {
                            innerHTML = (String) page.evaluate("() => {" +
                                    "const shadowHost = document.querySelector('#owlcmsTemplate');" + // Access the shadow host
                                    "const shadowRoot = shadowHost.shadowRoot;" + // Get the shadow root
                                    "const targetElement = shadowRoot.querySelector('div > div > div.attemptBar > div > div.fullName.ellipsis');" +
                                    "return targetElement.innerHTML;" + // Return the innerHTML of the target element
                                    "}");
                        } catch (Exception e) {
                            innerHTML = "error.";
                        }
                        System.out.println((i + 1) + " " + res + " " +innerHTML);
                        i++;
                    }
                    try {
                        Thread.sleep(pollingInterval);
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
