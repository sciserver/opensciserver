import Vue from 'vue';
import throttle from 'lodash/throttle';
import keyBy from 'lodash/keyBy';
import map from 'lodash/map';
import firstby from 'thenby';


/* All API calls that are throttled will
   wait at least this many milliseconds between calls */
const API_THROTTLE_TIME = 2000;

export default {
    state: {
        scienceDomains: [],
        scienceDomainsId: {},
    },
    mutations: {
        setScienceDomainsId(state, id) {
            state.scienceDomainsId = id;
        },
        setScienceDomains(state, domains) {
            state.scienceDomains = domains;
        },
    },
    actions: {
        loadScienceDomains: throttle(
            ({ commit, state, userProfile }) =>
                Vue.http.get(`${RACM_URL}/ugm/rest/publicgroups`)
                .then((response) => {
                    const checkScienceDomains = response.body.length;
                    if (checkScienceDomains === 0) {
                        commit('loadScienceDomainStatus', false);
                    } else {
                        commit('loadScienceDomainStatus', true);
                    }
                    const globalGroupsBody = response.body;
                    const myGlobalGroups = [];
                    let userRole;
                    let ownerId;
                    let numOfJoinedDomains = 0;
                    for (let i = 0; i < globalGroupsBody.length; i += 1) {
                        const members = [];
                        const formattedResources = globalGroupsBody[i].resources.map(
                            r => ({
                                entityId: r.id,
                                resourceId: r.id,
                                name: r.name,
                                resourceType: r.resourceTypeName,
                                actions: r.actions,
                                resourceContext: {
                                    contextClassName: r.contextClassName,
                                    description: r.resourceContextUUID,
                                    name: r.resourceTypeName,
                                },
                            }));
                        let _links = {};
                        for (let j = 0; j < globalGroupsBody[i].admins.length; j += 1) {
                            members.push({
                                status: globalGroupsBody[i].admins[j].role,
                                id: globalGroupsBody[i].admins[j].id,
                                role: globalGroupsBody[i].admins[j].role,
                            });
                            if (globalGroupsBody[i].admins[j].role === 'OWNER') {
                                ownerId = globalGroupsBody[i].admins[j].id;
                            }
                        }
                        if (userProfile && userProfile.id && ownerId === state.userProfile.id) {
                            userRole = 'OWNER';
                            _links = {
                                delete: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                editDescription: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                editMemberList: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                editName: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                forceAddMember: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                self: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                shareResource: {
                                    href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}/sharedResources?actions={actions}&resourceType={resourceType}&entityId={entityId}`,
                                    templated: true,
                                },
                            };
                        } else if (userProfile && userProfile.id && globalGroupsBody[i].userRole === 'ADMIN' && globalGroupsBody[i].userStatus === 'ACCEPTED') {
                            userRole = 'ADMIN';
                            _links = {
                                editDescription: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                editMemberList: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                editName: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                forceAddMember: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                self: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                                shareResource: {
                                    href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}/sharedResources?actions={actions}&resourceType={resourceType}&entityId={entityId}`,
                                    templated: true,
                                },
                            };
                        } else if (globalGroupsBody[i].userRole === 'MEMBER' && globalGroupsBody[i].userStatus === 'ACCEPTED') {
                            userRole = 'MEMBER';
                            _links = {
                                leave: { href: `${RACM_URL}/ugm/rest/groups/leave?groupId=${globalGroupsBody[i].id}` },
                                self: { href: `${RACM_URL}/ugm/rest/groups/${globalGroupsBody[i].id}` },
                            };
                        } else {
                            userRole = 'OTHER';
                            _links = {
                                join: { href: `${RACM_URL}ugm/rest/groups/join?groupId=${globalGroupsBody[i].id}` },
                                self: { href: `${RACM_URL}ugm/rest/groups/join?groupId=${globalGroupsBody[i].id}` },
                            };
                        }
                        if (_links.leave) {
                            numOfJoinedDomains += 1;
                        }
                        const description = globalGroupsBody[i].description.trim();
                        const titleIndex = description.match(/\[TITLE\]: # \(([^)]*)\)/);
                        if (titleIndex) {
                            globalGroupsBody[i].groupName = titleIndex[1];
                        }
                        myGlobalGroups.push({
                            groupId: globalGroupsBody[i].id,
                            members,
                            userRole,
                            _links,
                            description,
                            type: 'PUBLIC GROUP',
                            name: globalGroupsBody[i].groupName,
                            resources: formattedResources,
                        });
                    }
                    myGlobalGroups.sort(firstby('name'));
                    commit('setJoinedScienceDomains', numOfJoinedDomains);
                    commit('setTotalScienceDomains', globalGroupsBody.length);
                    commit('setScienceDomains', keyBy(myGlobalGroups, c => `${c.groupId}`));
                    commit('setScienceDomainsId', map(myGlobalGroups, r => r.groupId.toString()));
                }),
            API_THROTTLE_TIME, { trailing: false }),
    },
};
