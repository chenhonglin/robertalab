package de.fhg.iais.roberta.factory.mbed.calliope;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.iais.roberta.blockly.generated.BlockSet;
import de.fhg.iais.roberta.components.Configuration;
import de.fhg.iais.roberta.components.mbed.CalliopeConfiguration;
import de.fhg.iais.roberta.factory.ICompilerWorkflow;
import de.fhg.iais.roberta.factory.IRobotFactory;
import de.fhg.iais.roberta.syntax.check.hardware.mbed.UsedHardwareCollectorVisitor;
import de.fhg.iais.roberta.syntax.codegen.mbed.calliope.CppVisitor;
import de.fhg.iais.roberta.transformer.BlocklyProgramAndConfigTransformer;
import de.fhg.iais.roberta.transformer.mbed.Jaxb2CalliopeConfigurationTransformer;
import de.fhg.iais.roberta.util.Key;
import de.fhg.iais.roberta.util.dbc.Assert;
import de.fhg.iais.roberta.util.jaxb.JaxbHelper;

public class CompilerWorkflow implements ICompilerWorkflow {

    private static final Logger LOG = LoggerFactory.getLogger(CompilerWorkflow.class);

    public final String pathToCrosscompilerBaseDir;
    public final String robotCompilerResourcesDir;
    public String robotCompilerDir;

    private String compiledHex = "";

    public CompilerWorkflow(String pathToCrosscompilerBaseDir, String robotCompilerResourcesDir, String robotCompilerDir) {
        this.pathToCrosscompilerBaseDir = pathToCrosscompilerBaseDir;
        this.robotCompilerResourcesDir = robotCompilerResourcesDir;
        this.robotCompilerDir = robotCompilerDir;

    }

    /**
     * - load the program from the database<br>
     * - generate the AST<br>
     * - typecheck the AST, execute sanity checks, check a matching brick
     * configuration<br>
     * - generate Java code<br>
     * - store the code in a token-specific (thus user-specific) directory<br>
     * - compile the code and generate a jar in the token-specific directory
     * (use a ant script, will be replaced later)<br>
     * <b>Note:</b> the jar is prepared for upload, but not uploaded from here.
     * After a handshake with the brick (the brick has to tell, that it is
     * ready) the jar is uploaded to the brick from another thread and then
     * started on the brick
     *
     * @param token
     *        the credential the end user (at the terminal) and the brick
     *        have both agreed to use
     * @param programName
     *        name of the program
     * @param programText
     *        source of the program
     * @param configurationText
     *        the hardware configuration source that describes
     *        characteristic data of the robot
     * @return a message key in case of an error; null otherwise
     */
    @Override
    public Key execute(String token, String programName, BlocklyProgramAndConfigTransformer data) {
        String sourceCode = CppVisitor.generate((CalliopeConfiguration) data.getBrickConfiguration(), data.getProgramTransformer().getTree(), true);
        UsedHardwareCollectorVisitor usedHardwareVisitor =
            new UsedHardwareCollectorVisitor(data.getProgramTransformer().getTree(), data.getBrickConfiguration());
        try {
            storeGeneratedProgram(token, programName, sourceCode, ".cpp");
        } catch ( Exception e ) {
            CompilerWorkflow.LOG.error("Storing the generated program into directory " + token + " failed", e);
            return Key.COMPILERWORKFLOW_ERROR_PROGRAM_STORE_FAILED;
        }

        Key messageKey = runBuild(token, programName, "generated.main", usedHardwareVisitor.isRadioUsed());
        if ( messageKey == Key.COMPILERWORKFLOW_SUCCESS ) {
            CompilerWorkflow.LOG.info("hex for program {} generated successfully", programName);
        } else {
            CompilerWorkflow.LOG.info(messageKey.toString());
        }
        return messageKey;
    }

