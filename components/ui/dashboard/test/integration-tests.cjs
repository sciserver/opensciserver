const assert = require('assert');
const fs = require('fs');
const vm = require('vm');
const compiler = require('vue-template-compiler');
const source = fs.readFileSync('src/components/admin/IntegrationTests.vue', 'utf8');
const parsed = compiler.parseComponent(source);
assert.deepStrictEqual(compiler.compile(parsed.template.content).errors, []);
assert(!source.includes('v-html'), 'Reports and logs must be rendered as escaped text');

(async () => {
  const calls = [];
  const sandbox = {
    module: { exports: {} },
    transport: async request => { calls.push(request); return { data: [] }; },
    DASHBOARD_CONTEXT_PATH: '/test/dashboard/',
    setTimeout, clearTimeout,
  };
  vm.runInNewContext(parsed.script.content.replace("import axios from 'axios';", 'const axios = transport;')
    .replace('export default', 'module.exports ='), sandbox);
  const component = sandbox.module.exports;
  const context = { $store: { state: { token: 'current-session' } } };
  await component.methods.request.call(context, 'get');
  assert.strictEqual(calls[0].url, '/test/test-service/api/test-runs');
  assert.strictEqual(calls[0].headers['X-Auth-Token'], 'current-session');
  sandbox.DASHBOARD_CONTEXT_PATH = '//foreign.example/dashboard';
  await assert.rejects(component.methods.request.call(context, 'get'));
  assert.strictEqual(calls.length, 1);

  let complete;
  const stale = {
    generation: 0, current: null,
    request: () => new Promise(resolve => { complete = resolve; }),
  };
  const pending = component.methods.poll.call(stale, 'run', 0);
  component.methods.stop.call(stale);
  complete({ state: 'finished', cleanup: 'deleted' });
  await pending;
  assert.strictEqual(stale.current, null, 'Navigation must ignore an in-flight polling response');
  console.log('Integration UI template, routing, session header and polling checks passed.');
})().catch(error => { console.error(error); process.exitCode = 1; });
