import Document, { DocumentContext, DocumentInitialProps } from 'next/document';
import { ServerStyleSheet } from 'styled-components';

// Collect styled-components (and the MUI styles rendered through it) during SSR so the
// first paint on a page reload is already styled instead of flashing unstyled markup.
export default class MyDocument extends Document {
  static async getInitialProps(ctx: DocumentContext): Promise<DocumentInitialProps> {
    const sheet = new ServerStyleSheet();
    const originalRenderPage = ctx.renderPage;

    try {
      ctx.renderPage = () => originalRenderPage({
        enhanceApp: (App) => function EnhancedApp(props) {
          return sheet.collectStyles(<App {...props} />);
        }
      });

      const initialProps = await Document.getInitialProps(ctx);
      return {
        ...initialProps,
        styles: [initialProps.styles, sheet.getStyleElement()]
      };
    }
    finally {
      sheet.seal();
    }
  }
}
