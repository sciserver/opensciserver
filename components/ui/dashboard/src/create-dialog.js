const jQuery = require('jquery');
require('jquery-ui/ui/widgets/dialog');

export default function createdialog(id, htmlTitle) {
  const dialog = jQuery(id).dialog({
    autoOpen: false,
    width: 'auto',
    modal: true,
    buttons: [
      {
        text: 'Close',
        click() {
          jQuery(this).dialog('close');
        },
      },
    ],
  });
  if (htmlTitle != null) {
    dialog.dialog('option', 'title', htmlTitle);
  }
  return dialog;
}
