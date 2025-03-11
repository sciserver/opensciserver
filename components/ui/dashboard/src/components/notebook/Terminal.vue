<template>
  <div>
    <iframe id="terminalFrame" class="terminalBlock" @load="terminalLoad" ref="terminalFrame" v-bind:src="computeTerminalUrl"></iframe>
    <div v-if="!ready" id="terminalCurtain" class="terminalBlock"> loading terminal... </div>
  </div>
</template>

<script>
import axios from 'axios';
import Cookies from 'js-cookie';
import notebookUtils from '../../notebook-utils';

const MAX_COMPUTE_RETRIES = 20; // after which we probably wont get anything back
const COMPUTE_POLL_MS = 3000;
const TERMINAL_POLL_MS = 300;

export default {
    name: 'Terminal',
    props: [
        'computeId',
    ],
    data: () => ({
        terminalId: null,
        baseUrl: null,
        poller: null,
        computePoller: null,
        ready: false,
        processing: false,
        computeRetries: MAX_COMPUTE_RETRIES,
    }),
    created() {
        this.computePoller = setInterval(() => this.getComputeBaseUrl(), COMPUTE_POLL_MS);
    },
    watch: {
        computeId() {
            this.baseUrl = null;
            this.terminalId = null;
            this.computeRetries = MAX_COMPUTE_RETRIES;
        },
    },
    computed: {
        computeTerminalUrl() {
            if (this.terminalId) {
                return `${COMPUTE_URL}/go?id=${this.computeId}&path=/terminals/${this.terminalId}`;
            }
            if (this.computeId && this.baseUrl) {
                const conf = { headers: { 'X-Auth-Token': this.$store.state.token,
                                          'X-XSRFToken': Cookies.get('_xsrf') } };
                axios.post(`${this.baseUrl}`, null, conf)
                    .then((response) => {
                        this.terminalId = response.data.name;
                    });
            }
            return null;
        },
    },
    methods: {
        // This call is for determining redirect URL compute assigns. In various stages of starting this can be
        // erroneously redirected to dashboard or come back with some intermediate error codes, so a single triggered
        // call would not suffice, we really need to poll and validate the redirected URL prior to use.
        getComputeBaseUrl() {
            if (this.computeId && !this.baseUrl && !this.processing && this.computeRetries > 0) {
                this.processing = true;
                this.computeRetries -= 1;
                const conf = { headers: { 'X-Auth-Token': this.$store.state.token } };
                axios.get(`${COMPUTE_URL}/go?id=${this.computeId}&path=/api/terminals`, conf)
                    .then((response) => {
                        const reqUrl = response.request.responseURL;
                        if (Array.isArray(response.data) && reqUrl.includes('/api/terminals')) {
                            if (this.baseUrl !== reqUrl) {
                                this.baseUrl = reqUrl;
                            }
                        }
                    })
                    .catch((err) => {
                        console.error('error querying terminals', err);
                    })
                    .finally(() => { this.processing = false; });
            }
        },
        terminalLoad() {
            console.log('terminal load called');
            this.ready = false;
            clearInterval(this.poller);
            this.poller = setInterval(() => this.poll(), TERMINAL_POLL_MS);
        },
        poll() {
            const term = this.$refs.terminalFrame.contentWindow.terminal;
            if (!term) {
                return;
            }
            // If we loose connection or the user terminates (e.g. C-D), setup to allocate another
            if (![term.socket.CONNECTING, term.socket.OPEN].includes(term.socket.readyState)) {
                this.terminalId = null;
                this.ready = false;
                return;
            }
            if (!this.ready) {
                try {
                    notebookUtils.jupyterClassicTerminalModifications(this.$refs.terminalFrame);
                    this.ready = true;
                } catch (err) {
                    console.error('failed terminal mods');
                }
            }
            // this is readjusted by jupyter at resizing, but we want to fill the space
            this.$refs.terminalFrame.contentDocument.getElementById('site').style.setProperty('height', '100%');
        },
    },
    beforeDestroy() {
        clearInterval(this.poller);
        clearInterval(this.computePoller);
    },
};
</script>

<style>
.terminalBlock {
    display: block;
    position: absolute;
    left: 0px;
    width: 100%;
    height: 100%;
    border: 0px;
}
#terminalFrame {
    border-top: 1px solid black;
}
#terminalCurtain {
    background-color: #eeeeee;
    padding-top: 20px;
    text-align: center;
}
</style>
