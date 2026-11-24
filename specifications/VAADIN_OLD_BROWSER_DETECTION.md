# How Vaadin decides that a browser is too old

Date: 2026-11-24

Applies to Vaadin 25.3.4, which is the version set in the top-level `pom.xml`. That platform release runs on Flow 25.3.5: `flow-server-25.3.5.jar` is the JAR the development server has loaded.

## Summary

- The decision is made **in the browser**, by a small inline script that Vaadin adds to every page it serves. The script tests for two browser features. It does not look at the browser name or version, and the server never inspects the `User-Agent` header for this purpose.
- A browser passes if it has both features. In practice that means a **Chromium 123** engine or later (Chrome, Edge and other Chromium-based browsers), **Firefox 140** or later, or **Safari / iOS 17.5** or later.
- A browser that fails is sent to the same address with `?v-r=oldbrowser`, and the server answers with a static English page: "I'm sorry, but your browser is not supported".
- owlcms does not change the check or the page. It logs every refusal, see [Diagnosing a refusal](#diagnosing-a-refusal).

## The check

`IndexHtmlRequestHandler.redirectToOldBrowserPageWhenNeeded` adds this script to the `<head>` of the page:

```html
<script initial="">if (typeof Highlight !== "function"
        || !CSS.supports("color", "light-dark(red, red)")) {
    window.location.search='v-r=oldbrowser';
}
</script>
```

The call is unconditional. It applies to every page load, in development and in production mode. It is a plain inline script, so it runs before the application bundle is loaded.

The two features and the first browser versions that have them:

| Probe | Feature | Chrome / Edge | Firefox | Safari / iOS |
|:--|:--|:--|:--|:--|
| `typeof Highlight !== "function"` | CSS Custom Highlight API | 105 | 140 | 17.2 |
| `CSS.supports("color", "light-dark(red, red)")` | CSS `light-dark()` | 123 | 120 | 17.5 |
| **Both** (what the script requires) | | **123** | **140** | **17.5** |

The version numbers come from the comment in the Vaadin source and agree with the MDN browser compatibility data.

### The check is looser than the supported versions

| | Chrome / Edge | Firefox | Safari / iOS |
|:--|:--|:--|:--|
| Blocked by the script below | 123 | 140 | 17.5 |
| Bundle compiled for (`target` in `vite.generated.ts`) | 152 | 140 | 17.6 |
| Documented as supported by Vaadin 25.3 | 154 | 157 (ESR 140) | 17.6, 18.6, 26.6, 27 |

The Vaadin source comment says the gap is intentional: the check should not turn away a Chrome that is "a few versions behind". The consequence is that a browser between the first and second rows, for example Chrome 130, is not blocked and gets no warning, but the application is not built for it. Passing the check is therefore not proof that the browser works.

### What this means for the devices used with owlcms

- Only the engine counts. A Chromium-based browser on a television, a streaming stick or an old tablet is accepted or refused according to its Chromium engine version, whatever name it reports.
- On iPhone and iPad the Safari version follows the iOS version, so a device that cannot be updated to iOS 17.5 is refused.
- Changing the user agent string does not get a refused browser through, and does not cause a capable browser to be refused.

## What happens when the check fails

1. The script sets `window.location.search`. The browser reloads the same path with the query string replaced by `v-r=oldbrowser`. Any query parameters that were on the original address are dropped.
2. `UnsupportedBrowserHandler` handles every request whose `v-r` parameter is `oldbrowser`, on any path. It is consulted before `IndexHtmlRequestHandler`, so the normal page is not served again.
3. The handler fires an `UnsupportedBrowserEvent` on the service event bus. The event gives access to the session and to the request, so a listener can log the `User-Agent` header. This event is new in Vaadin 25.3.
4. The handler writes the page with status 200. It uses `browser-too-old.html` from the root of the application classpath if there is one, and otherwise the file of the same name inside `flow-server`.

## Limits

- **The page needs the session cookie.** A request with `v-r=oldbrowser` is not allowed to create a session. It works because the first page load created one and the browser sends the cookie back. Without the cookie, the answer is `403 Session expired` instead of the "not supported" page. A browser that refuses cookies would see that error.
- **No JavaScript, no check.** The check cannot run where scripts are disabled. Apart from that, the script uses only basic syntax, and the `||` stops at the first test when `Highlight` is missing, so `CSS.supports` is not reached in a browser that might lack it. This is from reading the script; it was not tried in a very old browser.
- **The server has no fallback.** `BrowserDetails.isTooOldToFunctionProperly()`, which compares user agent versions (Internet Explorer, old Edge, old Safari and so on), still exists both in `flow-server` and in the client engine, but nothing calls it. `WebBrowser` has no equivalent method, and its browser detection methods are deprecated since 25.0.

## The probe changes between Vaadin versions

| Flow version | Test in the script | Chrome / Edge | Firefox | Safari |
|:--|:--|:--|:--|:--|
| 24.9.26 | `!('CSSLayerBlockRule' in window)` | 99 | 97 | 15.4 |
| 25.2.9 | `!Object.hasOwn(HTMLElement.prototype, "popover")` | 114 | 125 | 17 |
| 25.3.5 | `Highlight` and `light-dark()`, as above | 123 | 140 | 17.5 |

The Vaadin documentation states that minimum browser versions can be raised in minor and major releases, not in maintenance releases. The three versions present on this machine each use a different test, with a higher threshold each time, so the check should be read again when the Vaadin minor version changes.

## Diagnosing a refusal

`AppShell` listens for `UnsupportedBrowserEvent` and logs each refusal as a warning:

```text
browser refused as too old: client=192.168.1.20 url=http://host:8080/displays/...?v-r=oldbrowser referer=... user-agent=...
```

A refusal is expected only when the script's test fails in that browser. If a test page running the same two probes in the same browser passes, the log tells the other causes apart:

- **`referer` is empty**: the browser did not come from an owlcms page. The address it was given already contains `v-r=oldbrowser`, for example copied from a browser that had been redirected, or saved in a vMix input. Remove the query string from the address.
- **`user-agent` shows an older Chrome than expected**: the refused browser is not the one that was tested. vMix keeps the Browser Version chosen when each Web Browser input was created, so an older input can use an older Chromium than a new one created for a test page. Check that setting on the input that shows owlcms.
- **`referer` is an owlcms page and `user-agent` is recent enough**: the script really failed in that browser. Compare the result of the two probes in the browser's own console on the owlcms page.

## Extension points

owlcms uses the event; it does not replace the page.

- **Replace the page**: put a `browser-too-old.html` file at the root of the application classpath (for example in `src/main/resources`). It is served as is, so it must be self-contained.
- **Record refused browsers**: add a listener for `UnsupportedBrowserEvent`, as `AppShell.serviceInit` does:

  ```java
  service.getEventBus().addListener(UnsupportedBrowserEvent.class,
          event -> log(event.getRequest().getHeader("User-Agent")));
  ```

## How this was established

No Maven build was run. The JARs were read from `~/.m2/repository` with the `javap` of the Homebrew OpenJDK, and the running development server on port 8080 was queried with `curl`.

| Finding | Evidence |
|:--|:--|
| Flow 25.3.5 is the version in use | The development server process has `flow-server-25.3.5.jar` open |
| Text of the script, and that the call is unconditional | Bytecode of `IndexHtmlRequestHandler`; the same script is present in the page served by the development server |
| Feature versions | Comment in `IndexHtmlRequestHandler.java` (sources JAR), and MDN `browser-compat-data` |
| Build target | `vite.generated.ts` inside `flow-server-25.3.5.jar` |
| Documented supported versions, and that the event is new in 25.3 | Vaadin 25.3 documentation, through the Vaadin MCP server |
| `v-r=oldbrowser` is what selects the handler | Bytecode of `HandlerHelper` and `UnsupportedBrowserHandler` |
| The handler runs before the page handler | Bytecode of `VaadinService` and `VaadinServletService`: the handler list is reversed, then tried in order until one accepts |
| The page served is the one in the JAR | `curl` with the session cookie, on `/` and on `/displays`: status 200, byte-identical to the file in the JAR |
| 403 without a session | `curl` without a cookie: `403 Session expired`; bytecode of `VaadinServletService.requestCanCreateSession` |
| Replacement page is looked up at the classpath root | Bytecode of `LookupInitializer$ResourceProviderImpl` (`ClassLoader.getResource`) |
| `isTooOldToFunctionProperly` is never called | Search of all 243 JARs loaded by the development server, and of the built client bundle: only the definitions are found |
| Before the refusal log was added, owlcms had no override and no listener | Search of the repository |

Not tested: the behavior in an actual old browser, and the order of the scripts in production mode. In development mode two other inline scripts are placed ahead of the check.

### Checking again after a Vaadin upgrade

Read the script from the new JAR:

```bash
JAR=~/.m2/repository/com/vaadin/flow-server/25.3.5/flow-server-25.3.5.jar
/opt/homebrew/opt/openjdk/bin/javap -classpath "$JAR" -p -c \
    com.vaadin.flow.server.communication.IndexHtmlRequestHandler | grep oldbrowser
```

Or ask the running server, first for the script and then for the page a refused browser gets:

```bash
curl -s -c /tmp/cookies.txt http://localhost:8080/ | grep -B3 oldbrowser
curl -s -b /tmp/cookies.txt 'http://localhost:8080/?v-r=oldbrowser' | grep '<h1>'
```
