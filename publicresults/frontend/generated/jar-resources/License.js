const noLicenseFallbackTimeout = 1000;
export const findAll = (element, tags) => {
    const lightDom = Array.from(element.querySelectorAll(tags.join(', ')));
    const shadowDom = Array.from(element.querySelectorAll('*'))
        .filter((e) => e.shadowRoot)
        .flatMap((e) => findAll(e.shadowRoot, tags));
    return [...lightDom, ...shadowDom];
};
let licenseCheckListener = false;
const showNoLicenseFallback = (element, productAndMessage) => {
    if (!licenseCheckListener) {
        // When a license check has succeeded, refresh so that all elements are properly shown again
        window.addEventListener('message', (e) => {
            if (e.data === 'validate-license') {
                window.location.reload();
            }
        }, false);
        licenseCheckListener = true;
    }
    const overlay = element._overlayElement;
    if (overlay) {
        if (overlay.shadowRoot) {
            const defaultSlot = overlay.shadowRoot.querySelector('slot:not([name])');
            if (defaultSlot && defaultSlot.assignedElements().length > 0) {
                showNoLicenseFallback(defaultSlot.assignedElements()[0], productAndMessage);
                return;
            }
        }
        showNoLicenseFallback(overlay, productAndMessage);
        return;
    }
    const htmlMessage = productAndMessage.messageHtml
        ? productAndMessage.messageHtml
        : `${productAndMessage.message} <p>Component: ${productAndMessage.product.name} ${productAndMessage.product.version}</p>`.replace(/https:([^ ]*)/g, "<a href='https:$1'>https:$1</a>");
    if (element.isConnected) {
        element.outerHTML = `<no-license style="display:flex;align-items:center;text-align:center;justify-content:center;"><div>${htmlMessage}</div></no-license>`;
    }
};
const productTagNames = {};
const productChecking = {};
const productMissingLicense = {};
const productCheckOk = {};
const key = (product) => {
    return `${product.name}_${product.version}`;
};
const checkLicenseIfNeeded = (cvdlElement) => {
    var _a;
    const { cvdlName, version } = cvdlElement.constructor;
    const product = { name: cvdlName, version };
    const tagName = cvdlElement.tagName.toLowerCase();
    productTagNames[cvdlName] = (_a = productTagNames[cvdlName]) !== null && _a !== void 0 ? _a : [];
    productTagNames[cvdlName].push(tagName);
    const failedLicenseCheck = productMissingLicense[key(product)];
    if (failedLicenseCheck) {
        // Has been checked and the check failed
        setTimeout(() => showNoLicenseFallback(cvdlElement, failedLicenseCheck), noLicenseFallbackTimeout);
    }
    if (productMissingLicense[key(product)] || productCheckOk[key(product)]) {
        // Already checked
    }
    else if (!productChecking[key(product)]) {
        // Has not been checked
        productChecking[key(product)] = true;
        window.Vaadin.devTools.checkLicense(product);
    }
};
export const licenseCheckOk = (data) => {
    productCheckOk[key(data)] = true;
    // eslint-disable-next-line no-console
    console.debug('License check ok for', data);
};
export const licenseCheckFailed = (data) => {
    const productName = data.product.name;
    productMissingLicense[key(data.product)] = data;
    // eslint-disable-next-line no-console
    console.error('License check failed for', productName);
    const tags = productTagNames[productName];
    if ((tags === null || tags === void 0 ? void 0 : tags.length) > 0) {
        findAll(document, tags).forEach((element) => {
            setTimeout(() => showNoLicenseFallback(element, productMissingLicense[key(data.product)]), noLicenseFallbackTimeout);
        });
    }
};
export const licenseCheckNoKey = (data) => {
    const keyUrl = data.message;
    const productName = data.product.name;
    data.messageHtml = `No license found. <a target=_blank onclick="javascript:window.open(this.href);return false;" href="${keyUrl}">Go here to start a trial or retrieve your license.</a>`;
    productMissingLicense[key(data.product)] = data;
    // eslint-disable-next-line no-console
    console.error('No license found when checking', productName);
    const tags = productTagNames[productName];
    if ((tags === null || tags === void 0 ? void 0 : tags.length) > 0) {
        findAll(document, tags).forEach((element) => {
            setTimeout(() => showNoLicenseFallback(element, productMissingLicense[key(data.product)]), noLicenseFallbackTimeout);
        });
    }
};
export const licenseInit = () => {
    // Process already registered elements
    window.Vaadin.devTools.createdCvdlElements.forEach((element) => {
        checkLicenseIfNeeded(element);
    });
    // Handle new elements directly
    window.Vaadin.devTools.createdCvdlElements = {
        push: (element) => {
            checkLicenseIfNeeded(element);
        }
    };
};
//# sourceMappingURL=License.js.map