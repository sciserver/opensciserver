<template>
  <div class="container-fluid">
    <h2>Integration Tests</h2>
    <div v-if="!isAdmin" class="alert alert-info">Administrator access is required.</div>
    <template v-else>
      <p>Run the account creation/deletion check, or the full integration suite. Tests modify this deployment.</p>
      <p>The account check requires RACM cleanup enabled. The full suite also needs fileservice and compute fixtures.</p>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <button class="btn btn-primary" :disabled="busy" @click="start('account-lifecycle')">Run account lifecycle</button>
      <button class="btn btn-default" :disabled="busy" @click="start('full')">Run full suite</button>
      <button class="btn btn-default" :disabled="busy" @click="refresh">Refresh runs</button>
      <ul>
        <li v-for="run in runs" :key="run.id">
          <a href="#" @click.prevent="open(run.id)">{{ run.createdAt }} — {{ run.suite }}</a> ({{ run.state }})
        </li>
      </ul>
      <section v-if="current">
        <h3>Run {{ current.id }}</h3>
        <p>Execution: {{ current.state }}; exit code: {{ current.exitCode == null ? 'pending' : current.exitCode }};</p>
        <p>Account cleanup: {{ current.cleanup }}; report: {{ current.reportStatus }}</p>
        <button v-if="!terminal" class="btn btn-warning" :disabled="busy" @click="cancel">Cancel</button>
        <div v-if="current.reportStatus !== 'pending' && current.reportStatus !== 'available'" class="alert alert-warning">
          A valid report is unavailable. Inspect pod status and logs; this is not a passing result.
        </div>
        <template v-if="current.report">
          <h4>Outcome: {{ current.report.outcome }}</h4>
          <p v-if="current.report.runnerError" class="alert alert-danger">{{ current.report.runnerError }}</p>
          <table class="table table-striped">
            <thead><tr><th>Test</th><th>Result</th><th>Seconds</th><th>Details</th></tr></thead>
            <tbody>
              <tr v-for="test in current.report.tests" :key="test.id">
                <td>{{ test.name }}</td><td>{{ test.status }}</td>
                <td>{{ test.durationSeconds.toFixed(3) }}</td><td><pre v-if="test.detail">{{ test.detail }}</pre></td>
              </tr>
            </tbody>
          </table>
        </template>
        <details><summary>Pod status</summary><pre>{{ current.podStatus || current.conditions }}</pre></details>
        <details><summary>Raw logs</summary><pre>{{ current.logs }}</pre></details>
      </section>
    </template>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'IntegrationTests',
  data: () => ({ runs: [], current: null, busy: false, error: '', timer: null, generation: 0 }),
  computed: {
    isAdmin() { return this.$store.getters.isAdmin; },
    terminal() { return this.current && ['finished', 'cancelled'].indexOf(this.current.state) !== -1; },
  },
  watch: {
    isAdmin: {
      immediate: true,
      handler(admin) {
        if (admin) this.refresh();
        else this.stop();
      },
    },
  },
  beforeDestroy() { this.stop(); },
  methods: {
    stop() { this.generation += 1; clearTimeout(this.timer); },
    async request(method, suffix = '', data) {
      if (!/^\/(?!\/)(?:[^?#]*\/)?dashboard\/?$/.test(DASHBOARD_CONTEXT_PATH)) {
        throw new Error('Cannot determine the test-service path.');
      }
      const root = DASHBOARD_CONTEXT_PATH.replace(/\/dashboard\/?$/, '/test-service/api/test-runs');
      const response = await axios({
        method,
        url: root + suffix,
        data,
        timeout: 25000,
        headers: { 'X-Auth-Token': this.$store.state.token },
      });
      return response.data;
    },
    explain(error) {
      const status = error.response && error.response.status;
      if (status === 401) return 'Session expired. Sign in again.';
      if (status === 403) return 'Administrator access is required.';
      if (status === 409) return 'A run is active, or the image/RACM cleanup is not configured.';
      return status ? `Test-service request failed (HTTP ${status}).` : 'Could not connect to test-service.';
    },
    async refresh() {
      if (!this.isAdmin) return;
      const generation = this.generation;
      try {
        const runs = await this.request('get');
        if (generation === this.generation) { this.runs = runs; this.error = ''; }
      } catch (error) {
        if (generation === this.generation) this.error = this.explain(error);
      }
    },
    async start(suite) {
      this.busy = true;
      const generation = this.generation;
      try {
        const run = await this.request('post', '', { suite });
        if (generation === this.generation) { this.open(run.id); this.refresh(); }
      } catch (error) {
        if (generation === this.generation) this.error = this.explain(error);
      } finally { this.busy = false; }
    },
    open(id) { this.stop(); this.current = null; this.poll(id, this.generation); },
    async poll(id, generation) {
      try {
        const run = await this.request('get', `/${encodeURIComponent(id)}`);
        if (generation !== this.generation) return;
        this.current = run;
        this.error = '';
        if (['finished', 'cancelled'].indexOf(run.state) === -1 || run.cleanup !== 'deleted') {
          this.timer = setTimeout(() => this.poll(id, generation), 5000);
        } else this.refresh();
      } catch (error) {
        if (generation === this.generation) this.error = this.explain(error);
      }
    },
    async cancel() {
      this.busy = true;
      try {
        const id = this.current.id;
        await this.request('delete', `/${encodeURIComponent(id)}`);
        this.open(id);
      } catch (error) { this.error = this.explain(error); } finally { this.busy = false; }
    },
  },
};
</script>

<style scoped>
pre { white-space: pre-wrap; word-break: break-word; max-height: 24em; overflow: auto; }
button { margin-right: 0.5em; margin-bottom: 0.5em; }
</style>
