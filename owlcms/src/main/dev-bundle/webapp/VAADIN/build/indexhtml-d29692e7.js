(function(){const e=document.createElement("link").relList;if(e&&e.supports&&e.supports("modulepreload"))return;for(const n of document.querySelectorAll('link[rel="modulepreload"]'))i(n);new MutationObserver(n=>{for(const s of n)if(s.type==="childList")for(const r of s.addedNodes)r.tagName==="LINK"&&r.rel==="modulepreload"&&i(r)}).observe(document,{childList:!0,subtree:!0});function t(n){const s={};return n.integrity&&(s.integrity=n.integrity),n.referrerPolicy&&(s.referrerPolicy=n.referrerPolicy),n.crossOrigin==="use-credentials"?s.credentials="include":n.crossOrigin==="anonymous"?s.credentials="omit":s.credentials="same-origin",s}function i(n){if(n.ep)return;n.ep=!0;const s=t(n);fetch(n.href,s)}})();window.Vaadin=window.Vaadin||{};window.Vaadin.featureFlags=window.Vaadin.featureFlags||{};window.Vaadin.featureFlags.exampleFeatureFlag=!1;window.Vaadin.featureFlags.collaborationEngineBackend=!1;window.Vaadin.featureFlags.sideNavComponent=!0;const xn="modulepreload",Cn=function(o,e){return new URL(o,e).href},Ko={},E=function(e,t,i){if(!t||t.length===0)return e();const n=document.getElementsByTagName("link");return Promise.all(t.map(s=>{if(s=Cn(s,i),s in Ko)return;Ko[s]=!0;const r=s.endsWith(".css"),l=r?'[rel="stylesheet"]':"";if(!!i)for(let h=n.length-1;h>=0;h--){const m=n[h];if(m.href===s&&(!r||m.rel==="stylesheet"))return}else if(document.querySelector(`link[href="${s}"]${l}`))return;const d=document.createElement("link");if(d.rel=r?"stylesheet":xn,r||(d.as="script",d.crossOrigin=""),d.href=s,document.head.appendChild(d),r)return new Promise((h,m)=>{d.addEventListener("load",h),d.addEventListener("error",()=>m(new Error(`Unable to preload CSS for ${s}`)))})})).then(()=>e())};function bt(o){return o=o||[],Array.isArray(o)?o:[o]}function Y(o){return`[Vaadin.Router] ${o}`}function kn(o){if(typeof o!="object")return String(o);const e=Object.prototype.toString.call(o).match(/ (.*)\]$/)[1];return e==="Object"||e==="Array"?`${e} ${JSON.stringify(o)}`:e}const _t="module",wt="nomodule",bo=[_t,wt];function Yo(o){if(!o.match(/.+\.[m]?js$/))throw new Error(Y(`Unsupported type for bundle "${o}": .js or .mjs expected.`))}function Fi(o){if(!o||!K(o.path))throw new Error(Y('Expected route config to be an object with a "path" string property, or an array of such objects'));const e=o.bundle,t=["component","redirect","bundle"];if(!ge(o.action)&&!Array.isArray(o.children)&&!ge(o.children)&&!St(e)&&!t.some(i=>K(o[i])))throw new Error(Y(`Expected route config "${o.path}" to include either "${t.join('", "')}" or "action" function but none found.`));if(e)if(K(e))Yo(e);else if(bo.some(i=>i in e))bo.forEach(i=>i in e&&Yo(e[i]));else throw new Error(Y('Expected route bundle to include either "'+wt+'" or "'+_t+'" keys, or both'));o.redirect&&["bundle","component"].forEach(i=>{i in o&&console.warn(Y(`Route config "${o.path}" has both "redirect" and "${i}" properties, and "redirect" will always override the latter. Did you mean to only use "${i}"?`))})}function Jo(o){bt(o).forEach(e=>Fi(e))}function Xo(o,e){let t=document.head.querySelector('script[src="'+o+'"][async]');return t||(t=document.createElement("script"),t.setAttribute("src",o),e===_t?t.setAttribute("type",_t):e===wt&&t.setAttribute(wt,""),t.async=!0),new Promise((i,n)=>{t.onreadystatechange=t.onload=s=>{t.__dynamicImportLoaded=!0,i(s)},t.onerror=s=>{t.parentNode&&t.parentNode.removeChild(t),n(s)},t.parentNode===null?document.head.appendChild(t):t.__dynamicImportLoaded&&i()})}function $n(o){return K(o)?Xo(o):Promise.race(bo.filter(e=>e in o).map(e=>Xo(o[e],e)))}function Fe(o,e){return!window.dispatchEvent(new CustomEvent(`vaadin-router-${o}`,{cancelable:o==="go",detail:e}))}function St(o){return typeof o=="object"&&!!o}function ge(o){return typeof o=="function"}function K(o){return typeof o=="string"}function Bi(o){const e=new Error(Y(`Page not found (${o.pathname})`));return e.context=o,e.code=404,e}const Pe=new class{};function Tn(o){const e=o.port,t=o.protocol,s=t==="http:"&&e==="80"||t==="https:"&&e==="443"?o.hostname:o.host;return`${t}//${s}`}function Qo(o){if(o.defaultPrevented||o.button!==0||o.shiftKey||o.ctrlKey||o.altKey||o.metaKey)return;let e=o.target;const t=o.composedPath?o.composedPath():o.path||[];for(let l=0;l<t.length;l++){const a=t[l];if(a.nodeName&&a.nodeName.toLowerCase()==="a"){e=a;break}}for(;e&&e.nodeName.toLowerCase()!=="a";)e=e.parentNode;if(!e||e.nodeName.toLowerCase()!=="a"||e.target&&e.target.toLowerCase()!=="_self"||e.hasAttribute("download")||e.hasAttribute("router-ignore")||e.pathname===window.location.pathname&&e.hash!==""||(e.origin||Tn(e))!==window.location.origin)return;const{pathname:n,search:s,hash:r}=e;Fe("go",{pathname:n,search:s,hash:r})&&(o.preventDefault(),o&&o.type==="click"&&window.scrollTo(0,0))}const Nn={activate(){window.document.addEventListener("click",Qo)},inactivate(){window.document.removeEventListener("click",Qo)}},Pn=/Trident/.test(navigator.userAgent);Pn&&!ge(window.PopStateEvent)&&(window.PopStateEvent=function(o,e){e=e||{};var t=document.createEvent("Event");return t.initEvent(o,!!e.bubbles,!!e.cancelable),t.state=e.state||null,t},window.PopStateEvent.prototype=window.Event.prototype);function Zo(o){if(o.state==="vaadin-router-ignore")return;const{pathname:e,search:t,hash:i}=window.location;Fe("go",{pathname:e,search:t,hash:i})}const An={activate(){window.addEventListener("popstate",Zo)},inactivate(){window.removeEventListener("popstate",Zo)}};var De=Yi,Rn=$o,In=Vn,On=Wi,Ln=Ki,Hi="/",qi="./",Mn=new RegExp(["(\\\\.)","(?:\\:(\\w+)(?:\\(((?:\\\\.|[^\\\\()])+)\\))?|\\(((?:\\\\.|[^\\\\()])+)\\))([+*?])?"].join("|"),"g");function $o(o,e){for(var t=[],i=0,n=0,s="",r=e&&e.delimiter||Hi,l=e&&e.delimiters||qi,a=!1,d;(d=Mn.exec(o))!==null;){var h=d[0],m=d[1],u=d.index;if(s+=o.slice(n,u),n=u+h.length,m){s+=m[1],a=!0;continue}var v="",se=o[n],re=d[2],te=d[3],Dt=d[4],B=d[5];if(!a&&s.length){var X=s.length-1;l.indexOf(s[X])>-1&&(v=s[X],s=s.slice(0,X))}s&&(t.push(s),s="",a=!1);var Se=v!==""&&se!==void 0&&se!==v,Ee=B==="+"||B==="*",zt=B==="?"||B==="*",oe=v||r,it=te||Dt;t.push({name:re||i++,prefix:v,delimiter:oe,optional:zt,repeat:Ee,partial:Se,pattern:it?Dn(it):"[^"+ae(oe)+"]+?"})}return(s||n<o.length)&&t.push(s+o.substr(n)),t}function Vn(o,e){return Wi($o(o,e))}function Wi(o){for(var e=new Array(o.length),t=0;t<o.length;t++)typeof o[t]=="object"&&(e[t]=new RegExp("^(?:"+o[t].pattern+")$"));return function(i,n){for(var s="",r=n&&n.encode||encodeURIComponent,l=0;l<o.length;l++){var a=o[l];if(typeof a=="string"){s+=a;continue}var d=i?i[a.name]:void 0,h;if(Array.isArray(d)){if(!a.repeat)throw new TypeError('Expected "'+a.name+'" to not repeat, but got array');if(d.length===0){if(a.optional)continue;throw new TypeError('Expected "'+a.name+'" to not be empty')}for(var m=0;m<d.length;m++){if(h=r(d[m],a),!e[l].test(h))throw new TypeError('Expected all "'+a.name+'" to match "'+a.pattern+'"');s+=(m===0?a.prefix:a.delimiter)+h}continue}if(typeof d=="string"||typeof d=="number"||typeof d=="boolean"){if(h=r(String(d),a),!e[l].test(h))throw new TypeError('Expected "'+a.name+'" to match "'+a.pattern+'", but got "'+h+'"');s+=a.prefix+h;continue}if(a.optional){a.partial&&(s+=a.prefix);continue}throw new TypeError('Expected "'+a.name+'" to be '+(a.repeat?"an array":"a string"))}return s}}function ae(o){return o.replace(/([.+*?=^!:${}()[\]|/\\])/g,"\\$1")}function Dn(o){return o.replace(/([=!:$/()])/g,"\\$1")}function Gi(o){return o&&o.sensitive?"":"i"}function zn(o,e){if(!e)return o;var t=o.source.match(/\((?!\?)/g);if(t)for(var i=0;i<t.length;i++)e.push({name:i,prefix:null,delimiter:null,optional:!1,repeat:!1,partial:!1,pattern:null});return o}function Un(o,e,t){for(var i=[],n=0;n<o.length;n++)i.push(Yi(o[n],e,t).source);return new RegExp("(?:"+i.join("|")+")",Gi(t))}function jn(o,e,t){return Ki($o(o,t),e,t)}function Ki(o,e,t){t=t||{};for(var i=t.strict,n=t.start!==!1,s=t.end!==!1,r=ae(t.delimiter||Hi),l=t.delimiters||qi,a=[].concat(t.endsWith||[]).map(ae).concat("$").join("|"),d=n?"^":"",h=o.length===0,m=0;m<o.length;m++){var u=o[m];if(typeof u=="string")d+=ae(u),h=m===o.length-1&&l.indexOf(u[u.length-1])>-1;else{var v=u.repeat?"(?:"+u.pattern+")(?:"+ae(u.delimiter)+"(?:"+u.pattern+"))*":u.pattern;e&&e.push(u),u.optional?u.partial?d+=ae(u.prefix)+"("+v+")?":d+="(?:"+ae(u.prefix)+"("+v+"))?":d+=ae(u.prefix)+"("+v+")"}}return s?(i||(d+="(?:"+r+")?"),d+=a==="$"?"$":"(?="+a+")"):(i||(d+="(?:"+r+"(?="+a+"))?"),h||(d+="(?="+r+"|"+a+")")),new RegExp(d,Gi(t))}function Yi(o,e,t){return o instanceof RegExp?zn(o,e):Array.isArray(o)?Un(o,e,t):jn(o,e,t)}De.parse=Rn;De.compile=In;De.tokensToFunction=On;De.tokensToRegExp=Ln;const{hasOwnProperty:Fn}=Object.prototype,_o=new Map;_o.set("|false",{keys:[],pattern:/(?:)/});function ei(o){try{return decodeURIComponent(o)}catch{return o}}function Bn(o,e,t,i,n){t=!!t;const s=`${o}|${t}`;let r=_o.get(s);if(!r){const d=[];r={keys:d,pattern:De(o,d,{end:t,strict:o===""})},_o.set(s,r)}const l=r.pattern.exec(e);if(!l)return null;const a=Object.assign({},n);for(let d=1;d<l.length;d++){const h=r.keys[d-1],m=h.name,u=l[d];(u!==void 0||!Fn.call(a,m))&&(h.repeat?a[m]=u?u.split(h.delimiter).map(ei):[]:a[m]=u&&ei(u))}return{path:l[0],keys:(i||[]).concat(r.keys),params:a}}function Ji(o,e,t,i,n){let s,r,l=0,a=o.path||"";return a.charAt(0)==="/"&&(t&&(a=a.substr(1)),t=!0),{next(d){if(o===d)return{done:!0};const h=o.__children=o.__children||o.children;if(!s&&(s=Bn(a,e,!h,i,n),s))return{done:!1,value:{route:o,keys:s.keys,params:s.params,path:s.path}};if(s&&h)for(;l<h.length;){if(!r){const u=h[l];u.parent=o;let v=s.path.length;v>0&&e.charAt(v)==="/"&&(v+=1),r=Ji(u,e.substr(v),t,s.keys,s.params)}const m=r.next(d);if(!m.done)return{done:!1,value:m.value};r=null,l++}return{done:!0}}}}function Hn(o){if(ge(o.route.action))return o.route.action(o)}function qn(o,e){let t=e;for(;t;)if(t=t.parent,t===o)return!0;return!1}function Wn(o){let e=`Path '${o.pathname}' is not properly resolved due to an error.`;const t=(o.route||{}).path;return t&&(e+=` Resolution had failed on route: '${t}'`),e}function Gn(o,e){const{route:t,path:i}=e;if(t&&!t.__synthetic){const n={path:i,route:t};if(!o.chain)o.chain=[];else if(t.parent){let s=o.chain.length;for(;s--&&o.chain[s].route&&o.chain[s].route!==t.parent;)o.chain.pop()}o.chain.push(n)}}class He{constructor(e,t={}){if(Object(e)!==e)throw new TypeError("Invalid routes");this.baseUrl=t.baseUrl||"",this.errorHandler=t.errorHandler,this.resolveRoute=t.resolveRoute||Hn,this.context=Object.assign({resolver:this},t.context),this.root=Array.isArray(e)?{path:"",__children:e,parent:null,__synthetic:!0}:e,this.root.parent=null}getRoutes(){return[...this.root.__children]}setRoutes(e){Jo(e);const t=[...bt(e)];this.root.__children=t}addRoutes(e){return Jo(e),this.root.__children.push(...bt(e)),this.getRoutes()}removeRoutes(){this.setRoutes([])}resolve(e){const t=Object.assign({},this.context,K(e)?{pathname:e}:e),i=Ji(this.root,this.__normalizePathname(t.pathname),this.baseUrl),n=this.resolveRoute;let s=null,r=null,l=t;function a(d,h=s.value.route,m){const u=m===null&&s.value.route;return s=r||i.next(u),r=null,!d&&(s.done||!qn(h,s.value.route))?(r=s,Promise.resolve(Pe)):s.done?Promise.reject(Bi(t)):(l=Object.assign(l?{chain:l.chain?l.chain.slice(0):[]}:{},t,s.value),Gn(l,s.value),Promise.resolve(n(l)).then(v=>v!=null&&v!==Pe?(l.result=v.result||v,l):a(d,h,v)))}return t.next=a,Promise.resolve().then(()=>a(!0,this.root)).catch(d=>{const h=Wn(l);if(d?console.warn(h):d=new Error(h),d.context=d.context||l,d instanceof DOMException||(d.code=d.code||500),this.errorHandler)return l.result=this.errorHandler(d),l;throw d})}static __createUrl(e,t){return new URL(e,t)}get __effectiveBaseUrl(){return this.baseUrl?this.constructor.__createUrl(this.baseUrl,document.baseURI||document.URL).href.replace(/[^\/]*$/,""):""}__normalizePathname(e){if(!this.baseUrl)return e;const t=this.__effectiveBaseUrl,i=this.constructor.__createUrl(e,t).href;if(i.slice(0,t.length)===t)return i.slice(t.length)}}He.pathToRegexp=De;const{pathToRegexp:ti}=He,oi=new Map;function Xi(o,e,t){const i=e.name||e.component;if(i&&(o.has(i)?o.get(i).push(e):o.set(i,[e])),Array.isArray(t))for(let n=0;n<t.length;n++){const s=t[n];s.parent=e,Xi(o,s,s.__children||s.children)}}function ii(o,e){const t=o.get(e);if(t&&t.length>1)throw new Error(`Duplicate route with name "${e}". Try seting unique 'name' route properties.`);return t&&t[0]}function ni(o){let e=o.path;return e=Array.isArray(e)?e[0]:e,e!==void 0?e:""}function Kn(o,e={}){if(!(o instanceof He))throw new TypeError("An instance of Resolver is expected");const t=new Map;return(i,n)=>{let s=ii(t,i);if(!s&&(t.clear(),Xi(t,o.root,o.root.__children),s=ii(t,i),!s))throw new Error(`Route "${i}" not found`);let r=oi.get(s.fullPath);if(!r){let a=ni(s),d=s.parent;for(;d;){const v=ni(d);v&&(a=v.replace(/\/$/,"")+"/"+a.replace(/^\//,"")),d=d.parent}const h=ti.parse(a),m=ti.tokensToFunction(h),u=Object.create(null);for(let v=0;v<h.length;v++)K(h[v])||(u[h[v].name]=!0);r={toPath:m,keys:u},oi.set(a,r),s.fullPath=a}let l=r.toPath(n,e)||"/";if(e.stringifyQueryParams&&n){const a={},d=Object.keys(n);for(let m=0;m<d.length;m++){const u=d[m];r.keys[u]||(a[u]=n[u])}const h=e.stringifyQueryParams(a);h&&(l+=h.charAt(0)==="?"?h:`?${h}`)}return l}}let si=[];function Yn(o){si.forEach(e=>e.inactivate()),o.forEach(e=>e.activate()),si=o}const Jn=o=>{const e=getComputedStyle(o).getPropertyValue("animation-name");return e&&e!=="none"},Xn=(o,e)=>{const t=()=>{o.removeEventListener("animationend",t),e()};o.addEventListener("animationend",t)};function ri(o,e){return o.classList.add(e),new Promise(t=>{if(Jn(o)){const i=o.getBoundingClientRect(),n=`height: ${i.bottom-i.top}px; width: ${i.right-i.left}px`;o.setAttribute("style",`position: absolute; ${n}`),Xn(o,()=>{o.classList.remove(e),o.removeAttribute("style"),t()})}else o.classList.remove(e),t()})}const Qn=256;function Bt(o){return o!=null}function Zn(o){const e=Object.assign({},o);return delete e.next,e}function W({pathname:o="",search:e="",hash:t="",chain:i=[],params:n={},redirectFrom:s,resolver:r},l){const a=i.map(d=>d.route);return{baseUrl:r&&r.baseUrl||"",pathname:o,search:e,hash:t,routes:a,route:l||a.length&&a[a.length-1]||null,params:n,redirectFrom:s,getUrl:(d={})=>mt(le.pathToRegexp.compile(Qi(a))(Object.assign({},n,d)),r)}}function ai(o,e){const t=Object.assign({},o.params);return{redirect:{pathname:e,from:o.pathname,params:t}}}function es(o,e){e.location=W(o);const t=o.chain.map(i=>i.route).indexOf(o.route);return o.chain[t].element=e,e}function pt(o,e,t){if(ge(o))return o.apply(t,e)}function li(o,e,t){return i=>{if(i&&(i.cancel||i.redirect))return i;if(t)return pt(t[o],e,t)}}function ts(o,e){if(!Array.isArray(o)&&!St(o))throw new Error(Y(`Incorrect "children" value for the route ${e.path}: expected array or object, but got ${o}`));e.__children=[];const t=bt(o);for(let i=0;i<t.length;i++)Fi(t[i]),e.__children.push(t[i])}function lt(o){if(o&&o.length){const e=o[0].parentNode;for(let t=0;t<o.length;t++)e.removeChild(o[t])}}function mt(o,e){const t=e.__effectiveBaseUrl;return t?e.constructor.__createUrl(o.replace(/^\//,""),t).pathname:o}function Qi(o){return o.map(e=>e.path).reduce((e,t)=>t.length?e.replace(/\/$/,"")+"/"+t.replace(/^\//,""):e,"")}class le extends He{constructor(e,t){const i=document.head.querySelector("base"),n=i&&i.getAttribute("href");super([],Object.assign({baseUrl:n&&He.__createUrl(n,document.URL).pathname.replace(/[^\/]*$/,"")},t)),this.resolveRoute=r=>this.__resolveRoute(r);const s=le.NavigationTrigger;le.setTriggers.apply(le,Object.keys(s).map(r=>s[r])),this.baseUrl,this.ready,this.ready=Promise.resolve(e),this.location,this.location=W({resolver:this}),this.__lastStartedRenderId=0,this.__navigationEventHandler=this.__onNavigationEvent.bind(this),this.setOutlet(e),this.subscribe(),this.__createdByRouter=new WeakMap,this.__addedByRouter=new WeakMap}__resolveRoute(e){const t=e.route;let i=Promise.resolve();ge(t.children)&&(i=i.then(()=>t.children(Zn(e))).then(s=>{!Bt(s)&&!ge(t.children)&&(s=t.children),ts(s,t)}));const n={redirect:s=>ai(e,s),component:s=>{const r=document.createElement(s);return this.__createdByRouter.set(r,!0),r}};return i.then(()=>{if(this.__isLatestRender(e))return pt(t.action,[e,n],t)}).then(s=>{if(Bt(s)&&(s instanceof HTMLElement||s.redirect||s===Pe))return s;if(K(t.redirect))return n.redirect(t.redirect);if(t.bundle)return $n(t.bundle).then(()=>{},()=>{throw new Error(Y(`Bundle not found: ${t.bundle}. Check if the file name is correct`))})}).then(s=>{if(Bt(s))return s;if(K(t.component))return n.component(t.component)})}setOutlet(e){e&&this.__ensureOutlet(e),this.__outlet=e}getOutlet(){return this.__outlet}setRoutes(e,t=!1){return this.__previousContext=void 0,this.__urlForName=void 0,super.setRoutes(e),t||this.__onNavigationEvent(),this.ready}render(e,t){const i=++this.__lastStartedRenderId,n=Object.assign({search:"",hash:""},K(e)?{pathname:e}:e,{__renderId:i});return this.ready=this.resolve(n).then(s=>this.__fullyResolveChain(s)).then(s=>{if(this.__isLatestRender(s)){const r=this.__previousContext;if(s===r)return this.__updateBrowserHistory(r,!0),this.location;if(this.location=W(s),t&&this.__updateBrowserHistory(s,i===1),Fe("location-changed",{router:this,location:this.location}),s.__skipAttach)return this.__copyUnchangedElements(s,r),this.__previousContext=s,this.location;this.__addAppearingContent(s,r);const l=this.__animateIfNeeded(s);return this.__runOnAfterEnterCallbacks(s),this.__runOnAfterLeaveCallbacks(s,r),l.then(()=>{if(this.__isLatestRender(s))return this.__removeDisappearingContent(),this.__previousContext=s,this.location})}}).catch(s=>{if(i===this.__lastStartedRenderId)throw t&&this.__updateBrowserHistory(n),lt(this.__outlet&&this.__outlet.children),this.location=W(Object.assign(n,{resolver:this})),Fe("error",Object.assign({router:this,error:s},n)),s}),this.ready}__fullyResolveChain(e,t=e){return this.__findComponentContextAfterAllRedirects(t).then(i=>{const s=i!==t?i:e,l=mt(Qi(i.chain),i.resolver)===i.pathname,a=(d,h=d.route,m)=>d.next(void 0,h,m).then(u=>u===null||u===Pe?l?d:h.parent!==null?a(d,h.parent,u):u:u);return a(i).then(d=>{if(d===null||d===Pe)throw Bi(s);return d&&d!==Pe&&d!==i?this.__fullyResolveChain(s,d):this.__amendWithOnBeforeCallbacks(i)})})}__findComponentContextAfterAllRedirects(e){const t=e.result;return t instanceof HTMLElement?(es(e,t),Promise.resolve(e)):t.redirect?this.__redirect(t.redirect,e.__redirectCount,e.__renderId).then(i=>this.__findComponentContextAfterAllRedirects(i)):t instanceof Error?Promise.reject(t):Promise.reject(new Error(Y(`Invalid route resolution result for path "${e.pathname}". Expected redirect object or HTML element, but got: "${kn(t)}". Double check the action return value for the route.`)))}__amendWithOnBeforeCallbacks(e){return this.__runOnBeforeCallbacks(e).then(t=>t===this.__previousContext||t===e?t:this.__fullyResolveChain(t))}__runOnBeforeCallbacks(e){const t=this.__previousContext||{},i=t.chain||[],n=e.chain;let s=Promise.resolve();const r=()=>({cancel:!0}),l=a=>ai(e,a);if(e.__divergedChainIndex=0,e.__skipAttach=!1,i.length){for(let a=0;a<Math.min(i.length,n.length)&&!(i[a].route!==n[a].route||i[a].path!==n[a].path&&i[a].element!==n[a].element||!this.__isReusableElement(i[a].element,n[a].element));a=++e.__divergedChainIndex);if(e.__skipAttach=n.length===i.length&&e.__divergedChainIndex==n.length&&this.__isReusableElement(e.result,t.result),e.__skipAttach){for(let a=n.length-1;a>=0;a--)s=this.__runOnBeforeLeaveCallbacks(s,e,{prevent:r},i[a]);for(let a=0;a<n.length;a++)s=this.__runOnBeforeEnterCallbacks(s,e,{prevent:r,redirect:l},n[a]),i[a].element.location=W(e,i[a].route)}else for(let a=i.length-1;a>=e.__divergedChainIndex;a--)s=this.__runOnBeforeLeaveCallbacks(s,e,{prevent:r},i[a])}if(!e.__skipAttach)for(let a=0;a<n.length;a++)a<e.__divergedChainIndex?a<i.length&&i[a].element&&(i[a].element.location=W(e,i[a].route)):(s=this.__runOnBeforeEnterCallbacks(s,e,{prevent:r,redirect:l},n[a]),n[a].element&&(n[a].element.location=W(e,n[a].route)));return s.then(a=>{if(a){if(a.cancel)return this.__previousContext.__renderId=e.__renderId,this.__previousContext;if(a.redirect)return this.__redirect(a.redirect,e.__redirectCount,e.__renderId)}return e})}__runOnBeforeLeaveCallbacks(e,t,i,n){const s=W(t);return e.then(r=>{if(this.__isLatestRender(t))return li("onBeforeLeave",[s,i,this],n.element)(r)}).then(r=>{if(!(r||{}).redirect)return r})}__runOnBeforeEnterCallbacks(e,t,i,n){const s=W(t,n.route);return e.then(r=>{if(this.__isLatestRender(t))return li("onBeforeEnter",[s,i,this],n.element)(r)})}__isReusableElement(e,t){return e&&t?this.__createdByRouter.get(e)&&this.__createdByRouter.get(t)?e.localName===t.localName:e===t:!1}__isLatestRender(e){return e.__renderId===this.__lastStartedRenderId}__redirect(e,t,i){if(t>Qn)throw new Error(Y(`Too many redirects when rendering ${e.from}`));return this.resolve({pathname:this.urlForPath(e.pathname,e.params),redirectFrom:e.from,__redirectCount:(t||0)+1,__renderId:i})}__ensureOutlet(e=this.__outlet){if(!(e instanceof Node))throw new TypeError(Y(`Expected router outlet to be a valid DOM Node (but got ${e})`))}__updateBrowserHistory({pathname:e,search:t="",hash:i=""},n){if(window.location.pathname!==e||window.location.search!==t||window.location.hash!==i){const s=n?"replaceState":"pushState";window.history[s](null,document.title,e+t+i),window.dispatchEvent(new PopStateEvent("popstate",{state:"vaadin-router-ignore"}))}}__copyUnchangedElements(e,t){let i=this.__outlet;for(let n=0;n<e.__divergedChainIndex;n++){const s=t&&t.chain[n].element;if(s)if(s.parentNode===i)e.chain[n].element=s,i=s;else break}return i}__addAppearingContent(e,t){this.__ensureOutlet(),this.__removeAppearingContent();const i=this.__copyUnchangedElements(e,t);this.__appearingContent=[],this.__disappearingContent=Array.from(i.children).filter(s=>this.__addedByRouter.get(s)&&s!==e.result);let n=i;for(let s=e.__divergedChainIndex;s<e.chain.length;s++){const r=e.chain[s].element;r&&(n.appendChild(r),this.__addedByRouter.set(r,!0),n===i&&this.__appearingContent.push(r),n=r)}}__removeDisappearingContent(){this.__disappearingContent&&lt(this.__disappearingContent),this.__disappearingContent=null,this.__appearingContent=null}__removeAppearingContent(){this.__disappearingContent&&this.__appearingContent&&(lt(this.__appearingContent),this.__disappearingContent=null,this.__appearingContent=null)}__runOnAfterLeaveCallbacks(e,t){if(t)for(let i=t.chain.length-1;i>=e.__divergedChainIndex&&this.__isLatestRender(e);i--){const n=t.chain[i].element;if(n)try{const s=W(e);pt(n.onAfterLeave,[s,{},t.resolver],n)}finally{this.__disappearingContent.indexOf(n)>-1&&lt(n.children)}}}__runOnAfterEnterCallbacks(e){for(let t=e.__divergedChainIndex;t<e.chain.length&&this.__isLatestRender(e);t++){const i=e.chain[t].element||{},n=W(e,e.chain[t].route);pt(i.onAfterEnter,[n,{},e.resolver],i)}}__animateIfNeeded(e){const t=(this.__disappearingContent||[])[0],i=(this.__appearingContent||[])[0],n=[],s=e.chain;let r;for(let l=s.length;l>0;l--)if(s[l-1].route.animate){r=s[l-1].route.animate;break}if(t&&i&&r){const l=St(r)&&r.leave||"leaving",a=St(r)&&r.enter||"entering";n.push(ri(t,l)),n.push(ri(i,a))}return Promise.all(n).then(()=>e)}subscribe(){window.addEventListener("vaadin-router-go",this.__navigationEventHandler)}unsubscribe(){window.removeEventListener("vaadin-router-go",this.__navigationEventHandler)}__onNavigationEvent(e){const{pathname:t,search:i,hash:n}=e?e.detail:window.location;K(this.__normalizePathname(t))&&(e&&e.preventDefault&&e.preventDefault(),this.render({pathname:t,search:i,hash:n},!0))}static setTriggers(...e){Yn(e)}urlForName(e,t){return this.__urlForName||(this.__urlForName=Kn(this)),mt(this.__urlForName(e,t),this)}urlForPath(e,t){return mt(le.pathToRegexp.compile(e)(t),this)}static go(e){const{pathname:t,search:i,hash:n}=K(e)?this.__createUrl(e,"http://a"):e;return Fe("go",{pathname:t,search:i,hash:n})}}const os=/\/\*[\*!]\s+vaadin-dev-mode:start([\s\S]*)vaadin-dev-mode:end\s+\*\*\//i,vt=window.Vaadin&&window.Vaadin.Flow&&window.Vaadin.Flow.clients;function is(){function o(){return!0}return Zi(o)}function ns(){try{return ss()?!0:rs()?vt?!as():!is():!1}catch{return!1}}function ss(){return localStorage.getItem("vaadin.developmentmode.force")}function rs(){return["localhost","127.0.0.1"].indexOf(window.location.hostname)>=0}function as(){return!!(vt&&Object.keys(vt).map(e=>vt[e]).filter(e=>e.productionMode).length>0)}function Zi(o,e){if(typeof o!="function")return;const t=os.exec(o.toString());if(t)try{o=new Function(t[1])}catch(i){console.log("vaadin-development-mode-detector: uncommentAndRun() failed",i)}return o(e)}window.Vaadin=window.Vaadin||{};const di=function(o,e){if(window.Vaadin.developmentMode)return Zi(o,e)};window.Vaadin.developmentMode===void 0&&(window.Vaadin.developmentMode=ns());function ls(){}const ds=function(){if(typeof di=="function")return di(ls)};window.Vaadin=window.Vaadin||{};window.Vaadin.registrations=window.Vaadin.registrations||[];window.Vaadin.registrations.push({is:"@vaadin/router",version:"1.7.4"});ds();le.NavigationTrigger={POPSTATE:An,CLICK:Nn};var Ht,$;(function(o){o.CONNECTED="connected",o.LOADING="loading",o.RECONNECTING="reconnecting",o.CONNECTION_LOST="connection-lost"})($||($={}));class cs{constructor(e){this.stateChangeListeners=new Set,this.loadingCount=0,this.connectionState=e,this.serviceWorkerMessageListener=this.serviceWorkerMessageListener.bind(this),navigator.serviceWorker&&(navigator.serviceWorker.addEventListener("message",this.serviceWorkerMessageListener),navigator.serviceWorker.ready.then(t=>{var i;(i=t==null?void 0:t.active)===null||i===void 0||i.postMessage({method:"Vaadin.ServiceWorker.isConnectionLost",id:"Vaadin.ServiceWorker.isConnectionLost"})}))}addStateChangeListener(e){this.stateChangeListeners.add(e)}removeStateChangeListener(e){this.stateChangeListeners.delete(e)}loadingStarted(){this.state=$.LOADING,this.loadingCount+=1}loadingFinished(){this.decreaseLoadingCount($.CONNECTED)}loadingFailed(){this.decreaseLoadingCount($.CONNECTION_LOST)}decreaseLoadingCount(e){this.loadingCount>0&&(this.loadingCount-=1,this.loadingCount===0&&(this.state=e))}get state(){return this.connectionState}set state(e){if(e!==this.connectionState){const t=this.connectionState;this.connectionState=e,this.loadingCount=0;for(const i of this.stateChangeListeners)i(t,this.connectionState)}}get online(){return this.connectionState===$.CONNECTED||this.connectionState===$.LOADING}get offline(){return!this.online}serviceWorkerMessageListener(e){typeof e.data=="object"&&e.data.id==="Vaadin.ServiceWorker.isConnectionLost"&&(e.data.result===!0&&(this.state=$.CONNECTION_LOST),navigator.serviceWorker.removeEventListener("message",this.serviceWorkerMessageListener))}}const hs=o=>!!(o==="localhost"||o==="[::1]"||o.match(/^127\.\d+\.\d+\.\d+$/)),dt=window;if(!(!((Ht=dt.Vaadin)===null||Ht===void 0)&&Ht.connectionState)){let o;hs(window.location.hostname)?o=!0:o=navigator.onLine,dt.Vaadin=dt.Vaadin||{},dt.Vaadin.connectionState=new cs(o?$.CONNECTED:$.CONNECTION_LOST)}function j(o,e,t,i){var n=arguments.length,s=n<3?e:i===null?i=Object.getOwnPropertyDescriptor(e,t):i,r;if(typeof Reflect=="object"&&typeof Reflect.decorate=="function")s=Reflect.decorate(o,e,t,i);else for(var l=o.length-1;l>=0;l--)(r=o[l])&&(s=(n<3?r(s):n>3?r(e,t,s):r(e,t))||s);return n>3&&s&&Object.defineProperty(e,t,s),s}/**
 * @license
 * Copyright 2019 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const us=!1,ft=window,To=ft.ShadowRoot&&(ft.ShadyCSS===void 0||ft.ShadyCSS.nativeShadow)&&"adoptedStyleSheets"in Document.prototype&&"replace"in CSSStyleSheet.prototype,No=Symbol(),ci=new WeakMap;class en{constructor(e,t,i){if(this._$cssResult$=!0,i!==No)throw new Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=e,this._strings=t}get styleSheet(){let e=this._styleSheet;const t=this._strings;if(To&&e===void 0){const i=t!==void 0&&t.length===1;i&&(e=ci.get(t)),e===void 0&&((this._styleSheet=e=new CSSStyleSheet).replaceSync(this.cssText),i&&ci.set(t,e))}return e}toString(){return this.cssText}}const ps=o=>{if(o._$cssResult$===!0)return o.cssText;if(typeof o=="number")return o;throw new Error(`Value passed to 'css' function must be a 'css' function result: ${o}. Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.`)},ms=o=>new en(typeof o=="string"?o:String(o),void 0,No),C=(o,...e)=>{const t=o.length===1?o[0]:e.reduce((i,n,s)=>i+ps(n)+o[s+1],o[0]);return new en(t,o,No)},vs=(o,e)=>{To?o.adoptedStyleSheets=e.map(t=>t instanceof CSSStyleSheet?t:t.styleSheet):e.forEach(t=>{const i=document.createElement("style"),n=ft.litNonce;n!==void 0&&i.setAttribute("nonce",n),i.textContent=t.cssText,o.appendChild(i)})},fs=o=>{let e="";for(const t of o.cssRules)e+=t.cssText;return ms(e)},hi=To||us?o=>o:o=>o instanceof CSSStyleSheet?fs(o):o;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var qt,Wt,Gt,tn;const Z=window;let on,de;const ui=Z.trustedTypes,gs=ui?ui.emptyScript:"",gt=Z.reactiveElementPolyfillSupportDevMode;{const o=(qt=Z.litIssuedWarnings)!==null&&qt!==void 0?qt:Z.litIssuedWarnings=new Set;de=(e,t)=>{t+=` See https://lit.dev/msg/${e} for more information.`,o.has(t)||(console.warn(t),o.add(t))},de("dev-mode","Lit is in dev mode. Not recommended for production!"),!((Wt=Z.ShadyDOM)===null||Wt===void 0)&&Wt.inUse&&gt===void 0&&de("polyfill-support-missing","Shadow DOM is being polyfilled via `ShadyDOM` but the `polyfill-support` module has not been loaded."),on=e=>({then:(t,i)=>{de("request-update-promise",`The \`requestUpdate\` method should no longer return a Promise but does so on \`${e}\`. Use \`updateComplete\` instead.`),t!==void 0&&t(!1)}})}const Kt=o=>{Z.emitLitDebugLogEvents&&Z.dispatchEvent(new CustomEvent("lit-debug",{detail:o}))},nn=(o,e)=>o,wo={toAttribute(o,e){switch(e){case Boolean:o=o?gs:null;break;case Object:case Array:o=o==null?o:JSON.stringify(o);break}return o},fromAttribute(o,e){let t=o;switch(e){case Boolean:t=o!==null;break;case Number:t=o===null?null:Number(o);break;case Object:case Array:try{t=JSON.parse(o)}catch{t=null}break}return t}},sn=(o,e)=>e!==o&&(e===e||o===o),Yt={attribute:!0,type:String,converter:wo,reflect:!1,hasChanged:sn},So="finalized";class ee extends HTMLElement{constructor(){super(),this.__instanceProperties=new Map,this.isUpdatePending=!1,this.hasUpdated=!1,this.__reflectingProperty=null,this.__initialize()}static addInitializer(e){var t;this.finalize(),((t=this._initializers)!==null&&t!==void 0?t:this._initializers=[]).push(e)}static get observedAttributes(){this.finalize();const e=[];return this.elementProperties.forEach((t,i)=>{const n=this.__attributeNameForProperty(i,t);n!==void 0&&(this.__attributeToPropertyMap.set(n,i),e.push(n))}),e}static createProperty(e,t=Yt){var i;if(t.state&&(t.attribute=!1),this.finalize(),this.elementProperties.set(e,t),!t.noAccessor&&!this.prototype.hasOwnProperty(e)){const n=typeof e=="symbol"?Symbol():`__${e}`,s=this.getPropertyDescriptor(e,n,t);s!==void 0&&(Object.defineProperty(this.prototype,e,s),this.hasOwnProperty("__reactivePropertyKeys")||(this.__reactivePropertyKeys=new Set((i=this.__reactivePropertyKeys)!==null&&i!==void 0?i:[])),this.__reactivePropertyKeys.add(e))}}static getPropertyDescriptor(e,t,i){return{get(){return this[t]},set(n){const s=this[e];this[t]=n,this.requestUpdate(e,s,i)},configurable:!0,enumerable:!0}}static getPropertyOptions(e){return this.elementProperties.get(e)||Yt}static finalize(){if(this.hasOwnProperty(So))return!1;this[So]=!0;const e=Object.getPrototypeOf(this);if(e.finalize(),e._initializers!==void 0&&(this._initializers=[...e._initializers]),this.elementProperties=new Map(e.elementProperties),this.__attributeToPropertyMap=new Map,this.hasOwnProperty(nn("properties"))){const t=this.properties,i=[...Object.getOwnPropertyNames(t),...Object.getOwnPropertySymbols(t)];for(const n of i)this.createProperty(n,t[n])}this.elementStyles=this.finalizeStyles(this.styles);{const t=(i,n=!1)=>{this.prototype.hasOwnProperty(i)&&de(n?"renamed-api":"removed-api",`\`${i}\` is implemented on class ${this.name}. It has been ${n?"renamed":"removed"} in this version of LitElement.`)};t("initialize"),t("requestUpdateInternal"),t("_getUpdateComplete",!0)}return!0}static finalizeStyles(e){const t=[];if(Array.isArray(e)){const i=new Set(e.flat(1/0).reverse());for(const n of i)t.unshift(hi(n))}else e!==void 0&&t.push(hi(e));return t}static __attributeNameForProperty(e,t){const i=t.attribute;return i===!1?void 0:typeof i=="string"?i:typeof e=="string"?e.toLowerCase():void 0}__initialize(){var e;this.__updatePromise=new Promise(t=>this.enableUpdating=t),this._$changedProperties=new Map,this.__saveInstanceProperties(),this.requestUpdate(),(e=this.constructor._initializers)===null||e===void 0||e.forEach(t=>t(this))}addController(e){var t,i;((t=this.__controllers)!==null&&t!==void 0?t:this.__controllers=[]).push(e),this.renderRoot!==void 0&&this.isConnected&&((i=e.hostConnected)===null||i===void 0||i.call(e))}removeController(e){var t;(t=this.__controllers)===null||t===void 0||t.splice(this.__controllers.indexOf(e)>>>0,1)}__saveInstanceProperties(){this.constructor.elementProperties.forEach((e,t)=>{this.hasOwnProperty(t)&&(this.__instanceProperties.set(t,this[t]),delete this[t])})}createRenderRoot(){var e;const t=(e=this.shadowRoot)!==null&&e!==void 0?e:this.attachShadow(this.constructor.shadowRootOptions);return vs(t,this.constructor.elementStyles),t}connectedCallback(){var e;this.renderRoot===void 0&&(this.renderRoot=this.createRenderRoot()),this.enableUpdating(!0),(e=this.__controllers)===null||e===void 0||e.forEach(t=>{var i;return(i=t.hostConnected)===null||i===void 0?void 0:i.call(t)})}enableUpdating(e){}disconnectedCallback(){var e;(e=this.__controllers)===null||e===void 0||e.forEach(t=>{var i;return(i=t.hostDisconnected)===null||i===void 0?void 0:i.call(t)})}attributeChangedCallback(e,t,i){this._$attributeToProperty(e,i)}__propertyToAttribute(e,t,i=Yt){var n;const s=this.constructor.__attributeNameForProperty(e,i);if(s!==void 0&&i.reflect===!0){const l=(((n=i.converter)===null||n===void 0?void 0:n.toAttribute)!==void 0?i.converter:wo).toAttribute(t,i.type);this.constructor.enabledWarnings.indexOf("migration")>=0&&l===void 0&&de("undefined-attribute-value",`The attribute value for the ${e} property is undefined on element ${this.localName}. The attribute will be removed, but in the previous version of \`ReactiveElement\`, the attribute would not have changed.`),this.__reflectingProperty=e,l==null?this.removeAttribute(s):this.setAttribute(s,l),this.__reflectingProperty=null}}_$attributeToProperty(e,t){var i;const n=this.constructor,s=n.__attributeToPropertyMap.get(e);if(s!==void 0&&this.__reflectingProperty!==s){const r=n.getPropertyOptions(s),l=typeof r.converter=="function"?{fromAttribute:r.converter}:((i=r.converter)===null||i===void 0?void 0:i.fromAttribute)!==void 0?r.converter:wo;this.__reflectingProperty=s,this[s]=l.fromAttribute(t,r.type),this.__reflectingProperty=null}}requestUpdate(e,t,i){let n=!0;return e!==void 0&&(i=i||this.constructor.getPropertyOptions(e),(i.hasChanged||sn)(this[e],t)?(this._$changedProperties.has(e)||this._$changedProperties.set(e,t),i.reflect===!0&&this.__reflectingProperty!==e&&(this.__reflectingProperties===void 0&&(this.__reflectingProperties=new Map),this.__reflectingProperties.set(e,i))):n=!1),!this.isUpdatePending&&n&&(this.__updatePromise=this.__enqueueUpdate()),on(this.localName)}async __enqueueUpdate(){this.isUpdatePending=!0;try{await this.__updatePromise}catch(t){Promise.reject(t)}const e=this.scheduleUpdate();return e!=null&&await e,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){var e,t;if(!this.isUpdatePending)return;if(Kt==null||Kt({kind:"update"}),!this.hasUpdated){const s=[];if((e=this.constructor.__reactivePropertyKeys)===null||e===void 0||e.forEach(r=>{var l;this.hasOwnProperty(r)&&!(!((l=this.__instanceProperties)===null||l===void 0)&&l.has(r))&&s.push(r)}),s.length)throw new Error(`The following properties on element ${this.localName} will not trigger updates as expected because they are set using class fields: ${s.join(", ")}. Native class fields and some compiled output will overwrite accessors used for detecting changes. See https://lit.dev/msg/class-field-shadowing for more information.`)}this.__instanceProperties&&(this.__instanceProperties.forEach((s,r)=>this[r]=s),this.__instanceProperties=void 0);let i=!1;const n=this._$changedProperties;try{i=this.shouldUpdate(n),i?(this.willUpdate(n),(t=this.__controllers)===null||t===void 0||t.forEach(s=>{var r;return(r=s.hostUpdate)===null||r===void 0?void 0:r.call(s)}),this.update(n)):this.__markUpdated()}catch(s){throw i=!1,this.__markUpdated(),s}i&&this._$didUpdate(n)}willUpdate(e){}_$didUpdate(e){var t;(t=this.__controllers)===null||t===void 0||t.forEach(i=>{var n;return(n=i.hostUpdated)===null||n===void 0?void 0:n.call(i)}),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(e)),this.updated(e),this.isUpdatePending&&this.constructor.enabledWarnings.indexOf("change-in-update")>=0&&de("change-in-update",`Element ${this.localName} scheduled an update (generally because a property was set) after an update completed, causing a new update to be scheduled. This is inefficient and should be avoided unless the next update can only be scheduled as a side effect of the previous update.`)}__markUpdated(){this._$changedProperties=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this.__updatePromise}shouldUpdate(e){return!0}update(e){this.__reflectingProperties!==void 0&&(this.__reflectingProperties.forEach((t,i)=>this.__propertyToAttribute(i,this[i],t)),this.__reflectingProperties=void 0),this.__markUpdated()}updated(e){}firstUpdated(e){}}tn=So;ee[tn]=!0;ee.elementProperties=new Map;ee.elementStyles=[];ee.shadowRootOptions={mode:"open"};gt==null||gt({ReactiveElement:ee});{ee.enabledWarnings=["change-in-update"];const o=function(e){e.hasOwnProperty(nn("enabledWarnings"))||(e.enabledWarnings=e.enabledWarnings.slice())};ee.enableWarning=function(e){o(this),this.enabledWarnings.indexOf(e)<0&&this.enabledWarnings.push(e)},ee.disableWarning=function(e){o(this);const t=this.enabledWarnings.indexOf(e);t>=0&&this.enabledWarnings.splice(t,1)}}((Gt=Z.reactiveElementVersions)!==null&&Gt!==void 0?Gt:Z.reactiveElementVersions=[]).push("1.6.3");Z.reactiveElementVersions.length>1&&de("multiple-versions","Multiple versions of Lit loaded. Loading multiple versions is not recommended.");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var Jt,Xt,Qt,Zt;const U=window,g=o=>{U.emitLitDebugLogEvents&&U.dispatchEvent(new CustomEvent("lit-debug",{detail:o}))};let ys=0,Et;(Jt=U.litIssuedWarnings)!==null&&Jt!==void 0||(U.litIssuedWarnings=new Set),Et=(o,e)=>{e+=o?` See https://lit.dev/msg/${o} for more information.`:"",U.litIssuedWarnings.has(e)||(console.warn(e),U.litIssuedWarnings.add(e))},Et("dev-mode","Lit is in dev mode. Not recommended for production!");const H=!((Xt=U.ShadyDOM)===null||Xt===void 0)&&Xt.inUse&&((Qt=U.ShadyDOM)===null||Qt===void 0?void 0:Qt.noPatch)===!0?U.ShadyDOM.wrap:o=>o,Ie=U.trustedTypes,pi=Ie?Ie.createPolicy("lit-html",{createHTML:o=>o}):void 0,bs=o=>o,Ot=(o,e,t)=>bs,_s=o=>{if(_e!==Ot)throw new Error("Attempted to overwrite existing lit-html security policy. setSanitizeDOMValueFactory should be called at most once.");_e=o},ws=()=>{_e=Ot},Eo=(o,e,t)=>_e(o,e,t),xo="$lit$",ie=`lit$${String(Math.random()).slice(9)}$`,rn="?"+ie,Ss=`<${rn}>`,ye=document,qe=()=>ye.createComment(""),We=o=>o===null||typeof o!="object"&&typeof o!="function",an=Array.isArray,Es=o=>an(o)||typeof(o==null?void 0:o[Symbol.iterator])=="function",eo=`[ 	
\f\r]`,xs=`[^ 	
\f\r"'\`<>=]`,Cs=`[^\\s"'>=/]`,ze=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,mi=1,to=2,ks=3,vi=/-->/g,fi=/>/g,me=new RegExp(`>|${eo}(?:(${Cs}+)(${eo}*=${eo}*(?:${xs}|("|')|))|$)`,"g"),$s=0,gi=1,Ts=2,yi=3,oo=/'/g,io=/"/g,ln=/^(?:script|style|textarea|title)$/i,Ns=1,xt=2,Po=1,Ct=2,Ps=3,As=4,Rs=5,Ao=6,Is=7,dn=o=>(e,...t)=>(e.some(i=>i===void 0)&&console.warn(`Some template strings are undefined.
This is probably caused by illegal octal escape sequences.`),{_$litType$:o,strings:e,values:t}),f=dn(Ns),Te=dn(xt),be=Symbol.for("lit-noChange"),x=Symbol.for("lit-nothing"),bi=new WeakMap,fe=ye.createTreeWalker(ye,129,null,!1);let _e=Ot;function cn(o,e){if(!Array.isArray(o)||!o.hasOwnProperty("raw")){let t="invalid template strings array";throw t=`
          Internal Error: expected template strings to be an array
          with a 'raw' field. Faking a template strings array by
          calling html or svg like an ordinary function is effectively
          the same as calling unsafeHtml and can lead to major security
          issues, e.g. opening your code up to XSS attacks.
          If you're using the html or svg tagged template functions normally
          and still seeing this error, please file a bug at
          https://github.com/lit/lit/issues/new?template=bug_report.md
          and include information about your build tooling, if any.
        `.trim().replace(/\n */g,`
`),new Error(t)}return pi!==void 0?pi.createHTML(e):e}const Os=(o,e)=>{const t=o.length-1,i=[];let n=e===xt?"<svg>":"",s,r=ze;for(let a=0;a<t;a++){const d=o[a];let h=-1,m,u=0,v;for(;u<d.length&&(r.lastIndex=u,v=r.exec(d),v!==null);)if(u=r.lastIndex,r===ze){if(v[mi]==="!--")r=vi;else if(v[mi]!==void 0)r=fi;else if(v[to]!==void 0)ln.test(v[to])&&(s=new RegExp(`</${v[to]}`,"g")),r=me;else if(v[ks]!==void 0)throw new Error("Bindings in tag names are not supported. Please use static templates instead. See https://lit.dev/docs/templates/expressions/#static-expressions")}else r===me?v[$s]===">"?(r=s??ze,h=-1):v[gi]===void 0?h=-2:(h=r.lastIndex-v[Ts].length,m=v[gi],r=v[yi]===void 0?me:v[yi]==='"'?io:oo):r===io||r===oo?r=me:r===vi||r===fi?r=ze:(r=me,s=void 0);console.assert(h===-1||r===me||r===oo||r===io,"unexpected parse state B");const se=r===me&&o[a+1].startsWith("/>")?" ":"";n+=r===ze?d+Ss:h>=0?(i.push(m),d.slice(0,h)+xo+d.slice(h)+ie+se):d+ie+(h===-2?(i.push(void 0),a):se)}const l=n+(o[t]||"<?>")+(e===xt?"</svg>":"");return[cn(o,l),i]};class Ge{constructor({strings:e,["_$litType$"]:t},i){this.parts=[];let n,s=0,r=0;const l=e.length-1,a=this.parts,[d,h]=Os(e,t);if(this.el=Ge.createElement(d,i),fe.currentNode=this.el.content,t===xt){const m=this.el.content,u=m.firstChild;u.remove(),m.append(...u.childNodes)}for(;(n=fe.nextNode())!==null&&a.length<l;){if(n.nodeType===1){{const m=n.localName;if(/^(?:textarea|template)$/i.test(m)&&n.innerHTML.includes(ie)){const u=`Expressions are not supported inside \`${m}\` elements. See https://lit.dev/msg/expression-in-${m} for more information.`;if(m==="template")throw new Error(u);Et("",u)}}if(n.hasAttributes()){const m=[];for(const u of n.getAttributeNames())if(u.endsWith(xo)||u.startsWith(ie)){const v=h[r++];if(m.push(u),v!==void 0){const re=n.getAttribute(v.toLowerCase()+xo).split(ie),te=/([.?@])?(.*)/.exec(v);a.push({type:Po,index:s,name:te[2],strings:re,ctor:te[1]==="."?Ms:te[1]==="?"?Ds:te[1]==="@"?zs:Lt})}else a.push({type:Ao,index:s})}for(const u of m)n.removeAttribute(u)}if(ln.test(n.tagName)){const m=n.textContent.split(ie),u=m.length-1;if(u>0){n.textContent=Ie?Ie.emptyScript:"";for(let v=0;v<u;v++)n.append(m[v],qe()),fe.nextNode(),a.push({type:Ct,index:++s});n.append(m[u],qe())}}}else if(n.nodeType===8)if(n.data===rn)a.push({type:Ct,index:s});else{let u=-1;for(;(u=n.data.indexOf(ie,u+1))!==-1;)a.push({type:Is,index:s}),u+=ie.length-1}s++}g==null||g({kind:"template prep",template:this,clonableTemplate:this.el,parts:this.parts,strings:e})}static createElement(e,t){const i=ye.createElement("template");return i.innerHTML=e,i}}function Oe(o,e,t=o,i){var n,s,r,l;if(e===be)return e;let a=i!==void 0?(n=t.__directives)===null||n===void 0?void 0:n[i]:t.__directive;const d=We(e)?void 0:e._$litDirective$;return(a==null?void 0:a.constructor)!==d&&((s=a==null?void 0:a._$notifyDirectiveConnectionChanged)===null||s===void 0||s.call(a,!1),d===void 0?a=void 0:(a=new d(o),a._$initialize(o,t,i)),i!==void 0?((r=(l=t).__directives)!==null&&r!==void 0?r:l.__directives=[])[i]=a:t.__directive=a),a!==void 0&&(e=Oe(o,a._$resolve(o,e.values),a,i)),e}class Ls{constructor(e,t){this._$parts=[],this._$disconnectableChildren=void 0,this._$template=e,this._$parent=t}get parentNode(){return this._$parent.parentNode}get _$isConnected(){return this._$parent._$isConnected}_clone(e){var t;const{el:{content:i},parts:n}=this._$template,s=((t=e==null?void 0:e.creationScope)!==null&&t!==void 0?t:ye).importNode(i,!0);fe.currentNode=s;let r=fe.nextNode(),l=0,a=0,d=n[0];for(;d!==void 0;){if(l===d.index){let h;d.type===Ct?h=new et(r,r.nextSibling,this,e):d.type===Po?h=new d.ctor(r,d.name,d.strings,this,e):d.type===Ao&&(h=new Us(r,this,e)),this._$parts.push(h),d=n[++a]}l!==(d==null?void 0:d.index)&&(r=fe.nextNode(),l++)}return fe.currentNode=ye,s}_update(e){let t=0;for(const i of this._$parts)i!==void 0&&(g==null||g({kind:"set part",part:i,value:e[t],valueIndex:t,values:e,templateInstance:this}),i.strings!==void 0?(i._$setValue(e,i,t),t+=i.strings.length-2):i._$setValue(e[t])),t++}}class et{constructor(e,t,i,n){var s;this.type=Ct,this._$committedValue=x,this._$disconnectableChildren=void 0,this._$startNode=e,this._$endNode=t,this._$parent=i,this.options=n,this.__isConnected=(s=n==null?void 0:n.isConnected)!==null&&s!==void 0?s:!0,this._textSanitizer=void 0}get _$isConnected(){var e,t;return(t=(e=this._$parent)===null||e===void 0?void 0:e._$isConnected)!==null&&t!==void 0?t:this.__isConnected}get parentNode(){let e=H(this._$startNode).parentNode;const t=this._$parent;return t!==void 0&&(e==null?void 0:e.nodeType)===11&&(e=t.parentNode),e}get startNode(){return this._$startNode}get endNode(){return this._$endNode}_$setValue(e,t=this){var i;if(this.parentNode===null)throw new Error("This `ChildPart` has no `parentNode` and therefore cannot accept a value. This likely means the element containing the part was manipulated in an unsupported way outside of Lit's control such that the part's marker nodes were ejected from DOM. For example, setting the element's `innerHTML` or `textContent` can do this.");if(e=Oe(this,e,t),We(e))e===x||e==null||e===""?(this._$committedValue!==x&&(g==null||g({kind:"commit nothing to child",start:this._$startNode,end:this._$endNode,parent:this._$parent,options:this.options}),this._$clear()),this._$committedValue=x):e!==this._$committedValue&&e!==be&&this._commitText(e);else if(e._$litType$!==void 0)this._commitTemplateResult(e);else if(e.nodeType!==void 0){if(((i=this.options)===null||i===void 0?void 0:i.host)===e){this._commitText("[probable mistake: rendered a template's host in itself (commonly caused by writing ${this} in a template]"),console.warn("Attempted to render the template host",e,"inside itself. This is almost always a mistake, and in dev mode ","we render some warning text. In production however, we'll ","render it, which will usually result in an error, and sometimes ","in the element disappearing from the DOM.");return}this._commitNode(e)}else Es(e)?this._commitIterable(e):this._commitText(e)}_insert(e){return H(H(this._$startNode).parentNode).insertBefore(e,this._$endNode)}_commitNode(e){var t;if(this._$committedValue!==e){if(this._$clear(),_e!==Ot){const i=(t=this._$startNode.parentNode)===null||t===void 0?void 0:t.nodeName;if(i==="STYLE"||i==="SCRIPT"){let n="Forbidden";throw i==="STYLE"?n="Lit does not support binding inside style nodes. This is a security risk, as style injection attacks can exfiltrate data and spoof UIs. Consider instead using css`...` literals to compose styles, and make do dynamic styling with css custom properties, ::parts, <slot>s, and by mutating the DOM rather than stylesheets.":n="Lit does not support binding inside script nodes. This is a security risk, as it could allow arbitrary code execution.",new Error(n)}}g==null||g({kind:"commit node",start:this._$startNode,parent:this._$parent,value:e,options:this.options}),this._$committedValue=this._insert(e)}}_commitText(e){if(this._$committedValue!==x&&We(this._$committedValue)){const t=H(this._$startNode).nextSibling;this._textSanitizer===void 0&&(this._textSanitizer=Eo(t,"data","property")),e=this._textSanitizer(e),g==null||g({kind:"commit text",node:t,value:e,options:this.options}),t.data=e}else{const t=ye.createTextNode("");this._commitNode(t),this._textSanitizer===void 0&&(this._textSanitizer=Eo(t,"data","property")),e=this._textSanitizer(e),g==null||g({kind:"commit text",node:t,value:e,options:this.options}),t.data=e}this._$committedValue=e}_commitTemplateResult(e){var t;const{values:i,["_$litType$"]:n}=e,s=typeof n=="number"?this._$getTemplate(e):(n.el===void 0&&(n.el=Ge.createElement(cn(n.h,n.h[0]),this.options)),n);if(((t=this._$committedValue)===null||t===void 0?void 0:t._$template)===s)g==null||g({kind:"template updating",template:s,instance:this._$committedValue,parts:this._$committedValue._$parts,options:this.options,values:i}),this._$committedValue._update(i);else{const r=new Ls(s,this),l=r._clone(this.options);g==null||g({kind:"template instantiated",template:s,instance:r,parts:r._$parts,options:this.options,fragment:l,values:i}),r._update(i),g==null||g({kind:"template instantiated and updated",template:s,instance:r,parts:r._$parts,options:this.options,fragment:l,values:i}),this._commitNode(l),this._$committedValue=r}}_$getTemplate(e){let t=bi.get(e.strings);return t===void 0&&bi.set(e.strings,t=new Ge(e)),t}_commitIterable(e){an(this._$committedValue)||(this._$committedValue=[],this._$clear());const t=this._$committedValue;let i=0,n;for(const s of e)i===t.length?t.push(n=new et(this._insert(qe()),this._insert(qe()),this,this.options)):n=t[i],n._$setValue(s),i++;i<t.length&&(this._$clear(n&&H(n._$endNode).nextSibling,i),t.length=i)}_$clear(e=H(this._$startNode).nextSibling,t){var i;for((i=this._$notifyConnectionChanged)===null||i===void 0||i.call(this,!1,!0,t);e&&e!==this._$endNode;){const n=H(e).nextSibling;H(e).remove(),e=n}}setConnected(e){var t;if(this._$parent===void 0)this.__isConnected=e,(t=this._$notifyConnectionChanged)===null||t===void 0||t.call(this,e);else throw new Error("part.setConnected() may only be called on a RootPart returned from render().")}}class Lt{constructor(e,t,i,n,s){this.type=Po,this._$committedValue=x,this._$disconnectableChildren=void 0,this.element=e,this.name=t,this._$parent=n,this.options=s,i.length>2||i[0]!==""||i[1]!==""?(this._$committedValue=new Array(i.length-1).fill(new String),this.strings=i):this._$committedValue=x,this._sanitizer=void 0}get tagName(){return this.element.tagName}get _$isConnected(){return this._$parent._$isConnected}_$setValue(e,t=this,i,n){const s=this.strings;let r=!1;if(s===void 0)e=Oe(this,e,t,0),r=!We(e)||e!==this._$committedValue&&e!==be,r&&(this._$committedValue=e);else{const l=e;e=s[0];let a,d;for(a=0;a<s.length-1;a++)d=Oe(this,l[i+a],t,a),d===be&&(d=this._$committedValue[a]),r||(r=!We(d)||d!==this._$committedValue[a]),d===x?e=x:e!==x&&(e+=(d??"")+s[a+1]),this._$committedValue[a]=d}r&&!n&&this._commitValue(e)}_commitValue(e){e===x?H(this.element).removeAttribute(this.name):(this._sanitizer===void 0&&(this._sanitizer=_e(this.element,this.name,"attribute")),e=this._sanitizer(e??""),g==null||g({kind:"commit attribute",element:this.element,name:this.name,value:e,options:this.options}),H(this.element).setAttribute(this.name,e??""))}}class Ms extends Lt{constructor(){super(...arguments),this.type=Ps}_commitValue(e){this._sanitizer===void 0&&(this._sanitizer=_e(this.element,this.name,"property")),e=this._sanitizer(e),g==null||g({kind:"commit property",element:this.element,name:this.name,value:e,options:this.options}),this.element[this.name]=e===x?void 0:e}}const Vs=Ie?Ie.emptyScript:"";class Ds extends Lt{constructor(){super(...arguments),this.type=As}_commitValue(e){g==null||g({kind:"commit boolean attribute",element:this.element,name:this.name,value:!!(e&&e!==x),options:this.options}),e&&e!==x?H(this.element).setAttribute(this.name,Vs):H(this.element).removeAttribute(this.name)}}class zs extends Lt{constructor(e,t,i,n,s){if(super(e,t,i,n,s),this.type=Rs,this.strings!==void 0)throw new Error(`A \`<${e.localName}>\` has a \`@${t}=...\` listener with invalid content. Event listeners in templates must have exactly one expression and no surrounding text.`)}_$setValue(e,t=this){var i;if(e=(i=Oe(this,e,t,0))!==null&&i!==void 0?i:x,e===be)return;const n=this._$committedValue,s=e===x&&n!==x||e.capture!==n.capture||e.once!==n.once||e.passive!==n.passive,r=e!==x&&(n===x||s);g==null||g({kind:"commit event listener",element:this.element,name:this.name,value:e,options:this.options,removeListener:s,addListener:r,oldListener:n}),s&&this.element.removeEventListener(this.name,this,n),r&&this.element.addEventListener(this.name,this,e),this._$committedValue=e}handleEvent(e){var t,i;typeof this._$committedValue=="function"?this._$committedValue.call((i=(t=this.options)===null||t===void 0?void 0:t.host)!==null&&i!==void 0?i:this.element,e):this._$committedValue.handleEvent(e)}}class Us{constructor(e,t,i){this.element=e,this.type=Ao,this._$disconnectableChildren=void 0,this._$parent=t,this.options=i}get _$isConnected(){return this._$parent._$isConnected}_$setValue(e){g==null||g({kind:"commit to element binding",element:this.element,value:e,options:this.options}),Oe(this,e)}}const no=U.litHtmlPolyfillSupportDevMode;no==null||no(Ge,et);((Zt=U.litHtmlVersions)!==null&&Zt!==void 0?Zt:U.litHtmlVersions=[]).push("2.8.0");U.litHtmlVersions.length>1&&Et("multiple-versions","Multiple versions of Lit loaded. Loading multiple versions is not recommended.");const Ae=(o,e,t)=>{var i,n;if(e==null)throw new TypeError(`The container to render into may not be ${e}`);const s=ys++,r=(i=t==null?void 0:t.renderBefore)!==null&&i!==void 0?i:e;let l=r._$litPart$;if(g==null||g({kind:"begin render",id:s,value:o,container:e,options:t,part:l}),l===void 0){const a=(n=t==null?void 0:t.renderBefore)!==null&&n!==void 0?n:null;r._$litPart$=l=new et(e.insertBefore(qe(),a),a,void 0,t??{})}return l._$setValue(o),g==null||g({kind:"end render",id:s,value:o,container:e,options:t,part:l}),l};Ae.setSanitizer=_s,Ae.createSanitizer=Eo,Ae._testOnlyClearSanitizerFactoryDoNotCallOrElse=ws;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var so,ro,ao;let Ro;{const o=(so=globalThis.litIssuedWarnings)!==null&&so!==void 0?so:globalThis.litIssuedWarnings=new Set;Ro=(e,t)=>{t+=` See https://lit.dev/msg/${e} for more information.`,o.has(t)||(console.warn(t),o.add(t))}}class A extends ee{constructor(){super(...arguments),this.renderOptions={host:this},this.__childPart=void 0}createRenderRoot(){var e,t;const i=super.createRenderRoot();return(e=(t=this.renderOptions).renderBefore)!==null&&e!==void 0||(t.renderBefore=i.firstChild),i}update(e){const t=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(e),this.__childPart=Ae(t,this.renderRoot,this.renderOptions)}connectedCallback(){var e;super.connectedCallback(),(e=this.__childPart)===null||e===void 0||e.setConnected(!0)}disconnectedCallback(){var e;super.disconnectedCallback(),(e=this.__childPart)===null||e===void 0||e.setConnected(!1)}render(){return be}}A.finalized=!0;A._$litElement$=!0;(ro=globalThis.litElementHydrateSupport)===null||ro===void 0||ro.call(globalThis,{LitElement:A});const lo=globalThis.litElementPolyfillSupportDevMode;lo==null||lo({LitElement:A});A.finalize=function(){if(!ee.finalize.call(this))return!1;const e=(t,i,n=!1)=>{if(t.hasOwnProperty(i)){const s=(typeof t=="function"?t:t.constructor).name;Ro(n?"renamed-api":"removed-api",`\`${i}\` is implemented on class ${s}. It has been ${n?"renamed":"removed"} in this version of LitElement.`)}};return e(this,"render"),e(this,"getStyles",!0),e(this.prototype,"adoptStyles"),!0};((ao=globalThis.litElementVersions)!==null&&ao!==void 0?ao:globalThis.litElementVersions=[]).push("3.3.3");globalThis.litElementVersions.length>1&&Ro("multiple-versions","Multiple versions of Lit loaded. Loading multiple versions is not recommended.");/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const js=(o,e)=>(customElements.define(o,e),e),Fs=(o,e)=>{const{kind:t,elements:i}=e;return{kind:t,elements:i,finisher(n){customElements.define(o,n)}}},F=o=>e=>typeof e=="function"?js(o,e):Fs(o,e);/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const Bs=(o,e)=>e.kind==="method"&&e.descriptor&&!("value"in e.descriptor)?{...e,finisher(t){t.createProperty(e.key,o)}}:{kind:"field",key:Symbol(),placement:"own",descriptor:{},originalKey:e.key,initializer(){typeof e.initializer=="function"&&(this[e.key]=e.initializer.call(this))},finisher(t){t.createProperty(e.key,o)}},Hs=(o,e,t)=>{e.constructor.createProperty(t,o)};function y(o){return(e,t)=>t!==void 0?Hs(o,e,t):Bs(o,e)}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function T(o){return y({...o,state:!0})}/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const qs=({finisher:o,descriptor:e})=>(t,i)=>{var n;if(i!==void 0){const s=t.constructor;e!==void 0&&Object.defineProperty(t,i,e(i)),o==null||o(s,i)}else{const s=(n=t.originalKey)!==null&&n!==void 0?n:t.key,r=e!=null?{kind:"method",placement:"prototype",key:s,descriptor:e(t.key)}:{...t,key:s};return o!=null&&(r.finisher=function(l){o(l,s)}),r}};/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */function tt(o,e){return qs({descriptor:t=>{const i={get(){var n,s;return(s=(n=this.renderRoot)===null||n===void 0?void 0:n.querySelector(o))!==null&&s!==void 0?s:null},enumerable:!0,configurable:!0};if(e){const n=typeof t=="symbol"?Symbol():`__${t}`;i.get=function(){var s,r;return this[n]===void 0&&(this[n]=(r=(s=this.renderRoot)===null||s===void 0?void 0:s.querySelector(o))!==null&&r!==void 0?r:null),this[n]}}return i}})}/**
 * @license
 * Copyright 2021 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */var co;const Ws=window;((co=Ws.HTMLSlotElement)===null||co===void 0?void 0:co.prototype.assignedElements)!=null;/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const Gs={ATTRIBUTE:1,CHILD:2,PROPERTY:3,BOOLEAN_ATTRIBUTE:4,EVENT:5,ELEMENT:6},Ks=o=>(...e)=>({_$litDirective$:o,values:e});class Ys{constructor(e){}get _$isConnected(){return this._$parent._$isConnected}_$initialize(e,t,i){this.__part=e,this._$parent=t,this.__attributeIndex=i}_$resolve(e,t){return this.update(e,t)}update(e,t){return this.render(...t)}}/**
 * @license
 * Copyright 2018 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */class Js extends Ys{constructor(e){var t;if(super(e),e.type!==Gs.ATTRIBUTE||e.name!=="class"||((t=e.strings)===null||t===void 0?void 0:t.length)>2)throw new Error("`classMap()` can only be used in the `class` attribute and must be the only part in the attribute.")}render(e){return" "+Object.keys(e).filter(t=>e[t]).join(" ")+" "}update(e,[t]){var i,n;if(this._previousClasses===void 0){this._previousClasses=new Set,e.strings!==void 0&&(this._staticClasses=new Set(e.strings.join(" ").split(/\s/).filter(r=>r!=="")));for(const r in t)t[r]&&!(!((i=this._staticClasses)===null||i===void 0)&&i.has(r))&&this._previousClasses.add(r);return this.render(t)}const s=e.element.classList;this._previousClasses.forEach(r=>{r in t||(s.remove(r),this._previousClasses.delete(r))});for(const r in t){const l=!!t[r];l!==this._previousClasses.has(r)&&!(!((n=this._staticClasses)===null||n===void 0)&&n.has(r))&&(l?(s.add(r),this._previousClasses.add(r)):(s.remove(r),this._previousClasses.delete(r)))}return be}}const Io=Ks(Js),ho="css-loading-indicator";var G;(function(o){o.IDLE="",o.FIRST="first",o.SECOND="second",o.THIRD="third"})(G||(G={}));class R extends A{constructor(){super(),this.firstDelay=450,this.secondDelay=1500,this.thirdDelay=5e3,this.expandedDuration=2e3,this.onlineText="Online",this.offlineText="Connection lost",this.reconnectingText="Connection lost, trying to reconnect...",this.offline=!1,this.reconnecting=!1,this.expanded=!1,this.loading=!1,this.loadingBarState=G.IDLE,this.applyDefaultThemeState=!0,this.firstTimeout=0,this.secondTimeout=0,this.thirdTimeout=0,this.expandedTimeout=0,this.lastMessageState=$.CONNECTED,this.connectionStateListener=()=>{this.expanded=this.updateConnectionState(),this.expandedTimeout=this.timeoutFor(this.expandedTimeout,this.expanded,()=>{this.expanded=!1},this.expandedDuration)}}static create(){var e,t;const i=window;return!((e=i.Vaadin)===null||e===void 0)&&e.connectionIndicator||(i.Vaadin=i.Vaadin||{},i.Vaadin.connectionIndicator=document.createElement("vaadin-connection-indicator"),document.body.appendChild(i.Vaadin.connectionIndicator)),(t=i.Vaadin)===null||t===void 0?void 0:t.connectionIndicator}render(){return f`
      <div class="v-loading-indicator ${this.loadingBarState}" style=${this.getLoadingBarStyle()}></div>

      <div
        class="v-status-message ${Io({active:this.reconnecting})}"
      >
        <span class="text"> ${this.renderMessage()} </span>
      </div>
    `}connectedCallback(){var e;super.connectedCallback();const t=window;!((e=t.Vaadin)===null||e===void 0)&&e.connectionState&&(this.connectionStateStore=t.Vaadin.connectionState,this.connectionStateStore.addStateChangeListener(this.connectionStateListener),this.updateConnectionState()),this.updateTheme()}disconnectedCallback(){super.disconnectedCallback(),this.connectionStateStore&&this.connectionStateStore.removeStateChangeListener(this.connectionStateListener),this.updateTheme()}get applyDefaultTheme(){return this.applyDefaultThemeState}set applyDefaultTheme(e){e!==this.applyDefaultThemeState&&(this.applyDefaultThemeState=e,this.updateTheme())}createRenderRoot(){return this}updateConnectionState(){var e;const t=(e=this.connectionStateStore)===null||e===void 0?void 0:e.state;return this.offline=t===$.CONNECTION_LOST,this.reconnecting=t===$.RECONNECTING,this.updateLoading(t===$.LOADING),this.loading?!1:t!==this.lastMessageState?(this.lastMessageState=t,!0):!1}updateLoading(e){this.loading=e,this.loadingBarState=G.IDLE,this.firstTimeout=this.timeoutFor(this.firstTimeout,e,()=>{this.loadingBarState=G.FIRST},this.firstDelay),this.secondTimeout=this.timeoutFor(this.secondTimeout,e,()=>{this.loadingBarState=G.SECOND},this.secondDelay),this.thirdTimeout=this.timeoutFor(this.thirdTimeout,e,()=>{this.loadingBarState=G.THIRD},this.thirdDelay)}renderMessage(){return this.reconnecting?this.reconnectingText:this.offline?this.offlineText:this.onlineText}updateTheme(){if(this.applyDefaultThemeState&&this.isConnected){if(!document.getElementById(ho)){const e=document.createElement("style");e.id=ho,e.textContent=this.getDefaultStyle(),document.head.appendChild(e)}}else{const e=document.getElementById(ho);e&&document.head.removeChild(e)}}getDefaultStyle(){return`
      @keyframes v-progress-start {
        0% {
          width: 0%;
        }
        100% {
          width: 50%;
        }
      }
      @keyframes v-progress-delay {
        0% {
          width: 50%;
        }
        100% {
          width: 90%;
        }
      }
      @keyframes v-progress-wait {
        0% {
          width: 90%;
          height: 4px;
        }
        3% {
          width: 91%;
          height: 7px;
        }
        100% {
          width: 96%;
          height: 7px;
        }
      }
      @keyframes v-progress-wait-pulse {
        0% {
          opacity: 1;
        }
        50% {
          opacity: 0.1;
        }
        100% {
          opacity: 1;
        }
      }
      .v-loading-indicator,
      .v-status-message {
        position: fixed;
        z-index: 251;
        left: 0;
        right: auto;
        top: 0;
        background-color: var(--lumo-primary-color, var(--material-primary-color, blue));
        transition: none;
      }
      .v-loading-indicator {
        width: 50%;
        height: 4px;
        opacity: 1;
        pointer-events: none;
        animation: v-progress-start 1000ms 200ms both;
      }
      .v-loading-indicator[style*='none'] {
        display: block !important;
        width: 100%;
        opacity: 0;
        animation: none;
        transition: opacity 500ms 300ms, width 300ms;
      }
      .v-loading-indicator.second {
        width: 90%;
        animation: v-progress-delay 3.8s forwards;
      }
      .v-loading-indicator.third {
        width: 96%;
        animation: v-progress-wait 5s forwards, v-progress-wait-pulse 1s 4s infinite backwards;
      }

      vaadin-connection-indicator[offline] .v-loading-indicator,
      vaadin-connection-indicator[reconnecting] .v-loading-indicator {
        display: none;
      }

      .v-status-message {
        opacity: 0;
        width: 100%;
        max-height: var(--status-height-collapsed, 8px);
        overflow: hidden;
        background-color: var(--status-bg-color-online, var(--lumo-primary-color, var(--material-primary-color, blue)));
        color: var(
          --status-text-color-online,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        font-size: 0.75rem;
        font-weight: 600;
        line-height: 1;
        transition: all 0.5s;
        padding: 0 0.5em;
      }

      vaadin-connection-indicator[offline] .v-status-message,
      vaadin-connection-indicator[reconnecting] .v-status-message {
        opacity: 1;
        background-color: var(--status-bg-color-offline, var(--lumo-shade, #333));
        color: var(
          --status-text-color-offline,
          var(--lumo-primary-contrast-color, var(--material-primary-contrast-color, #fff))
        );
        background-image: repeating-linear-gradient(
          45deg,
          rgba(255, 255, 255, 0),
          rgba(255, 255, 255, 0) 10px,
          rgba(255, 255, 255, 0.1) 10px,
          rgba(255, 255, 255, 0.1) 20px
        );
      }

      vaadin-connection-indicator[reconnecting] .v-status-message {
        animation: show-reconnecting-status 2s;
      }

      vaadin-connection-indicator[offline] .v-status-message:hover,
      vaadin-connection-indicator[reconnecting] .v-status-message:hover,
      vaadin-connection-indicator[expanded] .v-status-message {
        max-height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[expanded] .v-status-message {
        opacity: 1;
      }

      .v-status-message span {
        display: flex;
        align-items: center;
        justify-content: center;
        height: var(--status-height, 1.75rem);
      }

      vaadin-connection-indicator[reconnecting] .v-status-message span::before {
        content: '';
        width: 1em;
        height: 1em;
        border-top: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-left: 2px solid
          var(--status-spinner-color, var(--lumo-primary-color, var(--material-primary-color, blue)));
        border-right: 2px solid transparent;
        border-bottom: 2px solid transparent;
        border-radius: 50%;
        box-sizing: border-box;
        animation: v-spin 0.4s linear infinite;
        margin: 0 0.5em;
      }

      @keyframes v-spin {
        100% {
          transform: rotate(360deg);
        }
      }
    `}getLoadingBarStyle(){switch(this.loadingBarState){case G.IDLE:return"display: none";case G.FIRST:case G.SECOND:case G.THIRD:return"display: block";default:return""}}timeoutFor(e,t,i,n){return e!==0&&window.clearTimeout(e),t?window.setTimeout(i,n):0}static get instance(){return R.create()}}j([y({type:Number})],R.prototype,"firstDelay",void 0);j([y({type:Number})],R.prototype,"secondDelay",void 0);j([y({type:Number})],R.prototype,"thirdDelay",void 0);j([y({type:Number})],R.prototype,"expandedDuration",void 0);j([y({type:String})],R.prototype,"onlineText",void 0);j([y({type:String})],R.prototype,"offlineText",void 0);j([y({type:String})],R.prototype,"reconnectingText",void 0);j([y({type:Boolean,reflect:!0})],R.prototype,"offline",void 0);j([y({type:Boolean,reflect:!0})],R.prototype,"reconnecting",void 0);j([y({type:Boolean,reflect:!0})],R.prototype,"expanded",void 0);j([y({type:Boolean,reflect:!0})],R.prototype,"loading",void 0);j([y({type:String})],R.prototype,"loadingBarState",void 0);j([y({type:Boolean})],R.prototype,"applyDefaultTheme",null);customElements.get("vaadin-connection-indicator")===void 0&&customElements.define("vaadin-connection-indicator",R);R.instance;const Ke=window;Ke.Vaadin=Ke.Vaadin||{};Ke.Vaadin.registrations=Ke.Vaadin.registrations||[];Ke.Vaadin.registrations.push({is:"@vaadin/common-frontend",version:"0.0.18"});class _i extends Error{}const Ue=window.document.body,w=window;class Xs{constructor(e){this.response=void 0,this.pathname="",this.isActive=!1,this.baseRegex=/^\//,this.navigation="",Ue.$=Ue.$||[],this.config=e||{},w.Vaadin=w.Vaadin||{},w.Vaadin.Flow=w.Vaadin.Flow||{},w.Vaadin.Flow.clients={TypeScript:{isActive:()=>this.isActive}};const t=document.head.querySelector("base");this.baseRegex=new RegExp(`^${(document.baseURI||t&&t.href||"/").replace(/^https?:\/\/[^/]+/i,"")}`),this.appShellTitle=document.title,this.addConnectionIndicator()}get serverSideRoutes(){return[{path:"(.*)",action:this.action}]}loadingStarted(){this.isActive=!0,w.Vaadin.connectionState.loadingStarted()}loadingFinished(){this.isActive=!1,w.Vaadin.connectionState.loadingFinished(),!w.Vaadin.listener&&(w.Vaadin.listener={},document.addEventListener("click",e=>{e.target&&(e.target.hasAttribute("router-link")?this.navigation="link":e.composedPath().some(t=>t.nodeName==="A")&&(this.navigation="client"))},{capture:!0}))}get action(){return async e=>{if(this.pathname=e.pathname,w.Vaadin.connectionState.online)try{await this.flowInit()}catch(t){if(t instanceof _i)return w.Vaadin.connectionState.state=$.CONNECTION_LOST,this.offlineStubAction();throw t}else return this.offlineStubAction();return this.container.onBeforeEnter=(t,i)=>this.flowNavigate(t,i),this.container.onBeforeLeave=(t,i)=>this.flowLeave(t,i),this.container}}async flowLeave(e,t){const{connectionState:i}=w.Vaadin;return this.pathname===e.pathname||!this.isFlowClientLoaded()||i.offline?Promise.resolve({}):new Promise(n=>{this.loadingStarted(),this.container.serverConnected=s=>{n(t&&s?t.prevent():{}),this.loadingFinished()},Ue.$server.leaveNavigation(this.getFlowRoutePath(e),this.getFlowRouteQuery(e))})}async flowNavigate(e,t){return this.response?new Promise(i=>{this.loadingStarted(),this.container.serverConnected=(n,s)=>{t&&n?i(t.prevent()):t&&t.redirect&&s?i(t.redirect(s.pathname)):(this.container.style.display="",i(this.container)),this.loadingFinished()},Ue.$server.connectClient(this.getFlowRoutePath(e),this.getFlowRouteQuery(e),this.appShellTitle,history.state,this.navigation),this.navigation="history"}):Promise.resolve(this.container)}getFlowRoutePath(e){return decodeURIComponent(e.pathname).replace(this.baseRegex,"")}getFlowRouteQuery(e){return e.search&&e.search.substring(1)||""}async flowInit(){if(!this.isFlowClientLoaded()){this.loadingStarted(),this.response=await this.flowInitUi();const{pushScript:e,appConfig:t}=this.response;typeof e=="string"&&await this.loadScript(e);const{appId:i}=t;await(await E(()=>import("./FlowBootstrap-feff2646.js"),[],import.meta.url)).init(this.response),typeof this.config.imports=="function"&&(this.injectAppIdScript(i),await this.config.imports());const s=`flow-container-${i.toLowerCase()}`,r=document.querySelector(s);r?this.container=r:(this.container=document.createElement(s),this.container.id=i),Ue.$[i]=this.container;const l=await E(()=>import("./FlowClient-d5d5e377.js"),[],import.meta.url);await this.flowInitClient(l),this.loadingFinished()}return this.container&&!this.container.isConnected&&(this.container.style.display="none",document.body.appendChild(this.container)),this.response}async loadScript(e){return new Promise((t,i)=>{const n=document.createElement("script");n.onload=()=>t(),n.onerror=i,n.src=e,document.body.appendChild(n)})}injectAppIdScript(e){const t=e.substring(0,e.lastIndexOf("-")),i=document.createElement("script");i.type="module",i.setAttribute("data-app-id",t),document.body.append(i)}async flowInitClient(e){return e.init(),new Promise(t=>{const i=setInterval(()=>{Object.keys(w.Vaadin.Flow.clients).filter(s=>s!=="TypeScript").reduce((s,r)=>s||w.Vaadin.Flow.clients[r].isActive(),!1)||(clearInterval(i),t())},5)})}async flowInitUi(){const e=w.Vaadin&&w.Vaadin.TypeScript&&w.Vaadin.TypeScript.initial;return e?(w.Vaadin.TypeScript.initial=void 0,Promise.resolve(e)):new Promise((t,i)=>{const s=new XMLHttpRequest,r=`?v-r=init&location=${encodeURIComponent(this.getFlowRoutePath(location))}&query=${encodeURIComponent(this.getFlowRouteQuery(location))}`;s.open("GET",r),s.onerror=()=>i(new _i(`Invalid server response when initializing Flow UI.
        ${s.status}
        ${s.responseText}`)),s.onload=()=>{const l=s.getResponseHeader("content-type");l&&l.indexOf("application/json")!==-1?t(JSON.parse(s.responseText)):s.onerror()},s.send()})}addConnectionIndicator(){R.create(),w.addEventListener("online",()=>{if(!this.isFlowClientLoaded()){w.Vaadin.connectionState.state=$.RECONNECTING;const e=new XMLHttpRequest;e.open("HEAD","sw.js"),e.onload=()=>{w.Vaadin.connectionState.state=$.CONNECTED},e.onerror=()=>{w.Vaadin.connectionState.state=$.CONNECTION_LOST},setTimeout(()=>e.send(),50)}}),w.addEventListener("offline",()=>{this.isFlowClientLoaded()||(w.Vaadin.connectionState.state=$.CONNECTION_LOST)})}async offlineStubAction(){const e=document.createElement("iframe"),t="./offline-stub.html";e.setAttribute("src",t),e.setAttribute("style","width: 100%; height: 100%; border: 0"),this.response=void 0;let i;const n=()=>{i!==void 0&&(w.Vaadin.connectionState.removeStateChangeListener(i),i=void 0)};return e.onBeforeEnter=(s,r,l)=>{i=()=>{w.Vaadin.connectionState.online&&(n(),l.render(s,!1))},w.Vaadin.connectionState.addStateChangeListener(i)},e.onBeforeLeave=(s,r,l)=>{n()},e}isFlowClientLoaded(){return this.response!==void 0}}const{serverSideRoutes:Qs}=new Xs({imports:()=>E(()=>import("./generated-flow-imports-f43a7a5a.js"),[],import.meta.url)}),Zs=[...Qs],er=new le(document.querySelector("#outlet"));er.setRoutes(Zs);window.Vaadin.connectionState.connectionState="connected";(function(){if(typeof document>"u"||"adoptedStyleSheets"in document)return;var o="ShadyCSS"in window&&!ShadyCSS.nativeShadow,e=document.implementation.createHTMLDocument(""),t=new WeakMap,i=typeof DOMException=="object"?Error:DOMException,n=Object.defineProperty,s=Array.prototype.forEach,r=/@import.+?;?$/gm;function l(c){var p=c.replace(r,"");return p!==c&&console.warn("@import rules are not allowed here. See https://github.com/WICG/construct-stylesheets/issues/119#issuecomment-588352418"),p.trim()}function a(c){return"isConnected"in c?c.isConnected:document.contains(c)}function d(c){return c.filter(function(p,b){return c.indexOf(p)===b})}function h(c,p){return c.filter(function(b){return p.indexOf(b)===-1})}function m(c){c.parentNode.removeChild(c)}function u(c){return c.shadowRoot||t.get(c)}var v=["addRule","deleteRule","insertRule","removeRule"],se=CSSStyleSheet,re=se.prototype;re.replace=function(){return Promise.reject(new i("Can't call replace on non-constructed CSSStyleSheets."))},re.replaceSync=function(){throw new i("Failed to execute 'replaceSync' on 'CSSStyleSheet': Can't call replaceSync on non-constructed CSSStyleSheets.")};function te(c){return typeof c=="object"?xe.isPrototypeOf(c)||re.isPrototypeOf(c):!1}function Dt(c){return typeof c=="object"?re.isPrototypeOf(c):!1}var B=new WeakMap,X=new WeakMap,Se=new WeakMap,Ee=new WeakMap;function zt(c,p){var b=document.createElement("style");return Se.get(c).set(p,b),X.get(c).push(p),b}function oe(c,p){return Se.get(c).get(p)}function it(c,p){Se.get(c).delete(p),X.set(c,X.get(c).filter(function(b){return b!==p}))}function jo(c,p){requestAnimationFrame(function(){p.textContent=B.get(c).textContent,Ee.get(c).forEach(function(b){return p.sheet[b.method].apply(p.sheet,b.args)})})}function nt(c){if(!B.has(c))throw new TypeError("Illegal invocation")}function Ut(){var c=this,p=document.createElement("style");e.body.appendChild(p),B.set(c,p),X.set(c,[]),Se.set(c,new WeakMap),Ee.set(c,[])}var xe=Ut.prototype;xe.replace=function(p){try{return this.replaceSync(p),Promise.resolve(this)}catch(b){return Promise.reject(b)}},xe.replaceSync=function(p){if(nt(this),typeof p=="string"){var b=this;B.get(b).textContent=l(p),Ee.set(b,[]),X.get(b).forEach(function(L){L.isConnected()&&jo(b,oe(b,L))})}},n(xe,"cssRules",{configurable:!0,enumerable:!0,get:function(){return nt(this),B.get(this).sheet.cssRules}}),n(xe,"media",{configurable:!0,enumerable:!0,get:function(){return nt(this),B.get(this).sheet.media}}),v.forEach(function(c){xe[c]=function(){var p=this;nt(p);var b=arguments;Ee.get(p).push({method:c,args:b}),X.get(p).forEach(function(D){if(D.isConnected()){var I=oe(p,D).sheet;I[c].apply(I,b)}});var L=B.get(p).sheet;return L[c].apply(L,b)}}),n(Ut,Symbol.hasInstance,{configurable:!0,value:te});var Fo={childList:!0,subtree:!0},Bo=new WeakMap;function Ce(c){var p=Bo.get(c);return p||(p=new Wo(c),Bo.set(c,p)),p}function Ho(c){n(c.prototype,"adoptedStyleSheets",{configurable:!0,enumerable:!0,get:function(){return Ce(this).sheets},set:function(p){Ce(this).update(p)}})}function jt(c,p){for(var b=document.createNodeIterator(c,NodeFilter.SHOW_ELEMENT,function(D){return u(D)?NodeFilter.FILTER_ACCEPT:NodeFilter.FILTER_REJECT},null,!1),L=void 0;L=b.nextNode();)p(u(L))}var st=new WeakMap,ke=new WeakMap,rt=new WeakMap;function Sn(c,p){return p instanceof HTMLStyleElement&&ke.get(c).some(function(b){return oe(b,c)})}function qo(c){var p=st.get(c);return p instanceof Document?p.body:p}function Ft(c){var p=document.createDocumentFragment(),b=ke.get(c),L=rt.get(c),D=qo(c);L.disconnect(),b.forEach(function(I){p.appendChild(oe(I,c)||zt(I,c))}),D.insertBefore(p,null),L.observe(D,Fo),b.forEach(function(I){jo(I,oe(I,c))})}function Wo(c){var p=this;p.sheets=[],st.set(p,c),ke.set(p,[]),rt.set(p,new MutationObserver(function(b,L){if(!document){L.disconnect();return}b.forEach(function(D){o||s.call(D.addedNodes,function(I){I instanceof Element&&jt(I,function($e){Ce($e).connect()})}),s.call(D.removedNodes,function(I){I instanceof Element&&(Sn(p,I)&&Ft(p),o||jt(I,function($e){Ce($e).disconnect()}))})})}))}if(Wo.prototype={isConnected:function(){var c=st.get(this);return c instanceof Document?c.readyState!=="loading":a(c.host)},connect:function(){var c=qo(this);rt.get(this).observe(c,Fo),ke.get(this).length>0&&Ft(this),jt(c,function(p){Ce(p).connect()})},disconnect:function(){rt.get(this).disconnect()},update:function(c){var p=this,b=st.get(p)===document?"Document":"ShadowRoot";if(!Array.isArray(c))throw new TypeError("Failed to set the 'adoptedStyleSheets' property on "+b+": Iterator getter is not callable.");if(!c.every(te))throw new TypeError("Failed to set the 'adoptedStyleSheets' property on "+b+": Failed to convert value to 'CSSStyleSheet'");if(c.some(Dt))throw new TypeError("Failed to set the 'adoptedStyleSheets' property on "+b+": Can't adopt non-constructed stylesheets");p.sheets=c;var L=ke.get(p),D=d(c),I=h(L,D);I.forEach(function($e){m(oe($e,p)),it($e,p)}),ke.set(p,D),p.isConnected()&&D.length>0&&Ft(p)}},window.CSSStyleSheet=Ut,Ho(Document),"ShadowRoot"in window){Ho(ShadowRoot);var Go=Element.prototype,En=Go.attachShadow;Go.attachShadow=function(p){var b=En.call(this,p);return p.mode==="closed"&&t.set(this,b),b}}var at=Ce(document);at.isConnected()?at.connect():document.addEventListener("DOMContentLoaded",at.connect.bind(at))})();/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */const hn=Symbol.for(""),tr=o=>{if((o==null?void 0:o.r)===hn)return o==null?void 0:o._$litStatic$},or=o=>{if(o._$litStatic$!==void 0)return o._$litStatic$;throw new Error(`Value passed to 'literal' function must be a 'literal' result: ${o}. Use 'unsafeStatic' to pass non-literal values, but
            take care to ensure page security.`)},ct=(o,...e)=>({_$litStatic$:e.reduce((t,i,n)=>t+or(i)+o[n+1],o[0]),r:hn}),wi=new Map,ir=o=>(e,...t)=>{const i=t.length;let n,s;const r=[],l=[];let a=0,d=!1,h;for(;a<i;){for(h=e[a];a<i&&(s=t[a],(n=tr(s))!==void 0);)h+=n+e[++a],d=!0;a!==i&&l.push(s),r.push(h),a++}if(a===i&&r.push(e[i]),d){const m=r.join("$$lit$$");e=wi.get(m),e===void 0&&(r.raw=r,wi.set(m,e=r)),t=l}return o(e,...t)},nr=ir(f),sr="modulepreload",rr=function(o){return"/"+o},Si={},S=function(o,e,t){if(!e||e.length===0)return o();const i=document.getElementsByTagName("link");return Promise.all(e.map(n=>{if(n=rr(n),n in Si)return;Si[n]=!0;const s=n.endsWith(".css"),r=s?'[rel="stylesheet"]':"";if(t)for(let a=i.length-1;a>=0;a--){const d=i[a];if(d.href===n&&(!s||d.rel==="stylesheet"))return}else if(document.querySelector(`link[href="${n}"]${r}`))return;const l=document.createElement("link");if(l.rel=s?"stylesheet":sr,s||(l.as="script",l.crossOrigin=""),l.href=n,document.head.appendChild(l),s)return new Promise((a,d)=>{l.addEventListener("load",a),l.addEventListener("error",()=>d(new Error(`Unable to preload CSS for ${n}`)))})})).then(()=>o()).catch(n=>{const s=new Event("vite:preloadError",{cancelable:!0});if(s.payload=n,window.dispatchEvent(s),!s.defaultPrevented)throw n})};function ar(o){var e;const t=[];for(;o&&o.parentNode;){const i=lr(o);if(i.nodeId!==-1){if((e=i.element)!=null&&e.tagName.startsWith("FLOW-CONTAINER-"))break;t.push(i)}o=o.parentElement?o.parentElement:o.parentNode.host}return t.reverse()}function lr(o){const e=window.Vaadin;if(e&&e.Flow){const{clients:t}=e.Flow,i=Object.keys(t);for(const n of i){const s=t[n];if(s.getNodeId){const r=s.getNodeId(o);if(r>=0)return{nodeId:r,uiId:s.getUIId(),element:o}}}}return{nodeId:-1,uiId:-1,element:void 0}}function dr(o,e){if(o.contains(e))return!0;let t=e;const i=e.ownerDocument;for(;t&&t!==i&&t!==o;)t=t.parentNode||(t instanceof ShadowRoot?t.host:null);return t===o}const cr=(o,e)=>{const t=o[e];return t?typeof t=="function"?t():Promise.resolve(t):new Promise((i,n)=>{(typeof queueMicrotask=="function"?queueMicrotask:setTimeout)(n.bind(null,new Error("Unknown variable dynamic import: "+e)))})};var P=(o=>(o.text="text",o.checkbox="checkbox",o.range="range",o.color="color",o))(P||{});const J={lumoSize:["--lumo-size-xs","--lumo-size-s","--lumo-size-m","--lumo-size-l","--lumo-size-xl"],lumoSpace:["--lumo-space-xs","--lumo-space-s","--lumo-space-m","--lumo-space-l","--lumo-space-xl"],lumoBorderRadius:["0","--lumo-border-radius-m","--lumo-border-radius-l"],lumoFontSize:["--lumo-font-size-xxs","--lumo-font-size-xs","--lumo-font-size-s","--lumo-font-size-m","--lumo-font-size-l","--lumo-font-size-xl","--lumo-font-size-xxl","--lumo-font-size-xxxl"],lumoTextColor:["--lumo-header-text-color","--lumo-body-text-color","--lumo-secondary-text-color","--lumo-tertiary-text-color","--lumo-disabled-text-color","--lumo-primary-text-color","--lumo-error-text-color","--lumo-success-text-color"],basicBorderSize:["0px","1px","2px","3px"]},hr=Object.freeze(Object.defineProperty({__proto__:null,presets:J},Symbol.toStringTag,{value:"Module"})),je={textColor:{propertyName:"color",displayName:"Text color",editorType:P.color,presets:J.lumoTextColor},fontSize:{propertyName:"font-size",displayName:"Font size",editorType:P.range,presets:J.lumoFontSize,icon:"font"},fontWeight:{propertyName:"font-weight",displayName:"Bold",editorType:P.checkbox,checkedValue:"bold"},fontStyle:{propertyName:"font-style",displayName:"Italic",editorType:P.checkbox,checkedValue:"italic"}},Ne={backgroundColor:{propertyName:"background-color",displayName:"Background color",editorType:P.color},borderColor:{propertyName:"border-color",displayName:"Border color",editorType:P.color},borderWidth:{propertyName:"border-width",displayName:"Border width",editorType:P.range,presets:J.basicBorderSize,icon:"square"},borderRadius:{propertyName:"border-radius",displayName:"Border radius",editorType:P.range,presets:J.lumoBorderRadius,icon:"square"},padding:{propertyName:"padding",displayName:"Padding",editorType:P.range,presets:J.lumoSpace,icon:"square"},gap:{propertyName:"gap",displayName:"Spacing",editorType:P.range,presets:J.lumoSpace,icon:"square"}},ur={height:{propertyName:"height",displayName:"Size",editorType:P.range,presets:J.lumoSize,icon:"square"},paddingInline:{propertyName:"padding-inline",displayName:"Padding",editorType:P.range,presets:J.lumoSpace,icon:"square"}},pr={iconColor:{propertyName:"color",displayName:"Icon color",editorType:P.color,presets:J.lumoTextColor},iconSize:{propertyName:"font-size",displayName:"Icon size",editorType:P.range,presets:J.lumoFontSize,icon:"font"}},mr=Object.freeze(Object.defineProperty({__proto__:null,fieldProperties:ur,iconProperties:pr,shapeProperties:Ne,textProperties:je},Symbol.toStringTag,{value:"Module"}));function un(o){const e=o.charAt(0).toUpperCase()+o.slice(1);return{tagName:o,displayName:e,elements:[{selector:o,displayName:"Element",properties:[Ne.backgroundColor,Ne.borderColor,Ne.borderWidth,Ne.borderRadius,Ne.padding,je.textColor,je.fontSize,je.fontWeight,je.fontStyle]}]}}const vr=Object.freeze(Object.defineProperty({__proto__:null,createGenericMetadata:un},Symbol.toStringTag,{value:"Module"})),fr=o=>cr(Object.assign({"./components/defaults.ts":()=>S(()=>Promise.resolve().then(()=>mr),void 0),"./components/generic.ts":()=>S(()=>Promise.resolve().then(()=>vr),void 0),"./components/presets.ts":()=>S(()=>Promise.resolve().then(()=>hr),void 0),"./components/vaadin-app-layout.ts":()=>S(()=>E(()=>import("./vaadin-app-layout-37492a04-53d353bf.js"),[],import.meta.url),[]),"./components/vaadin-avatar.ts":()=>S(()=>E(()=>import("./vaadin-avatar-7047be31-f7e877ec.js"),[],import.meta.url),[]),"./components/vaadin-big-decimal-field.ts":()=>S(()=>E(()=>import("./vaadin-big-decimal-field-b42c1de1-6f148a44.js"),["./vaadin-big-decimal-field-b42c1de1-6f148a44.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-big-decimal-field-b42c1de1.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-button.ts":()=>S(()=>E(()=>import("./vaadin-button-79ad9d5f-e80bc853.js"),[],import.meta.url),[]),"./components/vaadin-checkbox-group.ts":()=>S(()=>E(()=>import("./vaadin-checkbox-group-a9a9e85d-ad8e7eff.js"),["./vaadin-checkbox-group-a9a9e85d-ad8e7eff.js","./vaadin-text-field-e82c445d-eef85f10.js","./vaadin-checkbox-13797fc9-469df855.js"],import.meta.url),["assets/vaadin-checkbox-group-a9a9e85d.js","assets/vaadin-text-field-e82c445d.js","assets/vaadin-checkbox-13797fc9.js"]),"./components/vaadin-checkbox.ts":()=>S(()=>E(()=>import("./vaadin-checkbox-13797fc9-469df855.js"),[],import.meta.url),[]),"./components/vaadin-combo-box.ts":()=>S(()=>E(()=>import("./vaadin-combo-box-9046f78f-745cb606.js"),["./vaadin-combo-box-9046f78f-745cb606.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-combo-box-9046f78f.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-email-field.ts":()=>S(()=>E(()=>import("./vaadin-email-field-da851bcb-3cad98fc.js"),["./vaadin-email-field-da851bcb-3cad98fc.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-email-field-da851bcb.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-horizontal-layout.ts":()=>S(()=>E(()=>import("./vaadin-horizontal-layout-f7b1ab51-44d7ac42.js"),[],import.meta.url),[]),"./components/vaadin-integer-field.ts":()=>S(()=>E(()=>import("./vaadin-integer-field-6e2954cf-c2c147f9.js"),["./vaadin-integer-field-6e2954cf-c2c147f9.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-integer-field-6e2954cf.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-menu-bar.ts":()=>S(()=>E(()=>import("./vaadin-menu-bar-be33385c-a653e994.js"),[],import.meta.url),[]),"./components/vaadin-number-field.ts":()=>S(()=>E(()=>import("./vaadin-number-field-31df11f5-ce3c44e5.js"),["./vaadin-number-field-31df11f5-ce3c44e5.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-number-field-31df11f5.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-password-field.ts":()=>S(()=>E(()=>import("./vaadin-password-field-49ffb113-77ead03b.js"),["./vaadin-password-field-49ffb113-77ead03b.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-password-field-49ffb113.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-progress-bar.ts":()=>S(()=>E(()=>import("./vaadin-progress-bar-3b53bb70-7a9db7c7.js"),[],import.meta.url),[]),"./components/vaadin-radio-group.ts":()=>S(()=>E(()=>import("./vaadin-radio-group-4a6e2cf4-2145d4ce.js"),["./vaadin-radio-group-4a6e2cf4-2145d4ce.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-radio-group-4a6e2cf4.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-scroller.ts":()=>S(()=>E(()=>import("./vaadin-scroller-35e68818-46131978.js"),[],import.meta.url),[]),"./components/vaadin-select.ts":()=>S(()=>E(()=>import("./vaadin-select-5d6ab45b-8e419c9a.js"),["./vaadin-select-5d6ab45b-8e419c9a.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-select-5d6ab45b.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-split-layout.ts":()=>S(()=>E(()=>import("./vaadin-split-layout-10c9713b-dd3517a7.js"),[],import.meta.url),[]),"./components/vaadin-text-area.ts":()=>S(()=>E(()=>import("./vaadin-text-area-41c5f60c-7c0bde8a.js"),["./vaadin-text-area-41c5f60c-7c0bde8a.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-text-area-41c5f60c.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-text-field.ts":()=>S(()=>E(()=>import("./vaadin-text-field-e82c445d-eef85f10.js"),[],import.meta.url),[]),"./components/vaadin-time-picker.ts":()=>S(()=>E(()=>import("./vaadin-time-picker-2fa5314f-68c3545c.js"),["./vaadin-time-picker-2fa5314f-68c3545c.js","./vaadin-text-field-e82c445d-eef85f10.js"],import.meta.url),["assets/vaadin-time-picker-2fa5314f.js","assets/vaadin-text-field-e82c445d.js"]),"./components/vaadin-vertical-layout.ts":()=>S(()=>E(()=>import("./vaadin-vertical-layout-ff73c403-ac8a9761.js"),[],import.meta.url),[]),"./components/vaadin-virtual-list.ts":()=>S(()=>E(()=>import("./vaadin-virtual-list-62d4499a-8d26df8e.js"),[],import.meta.url),[])}),`./components/${o}.ts`);class gr{constructor(e=fr){this.loader=e,this.metadata={}}async getMetadata(e){var t;const i=(t=e.element)==null?void 0:t.localName;if(!i)return null;if(!i.startsWith("vaadin-"))return un(i);let n=this.metadata[i];if(n)return n;try{n=(await this.loader(i)).default,this.metadata[i]=n}catch{console.warn(`Failed to load metadata for component: ${i}`)}return n||null}}const yr=new gr,yt={crosshair:Te`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M4 8v-2a2 2 0 0 1 2 -2h2"></path>
   <path d="M4 16v2a2 2 0 0 0 2 2h2"></path>
   <path d="M16 4h2a2 2 0 0 1 2 2v2"></path>
   <path d="M16 20h2a2 2 0 0 0 2 -2v-2"></path>
   <path d="M9 12l6 0"></path>
   <path d="M12 9l0 6"></path>
</svg>`,square:Te`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="currentColor" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M3 3m0 2a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v14a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2z"></path>
</svg>`,font:Te`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M4 20l3 0"></path>
   <path d="M14 20l7 0"></path>
   <path d="M6.9 15l6.9 0"></path>
   <path d="M10.2 6.3l5.8 13.7"></path>
   <path d="M5 20l6 -16l2 0l7 16"></path>
</svg>`,undo:Te`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M9 13l-4 -4l4 -4m-4 4h11a4 4 0 0 1 0 8h-1"></path>
</svg>`,redo:Te`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M15 13l4 -4l-4 -4m4 4h-11a4 4 0 0 0 0 8h1"></path>
</svg>`,cross:Te`<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" stroke-width="3" stroke="currentColor" fill="none" stroke-linecap="round" stroke-linejoin="round">
   <path stroke="none" d="M0 0h24v24H0z" fill="none"></path>
   <path d="M18 6l-12 12"></path>
   <path d="M6 6l12 12"></path>
</svg>`};var Ye=(o=>(o.disabled="disabled",o.enabled="enabled",o.missing_theme="missing_theme",o))(Ye||{}),z=(o=>(o.local="local",o.global="global",o))(z||{});function uo(o,e){return`${o}|${e}`}class ce{constructor(e){this._properties={},this._metadata=e}get metadata(){return this._metadata}get properties(){return Object.values(this._properties)}getPropertyValue(e,t){return this._properties[uo(e,t)]||null}updatePropertyValue(e,t,i,n){if(!i){delete this._properties[uo(e,t)];return}let s=this.getPropertyValue(e,t);s?(s.value=i,s.modified=n||!1):(s={elementSelector:e,propertyName:t,value:i,modified:n||!1},this._properties[uo(e,t)]=s)}addPropertyValues(e){e.forEach(t=>{this.updatePropertyValue(t.elementSelector,t.propertyName,t.value,t.modified)})}getPropertyValuesForElement(e){return this.properties.filter(t=>t.elementSelector===e)}static combine(...e){if(e.length<2)throw new Error("Must provide at least two themes");const t=new ce(e[0].metadata);return e.forEach(i=>t.addPropertyValues(i.properties)),t}static fromServerRules(e,t,i){const n=new ce(e);return e.elements.forEach(s=>{const r=Le(s,t),l=i.find(a=>a.selector===r);l&&s.properties.forEach(a=>{const d=l.properties[a.propertyName];d&&n.updatePropertyValue(s.selector,a.propertyName,d,!0)})}),n}}function Le(o,e){const t=o.selector;if(e.themeScope==="global")return t;if(!e.localClassName)throw new Error("Can not build local scoped selector without instance class name");const i=t.match(/^[\w\d-_]+/),n=i&&i[0];if(!n)throw new Error(`Selector does not start with a tag name: ${t}`);return`${n}.${e.localClassName}${t.substring(n.length,t.length)}`}function br(o,e,t,i){const n=Le(o,e),s={[t]:i};return t==="border-width"&&(parseInt(i)>0?s["border-style"]="solid":s["border-style"]=""),{selector:n,properties:s}}function _r(o){const e=Object.entries(o.properties).map(([t,i])=>`${t}: ${i};`).join(" ");return`${o.selector} { ${e} }`}let ht,Ei="";function Oo(o){ht||(ht=new CSSStyleSheet,document.adoptedStyleSheets=[...document.adoptedStyleSheets,ht]),Ei+=o.cssText,ht.replaceSync(Ei)}const pn=C`
  .editor-row {
    display: flex;
    align-items: baseline;
    padding: var(--theme-editor-section-horizontal-padding);
    gap: 10px;
  }

  .editor-row > .label {
    flex: 0 0 auto;
    width: 120px;
  }

  .editor-row > .editor {
    flex: 1 1 0;
  }
`,xi="__vaadin-theme-editor-measure-element",Ci=/((::before)|(::after))$/,ki=/::part\(([\w\d_-]+)\)$/;Oo(C`
  .__vaadin-theme-editor-measure-element {
    position: absolute;
    top: 0;
    left: 0;
    visibility: hidden;
  }
`);async function wr(o){const e=new ce(o),t=document.createElement(o.tagName);t.classList.add(xi),document.body.append(t),o.setupElement&&await o.setupElement(t);const i={themeScope:z.local,localClassName:xi};try{o.elements.forEach(n=>{$i(t,n,i,!0);let s=Le(n,i);const r=s.match(Ci);s=s.replace(Ci,"");const l=s.match(ki),a=s.replace(ki,"");let d=document.querySelector(a);if(d&&l){const u=`[part~="${l[1]}"]`;d=d.shadowRoot.querySelector(u)}if(!d)return;d.style.transition="none";const h=r?r[1]:null,m=getComputedStyle(d,h);n.properties.forEach(u=>{const v=m.getPropertyValue(u.propertyName)||u.defaultValue||"";e.updatePropertyValue(n.selector,u.propertyName,v)}),$i(t,n,i,!1)})}finally{try{o.cleanupElement&&await o.cleanupElement(t)}finally{t.remove()}}return e}function $i(o,e,t,i){if(e.stateAttribute){if(e.stateElementSelector){const n=Le({...e,selector:e.stateElementSelector},t);o=document.querySelector(n)}o&&(i?o.setAttribute(e.stateAttribute,""):o.removeAttribute(e.stateAttribute))}}function Ti(o){return o.trim()}function Sr(o){const e=o.element;if(!e)return null;const t=e.querySelector("label");if(t&&t.textContent)return Ti(t.textContent);const i=e.textContent;return i?Ti(i):null}class Er{constructor(){this._localClassNameMap=new Map}get stylesheet(){return this.ensureStylesheet(),this._stylesheet}add(e){this.ensureStylesheet(),this._stylesheet.replaceSync(e)}clear(){this.ensureStylesheet(),this._stylesheet.replaceSync("")}previewLocalClassName(e,t){if(!e)return;const i=this._localClassNameMap.get(e);i&&(e.classList.remove(i),e.overlayClass=null),t?(e.classList.add(t),e.overlayClass=t,this._localClassNameMap.set(e,t)):this._localClassNameMap.delete(e)}ensureStylesheet(){this._stylesheet||(this._stylesheet=new CSSStyleSheet,this._stylesheet.replaceSync(""),document.adoptedStyleSheets=[...document.adoptedStyleSheets,this._stylesheet])}}const ve=new Er;class xr{constructor(e){this.pendingRequests={},this.requestCounter=0,this.globalUiId=this.getGlobalUiId(),this.wrappedConnection=e;const t=this.wrappedConnection.onMessage;this.wrappedConnection.onMessage=i=>{i.command==="themeEditorResponse"?this.handleResponse(i.data):t.call(this.wrappedConnection,i)}}sendRequest(e,t){const i=(this.requestCounter++).toString(),n=t.uiId??this.globalUiId;return new Promise((s,r)=>{this.wrappedConnection.send(e,{...t,requestId:i,uiId:n}),this.pendingRequests[i]={resolve:s,reject:r}})}handleResponse(e){const t=this.pendingRequests[e.requestId];if(!t){console.warn("Received response for unknown request");return}delete this.pendingRequests[e.requestId],e.code==="ok"?t.resolve(e):t.reject(e)}loadComponentMetadata(e){return this.sendRequest("themeEditorComponentMetadata",{nodeId:e.nodeId})}setLocalClassName(e,t){return this.sendRequest("themeEditorLocalClassName",{nodeId:e.nodeId,className:t})}setCssRules(e){return this.sendRequest("themeEditorRules",{rules:e})}loadRules(e){return this.sendRequest("themeEditorLoadRules",{selectors:e})}markAsUsed(){return this.sendRequest("themeEditorMarkAsUsed",{})}undo(e){return this.sendRequest("themeEditorHistory",{undo:e})}redo(e){return this.sendRequest("themeEditorHistory",{redo:e})}openCss(e){return this.sendRequest("themeEditorOpenCss",{selector:e})}getGlobalUiId(){const e=window.Vaadin;if(e&&e.Flow){const{clients:t}=e.Flow,i=Object.keys(t);for(const n of i){const s=t[n];if(s.getNodeId)return s.getUIId()}}return-1}}const O={index:-1,entries:[]};class Cr{constructor(e){this.api=e}get allowUndo(){return O.index>=0}get allowRedo(){return O.index<O.entries.length-1}get allowedActions(){return{allowUndo:this.allowUndo,allowRedo:this.allowRedo}}push(e,t,i){const n={requestId:e,execute:t,rollback:i};if(O.index++,O.entries=O.entries.slice(0,O.index),O.entries.push(n),t)try{t()}catch(s){console.error("Execute history entry failed",s)}return this.allowedActions}async undo(){if(!this.allowUndo)return this.allowedActions;const e=O.entries[O.index];O.index--;try{await this.api.undo(e.requestId),e.rollback&&e.rollback()}catch(t){console.error("Undo failed",t)}return this.allowedActions}async redo(){if(!this.allowRedo)return this.allowedActions;O.index++;const e=O.entries[O.index];try{await this.api.redo(e.requestId),e.execute&&e.execute()}catch(t){console.error("Redo failed",t)}return this.allowedActions}static clear(){O.entries=[],O.index=-1}}var kr=Object.defineProperty,$r=Object.getOwnPropertyDescriptor,ue=(o,e,t,i)=>{for(var n=i>1?void 0:i?$r(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&kr(e,t,n),n};class Tr extends CustomEvent{constructor(e,t,i){super("theme-property-value-change",{bubbles:!0,composed:!0,detail:{element:e,property:t,value:i}})}}class q extends A{constructor(){super(...arguments),this.value=""}static get styles(){return[pn,C`
        :host {
          display: block;
        }

        .editor-row .label .modified {
          display: inline-block;
          width: 6px;
          height: 6px;
          background: orange;
          border-radius: 3px;
          margin-left: 3px;
        }
      `]}update(e){super.update(e),(e.has("propertyMetadata")||e.has("theme"))&&this.updateValueFromTheme()}render(){var e;return f`
      <div class="editor-row">
        <div class="label">
          ${this.propertyMetadata.displayName}
          ${(e=this.propertyValue)!=null&&e.modified?f`<span class="modified"></span>`:null}
        </div>
        <div class="editor">${this.renderEditor()}</div>
      </div>
    `}updateValueFromTheme(){var e;this.propertyValue=this.theme.getPropertyValue(this.elementMetadata.selector,this.propertyMetadata.propertyName),this.value=((e=this.propertyValue)==null?void 0:e.value)||""}dispatchChange(e){this.dispatchEvent(new Tr(this.elementMetadata,this.propertyMetadata,e))}}ue([y({})],q.prototype,"elementMetadata",2);ue([y({})],q.prototype,"propertyMetadata",2);ue([y({})],q.prototype,"theme",2);ue([T()],q.prototype,"propertyValue",2);ue([T()],q.prototype,"value",2);class kt{constructor(e){if(this._values=[],this._rawValues={},e){const t=e.propertyName,i=e.presets??[];this._values=(i||[]).map(s=>s.startsWith("--")?`var(${s})`:s);const n=document.createElement("div");n.style.borderStyle="solid",n.style.visibility="hidden",document.body.append(n);try{this._values.forEach(s=>{n.style.setProperty(t,s);const r=getComputedStyle(n);this._rawValues[s]=r.getPropertyValue(t).trim()})}finally{n.remove()}}}get values(){return this._values}get rawValues(){return this._rawValues}tryMapToRawValue(e){return this._rawValues[e]??e}tryMapToPreset(e){return this.findPreset(e)??e}findPreset(e){const t=e&&e.trim();return this.values.find(i=>this._rawValues[i]===t)}}class Ni extends CustomEvent{constructor(e){super("change",{detail:{value:e}})}}let $t=class extends A{constructor(){super(...arguments),this.value="",this.showClearButton=!1}static get styles(){return C`
      :host {
        display: inline-block;
        width: 100%;
        position: relative;
      }

      input {
        width: 100%;
        box-sizing: border-box;
        padding: 0.25rem 0.375rem;
        color: inherit;
        background: rgba(0, 0, 0, 0.2);
        border-radius: 0.25rem;
        border: none;
      }

      button {
        display: none;
        position: absolute;
        right: 4px;
        top: 4px;
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      button svg {
        width: 16px;
        height: 16px;
      }

      button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      :host(.show-clear-button) input {
        padding-right: 20px;
      }

      :host(.show-clear-button) button {
        display: block;
      }
    `}update(o){super.update(o),o.has("showClearButton")&&(this.showClearButton?this.classList.add("show-clear-button"):this.classList.remove("show-clear-button"))}render(){return f`
      <input class="input" .value=${this.value} @change=${this.handleInputChange} />
      <button @click=${this.handleClearClick}>${yt.cross}</button>
    `}handleInputChange(o){const e=o.target;this.dispatchEvent(new Ni(e.value))}handleClearClick(){this.dispatchEvent(new Ni(""))}};ue([y({})],$t.prototype,"value",2);ue([y({})],$t.prototype,"showClearButton",2);$t=ue([F("vaadin-dev-tools-theme-text-input")],$t);var Nr=Object.defineProperty,Pr=Object.getOwnPropertyDescriptor,Mt=(o,e,t,i)=>{for(var n=i>1?void 0:i?Pr(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Nr(e,t,n),n};class Ar extends CustomEvent{constructor(e){super("class-name-change",{detail:{value:e}})}}let Je=class extends A{constructor(){super(...arguments),this.editedClassName="",this.invalid=!1}static get styles(){return[pn,C`
        .editor-row {
          padding-top: 0;
        }

        .editor-row .editor .error {
          display: inline-block;
          color: var(--dev-tools-red-color);
          margin-top: 4px;
        }
      `]}update(o){super.update(o),o.has("className")&&(this.editedClassName=this.className,this.invalid=!1)}render(){return f` <div class="editor-row local-class-name">
      <div class="label">CSS class name</div>
      <div class="editor">
        <vaadin-dev-tools-theme-text-input
          type="text"
          .value=${this.editedClassName}
          @change=${this.handleInputChange}
        ></vaadin-dev-tools-theme-text-input>
        ${this.invalid?f`<br /><span class="error">Please enter a valid CSS class name</span>`:null}
      </div>
    </div>`}handleInputChange(o){this.editedClassName=o.detail.value;const e=/^-?[_a-zA-Z]+[_a-zA-Z0-9-]*$/;this.invalid=!this.editedClassName.match(e),!this.invalid&&this.editedClassName!==this.className&&this.dispatchEvent(new Ar(this.editedClassName))}};Mt([y({})],Je.prototype,"className",2);Mt([T()],Je.prototype,"editedClassName",2);Mt([T()],Je.prototype,"invalid",2);Je=Mt([F("vaadin-dev-tools-theme-class-name-editor")],Je);var Rr=Object.defineProperty,Ir=Object.getOwnPropertyDescriptor,Vt=(o,e,t,i)=>{for(var n=i>1?void 0:i?Ir(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Rr(e,t,n),n};class Or extends CustomEvent{constructor(e){super("scope-change",{detail:{value:e}})}}Oo(C`
  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] {
    --lumo-primary-color-50pct: rgba(255, 255, 255, 0.5);
    z-index: 100000 !important;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector']::part(overlay) {
    background: #333;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item {
    color: rgba(255, 255, 255, 0.8);
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(content) {
    font-size: 13px;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item .title {
    color: rgba(255, 255, 255, 0.95);
    font-weight: bold;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(checkmark) {
    margin: 6px;
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item::part(checkmark)::before {
    color: rgba(255, 255, 255, 0.95);
  }

  vaadin-select-overlay[theme~='vaadin-dev-tools-theme-scope-selector'] vaadin-item:hover {
    background: rgba(255, 255, 255, 0.1);
  }
`);let Xe=class extends A{constructor(){super(...arguments),this.value=z.local}static get styles(){return C`
      vaadin-select {
        --lumo-primary-color-50pct: rgba(255, 255, 255, 0.5);
        width: 100px;
      }

      vaadin-select::part(input-field) {
        background: rgba(0, 0, 0, 0.2);
      }

      vaadin-select vaadin-select-value-button,
      vaadin-select::part(toggle-button) {
        color: var(--dev-tools-text-color);
      }

      vaadin-select:hover vaadin-select-value-button,
      vaadin-select:hover::part(toggle-button) {
        color: var(--dev-tools-text-color-emphasis);
      }

      vaadin-select vaadin-select-item {
        font-size: 13px;
      }
    `}update(o){var e;super.update(o),o.has("metadata")&&((e=this.select)==null||e.requestContentUpdate())}render(){return f` <vaadin-select
      theme="small vaadin-dev-tools-theme-scope-selector"
      .value=${this.value}
      .renderer=${this.selectRenderer.bind(this)}
      @value-changed=${this.handleValueChange}
    ></vaadin-select>`}selectRenderer(o){var e;const t=((e=this.metadata)==null?void 0:e.displayName)||"Component",i=`${t}s`;Ae(f`
        <vaadin-list-box>
          <vaadin-item value=${z.local} label="Local">
            <span class="title">Local</span>
            <br />
            <span>Edit styles for this ${t}</span>
          </vaadin-item>
          <vaadin-item value=${z.global} label="Global">
            <span class="title">Global</span>
            <br />
            <span>Edit styles for all ${i}</span>
          </vaadin-item>
        </vaadin-list-box>
      `,o)}handleValueChange(o){const e=o.detail.value;e!==this.value&&this.dispatchEvent(new Or(e))}};Vt([y({})],Xe.prototype,"value",2);Vt([y({})],Xe.prototype,"metadata",2);Vt([tt("vaadin-select")],Xe.prototype,"select",2);Xe=Vt([F("vaadin-dev-tools-theme-scope-selector")],Xe);var Lr=Object.defineProperty,Mr=Object.getOwnPropertyDescriptor,Vr=(o,e,t,i)=>{for(var n=i>1?void 0:i?Mr(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Lr(e,t,n),n};let Pi=class extends q{static get styles(){return[q.styles,C`
        .editor-row {
          align-items: center;
        }
      `]}handleInputChange(o){const e=o.target.checked?this.propertyMetadata.checkedValue:"";this.dispatchChange(e||"")}renderEditor(){const o=this.value===this.propertyMetadata.checkedValue;return f` <input type="checkbox" .checked=${o} @change=${this.handleInputChange} /> `}};Pi=Vr([F("vaadin-dev-tools-theme-checkbox-property-editor")],Pi);var Dr=Object.defineProperty,zr=Object.getOwnPropertyDescriptor,Ur=(o,e,t,i)=>{for(var n=i>1?void 0:i?zr(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Dr(e,t,n),n};let Ai=class extends q{handleInputChange(o){this.dispatchChange(o.detail.value)}renderEditor(){var o;return f`
      <vaadin-dev-tools-theme-text-input
        .value=${this.value}
        .showClearButton=${((o=this.propertyValue)==null?void 0:o.modified)||!1}
        @change=${this.handleInputChange}
      ></vaadin-dev-tools-theme-text-input>
    `}};Ai=Ur([F("vaadin-dev-tools-theme-text-property-editor")],Ai);var jr=Object.defineProperty,Fr=Object.getOwnPropertyDescriptor,Lo=(o,e,t,i)=>{for(var n=i>1?void 0:i?Fr(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&jr(e,t,n),n};let Tt=class extends q{constructor(){super(...arguments),this.selectedPresetIndex=-1,this.presets=new kt}static get styles(){return[q.styles,C`
        :host {
          --preset-count: 3;
          --slider-bg: #fff;
          --slider-border: #333;
        }

        .editor-row {
          align-items: center;
        }

        .editor-row > .editor {
          display: flex;
          align-items: center;
          gap: 1rem;
        }

        .editor-row .input {
          flex: 0 0 auto;
          width: 80px;
        }

        .slider-wrapper {
          flex: 1 1 0;
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }

        .icon {
          width: 20px;
          height: 20px;
          color: #aaa;
        }

        .icon.prefix > svg {
          transform: scale(0.75);
        }

        .slider {
          flex: 1 1 0;
          -webkit-appearance: none;
          background: linear-gradient(to right, #666, #666 2px, transparent 2px);
          background-size: calc((100% - 13px) / (var(--preset-count) - 1)) 8px;
          background-position: 5px 50%;
          background-repeat: repeat-x;
        }

        .slider::-webkit-slider-runnable-track {
          width: 100%;
          box-sizing: border-box;
          height: 16px;
          background-image: linear-gradient(#666, #666);
          background-size: calc(100% - 12px) 2px;
          background-repeat: no-repeat;
          background-position: 6px 50%;
        }

        .slider::-moz-range-track {
          width: 100%;
          box-sizing: border-box;
          height: 16px;
          background-image: linear-gradient(#666, #666);
          background-size: calc(100% - 12px) 2px;
          background-repeat: no-repeat;
          background-position: 6px 50%;
        }

        .slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          height: 16px;
          width: 16px;
          border: 2px solid var(--slider-border);
          border-radius: 50%;
          background: var(--slider-bg);
          cursor: pointer;
        }

        .slider::-moz-range-thumb {
          height: 16px;
          width: 16px;
          border: 2px solid var(--slider-border);
          border-radius: 50%;
          background: var(--slider-bg);
          cursor: pointer;
        }

        .custom-value {
          opacity: 0.5;
        }

        .custom-value:hover,
        .custom-value:focus-within {
          opacity: 1;
        }

        .custom-value:not(:hover, :focus-within) {
          --slider-bg: #333;
          --slider-border: #666;
        }
      `]}update(o){o.has("propertyMetadata")&&(this.presets=new kt(this.propertyMetadata)),super.update(o)}renderEditor(){var o;const e={"slider-wrapper":!0,"custom-value":this.selectedPresetIndex<0},t=this.presets.values.length;return f`
      <div class=${Io(e)}>
        ${null}
        <input
          type="range"
          class="slider"
          style="--preset-count: ${t}"
          step="1"
          min="0"
          .max=${(t-1).toString()}
          .value=${this.selectedPresetIndex}
          @input=${this.handleSliderInput}
          @change=${this.handleSliderChange}
        />
        ${null}
      </div>
      <vaadin-dev-tools-theme-text-input
        class="input"
        .value=${this.value}
        .showClearButton=${((o=this.propertyValue)==null?void 0:o.modified)||!1}
        @change=${this.handleValueChange}
      ></vaadin-dev-tools-theme-text-input>
    `}handleSliderInput(o){const e=o.target,t=parseInt(e.value),i=this.presets.values[t];this.selectedPresetIndex=t,this.value=this.presets.rawValues[i]}handleSliderChange(){this.dispatchChange(this.value)}handleValueChange(o){this.value=o.detail.value,this.updateSliderValue(),this.dispatchChange(this.value)}dispatchChange(o){const e=this.presets.tryMapToPreset(o);super.dispatchChange(e)}updateValueFromTheme(){var o;super.updateValueFromTheme(),this.value=this.presets.tryMapToRawValue(((o=this.propertyValue)==null?void 0:o.value)||""),this.updateSliderValue()}updateSliderValue(){const o=this.presets.findPreset(this.value);this.selectedPresetIndex=o?this.presets.values.indexOf(o):-1}};Lo([T()],Tt.prototype,"selectedPresetIndex",2);Lo([T()],Tt.prototype,"presets",2);Tt=Lo([F("vaadin-dev-tools-theme-range-property-editor")],Tt);const Me=(o,e=0,t=1)=>o>t?t:o<e?e:o,V=(o,e=0,t=Math.pow(10,e))=>Math.round(t*o)/t,mn=({h:o,s:e,v:t,a:i})=>{const n=(200-e)*t/100;return{h:V(o),s:V(n>0&&n<200?e*t/100/(n<=100?n:200-n)*100:0),l:V(n/2),a:V(i,2)}},Co=o=>{const{h:e,s:t,l:i}=mn(o);return`hsl(${e}, ${t}%, ${i}%)`},po=o=>{const{h:e,s:t,l:i,a:n}=mn(o);return`hsla(${e}, ${t}%, ${i}%, ${n})`},Br=({h:o,s:e,v:t,a:i})=>{o=o/360*6,e=e/100,t=t/100;const n=Math.floor(o),s=t*(1-e),r=t*(1-(o-n)*e),l=t*(1-(1-o+n)*e),a=n%6;return{r:V([t,r,s,s,l,t][a]*255),g:V([l,t,t,r,s,s][a]*255),b:V([s,s,l,t,t,r][a]*255),a:V(i,2)}},Hr=o=>{const{r:e,g:t,b:i,a:n}=Br(o);return`rgba(${e}, ${t}, ${i}, ${n})`},qr=o=>{const e=/rgba?\(?\s*(-?\d*\.?\d+)(%)?[,\s]+(-?\d*\.?\d+)(%)?[,\s]+(-?\d*\.?\d+)(%)?,?\s*[/\s]*(-?\d*\.?\d+)?(%)?\s*\)?/i.exec(o);return e?Wr({r:Number(e[1])/(e[2]?100/255:1),g:Number(e[3])/(e[4]?100/255:1),b:Number(e[5])/(e[6]?100/255:1),a:e[7]===void 0?1:Number(e[7])/(e[8]?100:1)}):{h:0,s:0,v:0,a:1}},Wr=({r:o,g:e,b:t,a:i})=>{const n=Math.max(o,e,t),s=n-Math.min(o,e,t),r=s?n===o?(e-t)/s:n===e?2+(t-o)/s:4+(o-e)/s:0;return{h:V(60*(r<0?r+6:r)),s:V(n?s/n*100:0),v:V(n/255*100),a:i}},Gr=(o,e)=>{if(o===e)return!0;for(const t in o)if(o[t]!==e[t])return!1;return!0},Kr=(o,e)=>o.replace(/\s/g,"")===e.replace(/\s/g,""),Ri={},vn=o=>{let e=Ri[o];return e||(e=document.createElement("template"),e.innerHTML=o,Ri[o]=e),e},Mo=(o,e,t)=>{o.dispatchEvent(new CustomEvent(e,{bubbles:!0,detail:t}))};let Re=!1;const ko=o=>"touches"in o,Yr=o=>Re&&!ko(o)?!1:(Re||(Re=ko(o)),!0),Ii=(o,e)=>{const t=ko(e)?e.touches[0]:e,i=o.el.getBoundingClientRect();Mo(o.el,"move",o.getMove({x:Me((t.pageX-(i.left+window.pageXOffset))/i.width),y:Me((t.pageY-(i.top+window.pageYOffset))/i.height)}))},Jr=(o,e)=>{const t=e.keyCode;t>40||o.xy&&t<37||t<33||(e.preventDefault(),Mo(o.el,"move",o.getMove({x:t===39?.01:t===37?-.01:t===34?.05:t===33?-.05:t===35?1:t===36?-1:0,y:t===40?.01:t===38?-.01:0},!0)))};class Vo{constructor(e,t,i,n){const s=vn(`<div role="slider" tabindex="0" part="${t}" ${i}><div part="${t}-pointer"></div></div>`);e.appendChild(s.content.cloneNode(!0));const r=e.querySelector(`[part=${t}]`);r.addEventListener("mousedown",this),r.addEventListener("touchstart",this),r.addEventListener("keydown",this),this.el=r,this.xy=n,this.nodes=[r.firstChild,r]}set dragging(e){const t=e?document.addEventListener:document.removeEventListener;t(Re?"touchmove":"mousemove",this),t(Re?"touchend":"mouseup",this)}handleEvent(e){switch(e.type){case"mousedown":case"touchstart":if(e.preventDefault(),!Yr(e)||!Re&&e.button!=0)return;this.el.focus(),Ii(this,e),this.dragging=!0;break;case"mousemove":case"touchmove":e.preventDefault(),Ii(this,e);break;case"mouseup":case"touchend":this.dragging=!1;break;case"keydown":Jr(this,e);break}}style(e){e.forEach((t,i)=>{for(const n in t)this.nodes[i].style.setProperty(n,t[n])})}}class Xr extends Vo{constructor(e){super(e,"hue",'aria-label="Hue" aria-valuemin="0" aria-valuemax="360"',!1)}update({h:e}){this.h=e,this.style([{left:`${e/360*100}%`,color:Co({h:e,s:100,v:100,a:1})}]),this.el.setAttribute("aria-valuenow",`${V(e)}`)}getMove(e,t){return{h:t?Me(this.h+e.x*360,0,360):360*e.x}}}class Qr extends Vo{constructor(e){super(e,"saturation",'aria-label="Color"',!0)}update(e){this.hsva=e,this.style([{top:`${100-e.v}%`,left:`${e.s}%`,color:Co(e)},{"background-color":Co({h:e.h,s:100,v:100,a:1})}]),this.el.setAttribute("aria-valuetext",`Saturation ${V(e.s)}%, Brightness ${V(e.v)}%`)}getMove(e,t){return{s:t?Me(this.hsva.s+e.x*100,0,100):e.x*100,v:t?Me(this.hsva.v-e.y*100,0,100):Math.round(100-e.y*100)}}}const Zr=':host{display:flex;flex-direction:column;position:relative;width:200px;height:200px;user-select:none;-webkit-user-select:none;cursor:default}:host([hidden]){display:none!important}[role=slider]{position:relative;touch-action:none;user-select:none;-webkit-user-select:none;outline:0}[role=slider]:last-child{border-radius:0 0 8px 8px}[part$=pointer]{position:absolute;z-index:1;box-sizing:border-box;width:28px;height:28px;display:flex;place-content:center center;transform:translate(-50%,-50%);background-color:#fff;border:2px solid #fff;border-radius:50%;box-shadow:0 2px 4px rgba(0,0,0,.2)}[part$=pointer]::after{content:"";width:100%;height:100%;border-radius:inherit;background-color:currentColor}[role=slider]:focus [part$=pointer]{transform:translate(-50%,-50%) scale(1.1)}',ea="[part=hue]{flex:0 0 24px;background:linear-gradient(to right,red 0,#ff0 17%,#0f0 33%,#0ff 50%,#00f 67%,#f0f 83%,red 100%)}[part=hue-pointer]{top:50%;z-index:2}",ta="[part=saturation]{flex-grow:1;border-color:transparent;border-bottom:12px solid #000;border-radius:8px 8px 0 0;background-image:linear-gradient(to top,#000,transparent),linear-gradient(to right,#fff,rgba(255,255,255,0));box-shadow:inset 0 0 0 1px rgba(0,0,0,.05)}[part=saturation-pointer]{z-index:3}",ut=Symbol("same"),mo=Symbol("color"),Oi=Symbol("hsva"),vo=Symbol("update"),Li=Symbol("parts"),Nt=Symbol("css"),Pt=Symbol("sliders");let oa=class extends HTMLElement{static get observedAttributes(){return["color"]}get[Nt](){return[Zr,ea,ta]}get[Pt](){return[Qr,Xr]}get color(){return this[mo]}set color(o){if(!this[ut](o)){const e=this.colorModel.toHsva(o);this[vo](e),this[mo]=o}}constructor(){super();const o=vn(`<style>${this[Nt].join("")}</style>`),e=this.attachShadow({mode:"open"});e.appendChild(o.content.cloneNode(!0)),e.addEventListener("move",this),this[Li]=this[Pt].map(t=>new t(e))}connectedCallback(){if(this.hasOwnProperty("color")){const o=this.color;delete this.color,this.color=o}else this.color||(this.color=this.colorModel.defaultColor)}attributeChangedCallback(o,e,t){const i=this.colorModel.fromAttr(t);this[ut](i)||(this.color=i)}handleEvent(o){const e=this[Oi],t={...e,...o.detail};this[vo](t);let i;!Gr(t,e)&&!this[ut](i=this.colorModel.fromHsva(t))&&(this[mo]=i,Mo(this,"color-changed",{value:i}))}[ut](o){return this.color&&this.colorModel.equal(o,this.color)}[vo](o){this[Oi]=o,this[Li].forEach(e=>e.update(o))}};class ia extends Vo{constructor(e){super(e,"alpha",'aria-label="Alpha" aria-valuemin="0" aria-valuemax="1"',!1)}update(e){this.hsva=e;const t=po({...e,a:0}),i=po({...e,a:1}),n=e.a*100;this.style([{left:`${n}%`,color:po(e)},{"--gradient":`linear-gradient(90deg, ${t}, ${i}`}]);const s=V(n);this.el.setAttribute("aria-valuenow",`${s}`),this.el.setAttribute("aria-valuetext",`${s}%`)}getMove(e,t){return{a:t?Me(this.hsva.a+e.x):e.x}}}const na=`[part=alpha]{flex:0 0 24px}[part=alpha]::after{display:block;content:"";position:absolute;top:0;left:0;right:0;bottom:0;border-radius:inherit;background-image:var(--gradient);box-shadow:inset 0 0 0 1px rgba(0,0,0,.05)}[part^=alpha]{background-color:#fff;background-image:url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill-opacity=".05"><rect x="8" width="8" height="8"/><rect y="8" width="8" height="8"/></svg>')}[part=alpha-pointer]{top:50%}`;class sa extends oa{get[Nt](){return[...super[Nt],na]}get[Pt](){return[...super[Pt],ia]}}const ra={defaultColor:"rgba(0, 0, 0, 1)",toHsva:qr,fromHsva:Hr,equal:Kr,fromAttr:o=>o};class aa extends sa{get colorModel(){return ra}}/**
* @license
* Copyright (c) 2017 - 2023 Vaadin Ltd.
* This program is available under Apache License Version 2.0, available at https://vaadin.com/license/
*/function la(o){const e=[];for(;o;){if(o.nodeType===Node.DOCUMENT_NODE){e.push(o);break}if(o.nodeType===Node.DOCUMENT_FRAGMENT_NODE){e.push(o),o=o.host;continue}if(o.assignedSlot){o=o.assignedSlot;continue}o=o.parentNode}return e}const fo={start:"top",end:"bottom"},go={start:"left",end:"right"},Mi=new ResizeObserver(o=>{setTimeout(()=>{o.forEach(e=>{e.target.__overlay&&e.target.__overlay._updatePosition()})})}),da=o=>class extends o{static get properties(){return{positionTarget:{type:Object,value:null},horizontalAlign:{type:String,value:"start"},verticalAlign:{type:String,value:"top"},noHorizontalOverlap:{type:Boolean,value:!1},noVerticalOverlap:{type:Boolean,value:!1},requiredVerticalSpace:{type:Number,value:0}}}static get observers(){return["__positionSettingsChanged(horizontalAlign, verticalAlign, noHorizontalOverlap, noVerticalOverlap, requiredVerticalSpace)","__overlayOpenedChanged(opened, positionTarget)"]}constructor(){super(),this.__onScroll=this.__onScroll.bind(this),this._updatePosition=this._updatePosition.bind(this)}connectedCallback(){super.connectedCallback(),this.opened&&this.__addUpdatePositionEventListeners()}disconnectedCallback(){super.disconnectedCallback(),this.__removeUpdatePositionEventListeners()}__addUpdatePositionEventListeners(){window.addEventListener("resize",this._updatePosition),this.__positionTargetAncestorRootNodes=la(this.positionTarget),this.__positionTargetAncestorRootNodes.forEach(e=>{e.addEventListener("scroll",this.__onScroll,!0)})}__removeUpdatePositionEventListeners(){window.removeEventListener("resize",this._updatePosition),this.__positionTargetAncestorRootNodes&&(this.__positionTargetAncestorRootNodes.forEach(e=>{e.removeEventListener("scroll",this.__onScroll,!0)}),this.__positionTargetAncestorRootNodes=null)}__overlayOpenedChanged(e,t){if(this.__removeUpdatePositionEventListeners(),t&&(t.__overlay=null,Mi.unobserve(t),e&&(this.__addUpdatePositionEventListeners(),t.__overlay=this,Mi.observe(t))),e){const i=getComputedStyle(this);this.__margins||(this.__margins={},["top","bottom","left","right"].forEach(n=>{this.__margins[n]=parseInt(i[n],10)})),this.setAttribute("dir",i.direction),this._updatePosition(),requestAnimationFrame(()=>this._updatePosition())}}__positionSettingsChanged(){this._updatePosition()}__onScroll(e){this.contains(e.target)||this._updatePosition()}_updatePosition(){if(!this.positionTarget||!this.opened)return;const e=this.positionTarget.getBoundingClientRect(),t=this.__shouldAlignStartVertically(e);this.style.justifyContent=t?"flex-start":"flex-end";const i=this.__isRTL,n=this.__shouldAlignStartHorizontally(e,i),s=!i&&n||i&&!n;this.style.alignItems=s?"flex-start":"flex-end";const r=this.getBoundingClientRect(),l=this.__calculatePositionInOneDimension(e,r,this.noVerticalOverlap,fo,this,t),a=this.__calculatePositionInOneDimension(e,r,this.noHorizontalOverlap,go,this,n);Object.assign(this.style,l,a),this.toggleAttribute("bottom-aligned",!t),this.toggleAttribute("top-aligned",t),this.toggleAttribute("end-aligned",!s),this.toggleAttribute("start-aligned",s)}__shouldAlignStartHorizontally(e,t){const i=Math.max(this.__oldContentWidth||0,this.$.overlay.offsetWidth);this.__oldContentWidth=this.$.overlay.offsetWidth;const n=Math.min(window.innerWidth,document.documentElement.clientWidth),s=!t&&this.horizontalAlign==="start"||t&&this.horizontalAlign==="end";return this.__shouldAlignStart(e,i,n,this.__margins,s,this.noHorizontalOverlap,go)}__shouldAlignStartVertically(e){const t=this.requiredVerticalSpace||Math.max(this.__oldContentHeight||0,this.$.overlay.offsetHeight);this.__oldContentHeight=this.$.overlay.offsetHeight;const i=Math.min(window.innerHeight,document.documentElement.clientHeight),n=this.verticalAlign==="top";return this.__shouldAlignStart(e,t,i,this.__margins,n,this.noVerticalOverlap,fo)}__shouldAlignStart(e,t,i,n,s,r,l){const a=i-e[r?l.end:l.start]-n[l.end],d=e[r?l.start:l.end]-n[l.start],h=s?a:d,m=h>(s?d:a)||h>t;return s===m}__adjustBottomProperty(e,t,i){let n;if(e===t.end){if(t.end===fo.end){const s=Math.min(window.innerHeight,document.documentElement.clientHeight);if(i>s&&this.__oldViewportHeight){const r=this.__oldViewportHeight-s;n=i-r}this.__oldViewportHeight=s}if(t.end===go.end){const s=Math.min(window.innerWidth,document.documentElement.clientWidth);if(i>s&&this.__oldViewportWidth){const r=this.__oldViewportWidth-s;n=i-r}this.__oldViewportWidth=s}}return n}__calculatePositionInOneDimension(e,t,i,n,s,r){const l=r?n.start:n.end,a=r?n.end:n.start,d=parseFloat(s.style[l]||getComputedStyle(s)[l]),h=this.__adjustBottomProperty(l,n,d),m=t[r?n.start:n.end]-e[i===r?n.end:n.start],u=h?`${h}px`:`${d+m*(r?-1:1)}px`;return{[l]:u,[a]:""}}};var ca=Object.defineProperty,ha=Object.getOwnPropertyDescriptor,we=(o,e,t,i)=>{for(var n=i>1?void 0:i?ha(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&ca(e,t,n),n};class ua extends CustomEvent{constructor(e){super("color-picker-change",{detail:{value:e}})}}const fn=C`
  :host {
    --preview-size: 24px;
    --preview-color: rgba(0, 0, 0, 0);
  }

  .preview {
    --preview-bg-size: calc(var(--preview-size) / 2);
    --preview-bg-pos: calc(var(--preview-size) / 4);

    width: var(--preview-size);
    height: var(--preview-size);
    padding: 0;
    position: relative;
    overflow: hidden;
    background: none;
    border: solid 2px #888;
    border-radius: 4px;
    box-sizing: content-box;
  }

  .preview::before,
  .preview::after {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
  }

  .preview::before {
    content: '';
    background: white;
    background-image: linear-gradient(45deg, #666 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #666 75%), linear-gradient(45deg, transparent 75%, #666 75%),
      linear-gradient(45deg, #666 25%, transparent 25%);
    background-size: var(--preview-bg-size) var(--preview-bg-size);
    background-position: 0 0, 0 0, calc(var(--preview-bg-pos) * -1) calc(var(--preview-bg-pos) * -1),
      var(--preview-bg-pos) var(--preview-bg-pos);
  }

  .preview::after {
    content: '';
    background-color: var(--preview-color);
  }
`;let Qe=class extends A{constructor(){super(...arguments),this.commitValue=!1}static get styles(){return[fn,C`
        #toggle {
          display: block;
        }
      `]}update(o){super.update(o),o.has("value")&&this.overlay&&this.overlay.requestContentUpdate()}firstUpdated(){this.overlay=document.createElement("vaadin-dev-tools-color-picker-overlay"),this.overlay.renderer=this.renderOverlayContent.bind(this),this.overlay.owner=this,this.overlay.positionTarget=this.toggle,this.overlay.noVerticalOverlap=!0,this.overlay.addEventListener("vaadin-overlay-escape-press",this.handleOverlayEscape.bind(this)),this.overlay.addEventListener("vaadin-overlay-close",this.handleOverlayClose.bind(this)),this.append(this.overlay)}render(){const o=this.value||"rgba(0, 0, 0, 0)";return f` <button
      id="toggle"
      class="preview"
      style="--preview-color: ${o}"
      @click=${this.open}
    ></button>`}open(){this.commitValue=!1,this.overlay.opened=!0,this.overlay.style.zIndex="1000000";const o=this.overlay.shadowRoot.querySelector('[part="overlay"]');o.style.background="#333"}renderOverlayContent(o){const e=getComputedStyle(this.toggle,"::after").getPropertyValue("background-color");Ae(f` <div>
        <vaadin-dev-tools-color-picker-overlay-content
          .value=${e}
          .presets=${this.presets}
          @color-changed=${this.handleColorChange.bind(this)}
        ></vaadin-dev-tools-color-picker-overlay-content>
      </div>`,o)}handleColorChange(o){this.commitValue=!0,this.dispatchEvent(new ua(o.detail.value)),o.detail.close&&(this.overlay.opened=!1,this.handleOverlayClose())}handleOverlayEscape(){this.commitValue=!1}handleOverlayClose(){const o=this.commitValue?"color-picker-commit":"color-picker-cancel";this.dispatchEvent(new CustomEvent(o))}};we([y({})],Qe.prototype,"value",2);we([y({})],Qe.prototype,"presets",2);we([tt("#toggle")],Qe.prototype,"toggle",2);Qe=we([F("vaadin-dev-tools-color-picker")],Qe);let At=class extends A{static get styles(){return[fn,C`
        :host {
          display: block;
          padding: 12px;
        }

        .picker::part(saturation),
        .picker::part(hue) {
          margin-bottom: 10px;
        }

        .picker::part(hue),
        .picker::part(alpha) {
          flex: 0 0 20px;
        }

        .picker::part(saturation),
        .picker::part(hue),
        .picker::part(alpha) {
          border-radius: 3px;
        }

        .picker::part(saturation-pointer),
        .picker::part(hue-pointer),
        .picker::part(alpha-pointer) {
          width: 20px;
          height: 20px;
        }

        .swatches {
          display: grid;
          grid-template-columns: repeat(6, var(--preview-size));
          grid-column-gap: 10px;
          grid-row-gap: 6px;
          margin-top: 16px;
        }
      `]}render(){return f` <div>
      <vaadin-dev-tools-rgba-string-color-picker
        class="picker"
        .color=${this.value}
        @color-changed=${this.handlePickerChange}
      ></vaadin-dev-tools-rgba-string-color-picker>
      ${this.renderSwatches()}
    </div>`}renderSwatches(){if(!this.presets||this.presets.length===0)return;const o=this.presets.map(e=>f` <button
        class="preview"
        style="--preview-color: ${e}"
        @click=${()=>this.selectPreset(e)}
      ></button>`);return f` <div class="swatches">${o}</div>`}handlePickerChange(o){this.dispatchEvent(new CustomEvent("color-changed",{detail:{value:o.detail.value}}))}selectPreset(o){this.dispatchEvent(new CustomEvent("color-changed",{detail:{value:o,close:!0}}))}};we([y({})],At.prototype,"value",2);we([y({})],At.prototype,"presets",2);At=we([F("vaadin-dev-tools-color-picker-overlay-content")],At);customElements.whenDefined("vaadin-overlay").then(()=>{const o=customElements.get("vaadin-overlay");class e extends da(o){}customElements.define("vaadin-dev-tools-color-picker-overlay",e)});customElements.define("vaadin-dev-tools-rgba-string-color-picker",aa);var pa=Object.defineProperty,ma=Object.getOwnPropertyDescriptor,va=(o,e,t,i)=>{for(var n=i>1?void 0:i?ma(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&pa(e,t,n),n};let Vi=class extends q{constructor(){super(...arguments),this.presets=new kt}static get styles(){return[q.styles,C`
        .editor-row {
          align-items: center;
        }

        .editor-row > .editor {
          display: flex;
          align-items: center;
          gap: 0.5rem;
        }
      `]}update(o){o.has("propertyMetadata")&&(this.presets=new kt(this.propertyMetadata)),super.update(o)}renderEditor(){var o;return f`
      <vaadin-dev-tools-color-picker
        .value=${this.value}
        .presets=${this.presets.values}
        @color-picker-change=${this.handleColorPickerChange}
        @color-picker-commit=${this.handleColorPickerCommit}
        @color-picker-cancel=${this.handleColorPickerCancel}
      ></vaadin-dev-tools-color-picker>
      <vaadin-dev-tools-theme-text-input
        .value=${this.value}
        .showClearButton=${((o=this.propertyValue)==null?void 0:o.modified)||!1}
        @change=${this.handleInputChange}
      ></vaadin-dev-tools-theme-text-input>
    `}handleInputChange(o){this.value=o.detail.value,this.dispatchChange(this.value)}handleColorPickerChange(o){this.value=o.detail.value}handleColorPickerCommit(){this.dispatchChange(this.value)}handleColorPickerCancel(){this.updateValueFromTheme()}dispatchChange(o){const e=this.presets.tryMapToPreset(o);super.dispatchChange(e)}updateValueFromTheme(){var o;super.updateValueFromTheme(),this.value=this.presets.tryMapToRawValue(((o=this.propertyValue)==null?void 0:o.value)||"")}};Vi=va([F("vaadin-dev-tools-theme-color-property-editor")],Vi);var fa=Object.defineProperty,ga=Object.getOwnPropertyDescriptor,Do=(o,e,t,i)=>{for(var n=i>1?void 0:i?ga(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&fa(e,t,n),n};class ya extends CustomEvent{constructor(e){super("open-css",{detail:{element:e}})}}let Rt=class extends A{static get styles(){return C`
      .section .header {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        padding: 0.4rem var(--theme-editor-section-horizontal-padding);
        color: var(--dev-tools-text-color-emphasis);
        background-color: rgba(0, 0, 0, 0.2);
      }

      .section .property-list .property-editor:not(:last-child) {
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .section .header .open-css {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        background-color: rgba(255, 255, 255, 0.12);
        color: var(--dev-tools-text-color);
        font-weight: 600;
        padding: 0.25rem 0.375rem;
        border-radius: 0.25rem;
      }

      .section .header .open-css:hover {
        color: var(--dev-tools-text-color-emphasis);
      }
    `}render(){const o=this.metadata.elements.map(e=>this.renderSection(e));return f` <div>${o}</div> `}renderSection(o){const e=o.properties.map(t=>this.renderPropertyEditor(o,t));return f`
      <div class="section" data-testid=${o==null?void 0:o.displayName}>
        <div class="header">
          <span> ${o.displayName} </span>
          <button class="open-css" @click=${()=>this.handleOpenCss(o)}>Edit CSS</button>
        </div>
        <div class="property-list">${e}</div>
      </div>
    `}handleOpenCss(o){this.dispatchEvent(new ya(o))}renderPropertyEditor(o,e){let t;switch(e.editorType){case P.checkbox:t=ct`vaadin-dev-tools-theme-checkbox-property-editor`;break;case P.range:t=ct`vaadin-dev-tools-theme-range-property-editor`;break;case P.color:t=ct`vaadin-dev-tools-theme-color-property-editor`;break;default:t=ct`vaadin-dev-tools-theme-text-property-editor`}return nr` <${t}
          class="property-editor"
          .elementMetadata=${o}
          .propertyMetadata=${e}
          .theme=${this.theme}
          data-testid=${e.propertyName}
        >
        </${t}>`}};Do([y({})],Rt.prototype,"metadata",2);Do([y({})],Rt.prototype,"theme",2);Rt=Do([F("vaadin-dev-tools-theme-property-list")],Rt);var ba=Object.defineProperty,_a=Object.getOwnPropertyDescriptor,wa=(o,e,t,i)=>{for(var n=i>1?void 0:i?_a(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&ba(e,t,n),n};let It=class extends A{render(){return f`<div
      tabindex="-1"
      @mousemove=${this.onMouseMove}
      @click=${this.onClick}
      @keydown=${this.onKeyDown}
    ></div>`}onClick(o){const e=this.getTargetElement(o);this.dispatchEvent(new CustomEvent("shim-click",{detail:{target:e}}))}onMouseMove(o){const e=this.getTargetElement(o);this.dispatchEvent(new CustomEvent("shim-mousemove",{detail:{target:e}}))}onKeyDown(o){this.dispatchEvent(new CustomEvent("shim-keydown",{detail:{originalEvent:o}}))}getTargetElement(o){this.style.display="none";const e=document.elementFromPoint(o.clientX,o.clientY);return this.style.display="",e}};It.shadowRootOptions={...A.shadowRootOptions,delegatesFocus:!0};It.styles=[C`
      div {
        pointer-events: auto;
        background: rgba(255, 255, 255, 0);
        position: fixed;
        inset: 0px;
        z-index: 1000000;
      }
    `];It=wa([F("vaadin-dev-tools-shim")],It);const gn=C`
  .popup {
    width: auto;
    position: fixed;
    background-color: var(--dev-tools-background-color-active-blurred);
    color: var(--dev-tools-text-color-primary);
    padding: 0.1875rem 0.75rem 0.1875rem 1rem;
    background-clip: padding-box;
    border-radius: var(--dev-tools-border-radius);
    overflow: hidden;
    margin: 0.5rem;
    width: 30rem;
    max-width: calc(100% - 1rem);
    max-height: calc(100vh - 1rem);
    flex-shrink: 1;
    background-color: var(--dev-tools-background-color-active);
    color: var(--dev-tools-text-color);
    transition: var(--dev-tools-transition-duration);
    transform-origin: bottom right;
    display: flex;
    flex-direction: column;
    box-shadow: var(--dev-tools-box-shadow);
    outline: none;
  }
`;var Sa=Object.defineProperty,Ea=Object.getOwnPropertyDescriptor,ot=(o,e,t,i)=>{for(var n=i>1?void 0:i?Ea(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Sa(e,t,n),n};let he=class extends A{constructor(){super(...arguments),this.active=!1,this.components=[],this.selected=0}connectedCallback(){super.connectedCallback();const o=new CSSStyleSheet;o.replaceSync(`
    .vaadin-dev-tools-highlight-overlay {
      pointer-events: none;
      position: absolute;
      z-index: 10000;
      background: rgba(158,44,198,0.25);
    }`),document.adoptedStyleSheets=[...document.adoptedStyleSheets,o],this.overlayElement=document.createElement("div"),this.overlayElement.classList.add("vaadin-dev-tools-highlight-overlay")}render(){var o;return this.active?(this.style.display="block",f`
      <vaadin-dev-tools-shim
        @shim-click=${this.shimClick}
        @shim-mousemove=${this.shimMove}
        @shim-keydown=${this.shimKeydown}
      ></vaadin-dev-tools-shim>
      <div class="window popup component-picker-info">${(o=this.options)==null?void 0:o.infoTemplate}</div>
      <div class="window popup component-picker-components-info">
        <div>
          ${this.components.map((e,t)=>f`<div class=${t===this.selected?"selected":""}>
                ${e.element.tagName.toLowerCase()}
              </div>`)}
        </div>
      </div>
    `):(this.style.display="none",null)}open(o){this.options=o,this.active=!0,this.dispatchEvent(new CustomEvent("component-picker-opened",{}))}close(){this.active=!1,this.dispatchEvent(new CustomEvent("component-picker-closed",{}))}update(o){var e;if(super.update(o),(o.has("selected")||o.has("components"))&&this.highlight((e=this.components[this.selected])==null?void 0:e.element),o.has("active")){const t=o.get("active"),i=this.active;!t&&i?requestAnimationFrame(()=>this.shim.focus()):t&&!i&&this.highlight(void 0)}}shimKeydown(o){const e=o.detail.originalEvent;if(e.key==="Escape")this.close(),o.stopPropagation(),o.preventDefault();else if(e.key==="ArrowUp"){let t=this.selected-1;t<0&&(t=this.components.length-1),this.selected=t}else e.key==="ArrowDown"?this.selected=(this.selected+1)%this.components.length:e.key==="Enter"&&(this.pickSelectedComponent(),o.stopPropagation(),o.preventDefault())}shimMove(o){const e=o.detail.target;this.components=ar(e),this.selected=this.components.length-1}shimClick(o){this.pickSelectedComponent()}pickSelectedComponent(){const o=this.components[this.selected];if(o&&this.options)try{this.options.pickCallback(o)}catch(e){console.error("Pick callback failed",e)}this.close()}highlight(o){if(this.highlighted!==o)if(o){const e=o.getBoundingClientRect(),t=getComputedStyle(o);this.overlayElement.style.top=`${e.top}px`,this.overlayElement.style.left=`${e.left}px`,this.overlayElement.style.width=`${e.width}px`,this.overlayElement.style.height=`${e.height}px`,this.overlayElement.style.borderRadius=t.borderRadius,document.body.append(this.overlayElement)}else this.overlayElement.remove();this.highlighted=o}};he.styles=[gn,C`
      .component-picker-info {
        left: 1em;
        bottom: 1em;
      }

      .component-picker-components-info {
        right: 3em;
        bottom: 1em;
      }

      .component-picker-components-info .selected {
        font-weight: bold;
      }
    `];ot([T()],he.prototype,"active",2);ot([T()],he.prototype,"components",2);ot([T()],he.prototype,"selected",2);ot([tt("vaadin-dev-tools-shim")],he.prototype,"shim",2);he=ot([F("vaadin-dev-tools-component-picker")],he);const xa=Object.freeze(Object.defineProperty({__proto__:null,get ComponentPicker(){return he}},Symbol.toStringTag,{value:"Module"}));var Ca=Object.defineProperty,ka=Object.getOwnPropertyDescriptor,pe=(o,e,t,i)=>{for(var n=i>1?void 0:i?ka(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Ca(e,t,n),n};Oo(C`
  .vaadin-theme-editor-highlight {
    outline: solid 2px #9e2cc6;
    outline-offset: 3px;
  }
`);let ne=class extends A{constructor(){super(...arguments),this.expanded=!1,this.themeEditorState=Ye.enabled,this.context=null,this.baseTheme=null,this.editedTheme=null,this.effectiveTheme=null}static get styles(){return C`
      :host {
        animation: fade-in var(--dev-tools-transition-duration) ease-in;
        --theme-editor-section-horizontal-padding: 0.75rem;
        display: flex;
        flex-direction: column;
        max-height: 400px;
      }

      .notice {
        padding: var(--theme-editor-section-horizontal-padding);
      }

      .notice a {
        color: var(--dev-tools-text-color-emphasis);
      }

      .header {
        flex: 0 0 auto;
        border-bottom: solid 1px rgba(0, 0, 0, 0.2);
      }

      .header .picker-row {
        padding: var(--theme-editor-section-horizontal-padding);
        display: flex;
        gap: 20px;
        align-items: center;
        justify-content: space-between;
      }

      .picker {
        flex: 1 1 0;
        min-width: 0;
        display: flex;
        align-items: center;
      }

      .picker button {
        min-width: 0;
        display: inline-flex;
        align-items: center;
        padding: 0;
        line-height: 20px;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      .picker button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .picker svg,
      .picker .component-type {
        flex: 0 0 auto;
        margin-right: 4px;
      }

      .picker .instance-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #e5a2fce5;
      }

      .picker .instance-name-quote {
        color: #e5a2fce5;
      }

      .picker .no-selection {
        font-style: italic;
      }

      .actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .property-list {
        flex: 1 1 auto;
        overflow-y: auto;
      }

      .link-button {
        all: initial;
        font-family: inherit;
        font-size: var(--dev-tools-font-size-small);
        line-height: 1;
        white-space: nowrap;
        color: inherit;
        font-weight: 600;
        text-decoration: underline;
      }

      .link-button:focus,
      .link-button:hover {
        color: var(--dev-tools-text-color-emphasis);
      }

      .icon-button {
        padding: 0;
        line-height: 0;
        border: none;
        background: none;
        color: var(--dev-tools-text-color);
      }

      .icon-button:disabled {
        opacity: 0.5;
      }

      .icon-button:not(:disabled):hover {
        color: var(--dev-tools-text-color-emphasis);
      }
    `}firstUpdated(){this.api=new xr(this.connection),this.history=new Cr(this.api),this.historyActions=this.history.allowedActions,this.api.markAsUsed(),document.addEventListener("vaadin-theme-updated",()=>{ve.clear(),this.refreshTheme()})}update(o){var e,t;super.update(o),o.has("expanded")&&(this.expanded?this.highlightElement((e=this.context)==null?void 0:e.component.element):this.removeElementHighlight((t=this.context)==null?void 0:t.component.element))}disconnectedCallback(){var o;super.disconnectedCallback(),this.removeElementHighlight((o=this.context)==null?void 0:o.component.element)}render(){var o,e,t;return this.themeEditorState===Ye.missing_theme?this.renderMissingThemeNotice():f`
      <div class="header">
        <div class="picker-row">
          ${this.renderPicker()}
          <div class="actions">
            ${(o=this.context)!=null&&o.metadata?f` <vaadin-dev-tools-theme-scope-selector
                  .value=${this.context.scope}
                  .metadata=${this.context.metadata}
                  @scope-change=${this.handleScopeChange}
                ></vaadin-dev-tools-theme-scope-selector>`:null}
            <button
              class="icon-button"
              data-testid="undo"
              ?disabled=${!((e=this.historyActions)!=null&&e.allowUndo)}
              @click=${this.handleUndo}
            >
              ${yt.undo}
            </button>
            <button
              class="icon-button"
              data-testid="redo"
              ?disabled=${!((t=this.historyActions)!=null&&t.allowRedo)}
              @click=${this.handleRedo}
            >
              ${yt.redo}
            </button>
          </div>
        </div>
        ${this.renderLocalClassNameEditor()}
      </div>
      ${this.renderPropertyList()}
    `}renderMissingThemeNotice(){return f`
      <div class="notice">
        It looks like you have not set up a custom theme yet. Theme editor requires an existing theme to work with.
        Please check our
        <a href="https://vaadin.com/docs/latest/styling/custom-theme/creating-custom-theme" target="_blank"
          >documentation</a
        >
        on how to set up a custom theme.
      </div>
    `}renderPropertyList(){if(!this.context)return null;if(!this.context.metadata){const o=this.context.component.element.localName;return f`
        <div class="notice">Styling <code>&lt;${o}&gt;</code> components is not supported at the moment.</div>
      `}if(this.context.scope===z.local&&!this.context.accessible){const o=this.context.metadata.displayName;return f`
        <div class="notice">
          The selected ${o} can not be styled locally. Currently, theme editor only supports styling
          instances that are assigned to a local variable, like so:
          <pre><code>Button saveButton = new Button("Save");</code></pre>
          If you want to modify the code so that it satisfies this requirement,
          <button class="link-button" @click=${this.handleShowComponent}>click here</button>
          to open it in your IDE. Alternatively you can choose to style all ${o}s by selecting "Global" from
          the scope dropdown above.
        </div>
      `}return f` <vaadin-dev-tools-theme-property-list
      class="property-list"
      .metadata=${this.context.metadata}
      .theme=${this.effectiveTheme}
      @theme-property-value-change=${this.handlePropertyChange}
      @open-css=${this.handleOpenCss}
    ></vaadin-dev-tools-theme-property-list>`}handleShowComponent(){if(!this.context)return;const o=this.context.component,e={nodeId:o.nodeId,uiId:o.uiId};this.connection.sendShowComponentCreateLocation(e)}async handleOpenCss(o){if(!this.context)return;await this.ensureLocalClassName();const e={themeScope:this.context.scope,localClassName:this.context.localClassName},t=Le(o.detail.element,e);await this.api.openCss(t)}renderPicker(){var o;let e;if((o=this.context)!=null&&o.metadata){const t=this.context.scope===z.local?this.context.metadata.displayName:`All ${this.context.metadata.displayName}s`,i=f`<span class="component-type">${t}</span>`,n=this.context.scope===z.local?Sr(this.context.component):null,s=n?f` <span class="instance-name-quote">"</span><span class="instance-name">${n}</span
            ><span class="instance-name-quote">"</span>`:null;e=f`${i} ${s}`}else e=f`<span class="no-selection">Pick an element to get started</span>`;return f`
      <div class="picker">
        <button @click=${this.pickComponent}>${yt.crosshair} ${e}</button>
      </div>
    `}renderLocalClassNameEditor(){var o;const e=((o=this.context)==null?void 0:o.scope)===z.local&&this.context.accessible;if(!this.context||!e)return null;const t=this.context.localClassName||this.context.suggestedClassName;return f` <vaadin-dev-tools-theme-class-name-editor
      .className=${t}
      @class-name-change=${this.handleClassNameChange}
    >
    </vaadin-dev-tools-theme-class-name-editor>`}async handleClassNameChange(o){if(!this.context)return;const e=this.context.localClassName,t=o.detail.value;if(e){const i=this.context.component.element;this.context.localClassName=t;const n=await this.api.setLocalClassName(this.context.component,t);this.historyActions=this.history.push(n.requestId,()=>ve.previewLocalClassName(i,t),()=>ve.previewLocalClassName(i,e))}else this.context={...this.context,suggestedClassName:t}}async pickComponent(){var o;this.removeElementHighlight((o=this.context)==null?void 0:o.component.element),this.pickerProvider().open({infoTemplate:f`
        <div>
          <h3>Locate the component to style</h3>
          <p>Use the mouse cursor to highlight components in the UI.</p>
          <p>Use arrow down/up to cycle through and highlight specific components under the cursor.</p>
          <p>Click the primary mouse button to select the component.</p>
        </div>
      `,pickCallback:async e=>{var t;const i=await yr.getMetadata(e);if(!i){this.context={component:e,scope:((t=this.context)==null?void 0:t.scope)||z.local},this.baseTheme=null,this.editedTheme=null,this.effectiveTheme=null;return}this.highlightElement(e.element),this.refreshComponentAndTheme(e,i)}})}handleScopeChange(o){this.context&&this.refreshTheme({...this.context,scope:o.detail.value})}async handlePropertyChange(o){if(!this.context||!this.baseTheme||!this.editedTheme)return;const{element:e,property:t,value:i}=o.detail;this.editedTheme.updatePropertyValue(e.selector,t.propertyName,i,!0),this.effectiveTheme=ce.combine(this.baseTheme,this.editedTheme),await this.ensureLocalClassName();const n={themeScope:this.context.scope,localClassName:this.context.localClassName},s=br(e,n,t.propertyName,i);try{const r=await this.api.setCssRules([s]);this.historyActions=this.history.push(r.requestId);const l=_r(s);ve.add(l)}catch(r){console.error("Failed to update property value",r)}}async handleUndo(){this.historyActions=await this.history.undo(),await this.refreshComponentAndTheme()}async handleRedo(){this.historyActions=await this.history.redo(),await this.refreshComponentAndTheme()}async ensureLocalClassName(){if(!this.context||this.context.scope===z.global||this.context.localClassName)return;if(!this.context.localClassName&&!this.context.suggestedClassName)throw new Error("Cannot assign local class name for the component because it does not have a suggested class name");const o=this.context.component.element,e=this.context.suggestedClassName;this.context.localClassName=e;const t=await this.api.setLocalClassName(this.context.component,e);this.historyActions=this.history.push(t.requestId,()=>ve.previewLocalClassName(o,e),()=>ve.previewLocalClassName(o))}async refreshComponentAndTheme(o,e){var t,i,n;if(o=o||((t=this.context)==null?void 0:t.component),e=e||((i=this.context)==null?void 0:i.metadata),!o||!e)return;const s=await this.api.loadComponentMetadata(o);ve.previewLocalClassName(o.element,s.className),await this.refreshTheme({scope:((n=this.context)==null?void 0:n.scope)||z.local,metadata:e,component:o,localClassName:s.className,suggestedClassName:s.suggestedClassName,accessible:s.accessible})}async refreshTheme(o){const e=o||this.context;if(!e||!e.metadata)return;if(e.scope===z.local&&!e.accessible){this.context=e,this.baseTheme=null,this.editedTheme=null,this.effectiveTheme=null;return}let t=new ce(e.metadata);if(!(e.scope===z.local&&!e.localClassName)){const n={themeScope:e.scope,localClassName:e.localClassName},s=e.metadata.elements.map(l=>Le(l,n)),r=await this.api.loadRules(s);t=ce.fromServerRules(e.metadata,n,r.rules)}const i=await wr(e.metadata);this.context=e,this.baseTheme=i,this.editedTheme=t,this.effectiveTheme=ce.combine(i,this.editedTheme)}highlightElement(o){o&&o.classList.add("vaadin-theme-editor-highlight")}removeElementHighlight(o){o&&o.classList.remove("vaadin-theme-editor-highlight")}};pe([y({})],ne.prototype,"expanded",2);pe([y({})],ne.prototype,"themeEditorState",2);pe([y({})],ne.prototype,"pickerProvider",2);pe([y({})],ne.prototype,"connection",2);pe([T()],ne.prototype,"historyActions",2);pe([T()],ne.prototype,"context",2);pe([T()],ne.prototype,"effectiveTheme",2);ne=pe([F("vaadin-dev-tools-theme-editor")],ne);var $a=function(){var o=document.getSelection();if(!o.rangeCount)return function(){};for(var e=document.activeElement,t=[],i=0;i<o.rangeCount;i++)t.push(o.getRangeAt(i));switch(e.tagName.toUpperCase()){case"INPUT":case"TEXTAREA":e.blur();break;default:e=null;break}return o.removeAllRanges(),function(){o.type==="Caret"&&o.removeAllRanges(),o.rangeCount||t.forEach(function(n){o.addRange(n)}),e&&e.focus()}},Di={"text/plain":"Text","text/html":"Url",default:"Text"},Ta="Copy to clipboard: #{key}, Enter";function Na(o){var e=(/mac os x/i.test(navigator.userAgent)?"⌘":"Ctrl")+"+C";return o.replace(/#{\s*key\s*}/g,e)}function Pa(o,e){var t,i,n,s,r,l,a=!1;e||(e={}),t=e.debug||!1;try{n=$a(),s=document.createRange(),r=document.getSelection(),l=document.createElement("span"),l.textContent=o,l.style.all="unset",l.style.position="fixed",l.style.top=0,l.style.clip="rect(0, 0, 0, 0)",l.style.whiteSpace="pre",l.style.webkitUserSelect="text",l.style.MozUserSelect="text",l.style.msUserSelect="text",l.style.userSelect="text",l.addEventListener("copy",function(h){if(h.stopPropagation(),e.format)if(h.preventDefault(),typeof h.clipboardData>"u"){t&&console.warn("unable to use e.clipboardData"),t&&console.warn("trying IE specific stuff"),window.clipboardData.clearData();var m=Di[e.format]||Di.default;window.clipboardData.setData(m,o)}else h.clipboardData.clearData(),h.clipboardData.setData(e.format,o);e.onCopy&&(h.preventDefault(),e.onCopy(h.clipboardData))}),document.body.appendChild(l),s.selectNodeContents(l),r.addRange(s);var d=document.execCommand("copy");if(!d)throw new Error("copy command was unsuccessful");a=!0}catch(h){t&&console.error("unable to copy using execCommand: ",h),t&&console.warn("trying IE specific stuff");try{window.clipboardData.setData(e.format||"text",o),e.onCopy&&e.onCopy(window.clipboardData),a=!0}catch(m){t&&console.error("unable to copy using clipboardData: ",m),t&&console.error("falling back to prompt"),i=Na("message"in e?e.message:Ta),window.prompt(i,o)}}finally{r&&(typeof r.removeRange=="function"?r.removeRange(s):r.removeAllRanges()),l&&document.body.removeChild(l),n()}return a}const zo=1e3,Uo=(o,e)=>{const t=Array.from(o.querySelectorAll(e.join(", "))),i=Array.from(o.querySelectorAll("*")).filter(n=>n.shadowRoot).flatMap(n=>Uo(n.shadowRoot,e));return[...t,...i]};let zi=!1;const Ze=(o,e)=>{zi||(window.addEventListener("message",n=>{n.data==="validate-license"&&window.location.reload()},!1),zi=!0);const t=o._overlayElement;if(t){if(t.shadowRoot){const n=t.shadowRoot.querySelector("slot:not([name])");if(n&&n.assignedElements().length>0){Ze(n.assignedElements()[0],e);return}}Ze(t,e);return}const i=e.messageHtml?e.messageHtml:`${e.message} <p>Component: ${e.product.name} ${e.product.version}</p>`.replace(/https:([^ ]*)/g,"<a href='https:$1'>https:$1</a>");o.isConnected&&(o.outerHTML=`<no-license style="display:flex;align-items:center;text-align:center;justify-content:center;"><div>${i}</div></no-license>`)},Be={},Ui={},Ve={},yn={},Q=o=>`${o.name}_${o.version}`,ji=o=>{const{cvdlName:e,version:t}=o.constructor,i={name:e,version:t},n=o.tagName.toLowerCase();Be[e]=Be[e]??[],Be[e].push(n);const s=Ve[Q(i)];s&&setTimeout(()=>Ze(o,s),zo),Ve[Q(i)]||yn[Q(i)]||Ui[Q(i)]||(Ui[Q(i)]=!0,window.Vaadin.devTools.checkLicense(i))},Aa=o=>{yn[Q(o)]=!0,console.debug("License check ok for",o)},bn=o=>{const e=o.product.name;Ve[Q(o.product)]=o,console.error("License check failed for",e);const t=Be[e];(t==null?void 0:t.length)>0&&Uo(document,t).forEach(i=>{setTimeout(()=>Ze(i,Ve[Q(o.product)]),zo)})},Ra=o=>{const e=o.message,t=o.product.name;o.messageHtml=`No license found. <a target=_blank onclick="javascript:window.open(this.href);return false;" href="${e}">Go here to start a trial or retrieve your license.</a>`,Ve[Q(o.product)]=o,console.error("No license found when checking",t);const i=Be[t];(i==null?void 0:i.length)>0&&Uo(document,i).forEach(n=>{setTimeout(()=>Ze(n,Ve[Q(o.product)]),zo)})},Ia=()=>{window.Vaadin.devTools.createdCvdlElements.forEach(o=>{ji(o)}),window.Vaadin.devTools.createdCvdlElements={push:o=>{ji(o)}}};var M=(o=>(o.ACTIVE="active",o.INACTIVE="inactive",o.UNAVAILABLE="unavailable",o.ERROR="error",o))(M||{});const _n=class wn extends Object{constructor(e){super(),this.status="unavailable",e&&(this.webSocket=new WebSocket(e),this.webSocket.onmessage=t=>this.handleMessage(t),this.webSocket.onerror=t=>this.handleError(t),this.webSocket.onclose=t=>{this.status!=="error"&&this.setStatus("unavailable"),this.webSocket=void 0}),setInterval(()=>{this.webSocket&&self.status!=="error"&&this.status!=="unavailable"&&this.webSocket.send("")},wn.HEARTBEAT_INTERVAL)}onHandshake(){}onReload(){}onUpdate(e,t){}onConnectionError(e){}onStatusChange(e){}onMessage(e){console.error("Unknown message received from the live reload server:",e)}handleMessage(e){let t;try{t=JSON.parse(e.data)}catch(i){this.handleError(`[${i.name}: ${i.message}`);return}t.command==="hello"?(this.setStatus("active"),this.onHandshake()):t.command==="reload"?this.status==="active"&&this.onReload():t.command==="update"?this.status==="active"&&this.onUpdate(t.path,t.content):t.command==="license-check-ok"?Aa(t.data):t.command==="license-check-failed"?bn(t.data):t.command==="license-check-nokey"?Ra(t.data):this.onMessage(t)}handleError(e){console.error(e),this.setStatus("error"),e instanceof Event&&this.webSocket?this.onConnectionError(`Error in WebSocket connection to ${this.webSocket.url}`):this.onConnectionError(e)}setActive(e){!e&&this.status==="active"?this.setStatus("inactive"):e&&this.status==="inactive"&&this.setStatus("active")}setStatus(e){this.status!==e&&(this.status=e,this.onStatusChange(e))}send(e,t){const i=JSON.stringify({command:e,data:t});this.webSocket?this.webSocket.readyState!==WebSocket.OPEN?this.webSocket.addEventListener("open",()=>this.webSocket.send(i)):this.webSocket.send(i):console.error(`Unable to send message ${e}. No websocket is available`)}setFeature(e,t){this.send("setFeature",{featureId:e,enabled:t})}sendTelemetry(e){this.send("reportTelemetry",{browserData:e})}sendLicenseCheck(e){this.send("checkLicense",e)}sendShowComponentCreateLocation(e){this.send("showComponentCreateLocation",e)}sendShowComponentAttachLocation(e){this.send("showComponentAttachLocation",e)}};_n.HEARTBEAT_INTERVAL=18e4;let yo=_n;var Oa=Object.defineProperty,La=Object.getOwnPropertyDescriptor,N=(o,e,t,i)=>{for(var n=i>1?void 0:i?La(e,t):e,s=o.length-1,r;s>=0;s--)(r=o[s])&&(n=(i?r(e,t,n):r(n))||n);return i&&n&&Oa(e,t,n),n},_;const k=(_=class extends A{constructor(){super(),this.expanded=!1,this.messages=[],this.notifications=[],this.frontendStatus=M.UNAVAILABLE,this.javaStatus=M.UNAVAILABLE,this.tabs=[{id:"log",title:"Log",render:()=>this.renderLog(),activate:this.activateLog},{id:"info",title:"Info",render:()=>this.renderInfo()},{id:"features",title:"Feature Flags",render:()=>this.renderFeatures()}],this.activeTab="log",this.serverInfo={flowVersion:"",vaadinVersion:"",javaVersion:"",osVersion:"",productName:""},this.features=[],this.unreadErrors=!1,this.componentPickActive=!1,this.themeEditorState=Ye.disabled,this.nextMessageId=1,this.transitionDuration=0,this.disableLiveReloadTimeout=null,window.Vaadin.Flow&&this.tabs.push({id:"code",title:"Code",render:()=>this.renderCode()})}static get styles(){return[C`
        :host {
          --dev-tools-font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen-Sans, Ubuntu, Cantarell,
            'Helvetica Neue', sans-serif;
          --dev-tools-font-family-monospace: SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New',
            monospace;

          --dev-tools-font-size: 0.8125rem;
          --dev-tools-font-size-small: 0.75rem;

          --dev-tools-text-color: rgba(255, 255, 255, 0.8);
          --dev-tools-text-color-secondary: rgba(255, 255, 255, 0.65);
          --dev-tools-text-color-emphasis: rgba(255, 255, 255, 0.95);
          --dev-tools-text-color-active: rgba(255, 255, 255, 1);

          --dev-tools-background-color-inactive: rgba(45, 45, 45, 0.25);
          --dev-tools-background-color-active: rgba(45, 45, 45, 0.98);
          --dev-tools-background-color-active-blurred: rgba(45, 45, 45, 0.85);

          --dev-tools-border-radius: 0.5rem;
          --dev-tools-box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.4);

          --dev-tools-blue-hsl: 206, 100%, 70%;
          --dev-tools-blue-color: hsl(var(--dev-tools-blue-hsl));
          --dev-tools-green-hsl: 145, 80%, 42%;
          --dev-tools-green-color: hsl(var(--dev-tools-green-hsl));
          --dev-tools-grey-hsl: 0, 0%, 50%;
          --dev-tools-grey-color: hsl(var(--dev-tools-grey-hsl));
          --dev-tools-yellow-hsl: 38, 98%, 64%;
          --dev-tools-yellow-color: hsl(var(--dev-tools-yellow-hsl));
          --dev-tools-red-hsl: 355, 100%, 68%;
          --dev-tools-red-color: hsl(var(--dev-tools-red-hsl));

          /* Needs to be in ms, used in JavaScript as well */
          --dev-tools-transition-duration: 180ms;

          all: initial;

          direction: ltr;
          cursor: default;
          font: normal 400 var(--dev-tools-font-size) / 1.125rem var(--dev-tools-font-family);
          color: var(--dev-tools-text-color);
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
          color-scheme: dark;

          position: fixed;
          z-index: 20000;
          pointer-events: none;
          bottom: 0;
          right: 0;
          width: 100%;
          height: 100%;
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
        }

        .dev-tools {
          pointer-events: auto;
          display: flex;
          align-items: center;
          position: fixed;
          z-index: inherit;
          right: 0.5rem;
          bottom: 0.5rem;
          min-width: 1.75rem;
          height: 1.75rem;
          max-width: 1.75rem;
          border-radius: 0.5rem;
          padding: 0.375rem;
          box-sizing: border-box;
          background-color: var(--dev-tools-background-color-inactive);
          box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          white-space: nowrap;
          line-height: 1rem;
        }

        .dev-tools:hover,
        .dev-tools.active {
          background-color: var(--dev-tools-background-color-active);
          box-shadow: var(--dev-tools-box-shadow);
        }

        .dev-tools.active {
          max-width: calc(100% - 1rem);
        }

        .dev-tools .dev-tools-icon {
          flex: none;
          pointer-events: none;
          display: inline-block;
          width: 1rem;
          height: 1rem;
          fill: #fff;
          transition: var(--dev-tools-transition-duration);
          margin: 0;
        }

        .dev-tools.active .dev-tools-icon {
          opacity: 0;
          position: absolute;
          transform: scale(0.5);
        }

        .dev-tools .status-blip {
          flex: none;
          display: block;
          width: 6px;
          height: 6px;
          border-radius: 50%;
          z-index: 20001;
          background: var(--dev-tools-grey-color);
          position: absolute;
          top: -1px;
          right: -1px;
        }

        .dev-tools .status-description {
          overflow: hidden;
          text-overflow: ellipsis;
          padding: 0 0.25rem;
        }

        .dev-tools.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.15);
          animation: bounce 0.5s;
          animation-iteration-count: 2;
        }

        .switch {
          display: inline-flex;
          align-items: center;
        }

        .switch input {
          opacity: 0;
          width: 0;
          height: 0;
          position: absolute;
        }

        .switch .slider {
          display: block;
          flex: none;
          width: 28px;
          height: 18px;
          border-radius: 9px;
          background-color: rgba(255, 255, 255, 0.3);
          transition: var(--dev-tools-transition-duration);
          margin-right: 0.5rem;
        }

        .switch:focus-within .slider,
        .switch .slider:hover {
          background-color: rgba(255, 255, 255, 0.35);
          transition: none;
        }

        .switch input:focus-visible ~ .slider {
          box-shadow: 0 0 0 2px var(--dev-tools-background-color-active), 0 0 0 4px var(--dev-tools-blue-color);
        }

        .switch .slider::before {
          content: '';
          display: block;
          margin: 2px;
          width: 14px;
          height: 14px;
          background-color: #fff;
          transition: var(--dev-tools-transition-duration);
          border-radius: 50%;
        }

        .switch input:checked + .slider {
          background-color: var(--dev-tools-green-color);
        }

        .switch input:checked + .slider::before {
          transform: translateX(10px);
        }

        .switch input:disabled + .slider::before {
          background-color: var(--dev-tools-grey-color);
        }

        .window.hidden {
          opacity: 0;
          transform: scale(0);
          position: absolute;
        }

        .window.visible {
          transform: none;
          opacity: 1;
          pointer-events: auto;
        }

        .window.visible ~ .dev-tools {
          opacity: 0;
          pointer-events: none;
        }

        .window.visible ~ .dev-tools .dev-tools-icon,
        .window.visible ~ .dev-tools .status-blip {
          transition: none;
          opacity: 0;
        }

        .window {
          border-radius: var(--dev-tools-border-radius);
          overflow: hidden;
          margin: 0.5rem;
          width: 30rem;
          max-width: calc(100% - 1rem);
          max-height: calc(100vh - 1rem);
          flex-shrink: 1;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          display: flex;
          flex-direction: column;
          box-shadow: var(--dev-tools-box-shadow);
          outline: none;
        }

        .window-toolbar {
          display: flex;
          flex: none;
          align-items: center;
          padding: 0.375rem;
          white-space: nowrap;
          order: 1;
          background-color: rgba(0, 0, 0, 0.2);
          gap: 0.5rem;
        }

        .tab {
          color: var(--dev-tools-text-color-secondary);
          font: inherit;
          font-size: var(--dev-tools-font-size-small);
          font-weight: 500;
          line-height: 1;
          padding: 0.25rem 0.375rem;
          background: none;
          border: none;
          margin: 0;
          border-radius: 0.25rem;
          transition: var(--dev-tools-transition-duration);
        }

        .tab:hover,
        .tab.active {
          color: var(--dev-tools-text-color-active);
        }

        .tab.active {
          background-color: rgba(255, 255, 255, 0.12);
        }

        .tab.unreadErrors::after {
          content: '•';
          color: hsl(var(--dev-tools-red-hsl));
          font-size: 1.5rem;
          position: absolute;
          transform: translate(0, -50%);
        }

        .ahreflike {
          font-weight: 500;
          color: var(--dev-tools-text-color-secondary);
          text-decoration: underline;
          cursor: pointer;
        }

        .ahreflike:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .button {
          all: initial;
          font-family: inherit;
          font-size: var(--dev-tools-font-size-small);
          line-height: 1;
          white-space: nowrap;
          background-color: rgba(0, 0, 0, 0.2);
          color: inherit;
          font-weight: 600;
          padding: 0.25rem 0.375rem;
          border-radius: 0.25rem;
        }

        .button:focus,
        .button:hover {
          color: var(--dev-tools-text-color-emphasis);
        }

        .minimize-button {
          flex: none;
          width: 1rem;
          height: 1rem;
          color: inherit;
          background-color: transparent;
          border: 0;
          padding: 0;
          margin: 0 0 0 auto;
          opacity: 0.8;
        }

        .minimize-button:hover {
          opacity: 1;
        }

        .minimize-button svg {
          max-width: 100%;
        }

        .message.information {
          --dev-tools-notification-color: var(--dev-tools-blue-color);
        }

        .message.warning {
          --dev-tools-notification-color: var(--dev-tools-yellow-color);
        }

        .message.error {
          --dev-tools-notification-color: var(--dev-tools-red-color);
        }

        .message {
          display: flex;
          padding: 0.1875rem 0.75rem 0.1875rem 2rem;
          background-clip: padding-box;
        }

        .message.log {
          padding-left: 0.75rem;
        }

        .message-content {
          margin-right: 0.5rem;
          -webkit-user-select: text;
          -moz-user-select: text;
          user-select: text;
        }

        .message-heading {
          position: relative;
          display: flex;
          align-items: center;
          margin: 0.125rem 0;
        }

        .message.log {
          color: var(--dev-tools-text-color-secondary);
        }

        .message:not(.log) .message-heading {
          font-weight: 500;
        }

        .message.has-details .message-heading {
          color: var(--dev-tools-text-color-emphasis);
          font-weight: 600;
        }

        .message-heading::before {
          position: absolute;
          margin-left: -1.5rem;
          display: inline-block;
          text-align: center;
          font-size: 0.875em;
          font-weight: 600;
          line-height: calc(1.25em - 2px);
          width: 14px;
          height: 14px;
          box-sizing: border-box;
          border: 1px solid transparent;
          border-radius: 50%;
        }

        .message.information .message-heading::before {
          content: 'i';
          border-color: currentColor;
          color: var(--dev-tools-notification-color);
        }

        .message.warning .message-heading::before,
        .message.error .message-heading::before {
          content: '!';
          color: var(--dev-tools-background-color-active);
          background-color: var(--dev-tools-notification-color);
        }

        .features-tray {
          padding: 0.75rem;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .features-tray p {
          margin-top: 0;
          color: var(--dev-tools-text-color-secondary);
        }

        .features-tray .feature {
          display: flex;
          align-items: center;
          gap: 1rem;
          padding-bottom: 0.5em;
        }

        .message .message-details {
          font-weight: 400;
          color: var(--dev-tools-text-color-secondary);
          margin: 0.25rem 0;
        }

        .message .message-details[hidden] {
          display: none;
        }

        .message .message-details p {
          display: inline;
          margin: 0;
          margin-right: 0.375em;
          word-break: break-word;
        }

        .message .persist {
          color: var(--dev-tools-text-color-secondary);
          white-space: nowrap;
          margin: 0.375rem 0;
          display: flex;
          align-items: center;
          position: relative;
          -webkit-user-select: none;
          -moz-user-select: none;
          user-select: none;
        }

        .message .persist::before {
          content: '';
          width: 1em;
          height: 1em;
          border-radius: 0.2em;
          margin-right: 0.375em;
          background-color: rgba(255, 255, 255, 0.3);
        }

        .message .persist:hover::before {
          background-color: rgba(255, 255, 255, 0.4);
        }

        .message .persist.on::before {
          background-color: rgba(255, 255, 255, 0.9);
        }

        .message .persist.on::after {
          content: '';
          order: -1;
          position: absolute;
          width: 0.75em;
          height: 0.25em;
          border: 2px solid var(--dev-tools-background-color-active);
          border-width: 0 0 2px 2px;
          transform: translate(0.05em, -0.05em) rotate(-45deg) scale(0.8, 0.9);
        }

        .message .dismiss-message {
          font-weight: 600;
          align-self: stretch;
          display: flex;
          align-items: center;
          padding: 0 0.25rem;
          margin-left: 0.5rem;
          color: var(--dev-tools-text-color-secondary);
        }

        .message .dismiss-message:hover {
          color: var(--dev-tools-text-color);
        }

        .notification-tray {
          display: flex;
          flex-direction: column-reverse;
          align-items: flex-end;
          margin: 0.5rem;
          flex: none;
        }

        .window.hidden + .notification-tray {
          margin-bottom: 3rem;
        }

        .notification-tray .message {
          pointer-events: auto;
          background-color: var(--dev-tools-background-color-active);
          color: var(--dev-tools-text-color);
          max-width: 30rem;
          box-sizing: border-box;
          border-radius: var(--dev-tools-border-radius);
          margin-top: 0.5rem;
          transition: var(--dev-tools-transition-duration);
          transform-origin: bottom right;
          animation: slideIn var(--dev-tools-transition-duration);
          box-shadow: var(--dev-tools-box-shadow);
          padding-top: 0.25rem;
          padding-bottom: 0.25rem;
        }

        .notification-tray .message.animate-out {
          animation: slideOut forwards var(--dev-tools-transition-duration);
        }

        .notification-tray .message .message-details {
          max-height: 10em;
          overflow: hidden;
        }

        .message-tray {
          flex: auto;
          overflow: auto;
          max-height: 20rem;
          user-select: text;
        }

        .message-tray .message {
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          padding-left: 2.25rem;
        }

        .message-tray .message.warning {
          background-color: hsla(var(--dev-tools-yellow-hsl), 0.09);
        }

        .message-tray .message.error {
          background-color: hsla(var(--dev-tools-red-hsl), 0.09);
        }

        .message-tray .message.error .message-heading {
          color: hsl(var(--dev-tools-red-hsl));
        }

        .message-tray .message.warning .message-heading {
          color: hsl(var(--dev-tools-yellow-hsl));
        }

        .message-tray .message + .message {
          border-top: 1px solid rgba(255, 255, 255, 0.07);
        }

        .message-tray .dismiss-message,
        .message-tray .persist {
          display: none;
        }

        .info-tray {
          padding: 0.75rem;
          position: relative;
          flex: auto;
          overflow: auto;
          animation: fade-in var(--dev-tools-transition-duration) ease-in;
          user-select: text;
        }

        .info-tray dl {
          margin: 0;
          display: grid;
          grid-template-columns: max-content 1fr;
          column-gap: 0.75rem;
          position: relative;
        }

        .info-tray dt {
          grid-column: 1;
          color: var(--dev-tools-text-color-emphasis);
        }

        .info-tray dt:not(:first-child)::before {
          content: '';
          width: 100%;
          position: absolute;
          height: 1px;
          background-color: rgba(255, 255, 255, 0.1);
          margin-top: -0.375rem;
        }

        .info-tray dd {
          grid-column: 2;
          margin: 0;
        }

        .info-tray :is(dt, dd):not(:last-child) {
          margin-bottom: 0.75rem;
        }

        .info-tray dd + dd {
          margin-top: -0.5rem;
        }

        .info-tray .live-reload-status::before {
          content: '•';
          color: var(--status-color);
          width: 0.75rem;
          display: inline-block;
          font-size: 1rem;
          line-height: 0.5rem;
        }

        .info-tray .copy {
          position: fixed;
          z-index: 1;
          top: 0.5rem;
          right: 0.5rem;
        }

        .info-tray .switch {
          vertical-align: -4px;
        }

        @keyframes slideIn {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0%);
            opacity: 1;
          }
        }

        @keyframes slideOut {
          from {
            transform: translateX(0%);
            opacity: 1;
          }
          to {
            transform: translateX(100%);
            opacity: 0;
          }
        }

        @keyframes fade-in {
          0% {
            opacity: 0;
          }
        }

        @keyframes bounce {
          0% {
            transform: scale(0.8);
          }
          50% {
            transform: scale(1.5);
            background-color: hsla(var(--dev-tools-red-hsl), 1);
          }
          100% {
            transform: scale(1);
          }
        }

        @supports (backdrop-filter: blur(1px)) {
          .dev-tools,
          .window,
          .notification-tray .message {
            backdrop-filter: blur(8px);
          }
          .dev-tools:hover,
          .dev-tools.active,
          .window,
          .notification-tray .message {
            background-color: var(--dev-tools-background-color-active-blurred);
          }
        }
      `,gn]}static get isActive(){const o=window.sessionStorage.getItem(_.ACTIVE_KEY_IN_SESSION_STORAGE);return o===null||o!=="false"}static notificationDismissed(o){const e=window.localStorage.getItem(_.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);return e!==null&&e.includes(o)}elementTelemetry(){let o={};try{const e=localStorage.getItem("vaadin.statistics.basket");if(!e)return;o=JSON.parse(e)}catch{return}this.frontendConnection&&this.frontendConnection.sendTelemetry(o)}openWebSocketConnection(){this.frontendStatus=M.UNAVAILABLE,this.javaStatus=M.UNAVAILABLE;const o=l=>this.log("error",l),e=()=>{this.showSplashMessage("Reloading…");const l=window.sessionStorage.getItem(_.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE),a=l?parseInt(l,10)+1:1;window.sessionStorage.setItem(_.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE,a.toString()),window.sessionStorage.setItem(_.TRIGGERED_KEY_IN_SESSION_STORAGE,"true"),window.location.reload()},t=(l,a)=>{let d=document.head.querySelector(`style[data-file-path='${l}']`);d?(this.log("information","Hot update of "+l),d.textContent=a,document.dispatchEvent(new CustomEvent("vaadin-theme-updated"))):e()},i=new yo(this.getDedicatedWebSocketUrl());i.onHandshake=()=>{this.log("log","Vaadin development mode initialized"),_.isActive||i.setActive(!1),this.elementTelemetry()},i.onConnectionError=o,i.onReload=e,i.onUpdate=t,i.onStatusChange=l=>{this.frontendStatus=l},i.onMessage=l=>this.handleFrontendMessage(l),this.frontendConnection=i;let n;this.backend===_.SPRING_BOOT_DEVTOOLS&&this.springBootLiveReloadPort?(n=new yo(this.getSpringBootWebSocketUrl(window.location)),n.onHandshake=()=>{_.isActive||n.setActive(!1)},n.onReload=e,n.onConnectionError=o):this.backend===_.JREBEL||this.backend===_.HOTSWAP_AGENT?n=i:n=new yo(void 0);const s=n.onStatusChange;n.onStatusChange=l=>{s(l),this.javaStatus=l};const r=n.onHandshake;n.onHandshake=()=>{r(),this.backend&&this.log("information",`Java live reload available: ${_.BACKEND_DISPLAY_NAME[this.backend]}`)},this.javaConnection=n,this.backend||this.showNotification("warning","Java live reload unavailable","Live reload for Java changes is currently not set up. Find out how to make use of this functionality to boost your workflow.","https://vaadin.com/docs/latest/flow/configuration/live-reload","liveReloadUnavailable")}handleFrontendMessage(o){if((o==null?void 0:o.command)==="serverInfo")this.serverInfo=o.data;else if((o==null?void 0:o.command)==="featureFlags")this.features=o.data.features;else if((o==null?void 0:o.command)==="themeEditorState"){const e=!!window.Vaadin.Flow;this.themeEditorState=o.data,e&&this.themeEditorState!==Ye.disabled&&(this.tabs.push({id:"theme-editor",title:"Theme Editor (Preview)",render:()=>this.renderThemeEditor()}),this.requestUpdate())}else console.error("Unknown message from front-end connection:",JSON.stringify(o))}getDedicatedWebSocketUrl(){function o(t){const i=document.createElement("div");return i.innerHTML=`<a href="${t}"/>`,i.firstChild.href}if(this.url===void 0)return;const e=o(this.url);if(!e.startsWith("http://")&&!e.startsWith("https://")){console.error("The protocol of the url should be http or https for live reload to work.");return}return`${e.replace(/^http/,"ws")}?v-r=push&debug_window`}getSpringBootWebSocketUrl(o){const{hostname:e}=o,t=o.protocol==="https:"?"wss":"ws";if(e.endsWith("gitpod.io")){const i=e.replace(/.*?-/,"");return`${t}://${this.springBootLiveReloadPort}-${i}`}else return`${t}://${e}:${this.springBootLiveReloadPort}`}connectedCallback(){if(super.connectedCallback(),this.catchErrors(),this.disableEventListener=e=>this.demoteSplashMessage(),document.body.addEventListener("focus",this.disableEventListener),document.body.addEventListener("click",this.disableEventListener),this.openWebSocketConnection(),window.sessionStorage.getItem(_.TRIGGERED_KEY_IN_SESSION_STORAGE)){const e=new Date,t=`${`0${e.getHours()}`.slice(-2)}:${`0${e.getMinutes()}`.slice(-2)}:${`0${e.getSeconds()}`.slice(-2)}`;this.showSplashMessage(`Page reloaded at ${t}`),window.sessionStorage.removeItem(_.TRIGGERED_KEY_IN_SESSION_STORAGE)}this.transitionDuration=parseInt(window.getComputedStyle(this).getPropertyValue("--dev-tools-transition-duration"),10);const o=window;o.Vaadin=o.Vaadin||{},o.Vaadin.devTools=Object.assign(this,o.Vaadin.devTools),Ia(),document.documentElement.addEventListener("vaadin-overlay-outside-click",e=>{const t=e,i=t.target.owner;i&&dr(this,i)||t.detail.sourceEvent.composedPath().includes(this)&&e.preventDefault()})}format(o){return o.toString()}catchErrors(){const o=window.Vaadin.ConsoleErrors;o&&o.forEach(e=>{this.log("error",e.map(t=>this.format(t)).join(" "))}),window.Vaadin.ConsoleErrors={push:e=>{this.log("error",e.map(t=>this.format(t)).join(" "))}}}disconnectedCallback(){this.disableEventListener&&(document.body.removeEventListener("focus",this.disableEventListener),document.body.removeEventListener("click",this.disableEventListener)),super.disconnectedCallback()}toggleExpanded(){this.notifications.slice().forEach(o=>this.dismissNotification(o.id)),this.expanded=!this.expanded,this.expanded&&this.root.focus()}showSplashMessage(o){this.splashMessage=o,this.splashMessage&&(this.expanded?this.demoteSplashMessage():setTimeout(()=>{this.demoteSplashMessage()},_.AUTO_DEMOTE_NOTIFICATION_DELAY))}demoteSplashMessage(){this.splashMessage&&this.log("log",this.splashMessage),this.showSplashMessage(void 0)}checkLicense(o){this.frontendConnection?this.frontendConnection.sendLicenseCheck(o):bn({message:"Internal error: no connection",product:o})}log(o,e,t,i){const n=this.nextMessageId;for(this.nextMessageId+=1,this.messages.push({id:n,type:o,message:e,details:t,link:i,dontShowAgain:!1,deleted:!1});this.messages.length>_.MAX_LOG_ROWS;)this.messages.shift();this.requestUpdate(),this.updateComplete.then(()=>{const s=this.renderRoot.querySelector(".message-tray .message:last-child");this.expanded&&s?(setTimeout(()=>s.scrollIntoView({behavior:"smooth"}),this.transitionDuration),this.unreadErrors=!1):o==="error"&&(this.unreadErrors=!0)})}showNotification(o,e,t,i,n){if(n===void 0||!_.notificationDismissed(n)){if(this.notifications.filter(r=>r.persistentId===n).filter(r=>!r.deleted).length>0)return;const s=this.nextMessageId;this.nextMessageId+=1,this.notifications.push({id:s,type:o,message:e,details:t,link:i,persistentId:n,dontShowAgain:!1,deleted:!1}),i===void 0&&setTimeout(()=>{this.dismissNotification(s)},_.AUTO_DEMOTE_NOTIFICATION_DELAY),this.requestUpdate()}else this.log(o,e,t,i)}dismissNotification(o){const e=this.findNotificationIndex(o);if(e!==-1&&!this.notifications[e].deleted){const t=this.notifications[e];if(t.dontShowAgain&&t.persistentId&&!_.notificationDismissed(t.persistentId)){let i=window.localStorage.getItem(_.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE);i=i===null?t.persistentId:`${i},${t.persistentId}`,window.localStorage.setItem(_.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE,i)}t.deleted=!0,this.log(t.type,t.message,t.details,t.link),setTimeout(()=>{const i=this.findNotificationIndex(o);i!==-1&&(this.notifications.splice(i,1),this.requestUpdate())},this.transitionDuration)}}findNotificationIndex(o){let e=-1;return this.notifications.some((t,i)=>t.id===o?(e=i,!0):!1),e}toggleDontShowAgain(o){const e=this.findNotificationIndex(o);if(e!==-1&&!this.notifications[e].deleted){const t=this.notifications[e];t.dontShowAgain=!t.dontShowAgain,this.requestUpdate()}}setActive(o){var e,t;(e=this.frontendConnection)==null||e.setActive(o),(t=this.javaConnection)==null||t.setActive(o),window.sessionStorage.setItem(_.ACTIVE_KEY_IN_SESSION_STORAGE,o?"true":"false")}getStatusColor(o){return o===M.ACTIVE?"var(--dev-tools-green-color)":o===M.INACTIVE?"var(--dev-tools-grey-color)":o===M.UNAVAILABLE?"var(--dev-tools-yellow-hsl)":o===M.ERROR?"var(--dev-tools-red-color)":"none"}renderMessage(o){return f`
      <div
        class="message ${o.type} ${o.deleted?"animate-out":""} ${o.details||o.link?"has-details":""}"
      >
        <div class="message-content">
          <div class="message-heading">${o.message}</div>
          <div class="message-details" ?hidden="${!o.details&&!o.link}">
            ${o.details?f`<p>${o.details}</p>`:""}
            ${o.link?f`<a class="ahreflike" href="${o.link}" target="_blank">Learn more</a>`:""}
          </div>
          ${o.persistentId?f`<div
                class="persist ${o.dontShowAgain?"on":"off"}"
                @click=${()=>this.toggleDontShowAgain(o.id)}
              >
                Don’t show again
              </div>`:""}
        </div>
        <div class="dismiss-message" @click=${()=>this.dismissNotification(o.id)}>Dismiss</div>
      </div>
    `}render(){return f` <div
        class="window ${this.expanded&&!this.componentPickActive?"visible":"hidden"}"
        tabindex="0"
        @keydown=${o=>o.key==="Escape"&&this.expanded&&this.toggleExpanded()}
      >
        <div class="window-toolbar">
          ${this.tabs.map(o=>f`<button
                class=${Io({tab:!0,active:this.activeTab===o.id,unreadErrors:o.id==="log"&&this.unreadErrors})}
                id="${o.id}"
                @click=${()=>{this.activeTab=o.id,o.activate&&o.activate.call(this)}}
              >
                ${o.title}
              </button> `)}
          <button class="minimize-button" title="Minimize" @click=${()=>this.toggleExpanded()}>
            <svg fill="none" height="16" viewBox="0 0 16 16" width="16" xmlns="http://www.w3.org/2000/svg">
              <g fill="#fff" opacity=".8">
                <path
                  d="m7.25 1.75c0-.41421.33579-.75.75-.75h3.25c2.0711 0 3.75 1.67893 3.75 3.75v6.5c0 2.0711-1.6789 3.75-3.75 3.75h-6.5c-2.07107 0-3.75-1.6789-3.75-3.75v-3.25c0-.41421.33579-.75.75-.75s.75.33579.75.75v3.25c0 1.2426 1.00736 2.25 2.25 2.25h6.5c1.2426 0 2.25-1.0074 2.25-2.25v-6.5c0-1.24264-1.0074-2.25-2.25-2.25h-3.25c-.41421 0-.75-.33579-.75-.75z"
                />
                <path
                  d="m2.96967 2.96967c.29289-.29289.76777-.29289 1.06066 0l5.46967 5.46967v-2.68934c0-.41421.33579-.75.75-.75.4142 0 .75.33579.75.75v4.5c0 .4142-.3358.75-.75.75h-4.5c-.41421 0-.75-.3358-.75-.75 0-.41421.33579-.75.75-.75h2.68934l-5.46967-5.46967c-.29289-.29289-.29289-.76777 0-1.06066z"
                />
              </g>
            </svg>
          </button>
        </div>
        ${this.tabs.map(o=>this.activeTab===o.id?o.render():x)}
      </div>

      <div class="notification-tray">${this.notifications.map(o=>this.renderMessage(o))}</div>
      <vaadin-dev-tools-component-picker
        .active=${this.componentPickActive}
        @component-picker-opened=${()=>{this.componentPickActive=!0}}
        @component-picker-closed=${()=>{this.componentPickActive=!1}}
      ></vaadin-dev-tools-component-picker>
      <div
        class="dev-tools ${this.splashMessage?"active":""}${this.unreadErrors?" error":""}"
        @click=${()=>this.toggleExpanded()}
      >
        ${this.unreadErrors?f`<svg
              fill="none"
              height="16"
              viewBox="0 0 16 16"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
              xmlns:xlink="http://www.w3.org/1999/xlink"
              class="dev-tools-icon error"
            >
              <clipPath id="a"><path d="m0 0h16v16h-16z" /></clipPath>
              <g clip-path="url(#a)">
                <path
                  d="m6.25685 2.09894c.76461-1.359306 2.72169-1.359308 3.4863 0l5.58035 9.92056c.7499 1.3332-.2135 2.9805-1.7432 2.9805h-11.1606c-1.529658 0-2.4930857-1.6473-1.743156-2.9805z"
                  fill="#ff5c69"
                />
                <path
                  d="m7.99699 4c-.45693 0-.82368.37726-.81077.834l.09533 3.37352c.01094.38726.32803.69551.71544.69551.38741 0 .70449-.30825.71544-.69551l.09533-3.37352c.0129-.45674-.35384-.834-.81077-.834zm.00301 8c.60843 0 1-.3879 1-.979 0-.5972-.39157-.9851-1-.9851s-1 .3879-1 .9851c0 .5911.39157.979 1 .979z"
                  fill="#fff"
                />
              </g>
            </svg>`:f`<svg
              fill="none"
              height="17"
              viewBox="0 0 16 17"
              width="16"
              xmlns="http://www.w3.org/2000/svg"
              class="dev-tools-icon logo"
            >
              <g fill="#fff">
                <path
                  d="m8.88273 5.97926c0 .04401-.0032.08898-.00801.12913-.02467.42848-.37813.76767-.8117.76767-.43358 0-.78704-.34112-.81171-.76928-.00481-.04015-.00801-.08351-.00801-.12752 0-.42784-.10255-.87656-1.14434-.87656h-3.48364c-1.57118 0-2.315271-.72849-2.315271-2.21758v-1.26683c0-.42431.324618-.768314.748261-.768314.42331 0 .74441.344004.74441.768314v.42784c0 .47924.39576.81265 1.11293.81265h3.41538c1.5542 0 1.67373 1.156 1.725 1.7679h.03429c.05095-.6119.17048-1.7679 1.72468-1.7679h3.4154c.7172 0 1.0145-.32924 1.0145-.80847l-.0067-.43202c0-.42431.3227-.768314.7463-.768314.4234 0 .7255.344004.7255.768314v1.26683c0 1.48909-.6181 2.21758-2.1893 2.21758h-3.4836c-1.04182 0-1.14437.44872-1.14437.87656z"
                />
                <path
                  d="m8.82577 15.1648c-.14311.3144-.4588.5335-.82635.5335-.37268 0-.69252-.2249-.83244-.5466-.00206-.0037-.00412-.0073-.00617-.0108-.00275-.0047-.00549-.0094-.00824-.0145l-3.16998-5.87318c-.08773-.15366-.13383-.32816-.13383-.50395 0-.56168.45592-1.01879 1.01621-1.01879.45048 0 .75656.22069.96595.6993l2.16882 4.05042 2.17166-4.05524c.2069-.47379.513-.69448.9634-.69448.5603 0 1.0166.45711 1.0166 1.01879 0 .17579-.0465.35029-.1348.50523l-3.1697 5.8725c-.00503.0096-.01006.0184-.01509.0272-.00201.0036-.00402.0071-.00604.0106z"
                />
              </g>
            </svg>`}

        <span
          class="status-blip"
          style="background: linear-gradient(to right, ${this.getStatusColor(this.frontendStatus)} 50%, ${this.getStatusColor(this.javaStatus)} 50%)"
        ></span>
        ${this.splashMessage?f`<span class="status-description">${this.splashMessage}</span></div>`:x}
      </div>`}renderLog(){return f`<div class="message-tray">${this.messages.map(o=>this.renderMessage(o))}</div>`}activateLog(){this.unreadErrors=!1,this.updateComplete.then(()=>{const o=this.renderRoot.querySelector(".message-tray .message:last-child");o&&o.scrollIntoView()})}renderCode(){return f`<div class="info-tray">
      <div>
        <select id="locationType">
          <option value="create" selected>Create</option>
          <option value="attach">Attach</option>
        </select>
        <button
          class="button pick"
          @click=${async()=>{await S(()=>Promise.resolve().then(()=>xa),void 0),this.componentPicker.open({infoTemplate:f`
                <div>
                  <h3>Locate a component in source code</h3>
                  <p>Use the mouse cursor to highlight components in the UI.</p>
                  <p>Use arrow down/up to cycle through and highlight specific components under the cursor.</p>
                  <p>
                    Click the primary mouse button to open the corresponding source code line of the highlighted
                    component in your IDE.
                  </p>
                </div>
              `,pickCallback:o=>{const e={nodeId:o.nodeId,uiId:o.uiId};this.renderRoot.querySelector("#locationType").value==="create"?this.frontendConnection.sendShowComponentCreateLocation(e):this.frontendConnection.sendShowComponentAttachLocation(e)}})}}
        >
          Find component in code
        </button>
      </div>
      </div>
    </div>`}renderInfo(){return f`<div class="info-tray">
      <button class="button copy" @click=${this.copyInfoToClipboard}>Copy</button>
      <dl>
        <dt>${this.serverInfo.productName}</dt>
        <dd>${this.serverInfo.vaadinVersion}</dd>
        <dt>Flow</dt>
        <dd>${this.serverInfo.flowVersion}</dd>
        <dt>Java</dt>
        <dd>${this.serverInfo.javaVersion}</dd>
        <dt>OS</dt>
        <dd>${this.serverInfo.osVersion}</dd>
        <dt>Browser</dt>
        <dd>${navigator.userAgent}</dd>
        <dt>
          Live reload
          <label class="switch">
            <input
              id="toggle"
              type="checkbox"
              ?disabled=${this.liveReloadDisabled||(this.frontendStatus===M.UNAVAILABLE||this.frontendStatus===M.ERROR)&&(this.javaStatus===M.UNAVAILABLE||this.javaStatus===M.ERROR)}
              ?checked="${this.frontendStatus===M.ACTIVE||this.javaStatus===M.ACTIVE}"
              @change=${o=>this.setActive(o.target.checked)}
            />
            <span class="slider"></span>
          </label>
        </dt>
        <dd class="live-reload-status" style="--status-color: ${this.getStatusColor(this.javaStatus)}">
          Java ${this.javaStatus} ${this.backend?`(${_.BACKEND_DISPLAY_NAME[this.backend]})`:""}
        </dd>
        <dd class="live-reload-status" style="--status-color: ${this.getStatusColor(this.frontendStatus)}">
          Front end ${this.frontendStatus}
        </dd>
      </dl>
    </div>`}renderFeatures(){return f`<div class="features-tray">
      ${this.features.map(o=>f`<div class="feature">
          <label class="switch">
            <input
              class="feature-toggle"
              id="feature-toggle-${o.id}"
              type="checkbox"
              ?checked=${o.enabled}
              @change=${e=>this.toggleFeatureFlag(e,o)}
            />
            <span class="slider"></span>
            ${o.title}
          </label>
          <a class="ahreflike" href="${o.moreInfoLink}" target="_blank">Learn more</a>
        </div>`)}
    </div>`}renderThemeEditor(){return f` <vaadin-dev-tools-theme-editor
      .expanded=${this.expanded}
      .themeEditorState=${this.themeEditorState}
      .pickerProvider=${()=>this.componentPicker}
      .connection=${this.frontendConnection}
    ></vaadin-dev-tools-theme-editor>`}copyInfoToClipboard(){const o=this.renderRoot.querySelectorAll(".info-tray dt, .info-tray dd"),e=Array.from(o).map(t=>(t.localName==="dd"?": ":`
`)+t.textContent.trim()).join("").replace(/^\n/,"");Pa(e),this.showNotification("information","Environment information copied to clipboard",void 0,void 0,"versionInfoCopied")}toggleFeatureFlag(o,e){const t=o.target.checked;this.frontendConnection?(this.frontendConnection.setFeature(e.id,t),this.showNotification("information",`“${e.title}” ${t?"enabled":"disabled"}`,e.requiresServerRestart?"This feature requires a server restart":void 0,void 0,`feature${e.id}${t?"Enabled":"Disabled"}`)):this.log("error",`Unable to toggle feature ${e.title}: No server connection available`)}},_.MAX_LOG_ROWS=1e3,_.DISMISSED_NOTIFICATIONS_IN_LOCAL_STORAGE="vaadin.live-reload.dismissedNotifications",_.ACTIVE_KEY_IN_SESSION_STORAGE="vaadin.live-reload.active",_.TRIGGERED_KEY_IN_SESSION_STORAGE="vaadin.live-reload.triggered",_.TRIGGERED_COUNT_KEY_IN_SESSION_STORAGE="vaadin.live-reload.triggeredCount",_.AUTO_DEMOTE_NOTIFICATION_DELAY=5e3,_.HOTSWAP_AGENT="HOTSWAP_AGENT",_.JREBEL="JREBEL",_.SPRING_BOOT_DEVTOOLS="SPRING_BOOT_DEVTOOLS",_.BACKEND_DISPLAY_NAME={HOTSWAP_AGENT:"HotswapAgent",JREBEL:"JRebel",SPRING_BOOT_DEVTOOLS:"Spring Boot Devtools"},_);N([y({type:String})],k.prototype,"url",2);N([y({type:Boolean,attribute:!0})],k.prototype,"liveReloadDisabled",2);N([y({type:String})],k.prototype,"backend",2);N([y({type:Number})],k.prototype,"springBootLiveReloadPort",2);N([y({type:Boolean,attribute:!1})],k.prototype,"expanded",2);N([y({type:Array,attribute:!1})],k.prototype,"messages",2);N([y({type:String,attribute:!1})],k.prototype,"splashMessage",2);N([y({type:Array,attribute:!1})],k.prototype,"notifications",2);N([y({type:String,attribute:!1})],k.prototype,"frontendStatus",2);N([y({type:String,attribute:!1})],k.prototype,"javaStatus",2);N([T()],k.prototype,"tabs",2);N([T()],k.prototype,"activeTab",2);N([T()],k.prototype,"serverInfo",2);N([T()],k.prototype,"features",2);N([T()],k.prototype,"unreadErrors",2);N([tt(".window")],k.prototype,"root",2);N([tt("vaadin-dev-tools-component-picker")],k.prototype,"componentPicker",2);N([T()],k.prototype,"componentPickActive",2);N([T()],k.prototype,"themeEditorState",2);let Ma=k;customElements.get("vaadin-dev-tools")===void 0&&customElements.define("vaadin-dev-tools",Ma);export{en as C,Ys as D,je as F,A as L,Gs as P,J as T,Ne as U,E as _,be as a,T as b,C as c,Ks as d,F as e,pr as f,ur as g,f as h,x as n,y as p,Ae as r,Te as s,ms as u,P as y};
