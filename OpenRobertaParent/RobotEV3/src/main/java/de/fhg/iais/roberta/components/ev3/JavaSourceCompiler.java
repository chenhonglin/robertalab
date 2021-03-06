package de.fhg.iais.roberta.components.ev3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaSourceCompiler {
    private static final Logger LOG = LoggerFactory.getLogger(JavaSourceCompiler.class);

    private CustomJavaFileManager fileManager;
    private CompilerFeedback feedback;
    private final JavaCompiler compiler;
    private final String programName;
    private final String sourceCode;
    private final String packageName = "generated.main.";
    private final String classPath;

    /**
     * @param programName
     *        Name of the program
     * @param sourceCode
     *        Here we specify the source code of the class to be compiled
     */
    public JavaSourceCompiler(String programName, String sourceCode, String classPath) {
        this.programName = programName;
        this.sourceCode = sourceCode;
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.fileManager = initFileManager();
        this.classPath = classPath;
    }

    private CustomJavaFileManager initFileManager() {
        if ( this.fileManager != null ) {
            return this.fileManager;
        } else {
            this.fileManager = new CustomJavaFileManager(this.compiler.getStandardFileManager(null, null, null));
            return this.fileManager;
        }
    }

    private Boolean compile() {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<JavaFileObject> javaFiles = getJavaFiles();
        List<String> compilationOptions = getCompilerProperties();
        CompilationTask task = this.compiler.getTask(null, this.fileManager, diagnostics, compilationOptions, null, javaFiles);
        Boolean isSuccess = task.call();
        this.feedback = new CompilerFeedback(isSuccess, diagnostics);
        return isSuccess;
    }

    public boolean compileAndPackage(String pathToCrosscompilerBaseDir, String token) {
        Boolean isSuccess = compile();
        File jarFile;
        if ( isSuccess ) {
            ByteArrayOutputStream jarArchive = createJarArchive();
            jarFile = new File(pathToCrosscompilerBaseDir + "/" + token + "/target/" + this.programName + ".jar");
            try {
                FileUtils.writeByteArrayToFile(jarFile, jarArchive.toByteArray());
            } catch ( IOException e ) {
                JavaSourceCompiler.LOG.error("build exception. Messages from the build script are:\n" + e);
                return false;
            }
        }
        return isSuccess;
    }

    private List<JavaFileObject> getJavaFiles() {
        List<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
        javaFiles.add(new SourceJavaFileObject(this.packageName + this.programName, this.sourceCode));
        return javaFiles;
    }

    private List<String> getCompilerProperties() {
        List<String> compilationOptions = new ArrayList<String>();
        compilationOptions.add("-source");
        compilationOptions.add("1.7");
        compilationOptions.add("-target");
        compilationOptions.add("1.7");
        compilationOptions.add("-classpath");
        String separator = ":";
        if ( SystemUtils.IS_OS_WINDOWS ) {
            separator = ";";
        }
        compilationOptions.add(
            this.classPath
                + "dbusjava.jar"
                + separator
                + this.classPath
                + "ev3classes.jar"
                + separator
                + this.classPath
                + "EV3Runtime.jar"
                + separator
                + this.classPath
                + "Java-WebSocket.jar"
                + separator
                + this.classPath
                + "jna.jar"
                + separator
                + this.classPath
                + "json.jar");
        return compilationOptions;
    }

    private ByteArrayOutputStream createJarArchive() {
        // Open archive file
        byte[] classToBeJared = this.fileManager.getClassJavaFileObject().getBytes();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JarOutputStream out;
        try {
            Manifest mf = createManifest();
            out = new JarOutputStream(stream, mf);
            // Add archive entry
            JarEntry jarAdd = new JarEntry("generated/main/" + this.programName + ".class");
            out.putNextEntry(jarAdd);
            out.write(classToBeJared);
            out.close();
            stream.close();
            return stream;
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return stream;

    }

    private Manifest createManifest() {
        final String brickRuntime = "/home/root/lejos";
        final String brickRoberta = "/home/roberta/lib";
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, this.packageName + this.programName);
        mf.getMainAttributes().put(
            Attributes.Name.CLASS_PATH,
            brickRuntime
                + "/lib/ev3classes.jar "
                + brickRuntime
                + "/lib/dbusjava.jar "
                + brickRuntime
                + "/libjna/usr/share/java/jna.jar "
                + brickRoberta
                + "/EV3Runtime.jar "
                + brickRoberta
                + "/Java-WebSocket.jar "
                + brickRoberta
                + "/json.jar");
        return mf;
    }

    public Boolean isSuccess() {
        return this.feedback.isSuccess();
    }

    public String getCompilationMessages() {
        return this.feedback.toString();
    }

    public byte[] getCompiledClass() {
        return this.fileManager.getClassJavaFileObject().getBytes();
    }
}
