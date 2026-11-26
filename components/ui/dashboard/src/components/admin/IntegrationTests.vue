<template>
  <div class="container-fluid integration-tests">
    <h2>Integration Tests</h2>
    <div v-if="!isAdmin" class="alert alert-info">Administrator access is required.</div>
    <template v-else>
      <p>Run checks against this SciServer deployment. Tests can create users, files, groups and compute sessions.</p>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <div class="test-actions">
        <button class="btn btn-default" :disabled="submitting || working" @click="start('catalog', null)">
          Load test catalog
        </button>
        <button class="btn btn-default" :disabled="!selected.length || submitting || working" @click="start('plan', selected)">
          Preview prerequisites
        </button>
        <button class="btn btn-primary" :disabled="!selected.length || submitting || working" @click="start('run', selected)">
          Run selected tests
        </button>
        <button class="btn btn-primary" :disabled="submitting || working" @click="start('run', null)">Run all tests</button>
      </div>
      <div v-if="catalog.length" class="panel panel-default">
        <div class="panel-heading">Available tests</div>
        <div class="panel-body">
          <div v-for="test in catalog" :key="test.id" class="checkbox">
            <label><input v-model="selected" type="checkbox" :value="test.id" :disabled="working || submitting">
              {{ test.name }} <small class="text-muted">{{ test.id }}</small>
            </label>
            <p>{{ test.description }}
              <span v-if="test.dependsOn.length">Requires: {{ test.dependsOn.join(', ') }}.</span>
            </p>
          </div>
        </div>
      </div>
      <div v-if="current" class="panel panel-default" aria-live="polite">
        <div class="panel-heading">
          {{ current.kind }}: {{ current.state }}
          <button v-if="working && current.state !== 'cancelling'" class="btn btn-warning btn-xs pull-right"
                  :disabled="submitting" @click="cancel">Cancel</button>
        </div>
        <div class="panel-body">
          <p>Run {{ current.id }} <span v-if="current.requestedBy">requested by {{ current.requestedBy }}</span></p>
          <button class="btn btn-default btn-xs" :disabled="submitting" @click="follow(current)">Refresh operation</button>
          <p v-if="working">Waiting for results…</p>
          <p v-if="current.reportDelivery && current.reportDelivery !== 'not_applicable'">
            Report delivery: {{ current.reportDelivery }}
          </p>
          <p v-if="logsError" class="text-muted">{{ logsError }}</p>
          <p v-if="resultError" class="alert alert-warning">{{ resultError }}</p>
          <p v-if="result && result.outcome"><strong>Test outcome: {{ result.outcome }}</strong></p>
          <p v-if="result && result.counts"><span v-for="(count, status) in result.counts" :key="status">
            {{ status }}: {{ count }} &nbsp;
          </span></p>
          <p v-if="result && result.runnerError" class="alert alert-danger">{{ result.runnerError }}</p>
          <div v-if="result && result.tests && current.kind !== 'catalog'" class="table-responsive">
            <table class="table table-striped">
              <thead><tr><th>Test</th><th>Selection</th><th>Result</th><th>Details</th></tr></thead>
              <tbody>
                <tr v-for="test in result.tests" :key="test.id">
                  <td>{{ test.name }}<br><small>{{ test.id }}</small></td>
                  <td>{{ test.selected ? 'Selected' : 'Prerequisite' }}</td>
                  <td>{{ test.status || 'Planned' }}
                    <span v-if="test.durationSeconds !== undefined">({{ test.durationSeconds.toFixed(2) }}s)</span>
                  </td>
                  <td><span v-if="test.blockedBy && test.blockedBy.length">Blocked by {{ test.blockedBy.join(', ') }}</span>
                    <pre v-if="test.detail">{{ test.detail }}</pre>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <details v-if="current.conditions && current.conditions.length">
            <summary>Execution conditions</summary><pre>{{ JSON.stringify(current.conditions, null, 2) }}</pre>
          </details>
          <details v-if="logs"><summary>Logs</summary><pre>{{ logs }}</pre></details>
        </div>
      </div>
      <h3>Recent operations
        <button class="btn btn-default btn-sm" :disabled="refreshing" @click="refreshRuns">Refresh</button>
      </h3>
      <p v-if="!runs.length">No operations loaded.</p>
      <div class="table-responsive">
        <table v-if="runs.length" class="table table-striped">
          <thead><tr><th>Operation</th><th>State</th><th>Requested by</th><th>Created</th></tr></thead>
          <tbody><tr v-for="run in runs" :key="run.id">
            <td><button class="btn btn-link" :disabled="submitting" @click="follow(run)">{{ run.kind }}: {{ run.id }}</button></td>
            <td>{{ run.state }}</td><td>{{ run.requestedBy }}</td><td>{{ run.createdAt }}</td>
          </tr></tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script>
