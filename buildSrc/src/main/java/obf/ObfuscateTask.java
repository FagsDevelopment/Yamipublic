package obf;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.*;
import org.gradle.api.file.RegularFileProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle task — remapJar çıktısını obfuscate eder.
 */
public abstract class ObfuscateTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @InputFiles
    @Optional
    public abstract ConfigurableFileCollection getClasspath();

    @TaskAction
    public void execute() throws Exception {
        File input = getInputJar().get().getAsFile();
        File output = getOutputJar().get().getAsFile();

        getLogger().lifecycle("══════════════════════════════════════════════");
        getLogger().lifecycle("   ███████╗ █████╗  ██████╗ ███████╗");
        getLogger().lifecycle("   ██╔════╝██╔══██╗██╔════╝ ██╔════╝");
        getLogger().lifecycle("   █████╗  ███████║██║  ███╗███████╗");
        getLogger().lifecycle("   ██╔══╝  ██╔══██║██║   ██║╚════██║");
        getLogger().lifecycle("   ██║     ██║  ██║╚██████╔╝███████║");
        getLogger().lifecycle("   ╚═╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝");
        getLogger().lifecycle("                OBFUSCATOR          ");
        getLogger().lifecycle("  Giris:  " + input.getName());
        getLogger().lifecycle("  Cikis:  " + output.getName());
        getLogger().lifecycle("══════════════════════════════════════════════");

        List<File> cpFiles = new ArrayList<>();
        if (getClasspath().getFiles() != null) {
            for (File f : getClasspath().getFiles()) {
                if (f.exists() && (f.getName().endsWith(".jar") || f.isDirectory())) {
                    cpFiles.add(f);
                }
            }
        }
        getLogger().lifecycle("[Obfuscator] Classpath: " + cpFiles.size() + " JAR/dizin yüklendi.");

        Obfuscator obf = new Obfuscator();
        obf.process(input, output, cpFiles);
    }
}
