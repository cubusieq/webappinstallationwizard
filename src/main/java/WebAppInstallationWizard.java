
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppInstallationWizard {
	static Logger logger = LoggerFactory.getLogger(WebAppInstallationWizard.class);
	private Option[] options;

	public WebAppInstallationWizard(CommandLine cl) {
		logger.debug(cl.getOptionValue("dest"));
		logger.debug(cl.getOptionValue("src"));
		extractWar(cl.getOptionValue("dest"), cl.getOptionValue("src"));
	}

	private boolean extractWar(String installerName, String warPath) {
		File installatorJarFile = new File("WebAppInstallator.jar");
		try {
			
			OutputStream outputStream = new FileOutputStream(installatorJarFile);
			IOUtils.copy(this.getClass().getResourceAsStream("WebAppInstallator.jar"), outputStream);

			// Open JarFile
			JarFile installatorJar = new JarFile(installatorJarFile);
			// Make new JarFile
			File newInstallatorJarFile = new File(installerName + ".jar");

			Enumeration<JarEntry> enumEntries = installatorJar.entries();
		
			// create stream for new File
			JarOutputStream newJarStream = new JarOutputStream(new FileOutputStream(newInstallatorJarFile));
			try {
				// WarFile path stream
				FileInputStream warFile = new FileInputStream(warPath);

				JarEntry warEntry = new JarEntry(warPath);
				newJarStream.putNextEntry(warEntry);
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = warFile.read(buffer)) != -1) {
					newJarStream.write(buffer, 0, bytesRead);
				}
				warFile.close();
				while (enumEntries.hasMoreElements()) {
					JarEntry file = (JarEntry) enumEntries.nextElement();
					if (!file.getName().equals(warPath)) {
						// Get an input stream for the entry.
						InputStream entryStream = installatorJar.getInputStream(file);
						// Read the entry and write it to the temp jar.
						newJarStream.putNextEntry(file);
						while ((bytesRead = entryStream.read(buffer)) != -1) {
							newJarStream.write(buffer, 0, bytesRead);
						}
					}
				}

			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
				newJarStream.putNextEntry(new JarEntry("empty"));

			} finally {
				newJarStream.close();
				installatorJar.close();
				outputStream.close();
				installatorJar.close();

			}
		
			return true;
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
			return false;
		}finally
		{
			installatorJarFile.delete();
		}

	}

	public static void main(String[] args) {
		try {
			final CommandLineParser parser = new DefaultParser();
			Options options = new Options();
			Option src = Option.builder("src").hasArg().argName("src").required().desc("Path to WAR file.").build();
			Option dest = Option.builder("dest").hasArg().argName("dest").required()
					.desc("Created installator file path.").build();
			options.addOption(src);
			options.addOption(dest);
			CommandLine parsedCl = parser.parse(options, args);
			new WebAppInstallationWizard(parsedCl);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
			logger.error("Check -src and -dest arugments, and try again.");
		}
	}

}
