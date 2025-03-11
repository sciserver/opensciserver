import { graphviz } from '@hpcc-js/wasm';
import svgPanZoom from 'svg-pan-zoom';
import axios from 'axios';
import '../node_modules/@hpcc-js/wasm/dist/graphvizlib.wasm';

const marked = require('marked');
const dompurify = require('dompurify');

dompurify.addHook('afterSanitizeAttributes', (node) => {
    if (node.tagName === 'A') {
        node.setAttribute('target', '_blank');
        node.setAttribute('rel', 'noopener noreferrer');
    }
});

function extensionToModeMimeType(extension) {
    const ext = extension.toLowerCase();
    if (ext === 'py') {
        return 'text/x-python';
    } else if (ext === 'md') {
        return 'text/x-markdown';
    } else if (ext === 'java') {
        return 'text/x-java';
    } else if (ext === 'sh') {
        return 'text/x-sh';
    } else if (ext === 'js') {
        return 'text/javascript';
    } else if (ext === 'json') {
        return 'application/json';
    } else if (ext === 'scala') {
        return 'text/x-scala';
    } else if (ext === 'tex') {
        return 'text/x-stex';
    } else if (ext === 'sql') {
        return 'text/x-sql';
    } else if (ext === 'r') {
        return 'text/x-rsrc';
    } else if (ext === 'go') {
        return 'text/x-go';
    } else if (['xml', 'htm', 'html'].includes(ext)) {
        return 'text/html';
    } else if (['yaml', 'yml'].includes(ext)) {
        return 'text/x-yaml';
    } else if (['txt', 'csv', 'lis', 'dot'].includes(ext)) {
        return 'text/plain';
    }
    return null;
}

function getExtension(filename) {
    return filename.split('.').pop().toLowerCase();
}

function filenameToModeMimeType(filename) {
    return extensionToModeMimeType(getExtension(filename));
}

function extensionSupported(extension) {
    return extensionToModeMimeType(extension) !== null;
}

function fileSupported(filename) {
    return extensionSupported(getExtension(filename));
}

function isInlineExtension(extension) {
    return [
        'pdf', 'jpeg', 'jpg', 'gif', 'tiff', 'png', 'mov', 'mp4', 'avi', 'mp3', 'ogg', 'mpg', 'mkv', 'mpeg', 'wmv',
        'swf',
    ].includes(extension);
}

function shouldInline(filename) {
    return isInlineExtension(getExtension(filename));
}

function renderMarkdown(elem, code) {
    elem.innerHTML = dompurify.sanitize(marked(code, { headerPrefix: 'sciserverMd-hd_' }));
}

function renderHTML(elem, code) {
    elem.replaceChildren();
    const iframe = document.createElement('iframe');
    elem.appendChild(iframe);
    iframe.contentDocument.write(dompurify.sanitize(code, { FORCE_BODY: true }));
}

function renderSVG(elem, code) {
    elem.innerHTML = dompurify.sanitize(code);
    const svgElem = elem.getElementsByTagName('svg')[0];
    // the border style is unset here to enable the render error detection in graphviz, while the overflow is explicitly
    // unset as the svg pan/zoom allows navigation.
    svgElem.style.width = '100%';
    svgElem.style.height = '100%';
    elem.style.border = 'inherit';
    elem.style.overflow = 'hidden';
    const svgpan = svgPanZoom(svgElem, { controlIconsEnabled: true });
    // There are a couple undersirables about this, in that a resize will lose the current focus point if the svg has
    // been moved around, but it is simple and always works (and in some ways better for a major resize). Optionally
    // could consider discovering pan/zoom of any existing svg prior to this, and update to that.
    const resizeObserver = new ResizeObserver(() => {
        svgpan.updateBBox();
        svgpan.resize();
        svgpan.fit();
        svgpan.center();
    });
    resizeObserver.observe(elem);
}

function renderDOT(elem, code) {
    graphviz.layout(code, 'svg', 'dot')
        .then((svg) => {
            renderSVG(elem, svg);
        })
        .catch(() => {
            elem.style.border = '4px solid red';
        });
}

function renderNotebook(elem, code) {
    elem.innerHTML = 'Rendering notebook... <i class="fa fa-spinner fa-spin fa-2x"></i>';
    axios.post(NBCONV_URL, code)
        .then((response) => {
            renderHTML(elem, response.data);
        })
        .catch(() => {
            elem.innerHTML = 'ERROR';
        });
}

const renderFunctions = {
    md: renderMarkdown,
    svg: renderSVG,
    html: renderHTML,
    htm: renderHTML,
    dot: renderDOT,
    ipynb: NBCONV_URL ? renderNotebook : undefined,
};

function getRenderFunction(filename) {
    return renderFunctions[getExtension(filename)];
}


export default {
    extensionToModeMimeType,
    getExtension,
    filenameToModeMimeType,
    extensionSupported,
    fileSupported,
    shouldInline,
    getRenderFunction,
};
