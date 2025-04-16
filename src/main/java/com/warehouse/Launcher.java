package com.warehouse;

import javax.script.*;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Launcher {

    public static void main(String[] args) {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");

        try (FileReader reader = new FileReader("PalletClient.js")) {
            scriptEngine.eval("" +
                    "load(\"nashorn:mozilla_compat.js\");" +
                    "importPackage(com.warehouse);");
            scriptEngine.eval(reader);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        } finally {
            Bindings bindings = scriptEngine.getBindings(ScriptContext. ENGINE_SCOPE);
            for (Object obj : bindings.values()) {
                if (obj instanceof Closeable) {
                    try {
                        ((Closeable) obj).close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
