package org.sciserver.springapp.fileservice.model;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

/**
 * This class uses javax.nio.file.Files.walkFileTree to build up a JSON node
 * with folder and file info.<br/>
 * It extends java.nio.file.SimpleFileVisitor<Path>.
 * It can be configured to recurse down to a maximum depth, and to only accept
 * files with names conforming to a given pattern.
 *
 * @author gerard
 */
public class FileTree extends SimpleFileVisitor<Path> {
    private final JsonNodeFactory json = new JsonNodeFactory(false);

    private ObjectNode root = null;
    private final Stack<ObjectNode> stack = new Stack<>();
    private ObjectNode current;
    private final int maxDepth;
    private PathMatcher matcher = null;
    private boolean doMatch = false;
    private final String queryPath;
    private String replaceTopDirName;

    @SuppressWarnings("unused")
    public String getQueryPath() {
        return queryPath;
    }

    FileTree(String queryPath, int md) {
        this.queryPath = queryPath;
        this.maxDepth = md;
    }

    public FileTree(String queryPath, int md, String pattern, String replaceRootDirName) {
        this(queryPath, md);
        if (!StringUtils.isEmpty(pattern)) {
            this.matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            this.doMatch = true;
        }
        this.replaceTopDirName = replaceRootDirName;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (stack.size() >= maxDepth){
            return FileVisitResult.SKIP_SUBTREE;
        }
        addDir(dir, attrs);
        return FileVisitResult.CONTINUE;
    }

	@Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        stack.pop();
        if (stack.isEmpty()) {
            return FileVisitResult.TERMINATE;
        } else {
            current = stack.peek();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        if (stack.size() >= maxDepth) {
            return FileVisitResult.SKIP_SIBLINGS; // return so will not continue looking
        }
        if (doMatch && !matcher.matches(path)) {
            return FileVisitResult.CONTINUE;
        }
        ArrayNode files = (ArrayNode) current.get("files");
        if (files == null) {
            files = current.putArray("files");
        }
        ObjectNode file = json.objectNode();
        file.put("name", path.getFileName().toString());
        file.put("size", attrs.size());
        file.put("lastModified", attrs.lastModifiedTime().toString());
        file.put("creationTime", attrs.creationTime().toString());
        files.add(file);
        return FileVisitResult.CONTINUE;
    }

    private void addDir(ObjectNode p) {
        ArrayNode folders = (ArrayNode) current.get("folders");
        if (folders == null) {
            folders = current.putArray("folders");
        }
        folders.add(p);
    }

    private void addDir(Path path, BasicFileAttributes attrs) {

        String dirName;
        int st = stack.size();
        if (st == 0 && replaceTopDirName != null) {
            dirName = replaceTopDirName;
        } else {
            dirName = path.getFileName().toString();
        }
        ObjectNode p = json.objectNode();
        p.put("name", dirName);
        // p.put("size", attrs.size());
        p.put("lastModified", attrs.lastModifiedTime().toString());
        p.put("creationTime", attrs.creationTime().toString());
        if (root == null) {
            root = p;
        } else {
            addDir(p);
        }
        push(p);
    }

    private void push(ObjectNode d) {
        current = d;
        stack.push(d);
    }

    @SuppressWarnings("unused")
    public ObjectNode getRoot() {
        return root;
    }
}
