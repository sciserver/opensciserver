const jQuery = require('jquery');
const apiCall = require('./apiCall').default;
const GrantAccess = require('./grant-access').default;

const $ = jQuery;

export default function (_config, _divid) {
  const that = this;
  this.config = _config;
  this.token = _config.token;
  this.user = _config.user;
 // initialize elements wrt rootdiv
  this.rootdiv = jQuery(`#${_divid}`);
  this.resourcesTable = this.rootdiv.find('#resourcesTable');
  // an object with {columns,rows,hrNames}
  this.resources = null;
  // data: contextClass,resourceContextUUID,resourceContextAPIEndpoint,
  //       resourceType,resourceName, resourcePubDID,resourceuuid,action,actionCategory
  // table: Context Class,RACM Link,Resource Type,Resource - Name,Resource - PubID,Action,[]
  function loadResources() {
    apiCall(that.config.getResourcesUrl, that.token, 'GET', '', (json) => {
      that.resources = json;
      const columnsFromRequest = that.resources.columns;
      const columnsHrFromRequest = that.resources.hrNames;
      const columnsToBeDeleted = ['resourceContextUUID', 'resourceuuid', 'action'];
      const columnIndexesToKeep = [];
      const columnsForDataTable = [];
      const resourceUUIDIndex = columnsFromRequest.indexOf('resourceuuid');
      const actionCategoryIndex = columnsFromRequest.indexOf('actionCategory');
      // eslint-disable-next-line guard-for-in,no-restricted-syntax
      for (const k in columnsFromRequest) {
        if (!columnsToBeDeleted.includes(columnsFromRequest[k])) {
            columnIndexesToKeep.push(k);
            if (columnsFromRequest[k] === 'actionCategory') {
              columnsForDataTable.push({ title: '' });
            } else {
              columnsForDataTable.push({ title: columnsHrFromRequest[k] });
            }
        }
      }
      $('#resourcesTable').DataTable({
          columns: columnsForDataTable,
        },
      );
      const rows = [];
      $.each(that.resources.rows, (index, row) => {
        const filteredRow = [];
        const resourceUUID = row[resourceUUIDIndex];
        if (row[actionCategoryIndex] === 'G') {
          row[actionCategoryIndex] = `<button onclick='Config.Resources.grant("${resourceUUID}")'>Grant</button>`;
          // eslint-disable-next-line guard-for-in,no-restricted-syntax
          for (const k in row) {
            if (columnIndexesToKeep.includes(k)) {
              filteredRow.push(row[k]);
            }
          }
          rows.push(filteredRow);
        }
      });
      if ($.fn.dataTable.isDataTable('#resourcesTable')) { // trying to refresh the table
        $('#resourcesTable').DataTable().clear().destroy();
      }
      $('#resourcesTable').DataTable(
        {
          data: rows,
          processing: true,
          bFilter: true,
          paging: true,
          select: {
            style: 'single',
          },
          language: {
            emptyTable: 'No resources to display',
          },
        },
      );
    }, null);
  }
  that.grantAccess = new GrantAccess(_config, 'grantPrivileges', that);
  $('#resourcesRefreshLink').on('click', loadResources);
  this.grant = (resourceUUID) => {
    that.grantAccess.grant(resourceUUID);
  };
  // initialize
  loadResources();
}
