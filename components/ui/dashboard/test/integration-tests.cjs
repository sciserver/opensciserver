/* Run with: node test/integration-tests.cjs (after installing dashboard dev dependencies). */
const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');
const babel = require('babel-core');
// Vue 2.5 chooses MessageChannel on modern Node, leaving ports open after checks.
// Use its normal setTimeout fallback in this non-browser harness.
const messageChannel = global.MessageChannel;
global.MessageChannel = undefined;
const Vue = require('vue');
const compiler = require('vue-template-compiler');
global.MessageChannel = messageChannel;

const root = path.resolve(__dirname, '..');
const timers = [];
function load(source, dependencies) {
  const module = { exports: {} };
  const code = babel.transform(source, {
    babelrc: false,
    presets: [[require.resolve('babel-preset-env'), { targets: { node: '8' } }]],
  }).code;
  vm.runInNewContext(code, {
    module, exports: module.exports, require: name => dependencies[name],
    DASHBOARD_CONTEXT_PATH: '/sciserver/dashboard/',
    setTimeout: callback => { timers.push(callback); return timers.length; },
    clearTimeout: () => {},
  });
  return module.exports;
}
const api = load(fs.readFileSync(path.join(root, 'src/integration-tests.js'), 'utf8'), {
  axios: () => { throw new Error('No live HTTP allowed in checks'); },
});
const parsed = compiler.parseComponent(fs.readFileSync(
  path.join(root, 'src/components/admin/IntegrationTests.vue'), 'utf8'));
assert.deepStrictEqual(compiler.compile(parsed.template.content).errors, []);
const component = load(parsed.script.content, { '../../integration-tests': api }).default;
function page(fake) {
  return new Vue(Object.assign({}, component, {
    beforeCreate() { this.$store = { state: { token: 'session-token' }, getters: { isAdmin: true } }; },
    methods: Object.assign({}, component.methods, { api: () => fake }),
  }));
}
async function checks() {
  assert.strictEqual(api.testServiceBase('/dashboard/'), '/test-service/api');
  assert.strictEqual(api.testServiceBase('/sciserver/dashboard/'), '/sciserver/test-service/api');
  assert.throws(() => api.testServiceBase('https://untrusted.example/dashboard/'));
  assert.throws(() => api.testServiceBase('//untrusted.example/dashboard/'));
  const requests = [];
  let token = 'first';
  const client = api.createTestApi('/sciserver/dashboard/', () => token, async request => {
    requests.push(request);
    return { data: { id: 'operation' } };
  });
  await client.create('run', null);
  assert.strictEqual(JSON.stringify(requests[0].data), '{}');
  token = 'second';
  await client.create('plan', ['fileservice.upload']);
  assert.strictEqual(requests[1].url, '/sciserver/test-service/api/plans');
  assert.strictEqual(requests[1].headers['X-Auth-Token'], 'second');
  assert.strictEqual(requests[1].data.tests[0], 'fileservice.upload');
  assert.strictEqual(api.isTerminal({ state: 'cancelling' }), false);
  assert.strictEqual(api.isTerminal({ state: 'finished' }), true);

  // A failed test report is still displayed when its pod/logs no longer exist.
  const fake = {
    list: async () => [],
    get: async () => ({ id: 'run', kind: 'run', state: 'finished' }),
    logs: async () => { throw { response: { status: 404 } }; },
    result: async () => ({ outcome: 'failed', tests: [] }),
  };
  const first = page(fake);
  await first.poll('run', first.generation);
  assert.strictEqual(first.result.outcome, 'failed');
  assert(first.logsError);
  first.$destroy();

  // Switching operations or leaving the route discards stale responses.
  let resolve;
  const second = page(Object.assign({}, fake, { get: () => new Promise(done => { resolve = done; }) }));
  const pending = second.poll('old', second.generation);
  second.$destroy();
  resolve({ id: 'old', kind: 'run', state: 'running' });
  await pending;
  assert.strictEqual(second.current, null);

  // Leaving the route while a POST is pending must not start a background poll.
  let finish;
  const third = page(Object.assign({}, fake, { create: () => new Promise(done => { finish = done; }) }));
  const posting = third.start('run', null);
  third.$destroy();
  finish({ id: 'late', kind: 'run', state: 'running' });
  await posting;
  assert.strictEqual(third.current, null);

  // Execution stopping without a report is explained without fabricating success.
  const fourth = page(Object.assign({}, fake, {
    result: async () => { throw { response: { status: 409 } }; },
  }));
  await fourth.poll('run', fourth.generation);
  assert.strictEqual(fourth.result, null);
  assert(fourth.resultError.includes('No report is retained'));
  fourth.$destroy();
  console.log('Dashboard integration-test checks passed (template, API, lifecycle and results).');
}
checks().catch(error => { console.error(error); process.exitCode = 1; });
