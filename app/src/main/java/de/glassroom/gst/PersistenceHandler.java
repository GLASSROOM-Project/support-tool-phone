package de.glassroom.gst;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import de.glassroom.gpe.Guide;
import de.glassroom.gpe.content.ContentDescriptor;
import de.glassroom.gpe.utils.ContentSerializer;
import de.glassroom.gpe.utils.GuideSerializer;

/**
 * Handler to store guides and media.
 */
public class PersistenceHandler {
    private static boolean isInitialized;
    private static File guidesDir;
    private static Properties clientProperties;

    static {
        initialize();
    }

    private static void initialize() {
        isInitialized = true;
        List<String> possibleDirs = Arrays.asList("/storage/extSdCard", "/mnt/extSdCard", "/mnt/sdcard", Environment.getExternalStorageDirectory().getAbsolutePath(), Environment.getDataDirectory().getAbsolutePath());
        File glassroomDir = null;
        for (String path : possibleDirs) {
            File dir = new File(path);
            if (dir.canRead()) {
                File subDir = new File(dir.getAbsolutePath() + "/glassroom");
                if (subDir.canRead()) {
                    glassroomDir = subDir;
                    break;
                }
            }
        }

        if (glassroomDir != null) {
            guidesDir = new File(glassroomDir, "guides");
            Log.i("PersistenceHandler", "Guides folder: " + guidesDir.getAbsolutePath());
            File propertiesFile = new File(glassroomDir, "client.properties");
            clientProperties = new Properties();
            if (propertiesFile.exists()) {
                try {
                    FileInputStream in = new FileInputStream(propertiesFile);
                    clientProperties.load(in);
                } catch (IOException e) {
                    Log.w("PersistenceHandler", "Failed to load client.clientProperties.", e);
                }
            }
            Log.i("PersistenceHandler", "Client set language: " + clientProperties.getProperty("lang"));
        } else {
            Log.w("PersistenceHandler", "No guides folder found.");
        }
    }

    public static List<Guide> importGuides() {

        if (!isInitialized) {
            initialize();
        }

        if (guidesDir == null || !guidesDir.canRead()) {
            return new ArrayList<>();
        }

        List<Guide> guides = new ArrayList<>();
        for (File file : guidesDir.listFiles()) {
            if (!file.isDirectory()) continue;
            File manifest = new File(file, "guide.bpmn");
            if (!manifest.exists()) continue;
            try {
                String bpmnString = readFile(manifest);
                Guide guide = GuideSerializer.readFromBPMN(bpmnString);
                guide.getMetadata().setLastUpdate(new Date(manifest.lastModified()));
                guides.add(guide);
            } catch (Exception e) {
                Log.w("PersistenceHandler", "Failed to read guide manifest: " + file.getName(), e);
            }
        }
        return guides;
    }

    private static String readFile(File file) throws IOException {
        Scanner scanner = new Scanner(file).useDelimiter("\r\n");
        StringBuilder builder = new StringBuilder();
        while (scanner.hasNext()) {
            builder.append(scanner.next());
        }
        scanner.close();
        return builder.toString();
    }

    /**
     * Writes a guide manifest to the external storage.
     * @param guide Guide to persist.
     * @throws IOException Writing to the external storage failed.
     */
    public static void writeGuide(Guide guide) throws IOException {
        File guideDir = new File(guidesDir.getAbsolutePath() + "/" + guide.getId());
        if (!guideDir.exists()) guideDir.mkdir();

        File contentDir = new File(guideDir.getAbsolutePath() + "/content");
        if (!contentDir.exists()) contentDir.mkdir();

        File guideDescriptorFile = new File(guideDir, "guide.bpmn");
        FileWriter writer;
        writer  = new FileWriter(guideDescriptorFile);

        String serializedGuide = GuideSerializer.writeAsBPMN(guide, false);
        writer.write(serializedGuide);
        writer.flush();
        writer.close();
    }

    /**
     * Moves a (temporary) file to a content package.
     * @param guideId ID of the guide the content package is located in.
     * @param packageId ID of the content package.
     * @param fileToMove (Temporary) file to move.
     * @param newFileName Optional new file name to use. If <code>null</code> the current file name will be userd.
     * @return The newly generated file.
     * @throws IllegalArgumentException The file to bo moved does not exist.
     */
    public static File moveToContentPackage(String guideId, String packageId, File fileToMove, String newFileName) throws IllegalArgumentException {
        if (!fileToMove.exists()) {
            throw new IllegalArgumentException("The given file " + fileToMove.getName() + " does not exist.");
        }

        File contentPackageDir = new File(guidesDir.getAbsolutePath() + "/" + guideId + "/content/" + packageId);
        if (!contentPackageDir.exists()) contentPackageDir.mkdirs();

        File newFile = new File(contentPackageDir, newFileName != null ? newFileName : fileToMove.getName());
        fileToMove.renameTo(newFile);

        return newFile;
    }

    /**
     * Persists a content descriptor.
     * @param guideId ID of the guide the content is related to.
     * @param contentDescriptor Content descriptor to persist.
     * @throws IOException Failed to generate
     */
    public static void writeContentDescriptor(String guideId, ContentDescriptor contentDescriptor) throws IOException {
        File contentPackageDir = new File(guidesDir.getAbsolutePath() + "/" + guideId + "/content/" + contentDescriptor.getId());
        if (!contentPackageDir.exists()) contentPackageDir.mkdirs();

        String serializedContentDescriptor = ContentSerializer.writeAsXML(contentDescriptor, false);

        File contentDescriptorFile = new File(contentPackageDir, "content.xml");
        FileWriter writer;
        writer  = new FileWriter(contentDescriptorFile);
        writer.write(serializedContentDescriptor);
        writer.flush();
        writer.close();
    }

    public static ContentDescriptor readContentDescriptor(String guideId, String packageId) throws IOException {
        File contentPackageDir = new File(guidesDir.getAbsolutePath() + "/" + guideId + "/content/" + packageId);
        if (!contentPackageDir.exists()) {
            throw new IOException("Content package " + packageId + " does not exist.");
        }

        File contentDescriptorFile = new File(contentPackageDir, "content.xml");
        if (!contentDescriptorFile.exists()) {
            throw new IOException("Content descriptor for package " + packageId + " is missing.");
        }

        String descriptorString = readFile(contentDescriptorFile);
        ContentDescriptor descriptor = ContentSerializer.readFromXML(descriptorString);
        return descriptor;
    }

    public static String getURLForMediaFile(String guideId, String packageId, String relativeMediaPath) {
        String path = new StringBuilder(100)
                .append(guidesDir.getAbsolutePath())
                .append("/")
                .append(guideId)
                .append("/content/")
                .append(packageId)
                .append("/")
                .append(relativeMediaPath)
                .toString();
        File mediaFile = new File(path);
        String url = null;
        try {
            url = mediaFile.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            Log.e("PersistenceHandler", "Failed to generate URL from file.", e);
        }
        return url;
    }

    /**
     * Deletes a guide with all of its content.
     * @param guideId Identifier of the guide to delete.
     */
    public static void deleteGuide(String guideId) {
        File guideDir = new File(guidesDir.getAbsolutePath() + "/" + guideId);
        if (guideDir.exists()) {
            deleteDir(guideDir);
        }
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for(File f : files) {
                if(f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    public static Properties getClientProperties() {
        if (clientProperties == null) {
            initialize();
        }
        return clientProperties;
    }
}
