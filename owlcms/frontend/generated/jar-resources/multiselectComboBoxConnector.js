// Connector for the MultiselectComboBox (based on the ComboBox connector)
import { Debouncer } from '@polymer/polymer/lib/utils/debounce.js';
import { timeOut } from '@polymer/polymer/lib/utils/async.js';

window.Vaadin.Flow.multiselectComboBoxConnector = {
  initLazy: function (multiselectComboBox) {

    // check if already initialized for the MultiselectComboBox
    if (multiselectComboBox.$connector) {
      return;
    }

    multiselectComboBox.$connector = {};

    let pageCallbacks = {};
    let cache = {};
    let lastFilter = '';

    multiselectComboBox.$connector.initDataConnector = function() {
      if (_hasDataProvider(multiselectComboBox)) {
        return;
      }
      multiselectComboBox.$.comboBox.dataProvider = function (params, callback) {
        if (params.pageSize != multiselectComboBox.$.comboBox.pageSize
            && multiselectComboBox.pageSize != multiselectComboBox.$.comboBox.pageSize) {

          throw 'Invalid pageSize';
        }

        if (multiselectComboBox._clientSideFilter) {
          // For clientside filter we first make sure we have all data which we also
          // filter based on comboBox.filter. While later we only filter client side data.
          if (cache[0]) {
            performClientSideFilter(cache[0], callback);
            return;
          } else {
            // If the client side filter is enabled then we need to first get all data
            // and filter it on client side. Otherwise the next time the user
            // inputs another filter, eg. continues to type, the local cache will be only
            // that which was received for the first filter, which may not be the whole
            // data from server (keep in mind that the client side filter is enabled only
            // when the items count does not exceed one page).
            params.filter = "";
          }
        }

        const filterChanged = params.filter !== lastFilter;
        if (filterChanged) {
          pageCallbacks = {};
          cache = {};
          lastFilter = params.filter;
        }

        if (cache[params.page]) {
          // This may happen after skipping pages by scrolling fast
          commitPage(params.page, callback);
        } else {
          const upperLimit = params.pageSize * (params.page + 1);

          if (filterChanged) {
            this._debouncer = Debouncer.debounce(
                this._debouncer,
                timeOut.after(500),
                () => {
                  multiselectComboBox.$server.setRequestedRange(0, upperLimit, params.filter);
                  if (params.filter === '') {
                    // Fixes the case when the filter changes
                    // from '' to something else and back to ''
                    // within debounce timeout, and the
                    // DataCommunicator thinks it doesn't need to send data
                    multiselectComboBox.$server.resetDataCommunicator();
                  }
                });
          } else {
            multiselectComboBox.$server.setRequestedRange(0, upperLimit, params.filter);
          }

          pageCallbacks[params.page] = callback;
        }
      };
    };

    multiselectComboBox.$connector.filter = function (item, filter) {
      filter = filter ? filter.toString().toLowerCase() : '';
      return multiselectComboBox.$.comboBox._getItemLabel(item).toString().toLowerCase().indexOf(filter) > -1;
    };

    multiselectComboBox.$connector.set = function (index, items, filter) {
      if (filter != lastFilter) {
        return;
      }

      if (index % multiselectComboBox.$.comboBox.pageSize != 0) {
        throw 'Got new data to index ' + index + ' which is not aligned with the page size of ' + multiselectComboBox.$.comboBox.pageSize;
      }

      if (index === 0 && items.length === 0 && pageCallbacks[0]) {
        // Makes sure that the dataProvider callback is called even when server
        // returns empty data set (no items match the filter).
        cache[0] = [];
        return;
      }

      const firstPageToSet = index / multiselectComboBox.$.comboBox.pageSize;
      const updatedPageCount = Math.ceil(items.length / multiselectComboBox.$.comboBox.pageSize);

      for (let i = 0; i < updatedPageCount; i++) {
        let page = firstPageToSet + i;
        let slice = items.slice(i * multiselectComboBox.$.comboBox.pageSize, (i + 1) * multiselectComboBox.$.comboBox.pageSize);

        cache[page] = slice;
      }
    };

    multiselectComboBox.$connector.updateData = function (items) {
      // IE11 doesn't work with the transpiled version of the forEach.
      for (let i = 0; i < items.length; i++) {
        let item = items[i];

        for (let j = 0; j < multiselectComboBox.$.comboBox.filteredItems.length; j++) {
          if (multiselectComboBox.$.comboBox.filteredItems[j].key === item.key) {
              multiselectComboBox.$.comboBox.set('filteredItems.' + j, item);
            break;
          }
        }
      }
    };

    multiselectComboBox.$connector.updateSize = function (newSize) {
      if (!multiselectComboBox._clientSideFilter) {
        // NOTE: It may be that this set size is unnecessary, since when
        // providing data to combobox via callback we may use the data size.
        // However, if this size reflects the whole data, including
        // data not yet fetched on the client side, and the combobox expects it
        // to be set, then at least, we don't need it in case the
        // filter is clientSide only (since it'll increase the height of
        // the popup at only the first user filter to this size, while the
        // filtered items count are less).
        customElements.whenDefined('multiselect-combo-box').then(() => {
          multiselectComboBox.$.comboBox.size = newSize;
        });
      }
    };

    multiselectComboBox.$connector.reset = function () {
      pageCallbacks = {};
      cache = {};
      multiselectComboBox.$.comboBox.clearCache();
    };

    multiselectComboBox.$connector.confirm = function (id, filter) {

      if (filter != lastFilter) {
        return;
      }

      // We're done applying changes from this batch,
      // resolve outstanding callbacks
      let outstandingRequests = Object.getOwnPropertyNames(pageCallbacks);
      for (let i = 0; i < outstandingRequests.length; i++) {
        let page = outstandingRequests[i];

        if (cache[page]) {
          let callback = pageCallbacks[page];
          delete pageCallbacks[page];

          commitPage(page, callback);
        }
      }

      // inform the server that we are done
      multiselectComboBox.$server.confirmUpdate(id);
    };

    multiselectComboBox.$connector.setCompactModeLabel = function(compactModeLabel) {
      multiselectComboBox.compactModeLabelGenerator = () => compactModeLabel;
    };

    const commitPage = function (page, callback) {
      let data = cache[page];

      if (multiselectComboBox._clientSideFilter) {
        performClientSideFilter(data, callback)

      } else {
        // Remove the data if server-side filtering,
        // but keep it for client-side filtering
        delete cache[page];

        // NOTE: It may be that we ought to provide data.length instead of
        // comboBox.size and remove the updateSize function.
        callback(data, multiselectComboBox.$.comboBox.size);
      }
    };

    // Perform filter on client side (here) using the items from specified page
    // and submitting the filtered items to specified callback.
    // The filter used is the one from combobox, not the lastFilter stored since
    // that may not reflect user's input.
    const performClientSideFilter = function (page, callback) {

      let filteredItems = page;

      if (multiselectComboBox.$.comboBox.filter) {
        filteredItems = page.filter(item =>
            multiselectComboBox.$connector.filter(item, multiselectComboBox.$.comboBox.filter));
      }

      callback(filteredItems, filteredItems.length);
    };

    const _hasDataProvider = function(multiselectComboBox) {
      return multiselectComboBox.$.comboBox.dataProvider && typeof multiselectComboBox.$.comboBox.dataProvider === 'function';
    };

  }
};
