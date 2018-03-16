package dk.kb.maven.helpers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @noinspection PointlessBooleanExpression
 */
public class NormalizeJarFile {
    public static void main(String args[]) throws Exception {
        File workJarFile = new File(args[0]);
        File tmpJarFile = new File(args[1]);
        File backupJarFile = new File(args[2]);

        if (workJarFile.exists() == false) {
            System.err.println(workJarFile.getAbsoluteFile() + " does not exist");
            System.exit(1);
        }
        if (tmpJarFile.exists() && tmpJarFile.delete() == false) {
            System.err.println(tmpJarFile.getAbsoluteFile() + " cannot be deleted");
            System.exit(2);
        }
        if (backupJarFile.exists() && backupJarFile.delete() == false) {
            System.err.println(backupJarFile.getAbsoluteFile() + " cannot be deleted");
            System.exit(3);
        }

        if (tmpJarFile.getParentFile().exists() == false && tmpJarFile.getParentFile().mkdirs() == false) {
            System.err.println(tmpJarFile.getAbsolutePath() + " could not make parent directory");
            System.exit(4);
        }
        ;

        if (backupJarFile.getParentFile().exists() == false && backupJarFile.getParentFile().mkdirs() == false) {
            System.err.println(backupJarFile.getAbsolutePath() + " could not make parent directory");
            System.exit(5);
        }
        if (backupJarFile.delete() == false) {
            // could not delete it, it may be because
        }
        ;

        // https://github.com/manouti/jar-timestamp-normalize-maven-plugin/blob/master/src/main/java/com/github/manouti/normalize/DefaultNormalizer.java

        try (ZipFile source = new ZipFile(workJarFile);
             ZipOutputStream tmp = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpJarFile)))) {

            Collections.list(source.entries()).stream()
                    .sorted(Comparator.comparing(ZipEntry::getName))
                    //.filter(entry -> entry.isDirectory() == false)
                    .peek(System.err::println)
                    .forEach(entry -> {
                        try {
                            entry.setTime(0);
                            tmp.putNextEntry(entry);
                            if (entry.isDirectory() == false) {
                                // https://stackoverflow.com/q/43157/53897
                                InputStream in = source.getInputStream(entry);
                                byte[] buffer = new byte[1024];
                                int len = in.read(buffer);
                                while (len != -1) {
                                    tmp.write(buffer, 0, len);
                                    len = in.read(buffer);
                                }
                                in.close();
                            }
                        } catch (Exception e) {
                            System.err.println("while copying " + entry);
                            e.printStackTrace(System.err);
                            System.exit(7);
                        }
                    });
        }
        // Done rewriting.  Now rename appropriately.

        if (workJarFile.renameTo(backupJarFile) == false) {
            System.err.println("Could not rename " + workJarFile.getAbsolutePath() + " to " + backupJarFile.getAbsolutePath());
            System.exit(8);
        };

        if (tmpJarFile.renameTo(workJarFile) == false) {
            System.err.println("Could not rename " + tmpJarFile.getAbsolutePath() + " to " + workJarFile.getAbsolutePath());
            System.exit(9);
        };

        // All ok.

        System.exit(0);
    }

}