    /**
     * - take the program given<br>
     * - generate the AST<br>
     * - typecheck the AST, execute sanity checks, check a matching brick
     * configuration<br>
     * - generate source code in the right language for the robot<br>
     * - and return it
     *
     * @param token
     *        the credential the end user (at the terminal) and the brick
     *        have both agreed to use
     * @param programName
     *        name of the program
     * @param programText
     *        source of the program
     * @param configurationText
     *        the hardware configuration source that describes
     *        characteristic data of the robot
     * @return the generated source code; null in case of an error
     */
    @Override
    public String generateSourceCode(IRobotFactory factory, String token, String programName, String programText, String configurationText) {
        BlocklyProgramAndConfigTransformer data = BlocklyProgramAndConfigTransformer.transform(factory, programText, configurationText);
        if ( data.getErrorMessage() != null ) {
            return null;
        }
        return CppVisitor.generate((CalliopeConfiguration) data.getBrickConfiguration(), data.getProgramTransformer().getTree(), true);
    }

    private void storeGeneratedProgram(String token, String programName, String sourceCode, String ext) throws Exception {
        Assert.isTrue(token != null && programName != null && sourceCode != null);
        File sourceFile = new File(this.pathToCrosscompilerBaseDir + token + "/" + programName + "/source/" + programName + ext);
        Path path = Paths.get(this.pathToCrosscompilerBaseDir + token + "/" + programName + "/target/");
        Files.createDirectories(path);
        CompilerWorkflow.LOG.info("stored under: " + sourceFile.getPath());
        FileUtils.writeStringToFile(sourceFile, sourceCode, StandardCharsets.UTF_8.displayName());
    }

    /**
     * 1. Make target folder (if not exists).<br>
     * 2. Clean target folder (everything inside).<br>
     * 3. Compile .java files to .class.<br>
     * 4. Make jar from class files and add META-INF entries.<br>
     *
     * @param token
     * @param mainFile
     * @param mainPackage
     */
    Key runBuild(String token, String mainFile, String mainPackage, boolean radioUsed) {
        final StringBuilder sb = new StringBuilder();

        String scriptName = this.robotCompilerResourcesDir + "/../compile.sh";

        if ( SystemUtils.IS_OS_WINDOWS ) {
            scriptName = this.robotCompilerResourcesDir + "/../compile.bat";
            if ( this.robotCompilerDir.equals("") ) {
                this.robotCompilerDir = "\"\"";
            }
        }
        String bluetooth = radioUsed ? "" : "-b";
        Path path = Paths.get(this.pathToCrosscompilerBaseDir + token + "/" + mainFile);
        Path base = Paths.get("");

        try {
            ProcessBuilder procBuilder =
                new ProcessBuilder(
                    new String[] {
                        scriptName,
                        this.robotCompilerDir,
                        mainFile,
                        base.resolve(path).toAbsolutePath().normalize().toString() + "/",
                        this.robotCompilerResourcesDir,
                        bluetooth
                    });

            procBuilder.redirectInput(Redirect.INHERIT);
            procBuilder.redirectOutput(Redirect.INHERIT);
            procBuilder.redirectError(Redirect.INHERIT);
            Process p = procBuilder.start();
            int ecode = p.waitFor();
            System.err.println("Exit code " + ecode);

            if ( ecode != 0 ) {
                return Key.COMPILERWORKFLOW_ERROR_PROGRAM_COMPILE_FAILED;
            }
            this.compiledHex = FileUtils.readFileToString(new File(path + "/target/" + mainFile + ".hex"), "UTF-8");
            return Key.COMPILERWORKFLOW_SUCCESS;
        } catch ( Exception e ) {
            if ( sb.length() > 0 ) {
                CompilerWorkflow.LOG.error("build exception. Messages from the build script are:\n" + sb.toString(), e);
            } else {
                CompilerWorkflow.LOG.error("exception when preparing the build", e);
            }
            e.printStackTrace();
            return Key.COMPILERWORKFLOW_ERROR_PROGRAM_COMPILE_FAILED;
        }
    }

    /**
     * return the brick configuration for given XML configuration text.
     *
     * @param blocklyXml
     *        the configuration XML as String
     * @return brick configuration
     * @throws Exception
     */
    @Override
    public Configuration generateConfiguration(IRobotFactory factory, String blocklyXml) throws Exception {
        BlockSet project = JaxbHelper.xml2BlockSet(blocklyXml);
        Jaxb2CalliopeConfigurationTransformer transformer = new Jaxb2CalliopeConfigurationTransformer(factory);
        return transformer.transform(project);
    }

    @Override
    public String getCompiledCode() {
        return this.compiledHex;
    }
}