import { createTestApi, errorMessage, isTerminal } from '../../integration-tests';

export default {
  name: 'IntegrationTests',
  data: () => ({
    catalog: [],
    selected: [],
    runs: [],
    current: null,
    result: null,
    logs: '',
    logsError: '',
    error: '',
    resultError: '',
    submitting: false,
    refreshing: false,
    generation: 0,
    timer: null,
  }),
  computed: {
    isAdmin() { return this.$store.getters.isAdmin; },
    working() { return this.current && !isTerminal(this.current); },
  },
  watch: {
    isAdmin: {
      immediate: true,
      handler(admin) {
        if (admin) this.refreshRuns();
        else this.stopPolling();
      },
    },
  },
  beforeDestroy() { this.stopPolling(); },
  methods: {
    api() { return createTestApi(DASHBOARD_CONTEXT_PATH, () => this.$store.state.token); },
    stopPolling() {
      this.generation += 1;
      clearTimeout(this.timer);
    },
    async refreshRuns() {
      if (!this.isAdmin) return;
      this.refreshing = true;
      try {
        const runs = await this.api().list();
        this.runs = runs.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''));
        this.error = '';
      } catch (error) {
        this.error = errorMessage(error);
      } finally {
        this.refreshing = false;
      }
    },
    async start(kind, tests) {
      if (!this.isAdmin || this.submitting) return;
      this.submitting = true;
      this.error = '';
      const generation = this.generation;
      try {
        const run = await this.api().create(kind, tests);
        if (generation !== this.generation) return;
        this.follow(run);
        this.refreshRuns();
      } catch (error) {
        this.error = errorMessage(error);
      } finally {
        this.submitting = false;
      }
    },
    follow(run) {
      this.stopPolling();
      this.current = run;
      this.result = null;
      this.logs = '';
      this.logsError = '';
      this.error = '';
      this.resultError = '';
      this.poll(run.id, this.generation);
    },
    async poll(id, generation) {
      if (generation !== this.generation || !this.isAdmin) return;
      try {
        const run = await this.api().get(id);
        if (generation !== this.generation) return;
        this.current = run;
        try {
          const logs = await this.api().logs(id);
          if (generation === this.generation) {
            this.logs = logs;
            this.logsError = '';
          }
        } catch (error) {
          if (generation !== this.generation) return;
          const status = error.response && error.response.status;
          if (status === 401 || status === 403) throw error;
          this.logsError = 'Logs are not available yet, or the runner pod is no longer available.';
        }
        if (generation !== this.generation) return;
        if (isTerminal(run)) {
          try {
            const result = await this.api().result(id);
            if (generation !== this.generation) return;
            this.result = result;
            if (run.kind === 'catalog') {
              this.catalog = result.tests;
              this.selected = this.selected.filter(test => this.catalog.some(item => item.id === test));
            }
          } catch (error) {
            if (generation !== this.generation) return;
            this.resultError = error.response && error.response.status === 409 ?
              'No report is retained. Check delivery, logs and execution conditions. ' +
              'Reports may be lost on service restart or expiry.' : errorMessage(error);
          }
          this.refreshRuns();
          return;
        }
        this.timer = setTimeout(() => this.poll(id, generation), 3000);
      } catch (error) {
        if (generation === this.generation) this.error = errorMessage(error);
      }
    },
    async cancel() {
      if (!this.current || this.submitting) return;
      const id = this.current.id;
      const generation = this.generation;
      this.submitting = true;
      try {
        const run = await this.api().cancel(id);
        if (generation === this.generation) this.follow(run);
      } catch (error) {
        this.error = errorMessage(error);
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>

<style scoped>
.integration-tests { padding-bottom: 2em; }
.test-actions { display: flex; flex-wrap: wrap; gap: 0.5em; margin: 1em 0; }
pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 25em; overflow-y: auto; }
.checkbox p { margin-left: 20px; }
</style>
