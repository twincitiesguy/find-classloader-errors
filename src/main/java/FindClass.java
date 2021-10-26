import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FindClass {
    private static final Logger log = LogManager.getLogger(FindClass.class);

    public static void main(String[] args) throws Exception {
        Path rootPath = Paths.get(args[0]);
        assert Files.isDirectory(rootPath) && Files.isReadable(rootPath);

        log.info("adding {} to classpath", rootPath);

        URL u = rootPath.toUri().toURL();
        URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> urlClass = URLClassLoader.class;
        Method method = urlClass.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(urlClassLoader, u);

        log.info("scanning {}", rootPath);

        Files.walk(rootPath)
                .parallel()
                .filter(path -> !Files.isDirectory(path)) // not a directory
                .map(path -> path.toString()
                        .replaceFirst("^" + rootPath + "/", "") // remove root path
                        .replaceAll("/", ".")) // change to dot-notation
                //.filter(name -> !name.startsWith("META-INF")) // ignore META-INF/*
                .filter(name -> name.endsWith(".class")) // only class files
                .forEach(name -> {
                    String className = name.replaceFirst("\\.class$", "");
                    try {
                        log.info("loading class: {}", className);
                        Class.forName(className);
//                    } catch (UnsupportedClassVersionError e) {
//                        log.error("bad class version: {} : {}", className, e);
                    } catch (Throwable t) {
                        if (t.toString().contains("59")) log.error("59! : {} : {}", className, t);
                        log.info("bad class: {} : {}", className, t);
                    }
                });
    }
}
