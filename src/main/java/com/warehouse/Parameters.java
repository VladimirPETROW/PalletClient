package com.warehouse;

import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class Parameters {

    Map<String, String> files;

    Parameters() {
        files = new HashMap<>();
    }

    public Parameters file(String name, String path) {
        files.put(name, path);
        return this;
    }

    public void addAll(MultipartEntityBuilder entityBuilder) {
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String name = entry.getKey();
            String path = entry.getValue();
            entityBuilder.addBinaryBody(name, new File(path));
        }
    }

    public void clear() {
        files.clear();
    }

    public void writeAll(String content) throws IOException {
        if (files.values().isEmpty()) {
            System.out.println(content);
            return;
        }
        for (String filePath : files.values()) {
            Path path = Paths.get(filePath);
            path.getParent().toFile().mkdirs();
            Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}
