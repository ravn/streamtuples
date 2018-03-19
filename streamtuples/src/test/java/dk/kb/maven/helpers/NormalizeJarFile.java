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
 * NormalizeJarFile is a simples-posible jar normalizator which creates a new jar with all
 * the entries in order on disk and their dates set to zero.  For now any error calls
 * System.exit which shuts down the Maven JVM
 * (which is a bug) but for now that is "good enough".  Three arguments for the current jar file
 * name, temporary jar file name and backup file name.  It is in the test folder because we need
 * it in the build class path (for now until it becomes a proper plugin) but it should not go in the final jar.
 *
 * @noinspection PointlessBooleanExpression, WeakerAccess
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

        if (backupJarFile.getParentFile().exists() == false && backupJarFile.getParentFile().mkdirs() == false) {
            System.err.println(backupJarFile.getAbsolutePath() + " could not make parent directory");
            System.exit(5);
        }
        //noinspection StatementWithEmptyBody
        if (backupJarFile.delete() == false) {
            // could not successfully delete it, typical case during maven build.
        }

        // https://github.com/manouti/jar-timestamp-normalize-maven-plugin/blob/master/src/main/java/com/github/manouti/normalize/DefaultNormalizer.java

        try (ZipFile source = new ZipFile(workJarFile);
             ZipOutputStream tmp = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmpJarFile)))) {

            Collections.list(source.entries()).stream()
                    .sorted(Comparator.comparing(ZipEntry::getName))
                    .forEach(entry -> {
                        try {
                            entry.setTime(0);
                            tmp.putNextEntry(entry);
                            if (entry.isDirectory() == false) {
                                // https://stackoverflow.com/q/43157/53897, WHY isn't this in the standard runtime??
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
        }

        if (tmpJarFile.renameTo(workJarFile) == false) {
            System.err.println("Could not rename " + tmpJarFile.getAbsolutePath() + " to " + workJarFile.getAbsolutePath());
            System.exit(9);
        }

        // All ok.  No system.exit as this kills the Maven build.
    }
}
