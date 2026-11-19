package org.sciserver.antlr4.tsql;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import org.sciserver.antlr4.tsql.gen.SciQueryTSqlLexer;
import org.sciserver.antlr4.tsql.gen.SciQueryTSqlParser;

public class ParserFacade {

    public static final boolean FIXCASE = false;

    public static class ParseError {
        private int line;
        private int charPositionInLine;
        private String msg;
        private Object offendingSymbol;

        private ParseError(int line, int charPositionInLine, String msg, Object offendingSymbol) {
            this.line = line;
            this.charPositionInLine = charPositionInLine;
            this.msg = msg;
            this.offendingSymbol = offendingSymbol;
        }

        public String toString() {
            return String.format("line %d : %d %s", line, charPositionInLine, msg);
        }
    }

    static class ParseErrorListener extends BaseErrorListener {
        public ArrayList<ParseError> getErrors() {
            return errors;
        }

        ArrayList<ParseError> errors = new ArrayList<ParseError>();
        public static final ParseErrorListener INSTANCE = new ParseErrorListener();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                String msg, org.antlr.v4.runtime.RecognitionException e) throws ParseCancellationException {
            ParseError error = new ParseError(line, charPositionInLine, msg, offendingSymbol);
            errors.add(error);
        }

        public boolean hasErrors() {
            return errors.size() > 0;
        }
    }

    private String readFile(File file, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        String content = new String(encoded, encoding);
        return content;
    }

    public SciQueryTSqlParser.TsqlContext parse(File file) throws IOException, ParseCancellationException {
        String code = readFile(file, Charset.forName("UTF-8"));
        return parse(code);
    }

    public SciQueryTSqlParser.TsqlContext parse(String code) throws IOException, ParseCancellationException {
        ParseErrorListener listener = new ParseErrorListener();
        SciQueryTSqlLexer lexer = new SciQueryTSqlLexer(CharStreams.fromString(code));
        lexer.removeErrorListeners(); // lexers may come by default with listeners, so first remove those
        lexer.addErrorListener(listener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        if (listener.hasErrors()) 
            throw tranformExceptions(listener.getErrors());

        SciQueryTSqlParser parser = new SciQueryTSqlParser(tokens);
        parser.removeErrorListeners(); // parsers may come by default with listeners, so first remove those
        parser.addErrorListener(listener);
        SciQueryTSqlParser.TsqlContext root = null;
        root = parser.tsql();

        if (listener.hasErrors())
            throw tranformExceptions(listener.getErrors());
        
        return root;
    }
    
    private static ParseCancellationException tranformExceptions(List<ParseError> errors) {
        return new ParseCancellationException(
                String.join("\n", errors.
                        stream().
                        map(ParseError::toString).
                        collect(Collectors.toList()))
        );
    }
}
