/*
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0.
  See LICENSE.txt in the project root for license information.
*/

$(document).ready(function() {
    /* https://stackoverflow.com/a/4835406 */
    const escapeHtml = function(text) {
        const map = {
            '&' : '&amp;',
            '<' : '&lt;',
            '>' : '&gt;',
            '"' : '&quot;',
            "'" : '&#039;'
        };

        return text.replace(/[&<>"']/g, function(m) {
            return map[m];
        });
    };

    const slimNotebookURI = function(uri) {
        return escapeHtml(uri.replace(/^\/home\/idies\/workspace\//g, ''));
    };

    const body = $('body');
    const jobsTable = $('#jobsTable');

    const token = body.data('token');
    const username = body.data('username');
    const isDaskAvailable = body.data('is-dask-available');
    const daskDefaultMemory = body.data('dask-default-memory');
    const daskDefaultThreads = body.data('dask-default-threads');
    const daskDefaultWorkers  = body.data('dask-default-workers');
    const RACM_BASE_URL = body.data('racm');
    const DASHBOARD_URL = body.data('dashboard');
    const JOBS_URL = RACM_BASE_URL + "/jobm/rest/jobs";
    const QUERY_JOBS_URL = RACM_BASE_URL + "/jobm/rest/dockerjobs";
    const DOMAINS_URL = RACM_BASE_URL + "/jobm/rest/computedomains?batch=true";
    const DOCKER_SUBMIT_URL = RACM_BASE_URL + '/jobm/rest/jobs/docker';
    const STATUS_TO_CLASSES = {8: 'success', 64: 'danger', 128: 'warning'};

    // regex for paths in the containers
    // First match is root volume name, then owner name, then user volume name
    // Fourth match is the path relative to the user volume
    const PATH_REGEX_PARSE = /\/home\/idies\/workspace\/([^/]+)\/([^/]+)\/([^/]+)\/(.*)$/;

    let checkedUserVolumes = false;
    const displayWarningIfNoUserVolumes = function(domains) {
        if(!checkedUserVolumes) {
            if (_.flatten(_.map(domains, 'userVolumes')).length === 0) {
                $('#visitDashboardDialog').modal('show');
            }
            checkedUserVolumes = true;
        };
    };

    const ERROR_ICON = '<span class="glyphicon glyphicon-exclamation-sign" aria-hidden="true"></span>'+
                       '<span class="sr-only">Error</span>';
    const ACTIONS = '<button type="button" class="btn btn-default btn-sm more-info-btn" aria-label="More Info">'+
                    '<span class="glyphicon glyphicon-triangle-bottom" aria-hidden="true"></span>'+
                    '</button>';

    const showAlert = function(htmlMessage) {
        $('<div class="alert alert-danger" id="ajax-errors" role="alert">' +
            '<button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
            htmlMessage +
        '</div>').insertBefore('main > div.container');
    };

    const fileServiceJsonTreePathFromUserVolume = function(userVolume) {
        const filterOption = '?options=**.ipynb';
        if (userVolume.isShareable) {
            return userVolume.fileServiceAPIEndpoint + 'api/jsontree/' + userVolume.rootVolumeName + '/' +
                userVolume.owner + '/' + userVolume.name + filterOption;
        } else {
            return userVolume.fileServiceAPIEndpoint + 'api/jsontree/' + userVolume.rootVolumeName + '/' +
                userVolume.owner + filterOption;
        }
    };

    const volumeNameSorter = function(a, b) {
        const aName = a.jsonTree ? a.jsonTree.queryPath : a.name
        const bName = b.jsonTree ? b.jsonTree.queryPath : b.name
        if (aName === 'persistent') {
            return -1;
        }
        if (bName === 'persistent') {
            return 1;
        }
        if (aName === 'scratch') {
            return -1;
        }
        if (bName === 'scratch') {
            return 1;
        }
        return aName.localeCompare(bName);
    };

    const saveJob = function(newJob) {
        $.ajax({
            url: DOCKER_SUBMIT_URL,
            method: 'POST',
            headers: {'X-Auth-Token': token, 'Content-Type': 'application/json'},
            data: JSON.stringify(newJob),
            error: function(jqXHR, textStatus, errorThrown) {
                showAlert("<p>Unable to add docker job. Please email " +
                        "<a href='mailto:sciserver-helpdesk@jhu.edu'>sciserver-helpdesk@jhu.edu</a> for more assistence.</p>" +
                        "The error message is: <pre>" + escapeHtml(errorThrown) + "\n" +
                        escapeHtml(jqXHR.responseText) + "</pre>");
            },
            success: function(data, textStatus, jqXHR) {
                refreshTableNow();
                $.toast({
                    text: 'Job created',
                    icon: 'success',
                    position: 'bottom-center',
                    bgColor: '#5cb85c'
                });
            }
        });
    };

    Vue.component('vue-multiselect', VueMultiselect.default)
    Vue.use(Vuex);
    const store = new Vuex.Store({
        state: {
            jsonTreeOfUserVolume: {},
            userVolumesState: {},
            jobs: {},
            listOfJobIds: [],
            domains: {},
            listOfDomainIds: [],
            images: {},
            userVolumes: {},
            volumeContainers: {},
        },
        mutations: {
            setJsonTree: function(state, payload) {
                Vue.set(state.jsonTreeOfUserVolume, payload.id, payload.tree);
                Vue.set(state.userVolumesState, payload.id, 'LOADED');
            },
            setUserVolumeAsLoading: function(state, userVolumeId) {
                Vue.set(state.userVolumesState, userVolumeId, 'LOADING');
            },
            setUserVolumeAsError: function(state, userVolumeId) {
                Vue.set(state.userVolumesState, userVolumeId, 'ERROR');
            },
            setJobs: function(state, jobs) {
                state.jobs = _.keyBy(jobs, 'id');
                state.listOfJobIds = _.map(jobs, 'id');
            },
            setDomains: function(state, domains) {
                const image = new normalizr.schema.Entity('images');
                const userVolume = new normalizr.schema.Entity('userVolumes');
                const volumeContainer = new normalizr.schema.Entity('volumeContainers');
                const domain = new normalizr.schema.Entity('domains', {images: [image], userVolumes: [userVolume], volumes: [volumeContainer]});
                const normalizedData = normalizr.normalize(domains, [domain]);
                state.images = _.assign({}, state.images, normalizedData.entities.images);
                state.userVolumes = _.assign({}, state.userVolumes, normalizedData.entities.userVolumes);
                state.volumeContainers = _.assign({}, state.volumeContainers, normalizedData.entities.volumeContainers);
                state.domains = _.assign({}, state.domains, normalizedData.entities.domains);
                state.listOfDomainIds = normalizedData.result;
            },
        },
        getters: {
            getDomainForJob: function(state) {
                return function (job) {
                    const domain = _.find(_.values(state.domains), {racmUUID: job.dockerComputeResourceContextUUID});
                    if (domain !== undefined) {
                        return domain.id;
                    }
                };
            },
            getUserVolumeState: function(state) {
                return function(id) {
                    if (!state.userVolumesState[id]) {
                        return 'NOT LOADED';
                    } else {
                        return state.userVolumesState[id];
                    }
                };
            },
            getJob: function(state) {
                return function(jobId) {
                    return state.jobs[jobId];
                };
            },
            getJobsForDomain: function(state, getters) {
                return function(domain) {
                    return state.listOfJobIds
                        .map(getters.getJob)
                        .filter(function(job) { return job.dockerComputeEndpoint === domain.apiEndpoint;});
                };
            },
            getImage: function(state) {
                return function(imageId) {
                    return state.images[imageId];
                };
            },
            getImagesForDomainId: function(state) {
                return function(domainId) {
                    return _.pick(state.images, state.domains[domainId].images);
                };
            },
            getImageForDomainIdAndName: function(state, getters) {
                return function(domainId, imageName) {
                    return _.find(_.values(getters.getImagesForDomainId(domainId)), {name: imageName});
                }
            },
            getUserVolume: function(state) {
                return function(userVolumeId) {
                    return state.userVolumes[userVolumeId];
                };
            },
            getUserVolumesForDomainId: function(state) {
                return function(domainId) {
                    return _.pick(state.userVolumes, state.domains[domainId].userVolumes);
                }
            },
            getVolumeContainersForDomainId: function(state) {
                return function(domainId) {
                    return _.pick(state.volumeContainers, state.domains[domainId].volumes);
                }
            },
            recentlyUsedDomains: function(state, getters) {
                if (!state.domains || !state.jobs) {
                    return [];
                }
                return _.uniq(state.listOfJobIds
                    .map(getters.getJob)
                    .map(getters.getDomainForJob));
            },
            recentlyUsedImagesByDomain: function(state, getters) {
                if (!state.domains || !state.jobs) {
                    return [];
                }
                return function(domain) {
                    return getters.getJobsForDomain(domain)
                        .map(function(job) {
                            return job.dockerImageName;
                        })
                        .map(function(imageName) {
                            return getters.getImageForDomainIdAndName(domain.id, imageName);
                        })
                        .map(function(image) {
                            return _.get(image, 'id');
                        });
                }
            },
            recentlyUsedUserVolumesByDomain: function(state, getters) {
                if (!state.domains || !state.jobs) {
                    return [];
                }
                return function(domain) {
                    return _.flatMap(
                        getters.getJobsForDomain(domain),
                        function(job) {
                            return _.map(job.userVolumes, _.property('userVolumeId'));
                        });
                }
            }
        },
        actions: {
            loadJsonTree: function(state, userVolume) {
                if (state.getters.getUserVolumeState(userVolume.id) === 'NOT LOADED') {
                    state.commit("setUserVolumeAsLoading", userVolume.id);
                }
                $.get({
                    url: fileServiceJsonTreePathFromUserVolume(userVolume),
                    headers: { "X-Auth-Token": token},
                    dataType: 'json'
                }).done(function(data, textStatus, jqXHR) {
                    state.commit('setJsonTree', {
                        id: userVolume.id,
                        tree: data
                    });
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    state.commit("setUserVolumeAsError", userVolume.id);
                });
            }
        }
    });
    window.store = store;

    Vue.component('file-picker-folder', {
        template: ''+
            '<li>'+
                '<div @click="toggle" :style="nameStyle">'+
                    '<span :class="iconClasses"></span> '+
                    '<span v-if="isUserVolume">{{ path }}</span>'+
                    '<template v-else>{{ data.name }}</template>'+
                '</div>'+
                '<ul v-show="open" v-if="isFolder">'+
                    '<file-picker-folder '+
                        'v-for="item in folders" '+
                        ':key="path + \'/\' + item.name" '+
                        ':selectedPath="selectedPath" '+
                        ':data="item" '+
                        ':is-folder="true" '+
                        '@input="reemit" '+
                        ':path="path + \'/\' + item.name"/>'+
                    '<file-picker-folder '+
                        'v-for="item in files" '+
                        ':key="path + \'/\' + item.name" '+
                        ':selectedPath="selectedPath" '+
                        ':data="item" '+
                        ':is-folder="false" '+
                        '@input="reemit" '+
                        ':path="path + \'/\' + item.name"/>'+
                    '<li class="small text-muted" v-if="!data.folders && files.length === 0" :key="path + \'//no-item\'">No *.ipynb files found</li>'+
                '</ul>'+
            '</li>',
        props: ['data', 'path', 'selectedPath', 'isFolder'],
        data: function() {
            return {
                open: false
            };
        },
        computed: {
            nameStyle: function() {
                return {
                    fontWeight: this.onPath ? 'bold' : 'normal'
                };
            },
            iconClasses: function() {
                return {
                    fa: true,
                    'fa-fw': true,
                    'fa-folder': this.isFolder && !this.open,
                    'fa-folder-open': this.isFolder && this.open,
                    'fa-dot-circle-o': !this.isFolder && this.selectedPath == this.path,
                    'fa-circle-o': !this.isFolder && this.selectedPath != this.path
                };
            },
            isUserVolume: function() {
                return this.path.split("/").length === 3;
            },
            onPath: function() {
                return this.selectedPath.startsWith(this.path) &&
                    (this.selectedPath.replace(this.path , '').startsWith("/") ||
                            this.selectedPath === this.path);
            },
            files: function() {
                if (!this.data.files) {
                    return [];
                }
                return this.data.files.filter(function(file) {
                    return file.name.endsWith(".ipynb");
                }).sort(function(a, b) {
                    return a.name.localeCompare(b.name);
                });
            },
            folders: function() {
                if (!this.data.folders) {
                    return [];
                }
                return this.data.folders.slice().sort(function(a, b) {
                    return a.name.localeCompare(b.name);
                });
            }
        },
        methods: {
            toggle: function() {
                if (this.isFolder) {
                    this.open = !this.open;
                } else {
                    this.$emit('input', this.path);
                }
            },
            reemit: function(newPath) {
                this.$emit('input', newPath);
            }
        }
    });

    Vue.component('fileservice-picker', {
        template: ''+
            '<div class="clt">'+
                '<ul :style="listStyle">'+
                    '<file-picker-folder '+
                        'v-for="item in data" '+
                        'v-if="item.state === \'LOADED\'" '+
                        ':key="item.id" '+
                        ':selectedPath="selectedPath"'+
                        ':data="item.jsonTree.root" '+
                        ':is-folder="true" '+
                        '@input="selectPath($event, item.id)" '+
                        ':path="itemToPath(item)" />'+
                    '<li '+
                        'v-for="item in data" '+
                        'v-if="item.state !== \'LOADED\'" '+
                        ':key="item.id">'+
                            '{{ statusToMsg(item.state, item.name) }}'+
                    '</li>'+
                '</ul>'+
                '<input name="notebookPath" class="form-control" '+
                    ':value="selectedPath" placeholder="Notebook Path" readonly>'+
            '</div>',
        props: ['domainId', 'selectedPath', 'selectedUserVolumeId'],
        computed: {
            data: function() {
                if (!this.domainId) {
                    return [];
                }
                const userVolumes = _.values(this.$store.getters.getUserVolumesForDomainId(this.domainId));

                const output = [];
                for (const uv of userVolumes)
                    output.push({
                        id: uv.id,
                        name: uv.name,
                        jsonTree: this.$store.state.jsonTreeOfUserVolume[uv.id],
                        state: this.$store.getters.getUserVolumeState(uv.id)
                    });

                return output.sort(volumeNameSorter);
            },
            listStyle: function() {
                return {
                    minHeight: '200px',
                    maxHeight: '400px',
                    overflowY: 'auto',
                };
            },
        },
        methods: {
            selectPath: function(path, userVolumeId) {
                this.$emit('selectFile', path, userVolumeId);
            },
            statusToMsg: function(status, name) {
                if (status === "LOADING") {
                    return "Loading "+name+"…"
                }
                if (status === 'NOT LOADED') {
                    return name + " not loaded yet";
                }
                if (status === "ERROR") {
                    return name + " not successfully loaded"
                }
            },
            itemToPath: function(item) {
                const uv = this.$store.getters.getUserVolume(item.id);
                return uv.rootVolumeName + '/' + uv.owner + '/' + uv.name;
            },
        }
    });

    const getDomainForJob = function(job) {
        return JSON.parse(sessionStorage.getItem("BatchComputeDomains")).filter(function(info) {
            return info.racmUUID === job.dockerComputeResourceContextUUID;
        })[0];
    };

    Vue.component('select-list-group-item', {
        template: ''+
            '<div class="list-group-item" role="button" @click="clickItem">'+
                '<h5 class="list-group-item-heading">'+
                    '<span class="state-icon fa" :class="iconClass"></span>'+
                    '{{ item.name }}'+
                '</h5>'+
                '<small class="list-group-item-text">{{ item.description }}</small>'+
            '</div>',
        props: ['item', 'selected'],
        model: {
            prop: 'selected'
        },
        computed: {
            iconClass: function() {
                return {
                    'fa-dot-circle-o': this.selected,
                    'fa-circle-o': !this.selected,
                }
            }
        },
        methods: {
            clickItem: function() {
                this.$emit('input', this.item.id);
            }
        }
    });

    Vue.component('select-list-group', {
        template: ''+
            '<div>'+
                '<input v-if="_.keys(items).length > 7" type="text" v-model="filter" class="form-control" placeholder="Filter By" :title="filterText">'+
                '<div class="list-group">'+
                    '<template v-if="recentList.length > 0 && !isFiltering">'+
                        '<p class="list-group-item text-center" :style="recentlyUsedTextCSS">'+
                            '<b>Recently used:</b>'+
                        '</p>'+
                        '<select-list-group-item '+
                            'v-for="item in recentList" '+
                            ':item="item" '+
                            ':key="item.id" '+
                            ':selected="selectedItemId === item.id" '+
                            ':style="recentlyUsedItemCSS(item.id)" '+
                            '@input="selectItem"/>'+
                        '<hr class="checked-list-divider"/>'+
                    '</template>'+
                    '<select-list-group-item v-for="item in mainList" :item="item" :key="item.id" :selected="selectedItemId === item.id" @input="selectItem"/>'+
                '</div>'+
            '</div>',
        props: {
            items: {
                type: Object,
                required: true,
            },
            recentlyUsedIds: Array,
            filterText: String,
            selectedItemId: Number,
        },
        model: {
            prop: 'selectedItemId',
        },
        data: function() {
            return {
                filter: '',
            };
        },
        computed: {
            isFiltering: function() {
                return this.filter !== '';
            },
            filteredList: function() {
                const that = this;
                return _.values(this.items).filter(function (item) {
                    return item.name.toLowerCase().indexOf(that.filter.toLowerCase()) !== -1 ||
                        item.description.toLowerCase().indexOf(that.filter.toLowerCase()) !== -1;
                });
            },
            filteredRecentlyUsed: function() {
                if (!this.recentlyUsedIds) {
                    return;
                }
                const that = this;
                return _.uniq(this.recentlyUsedIds)
                    // items can be recently used, but not usable now
                    .filter(function (itemId) { return that.items[itemId];})
                    .map(function(itemId) { return that.items[itemId];});
            },
            mainList: function() {
                return _.differenceBy(this.filteredList, this.recentList, 'id').sort(function (a, b) {
                    return a.name.localeCompare(b.name);
                });
            },
            recentList: function() {
                if (this.filteredRecentlyUsed && _.values(this.items).length > 7 && !this.isFiltering) {
                    return this.filteredRecentlyUsed.slice(0, 3);
                } else {
                    return [];
                }
            },
            recentlyUsedTextCSS: function() {
                return {
                    borderTop: 'thin solid',
                    borderLeft: 'thin solid',
                    borderRight: 'thin solid',
                }
            },
        },
        methods: {
            selectItem: function (itemId) {
                this.$emit('input', itemId);
            },
            recentlyUsedItemCSS: function(itemId) {
                return {
                    borderLeft: 'thin solid',
                    borderRight: 'thin solid',
                    borderBottom: itemId === this.recentList[this.recentList.length-1].id ?
                            'thin solid' : undefined,
                }
            },
        },
    });

    Vue.component('volume-list-group-item', {
        template: ''+
            '<div class="list-group-item" style="display: flex; align-items: stretch;">'+
                '<div style="flex: 1;" role="button" @click="clickItem">'+
                    '<h5 class="list-group-item-heading">'+
                        '<span class="state-icon fa" :class="iconClass"></span>'+
                        '{{ item.name }}'+
                        '<small v-if="item.rootVolumeName"><br/>{{ item.rootVolumeName }} Volume, created by {{ item.owner }}</small>'+
                    '</h5>'+
                    '<small class="list-group-item-text">{{ item.description }}</small>'+
                    '<p class="small" v-if="selected">Will be accessible to this job as <code>{{ pathInJob }}</code></p>'+
                '</div>'+
                '<dropdown menu-right v-if="selected">'+
                    '<btn v-if="isReadWrite" type="default" @click="toggleWritability">Read/Write</btn>'+
                    '<btn v-else type="default" :disabled="!isWritable" @click="toggleWritability">Read-only</btn>'+
                    '<btn v-if="isUserVolume || isWritable" '+
                        'type="default" class="dropdown-toggle"><span class="caret"></span></btn>'+
                    '<template slot="dropdown">'+
                        '<li v-if="isUserVolume">'+
                            '<a :href="dashboardLink" target="_blank">Open&hellip;</a>'+
                        '</li>'+
                        '<li v-if="isWritable" @click="toggleWritability">'+
                            '<a role="button" v-if="isReadWrite">Use as read-only</a>'+
                            '<a role="button" v-else>Use as writable</a>'+
                        '</li>'+
                    '</template>'+
                '</dropdown>'+
            '</div>',
        props: ['item', 'selected', 'isReadWrite'],
        model: {
            prop: 'selected'
        },
        computed: {
            isWritable: function() {
                return _.includes(this.item.allowedActions, "write") || this.item.writable;
            },
            iconClass: function() {
                return {
                    'fa-check-square-o': this.selected,
                    'fa-square-o': !this.selected,
                }
            },
            pathInJob: function() {
                if (this.isUserVolume) {
                        return '/home/idies/workspace/' +
                        this.item.rootVolumeName + '/' +
                        this.item.owner + '/' +
                        this.item.name;
                } else {
                    return '/home/idies/workspace/' + this.item.name;
                }
            },
            dashboardLink: function() {
                return DASHBOARD_URL + '/files/' + this.item.id;
            },
            isUserVolume: function() {
                return this.item.rootVolumeName;
            }
        },
        methods: {
            clickItem: function() {
                this.$emit('input', this.item.id, this.isReadWrite);
            },
            toggleWritability: function() {
                this.$emit('toggleWrite', this.item.id, !this.isReadWrite);
            }
        }
    });

    Vue.component('volumes-list-group', {
        template: ''+
            '<div>'+
                '<input v-if="_.keys(items).length > 7" type="text" v-model="filter" class="form-control" placeholder="Filter By" :title="filterText">'+
                '<div class="list-group">'+
                    '<template v-if="recentList.length > 0 && !isFiltering">'+
                        '<p class="list-group-item text-center" :style="recentlyUsedTextCSS">'+
                            '<b>Recently used:</b>'+
                        '</p>'+
                        '<volume-list-group-item '+
                            'v-for="item in recentList" '+
                            ':key="item.id" '+
                            ':item="item" '+
                            ':selected="_.includes(selectedItemIds, item.id)" '+
                            ':style="recentlyUsedItemCSS(item.id)" '+
                            '@input="selectItem" '+
                            '@toggleWrite="toggleWrite(item.id)" '+
                            ':isReadWrite="_.includes(writableVolumeIds, item.id)"/>'+
                        '<hr class="checked-list-divider"/>'+
                    '</template>'+
                    '<volume-list-group-item'+
                        ' v-for="item in mainList" :item="item" :key="item.id"'+
                        ' :selected="_.includes(selectedItemIds, item.id)"'+
                        ' @input="selectItem" @toggleWrite="toggleWrite(item.id)" :isReadWrite="_.includes(writableVolumeIds, item.id)"/>'+
                '</div>'+
            '</div>',
        props: {
            items: {
                type: Object,
                required: true,
            },
            recentlyUsedIds: Array,
            filterText: String,
            selectedItemIds: Array,
            writableVolumeIds: Array,
        },
        model: {
            prop: 'selectedItemIds',
        },
        data: function() {
            return {
                filter: '',
            };
        },
        computed: {
            isFiltering: function() {
                return this.filter !== '';
            },
            filteredList: function() {
                const that = this;
                return _.values(this.items).filter(function (item) {
                    return item.name.toLowerCase().indexOf(that.filter.toLowerCase()) !== -1 ||
                        item.description.toLowerCase().indexOf(that.filter.toLowerCase()) !== -1;
                });
            },
            filteredRecentlyUsed: function() {
                if (!this.recentlyUsedIds) {
                    return;
                }
                const that = this;
                return _.uniq(this.recentlyUsedIds)
                    // items can be recently used, but not usable now
                    .filter(function (itemId) {
                        return that.items[itemId];
                    })
                    .map(function(itemId) {
                        return that.items[itemId];
                    });
            },
            mainList: function() {
                return _.differenceBy(this.filteredList, this.recentList, 'id').sort(volumeNameSorter);
            },
            recentList: function() {
                if (this.filteredRecentlyUsed && _.keys(this.items).length > 7 && !this.isFiltering) {
                    return this.filteredRecentlyUsed.slice(0, 3).sort(volumeNameSorter);
                } else {
                    return [];
                }
            },
            recentlyUsedTextCSS: function() {
                return {
                    borderTop: 'thin solid',
                    borderLeft: 'thin solid',
                    borderRight: 'thin solid',
                }
            },
        },
        methods: {
            selectItem: function (itemId) {
                this.$emit('input', _.xor(this.selectedItemIds, [itemId]));
            },
            toggleWrite: function(itemId) {
                this.$emit('toggleWrite', itemId);
            },
            recentlyUsedItemCSS: function(itemId) {
                return {
                    borderLeft: 'thin solid',
                    borderRight: 'thin solid',
                    borderBottom: itemId === this.recentList[this.recentList.length-1].id ?
                            'thin solid' : undefined,
                }
            },
        },
    });
    Vue.component('working-directory-selector', {
        template: ''+
            '<div class="form-group">'+
                '<label>Working Directory:</label>'+
                '<p v-if="chosenUserVolume && !_.includes(selectedUserVolumeIds, chosenUserVolume.id)" class="text-danger">'+
                    'Selected user volume must be chosen in the Files tab'+
                '</p>'+
                '<p class="help-block">'+
                    'Select a location to store standard input/output logs, and '+
                    'act as the current working directory for this job. Enable other '+
                    'writable user volumes on the Files tab to be able to use them here. '+
                    '<strong>Do not use relative paths in the {{ jobType }}.</strong>'+
                '</p>'+
                '<div class="checkbox">'+
                    '<label>'+
                        '<input type="checkbox" value="" v-model="useDefault">'+
                        'Use a new folder in the "jobs" Temporary volume'+
                        '<template v-if="!hasJobsVolume"> (will be created)</template>'+
                    '</label>'+
                '</div>'+
                '<vue-multiselect '+
                    'v-if="!useDefault" '+
                    'track-by="id" '+
                    ':searchable="false" '+
                    'deselect-label="" '+
                    'placeholder="Select a user volume (required)" '+
                    'v-model="chosenUserVolume" '+
                    ':allow-empty="false" '+
                    ':options="_.values(userVolumes)" '+
                    ':disabled="_.size(userVolumes)===0">' +

                    '<template slot="option" slot-scope="props">'+
                        '<h5 class="list-group-item-heading">'+
                            '{{ props.option.name }}'+
                            '<small><br/>{{ props.option.rootVolumeName }} Volume, created by {{ props.option.owner }}</small>'+
                        '</h5>'+
                        '<small class="list-group-item-text">{{ props.option.description }}</small>'+
                    '</template>' +
                    '<template slot="singleLabel" slot-scope="props">{{ props.option.name }}</template>'+
                '</vue-multiselect>' +
                '<input '+
                    'v-if="!useDefault" '+
                    'v-model="folderPath" '+
                    'type="text" '+
                    'class="form-control" '+
                    'placeholder="Path within user volume">' +
                '<ul v-if="displayedWorkingDirectory">'+
                    '<li>A copy of this {{ jobType }} will be placed in a unique, nested subfolder of <code>{{ displayedWorkingDirectory }}</code>.</li>'+
                    '<li>Relative paths will be resolved from this location.</li>'+
                '</ul>'+
            '</div>',
        data: function() {
            return {
                useDefault: true,
                folderPath: '',
                chosenUserVolume: undefined,
            };
        },
        props: [ 'domainId', 'value', 'selectedUserVolumeIds', 'jobType'],
        computed: _.extend({
            userVolumes: function() {
                if (!this.domainId) {
                    return {};
                }
                const vm = this;

                return _.filter(
                    this.$store.getters.getUserVolumesForDomainId(this.domainId),
                    function(uv) {
                            return _.includes(uv.allowedActions, 'write') &&
                                _.includes(vm.selectedUserVolumeIds, uv.id);
                    });
            },
            workingDirectory: function() {
                if (this.useDefault) {
                    return '';
                }
                if (!this.chosenUserVolume) {
                    return undefined;
                }
                return '/home/idies/workspace/' +
                    this.chosenUserVolume.rootVolumeName + '/' +
                    this.chosenUserVolume.owner + '/' +
                    this.chosenUserVolume.name + '/' +
                    this.folderPath;
            },
            displayedWorkingDirectory: function() {
                if (this.useDefault) {
                    return '/home/idies/workspace/Temporary/' + username + '/jobs/';
                } else {
                    return this.workingDirectory;
                }
            },
            hasJobsVolume: function() {
                if (!this.domainId) {
                    return;
                }
                return _.some(
                        this.$store.getters.getUserVolumesForDomainId(this.domainId),
                        {
                            name: 'jobs',
                            rootVolumeName: 'Temporary'
                        });
            }
        }),
        methods: {
            reset: function() {
                this.useDefault = true;
                this.folderPath = '';
                this.chosenUserVolume = undefined;
            }
        }
    });
    Vue.component('dask-selector', {
        template: ''+
            '<div class="container-fluid">'+
                '<form>'+
                    '<div class="row">'+
                        '<div class="form-group col-xs-5">'+
                            '<label>Domain</label>'+
                            '<select class="form-control" v-model="selectedDomainId">'+
                                '<option v-for="domain in domains" :value="domain.id">{{domain.name}}</option>'+
                            '</select>'+
                        '</div>'+
                    '</div>'+

                    '<div class="row">'+
                        '<div class="form-group col-xs-5">'+
                            '<label>Domain</label>'+
                            '<select class="form-control" v-model="selectedImageId">'+
                                '<option v-for="image in images" :value="image.id">{{image.name}}</option>'+
                            '</select>'+
                        '</div>'+
                    '</div>'+

                    '<div class="row">'+
                        '<div class="form-group col-xs-5">'+
                            '<label>No. of workers</label>'+
                            '<input class="form-control" placeholder="" type="text" :value="workers" />'+
                        '</div>'+
                    '</div>'+

                    '<div class="row">'+
                        '<div class="form-group col-xs-5">'+
                            '<label>Memory limit</label>'+
                            '<input class="form-control" placeholder="" type="text" :value="memory" />'+
                        '</div>'+
                    '</div>'+

                    '<div class="row">'+
                        '<div class="form-group col-xs-5">'+
                            '<label>No. of threads</label>'+
                            '<input class="form-control" placeholder="" type="text" :value="threads" />'+
                        '</div>'+
                    '</div>'+

                    '<div class="row">'+
                        '<div class="form-group col-xs-5">'+
                            '<label>Data volumes</label>'+
                            '<div class="checkbox" v-for="volume in publicVolumes">'+
                                '<label><input :checked="volume.selected" type="checkbox" :value="volume.id"/>{{volume.name}}</label>'+
                            '</div>'+
                        '</div>'+
                    '</div>'+
                '</form>'+
            '</div>',
        data: function() {
            return {
                selectedDomainId: undefined,
                selectedImageId: undefined,
                domains: [],
                images: [],
                publicVolumes: [],
                workers: daskDefaultWorkers,
                memory: daskDefaultMemory,
                threads: daskDefaultThreads
            };
        },
        mounted() {
            this.init();
        },
        methods: {
            init: function() {
                const that = this;
                $.get({
                    'url': 'ui/daskDomains',
                    'headers': { "X-Auth-Token": token}
                })
                .done(function(data) {
                    that.domains = data;
                    that.selectedDomainId = data[0].id;
                })
            },
            updateDomain: function(domainId) {
                const that = this;
                this.images = [];
                this.publicVolumes = [];
                $.get({
                    'url': 'ui/domains/' + domainId,
                    'headers': { "X-Auth-Token": token}
                })
                .done(function(data) {
                    that.images = data.images;
                    that.selectedImageId = data.images[0].id;
                    that.publicVolumes = data.publicVolumes;
                })
            }
        },
        watch: {
            selectedDomainId: function(value) {
                this.updateDomain(value)
            }
        }

    });
    Vue.component('job-modal', {
        template: ''+
            '<modal :backdrop="false" v-model="openned" :title="title" size="lg" :transition-duration="0">'+
                '<form slot="default">'+
                    '<tabs v-model="currentTab" @change="changeTab" :transition-duration="0">'+
                        '<tab :title="page1title + \'Compute Domain\'" :html-title="true"  :style="tabStyle">'+
                            '<div class="form-group">'+
                                '<label>Job Alias</label>'+
                                '<input v-model="jobAlias" class="form-control" placeholder="Optional Name">'+
                            '</div>'+
                            '<div class="form-group">'+
                                '<label>Compute Domain:</label>'+
                                '<p class="help-block">Select a Compute Domain to choose where this job is run. Different domains may have different computational or data resources available.</p>'+
                                '<p v-if="hasSwitchedOffFirstTab" class="text-danger">Selecting a different Compute Domain will reset this wizard.</p>'+
                                '<select-list-group :items="domainsToDisplay" :recently-used-ids="recentlyUsedDomains" v-model="selectedDomainId" filter-text="Filter Compute Domains By"/>'+
                            '</div>'+
                        '</tab>'+

                        '<tab :title="page2title" :disabled="!domainIsSelected" :html-title="true" :style="tabStyle">'+
                            '<div class="form-group">'+
                                '<label>Compute Image:</label>&nbsp;'+
                                    '<a rel="noopener" target="_blank" '+
                                    'href="http://www.sciserver.org/support/compute-images/" title="More Info on Compute Images">' +
                                    '<i aria-hidden="true" class="fa fa-question-circle"></i>'+
                                    '<span class="sr-only">More Info on Compute Images</span>'+
                                '</a>'+
                                '<p class="help-block">Select a Docker image providing the libraries and applications needed for this job.</p>'+
                                '<select-list-group :items="images" :recently-used-ids="recentlyUsedImages" v-model="selectedImageId" filter-text="Filter Compute Images By"></select-list-group>'+
                            '</div>'+
                        '</tab>'+

                        '<tab title="Data Volumes" :disabled="!domainIsSelected" :style="tabStyle">'+
                            '<div class="form-group">'+
                                '<label>Data Volumes:</label>&nbsp;'+
                                '<a rel="noopener" target="_blank" '+
                                    'href="http://www.sciserver.org/datasets/" title="More Info on Data Volumes">' +
                                    '<i aria-hidden="true" class="fa fa-question-circle"></i>'+
                                    '<span class="sr-only">More Info on Data Volumes</span>'+
                                '</a>'+
                                '<p class="help-block">Select any data volumes needed for this job.</p>'+
                                '<volumes-list-group :items="volumeContainers" :recently-used-ids="recentlyUsedVolumeContainers" v-model="selectedVolumeContainerIds" filter-text="Filter Data Volumes By" :writableVolumeIds="writableVolumeContainerIds" @toggleWrite="toggleVolumeContainerWrite"/>'+
                            '</div>'+
                        '</tab>'+

                        '<tab title="User Volumes" :disabled="!domainIsSelected" :style="tabStyle">'+
                            '<div class="form-group">'+
                                '<label>Personal and Shared Folders:</label>'+
                                '<p class="help-block">Select any user volumes to make avaliable to this job.</p>'+
                                '<volumes-list-group :items="userVolumes" :recently-used-ids="recentlyUsedUserVolumes" v-model="selectedUserVolumeIds" filter-text="Filter Personal and Shared Folders By" :writableVolumeIds="writableUserVolumeIds" @toggleWrite="toggleUserVolumeWrite" />'+
                            '</div>'+
                        '</tab>'+

                        '<tab v-if="isDaskAvailable" title="Dask" :disabled="!domainIsSelected" :style="tabStyle">'+
                            '<form>'+
                            '<div class="form-group ">'+
                                '<label><input type="checkbox" style="margin:0 6px 0 0" v-model="addDaskCluster">Add Dask cluster</label>'+
                            '</div>'+
                            '</form>'+
                            '<div v-if="addDaskCluster"><dask-selector /></div>'+
                        '</tab>'+

                        '<slot name="extra-tab" :style="tabStyle"></slot>'+
                    '</tabs>'+
                '</form>'+
                '<div slot="footer">'+
                    '<btn type="primary" :class="{disabled: currentTab === 0}" @click="prevTab($event)" aria-label="Previous Page">'+
                        '<span class="glyphicon glyphicon glyphicon glyphicon-chevron-left" aria-hidden="true"></span>'+
                    '</btn>'+
                    '<btn type="primary" :class="{disabled: !domainIsSelected || currentTab === JOB_MODAL_LAST_TAB}" @click="nextTab($event)" aria-label="Next Page">'+
                        '<span class="glyphicon glyphicon glyphicon glyphicon-chevron-right" aria-hidden="true"></span>'+
                    '</btn>'+
                    '<button type="button" class="btn btn-primary" :class="{disabled: !isSavable}" @click="saveJob" data-toggle="tooltip" data-placement="left">Create Job</button>'+
                '</div>'+
            '</modal>',
        data: function() {
            return {
                openned: false,
                currentTab: 0,
                hasSwitchedOffFirstTab: false,
                title: 'New Job',
                jobAlias: '',
                selectedDomainId: undefined,
                selectedImageId: undefined,
                selectedUserVolumeIds: [],
                writableUserVolumeIds: [],
                selectedVolumeContainerIds: [],
                writableVolumeContainerIds: [],
                selectedOutputUserVolume: 0,
                isDaskAvailable,
                addDaskCluster: false
            };
        },
        props: ['extraTabValid'],
        computed: _.extend({
            domainIsSelected: function() {
                return this.selectedDomainId !== undefined;
            },
            imageIsSelected: function() {
                return this.selectedImageId !== undefined;
            },
            selectedDomain: function() {
                if (this.selectedDomainId) {
                    return this.domains[this.selectedDomainId];
                }
            },
            JOB_MODAL_LAST_TAB: function () {
                return 4;
            },
            tabStyle: function() {
                return {
                    maxHeight: '600px',
                    overflowY: 'auto',
                };
            },
            page1title: function() {
                if (this.domainIsSelected) {
                    return '';
                }
                return '<span class="glyphicon glyphicon-exclamation-sign text-danger" aria-hidden="true"></span>'+
                    '<span class="sr-only">Required:</span> ';
            },
            page2title: function() {
                if (this.imageIsSelected || !this.domainIsSelected) {
                    return 'Compute Image';
                }
                return '<span class="glyphicon glyphicon-exclamation-sign text-danger" aria-hidden="true"></span>'+
                    '<span class="sr-only">Required:</span> '+
                    'Compute Image';
            },
            domainsToDisplay: function() {
                return _.pickBy(this.domains, function(domain) {
                    return domain.images.length > 0;
                });
            },
            images: function() {
                if (!this.domainIsSelected) {
                    return {};
                }
                return this.$store.getters.getImagesForDomainId(this.selectedDomainId);
            },
            userVolumes: function() {
                if (!this.domainIsSelected) {
                    return {};
                }
                return this.$store.getters.getUserVolumesForDomainId(this.selectedDomainId);
            },
            volumeContainers: function() {
                if (!this.domainIsSelected) {
                    return {};
                }
                return this.$store.getters.getVolumeContainersForDomainId(this.selectedDomainId);
            },
            recentlyUsedImages: function() {
                if (!this.domainIsSelected) {
                    return [];
                }
                return this.$store.getters.recentlyUsedImagesByDomain(this.selectedDomain);
            },
            recentlyUsedUserVolumes: function() {
                if (!this.domainIsSelected) {
                    return [];
                }
                return this.$store.getters.recentlyUsedUserVolumesByDomain(this.selectedDomain);
            },
            recentlyUsedVolumeContainers: function() {
                return [];
            },
            isSavable: function() {
                return this.domainIsSelected && this.imageIsSelected && this.extraTabValid;
            },
            recentlyUsedDomains: function() {
                return this.$store.getters.recentlyUsedDomains;
            },
        }, Vuex.mapState(['domains', 'recentDomains'])),
        methods: {
            newJob: function() {
                this.selectedDomainId = undefined;
                this.currentTab = 0;
                this.hasSwitchedOffFirstTab = false;
                this.openned = true;
            },
            prevTab: function(event) {
                if ($(event.target).closest('button').hasClass("disabled")) {
                    return;
                }
                this.currentTab--;
            },
            nextTab: function(event) {
                if ($(event.target).closest('button').hasClass("disabled")) {
                    return;
                }
                this.currentTab++;
            },
            changeTab: function(index) {
                if (index !== 0) {
                    this.hasSwitchedOffFirstTab = true;
                }
                if (index === this.JOB_MODAL_LAST_TAB) {
                    this.$emit('change');
                }
            },
            toggleUserVolumeWrite: function (userVolumeId) {
                this.writableUserVolumeIds = _.xor(this.writableUserVolumeIds, [userVolumeId]);
            },
            toggleVolumeContainerWrite: function (volumeContainerId) {
                this.writableVolumeContainerIds = _.xor(this.writableVolumeContainerIds, [volumeContainerId]);
            },
            saveJob: function(event) {
                if ($(event.target).closest('button').hasClass("disabled")) {
                    return;
                }
                const that = this;
                this.$emit('saveJob', {
                    userVolumes: _.map(_.pick(this.$store.state.userVolumes, this.selectedUserVolumeIds), function(uv) {
                        return {
                            userVolumeId: uv.id,
                            needsWriteAccess: _.includes(that.writableUserVolumeIds, uv.id),
                        };
                    }),
                    volumeContainers: _.map(_.pick(this.$store.state.volumeContainers, this.selectedVolumeContainerIds), function(vc) {
                        return {
                            name: vc.name,
                            writable: _.includes(that.writableVolumeContainerIds, vc.id),
                        };
                    }),
                    jobAlias: this.jobAlias,
                    imageName: this.$store.getters.getImage(this.selectedImageId).name,
                    apiEndpoint: this.selectedDomain.apiEndpoint,
                });
                this.openned = false;
            }
        },
        watch: {
            selectedDomainId: function(newDomainId) {
                this.selectedImageId = undefined;
                this.hasSwitchedOffFirstTab = false;

                if (newDomainId !== undefined) {
                    /* Select any default volumes */
                    this.selectedUserVolumeIds = _.filter(
                            this.$store.getters.getUserVolumesForDomainId(this.selectedDomainId),
                            function(uv) {
                                return /(persistent|scratch)\/?$/.test(uv.fullPath);
                    }).map(_.property('id'));

                    /* select all volumes as writable by default */
                    this.writableUserVolumeIds = _.filter(
                            this.$store.getters.getUserVolumesForDomainId(this.selectedDomainId),
                            function(uv) {
                                return _.includes(uv.allowedActions, 'write');
                    }).map(_.property('id'));

                    this.$emit('selectedDomain', this.selectedDomainId);
                }
            },
            selectedUserVolumeIds: function(newUserVolumes) {
                this.$emit('selectedUserVolumeIds', newUserVolumes);
            }
        },
    });

    const notebookWizard = new Vue({
        el: "#create-job-notebook-wizard",
        store: store,
        template: ''+
            '<job-modal ref="modal" @selectedDomain="onSelectDomain" @saveJob="saveJob" :extra-tab-valid="isValid" @selectedUserVolumeIds="mountedVolumes">'+
                '<tab slot="extra-tab" :title="notebookPageTitle + \'Notebook\'" :html-title="true" :disabled="selectedDomainId === undefined">'+
                    '<div class="form-group">'+
                        '<label class="control-label">Select a Jupyter Notebook:</label>'+
                        '<fileservice-picker :selectedPath="selectedNotebookPath" :selectedUserVolumeId="selectedNotebookUserVolumeId" @selectFile="onSelection" :domain-id="selectedDomainId" />'+
                    '</div>'+
                    '<div class="form-group">'+
                        '<label class="control-label">Parameters:</label>'+
                        '<input type="text" class="form-control" placeholder="Optional Parameters" v-model="notebookParameters">'+
                        '<span class="help-block">Parameters are placed in a <code>parameters.txt</code> file in the same directory as the notebook.</span>'+
                    '</div>'+
                    '<working-directory-selector '+
                        'ref="workingDirectorySelector" '+
                        'job-type="notebook" '+
                        ':domainId="selectedDomainId" '+
                        ':selected-user-volume-ids="selectedUserVolumes" />'+
                '</tab>'+
            '</job-modal>',
        data: {
            selectedDomainId: undefined,
            selectedNotebookPath: '',
            selectedNotebookUserVolumeId: undefined,
            notebookParameters: '',
            selectedUserVolumes: undefined,
        },
        computed: _.extend({
            notebookPageTitle: function() {
                if (this.isValid || this.selectedDomainId === undefined) {
                    return '';
                }
                return '<span class="glyphicon glyphicon-exclamation-sign text-danger" aria-hidden="true"></span>'+
                    '<span class="sr-only">Required:</span> ';
            },
            isValid: function() {
                return this.selectedNotebookPath.substr(-6) === ".ipynb" &&
                    this.$refs.workingDirectorySelector.workingDirectory !== undefined;
            },
        }, Vuex.mapGetters(['getUserVolumesForDomainId'])),
        methods: {
            newJob: function() {
                this.selectedDomainId = undefined;
                this.$refs.workingDirectorySelector.reset();
                this.selectedNotebookPath = '';
                this.notebookParameters = '';
                this.$refs.modal.newJob();
            },
            onSelectDomain: function(domainId) {
                this.selectedDomainId = domainId;
                for (const uv of _.values(this.getUserVolumesForDomainId(domainId)))
                    store.dispatch('loadJsonTree', uv);
            },
            onSelection: function(path, userVolumeId) {
                this.selectedNotebookPath = path;
                this.selectedNotebookUserVolumeId = userVolumeId;
            },
            mountedVolumes: function(volumes) {
                this.selectedUserVolumes = volumes;
            },
            saveJob: function(info) {
                saveJob({
                    command: this.notebookParameters,
                    resultsFolderURI: this.$refs.workingDirectorySelector.workingDirectory,
                    scriptURI: "/home/idies/workspace/" + this.selectedNotebookPath,
                    dockerComputeEndpoint: info.apiEndpoint,
                    dockerImageName: info.imageName,
                    submitterDID: info.jobAlias,
                    volumeContainers: info.volumeContainers,
                    userVolumes: info.userVolumes,
                });
            }
        },
    });

    const commandWizard = new Vue({
        el: "#create-job-command-wizard",
        store: store,
        template: ''+
            '<job-modal ref="modal" @selectedDomain="onSelectDomain" @saveJob="saveJob" :extra-tab-valid="isValid" @selectedUserVolumeIds="onSelectVolumes">'+
                '<tab slot="extra-tab" :title="commandPageTitle + \'Command\'" :html-title="true" :disabled="selectedDomainId === undefined">'+
                    '<div class="form-group">'+
                        '<label class="control-label">Command:</label>'+
                        '<textarea '+
                            'class="form-control" '+
                            ':style="editorStyle" '+
                            'autocapitalize="none" '+
                            'spellcheck="false" '+
                            'wrap="off" '+
                            'v-model="content"></textarea>'+
                    '</div>'+
                    '<working-directory-selector '+
                        'ref="workingDirectorySelector" '+
                        'job-type="command" '+
                        ':domainId="selectedDomainId" '+
                        ':selected-user-volume-ids="selectedUserVolumeIds" />'+
                '</tab>'+
            '</job-modal>',
        data: {
            selectedDomainId: undefined,
            selectedUserVolumeIds: [],
            content: '',
        },
        computed: _.extend({
            commandPageTitle: function() {
                if (this.isValid || this.selectedDomainId === undefined) {
                    return '';
                }
                return '<span class="glyphicon glyphicon-exclamation-sign text-danger" aria-hidden="true"></span>'+
                    '<span class="sr-only">Required:</span> ';
            },
            editorStyle: function () {
                return {
                    height: '15ex',
                };
            },
            isValid: function() {
                return this.content !== '' &&
                    this.$refs.workingDirectorySelector.workingDirectory !== undefined;
            },
        }, Vuex.mapGetters(['getUserVolumesForDomainId'])),
        methods: {
            newJob: function() {
                this.selectedDomainId = undefined;
                this.$refs.workingDirectorySelector.reset();
                this.selectedUserVolumeIds = [];
                this.content = '';
                this.$refs.modal.newJob();
            },
            onSelectDomain: function(domainId) {
                this.selectedDomainId = domainId;
            },
            onSelectVolumes: function(volumeIds) {
                this.selectedUserVolumeIds = volumeIds;
            },
            saveJob: function(info) {
                saveJob({
                    command: this.content,
                    resultsFolderURI: this.$refs.workingDirectorySelector.workingDirectory,
                    dockerComputeEndpoint: info.apiEndpoint,
                    dockerImageName: info.imageName,
                    submitterDID: info.jobAlias,
                    volumeContainers: info.volumeContainers,
                    userVolumes: info.userVolumes,
                });
            }
        },
    });

    const activateRunButtons = function() {
        const buttons = $('#run-button-groups-holder').children('button');
        buttons.click(function() {
            const runType = $(this).data('runtype');
                switch (runType) {
                case 'command':
                    commandWizard.newJob();
                    break;
                case 'notebook':
                    notebookWizard.newJob();
                    break;
                }
        });
        buttons.removeClass('disabled');
    };

    const showJobDetail = function(data) {
        let output =
            '<div class="jobs-detail">';

        if (data.scriptURI) {
            output +=
                '<code class="pre-scrollable jobs-detail-heading">' +
                    "Notebook Path: " + escapeHtml(data.scriptURI) +
                '</code>';
        }
        if (data.command) {
            output +=
                '<code class="pre-scrollable jobs-detail-heading">' +
                    (data.scriptURI ? "Parameters: " : "Command: ") +
                    escapeHtml(data.command) +
                '</code>';
        }

        output +=
            "<span>Job ID: <strong>" + data.id + "</strong></span>" +
            "<span>Compute Image: <strong>"+escapeHtml(data.dockerImageName) + "</strong></span>" +
            "<span>Compute Domain: <strong>" + escapeHtml(getDomainForJob(data).name) + "</strong></span>";

        if (data.startTime || data.endTime) {
            output +=
                '</div><div class="jobs-detail">';
        }
        if (data.startTime) {
            output +=
                '<span>Started <strong>'+moment(data.startTime).format('llll') + '</strong></span>';
        }
        if (data.endTime) {
            output +=
                '<span>Finished <strong>'+ moment(data.endTime).format('llll')+ '</strong></span>';
        }

        const messages = _.uniq(_.map(
                _.filter(data.messages, function (x) {
                    return x.content && x.label !== 'STATUS';
                }), 'content'));
        if (messages.length > 0) {
            output += "<div class='messages'>";
            output += "Messages:";
            output += "<pre class='pre-scrollable' style='white-space: pre-wrap;'>";
            for (let message of messages) {
                output += escapeHtml(message) + "\n";
            }
            output += "</pre>";
            output += "</div>";
        }
        if (data.resultsFolderURI) {
            output += '</div><div class="jobs-detail">Results were stored in ' + escapeHtml(data.resultsFolderURI);
        }
        const match = PATH_REGEX_PARSE.exec(data.resultsFolderURI);
        const resultUserVolume = match ? _.find(store.state.userVolumes, {
            rootVolumeName: match[1],
            owner: match[2],
            name: match[3],
        }) : undefined;
        if (resultUserVolume) {
            const filesTabUrl = DASHBOARD_URL + "/files/uservolumes/" + resultUserVolume.id + "/" + match[4];
            const fileDownloadUrl = resultUserVolume.fileServiceAPIEndpoint + 'api/file/' + match[1] + '/' + match[2] + '/' + match[3] + '/' + match[4];
            output += '<div class="btn-group btn-group-justified" role="group">';
            output += '<a target="_blank" class="btn btn-link" href="' + escapeHtml(filesTabUrl) + '">Browse Working Directory</a>';
            output += '<div class="btn-group" role="group"><a class="fileservice-link btn btn-link" data-url="' + escapeHtml(fileDownloadUrl) + '/stdout.txt">Download Standard Output</a></div>';
            output += '<div class="btn-group" role="group"><a class="fileservice-link btn btn-link" data-url="' + escapeHtml(fileDownloadUrl) + '/stderr.txt">Download Standard Error</a></div>';
            output += '</div>';
        }

        output +=
            '</div>';

        return output;
    };

    jobsTable.on('click', '.fileservice-link', function(e) {
        const oReq = new XMLHttpRequest();
        const url = $(this).data('url');
        // https://stackoverflow.com/a/15270931
        const filename = url.split(/[\\/]/).pop();

        oReq.addEventListener('load', function() {
            if (this.status === 200) {
                saveAs(this.response, filename);
            } else {
                showAlert("<p>Unable to download "+filename+". Please email " +
                        "<a href='mailto:sciserver-helpdesk@jhu.edu'>sciserver-helpdesk@jhu.edu</a> for more assistence.</p>" +
                        "Information known: <pre>Status " + escapeHtml(this.status) + "\n" +
                        escapeHtml(this.statusText) + "</pre>");
            }
        });
        oReq.addEventListener('error', function() {
            showAlert("<p>Unable to download "+filename+". Please email " +
                        "<a href='mailto:sciserver-helpdesk@jhu.edu'>sciserver-helpdesk@jhu.edu</a> for more assistence.</p>");
        });
        oReq.open('GET', url);
        oReq.setRequestHeader("X-Auth-Token", token);
        oReq.responseType = 'blob';
        oReq.send();
        return false;
    });

    jobsTable.on('click', '.job-cancel-button', function(e) {
        const ele = $(this);
        if (ele.data('is-canceling')) {
            return;
        }
        ele.addClass('disabled');
        ele.data('is-canceling', true);

        const tr = $(this).closest('tr');
        const row = table.row(tr);
        const data = row.data();

        $.post({
            url: JOBS_URL + '/' + data.id + '/cancel',
            'headers': { "X-Auth-Token": token}
        }).done(function() {
            $.toast({
                text: 'Job canceled',
                position: 'bottom-center'
            });
            refreshTableNow();
        }).fail(function(jqXHR, textStatus, errorThrown) {
            showAlert("<p>Unable to canel this job. It may have already been queued on a compute server. Please email " +
                        "<a href='mailto:sciserver-helpdesk@jhu.edu'>sciserver-helpdesk@jhu.edu</a> for more assistence.</p>" +
                        "The error message is: <pre>" + escapeHtml(errorThrown) + "\n" +
                        escapeHtml(jqXHR.responseText) + "</pre>");
        })
    });

    const table = jobsTable.DataTable({
        'processing': true,
        'ajax': function (data, callback, settings) {
            $.when(
                $.get({
                    'url': QUERY_JOBS_URL,
                    'headers': { "X-Auth-Token": token}
                }), $.get({
                    'url': DOMAINS_URL,
                    'headers': { "X-Auth-Token": token}
                })
            ).done(function (allJobs, domains) {
                const jobs = allJobs[0].filter(function(job) {return job.type === "jobm.model.COMPMDockerJobModel";});
                sessionStorage.setItem("BatchComputeDomains", JSON.stringify(domains[0]));
                store.commit("setDomains", domains[0]);
                displayWarningIfNoUserVolumes(domains[0]);
                store.commit('setJobs', jobs);

                $('#jobsTable_wrapper').toggleClass("hidden", jobs.length === 0);
                $('#no-jobs-message').toggleClass('hidden', jobs.length !== 0);
                activateRunButtons();
                callback({'data':jobs});
            });
        },
        "order": [[1, 'desc']],
        'conditionalPaging': true,
        'language': {
            'search': 'Search Name/Command/Notebook',
            'zeroRecords': 'No jobs found'
        },
        'autoWidth': false,
        'columns': [
            {
                'render': function (data, type, row) {
                    if (row.status === 64) {
                        return ERROR_ICON;
                    }
                    return '';
                },
                'className': 'text-center'
            },
            {
                'data': 'submissionTime',
                'title': 'Submission Time',
                'render': function (data, type, row) {
                    if (type === 'sort') {
                        return data;
                    }
                    return moment(data).format('llll');
                },
                'type': 'numeric',
                'orderable': true
            },
            {
                'title': 'Job',
                'render': function (data, type, row) {
                    if (type === 'filter') {
                        return (row.submitterDID || "") +
                            " " + (row.command || "") +
                            " " + (row.scriptURI || "");
                    }
                    let output = "";
                    if (row.submitterDID) {
                        output += escapeHtml(row.submitterDID) + "<br/>";
                    }
                    if (row.scriptURI) {
                        output += "<code>" + slimNotebookURI(row.scriptURI) + "</code>";
                    } else if (row.command) {
                        output += "<code>" + escapeHtml(row.command) + "</code>";
                    }

                    return output;
                },
                'className': 'job-descript-cell',
                'searchable': true
            },
            {
                'title': 'Status',
                'render': function (data, type, row) {
                    switch(row.status) {
                    case 1:
                        return "Preparing to submit job";
                    case 2:
                        return "Submitted, not started";
                    case 4:
                        return "In job queue, not started";
                    case 8:
                        return "Running since " + moment(row.startTime).fromNow();
                    case 16:
                        return "Almost complete, running since " + moment(row.startTime).fromNow();
                    case 32:
                        return "Completed after "
                            + moment.duration(row.duration*1000).humanize();
                    case 64:
                        return "Ended Unsuccessfully" +
                            (row.duration ? " after " + moment.duration(row.duration*1000).humanize() : "");
                    case 128:
                        return "Canceled";
                    }
                },
                'orderable': false,
                'className': 'status-column',
            },
            {
                'defaultContent': ACTIONS,
                'className': 'action-column',
                'render': function(data, type, row) {
                    if (row.status < 16) {
                        return '' +
                            '<button type="button" class="btn btn-danger btn-sm job-cancel-button" aria-label="Cancel">' +
                            ' <span class="glyphicon glyphicon-remove" aria-hidden="true"></span>' +
                            '</button>' +
                            ACTIONS
                    } else {
                        return ACTIONS;
                    }
                }
            }
        ],
        'columnDefs': [
            {
                'data': null,
                'searchable': false,
                'orderable': false,
                'targets': '_all',
            }
        ],
        'createdRow': function (row, data, index) {
            $(row).addClass(STATUS_TO_CLASSES[data.status]);
            if (data.status < 8) {
                $(row).addClass('not-started-job');
            }
        },
        'initComplete': function(settings, json) {
            setupRefreshTimerSpacing();
        }
    });

    let opennedRows = [];
    const showChildRow = function(row, tr) {
        const data = row.data();
        row.child(showJobDetail(data), 'child').show();
        tr.addClass('showing-job-detail-row');
        tr.find('button.more-info-btn > span')
            .toggleClass('glyphicon-triangle-bottom glyphicon-triangle-top');
        opennedRows = _.union(opennedRows, [data.id]);
    };
    const hideChildRow = function(row, tr) {
        const data = row.data();
        row.child.hide();
        tr.removeClass('showing-job-detail-row');
        tr.find('button.more-info-btn > span')
            .toggleClass('glyphicon-triangle-bottom glyphicon-triangle-top');
        _.pull(opennedRows, data.id);
    };
    jobsTable.find('tbody').on('click', 'tr', function(event) {
        const tr = $(this).closest('tr');
        const row = table.row(tr);

        // expected for child rows
        if (row.data() === undefined) {
            return;
        }
        //skip button in row
        if ($(event.target).closest('.job-cancel-button').length > 0) {
            return;
        }
        if (row.child.isShown()) {
            hideChildRow(row, tr);
        } else {
            showChildRow(row, tr);
        }
    });

    /* timer-related code */
    const setupRefreshTimerSpacing = function() {
        let timeout = 1 * 60 * 1000;
        const data = table.data();

        if (data.filter(function (x) { return x.status < 16; }).length > 0) {
            timeout = 10 * 1000;
        }
        if (data.filter(function (x) { return x.status < 8; }).length > 0) {
            timeout = 5 * 1000;
        }

        sessionStorage.setItem('jobsTableRefreshID', setTimeout(refreshTableNow, timeout));
    };

    const refreshTableNow = function() {
        clearTimeout(sessionStorage.getItem('jobsTableRefreshID'));
        table.ajax.reload(function() {
            table.rows().every(function (rowIdx, tableLoop, rowLoop) {
                if (_.includes(opennedRows, this.data().id)) {
                    showChildRow(this, $(this.node()));
                }
            });
            setupRefreshTimerSpacing();
        }, false);
    }
});
