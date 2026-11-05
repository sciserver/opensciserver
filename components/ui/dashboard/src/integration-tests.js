import axios from 'axios';

// Keep requests on the dashboard origin and preserve the installation prefix.
export function testServiceBase(contextPath) {
  if (!/^\/(?!\/)(?:[^?#]*\/)?dashboard\/?$/.test(contextPath)) {
    throw new Error('Cannot determine the test service URL from the dashboard path.');
  }
  return contextPath.replace(/\/dashboard\/?$/, '/test-service/api');
}

export function isTerminal(run) {
  return ['finished', 'timed_out', 'cancelled'].indexOf(run.state) !== -1;
}

export function errorMessage(error) {
  const status = error.response && error.response.status;
  if (status === 401) return 'Your session has expired. Please sign in again.';
  if (status === 403) return 'Administrator access is required.';
  if (status === 404) return 'The test service is unavailable. It may not be enabled for this deployment.';
  if (status === 429) return 'The test service is busy. Wait for an active operation to finish.';
  return status ? `Test service request failed (HTTP ${status}).` : 'Could not reach the test service.';
}

export function createTestApi(contextPath, token, transport = axios) {
  const base = testServiceBase(contextPath);
  const request = async (method, path, data) => {
    const response = await transport({
      method,
      url: `${base}${path}`,
      headers: { 'X-Auth-Token': token() },
      timeout: 20000,
      data,
    });
    return response.data;
  };
  const runPath = id => `/runs/${encodeURIComponent(id)}`;
  return {
    list: () => request('get', '/runs'),
    create: (kind, tests) => request('post', { catalog: '/catalog', plan: '/plans', run: '/runs' }[kind],
      tests === null ? {} : { tests }),
    get: id => request('get', runPath(id)),
    result: id => request('get', `${runPath(id)}/result`),
    logs: id => request('get', `${runPath(id)}/logs`),
    cancel: id => request('delete', runPath(id)),
  };
}
