function removeElementIfExists(el) {
    if (el) {
        el.remove();
    }
}

function jupyterClassicVisualModifications(frame) {
    const framedoc = frame.contentDocument;

    removeElementIfExists(framedoc.getElementById('filelink'));
    removeElementIfExists(framedoc.getElementById('header-container'));
    removeElementIfExists(framedoc.getElementById('cmd_palette'));
    removeElementIfExists(framedoc.getElementById('toggle_header'));
    framedoc.getElementById('notebook-container').style.setProperty('width', '98%');
    framedoc.getElementById('menubar-container').style.setProperty('margin-left', '15px');
    framedoc.getElementById('notebook_panel').style.setProperty('background-color', '#f5faff');
    framedoc.getElementById('site').style.setProperty('background-color', '#f5faff');
    // The dynamic adjustent by jupyter on startup causes a shortened window
    const docHeight = framedoc.children[0].clientHeight;
    const headerHeight = framedoc.getElementById('header').clientHeight;
    framedoc.getElementById('site').style.setProperty('height', `${docHeight - headerHeight}px`);
    framedoc.getElementsByTagName('body')[0].style.setProperty('background-color', '#f5faff');

    return true;
}

function jupyterClassicTerminalModifications(frame) {
    frame.contentDocument.getElementById('header').style.setProperty('display', 'none');
    frame.contentDocument.getElementById('terminado-container').style.setProperty('width', '100%');
    frame.contentDocument.getElementsByTagName('body')[0].style.setProperty('background-color', 'transparent');
    frame.contentDocument.querySelector('.xterm').style.setProperty('height', '100%');
    frame.contentDocument.querySelector('.terminado-container-container').style.setProperty('padding-top', '0px');

    frame.contentWindow.terminal.term.setOption('theme', { foreground: 'black', background: 'white', cursor: 'black' });

    return true;
}

export default {
    jupyterClassicVisualModifications,
    jupyterClassicTerminalModifications,
};
