package webappinstallatorwizard;

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

	public WebAppInstallationWizard(CommandLine cl, Options declaredOptions) {
		boolean forceOverwrite = false;
		if (cl.hasOption("i") || !cl.hasOption("dest") || !cl.hasOption("src")) {
			this.printInfo(declaredOptions);
		} else {
			if (cl.hasOption("f")) {
				forceOverwrite = true;
			}
			makeInstallatorJar(cl.getOptionValue("dest"), cl.getOptionValue("src"), forceOverwrite);
		}
	}

	private void printInfo(Options options) {
		logger.info(
				"Application builds installator jar file with provided war file inside. Provide source war file and installator name using application options :");
		options.getOptions().forEach((option) -> {
			StringBuilder optionInfoStringBuilder = new StringBuilder();
			logger.info(optionInfoStringBuilder.append("Name : ").append(option.getOpt()).append(" ,")
					.append(option.getDescription()).append(" Is required :  ").append(option.isRequired())
					.append(". Is flag : ").append(!option.hasArg()).toString());
		});
	}

	private boolean makeInstallatorJar(String installerName, String warPath, boolean forceOverwrite) {
		File installatorJarFile = new File("WebAppInstallator.jar");
		try (OutputStream outputStream = new FileOutputStream(installatorJarFile)) {
			IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream("WebAppInstallator.jar"), outputStream);// fill
																														// jar
																														// file
			try (JarFile installatorJar = new JarFile(installatorJarFile)) {
				File newInstallatorJarFile = new File(installerName + ".jar");
				if (newInstallatorJarFile.exists() && !forceOverwrite) {
					logger.info(
							"Installator with given name already exists. Rerun installer with -f flag set to true to continue and overwrite this file");
					return false;
				}
				Enumeration<JarEntry> enumEntries = installatorJar.entries();

				try (FileOutputStream newJarFileStream = new FileOutputStream(newInstallatorJarFile);
						JarOutputStream newJarStream = new JarOutputStream(newJarFileStream);
						FileInputStream warFile = new FileInputStream(warPath)) {
					// WarFile path stream

					JarEntry warEntry = new JarEntry("target.war");
					newJarStream.putNextEntry(warEntry);
					byte[] buffer = new byte[1024];
					int bytesRead;
					while ((bytesRead = warFile.read(buffer)) != -1) {
						newJarStream.write(buffer, 0, bytesRead);
					}

					while (enumEntries.hasMoreElements()) {
						JarEntry file = (JarEntry) enumEntries.nextElement();
						if (!file.getName().equals(warPath)) {
							// Get an input stream for the entry.
							try (InputStream entryStream = installatorJar.getInputStream(file)) {
								newJarStream.putNextEntry(file);
								while ((bytesRead = entryStream.read(buffer)) != -1) {
									newJarStream.write(buffer, 0, bytesRead);
								}
							}
						}
					}
				}
			}
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
			return false;
		} finally {
			installatorJarFile.delete();
		}
		return true;
	}

	public static void main(String[] args) {
		try {
			final CommandLineParser parser = new DefaultParser();
			Options options = new Options();
			Option src = Option.builder("src").hasArg().argName("src").desc("Path to input WAR file.").build();
			Option dest = Option.builder("dest").hasArg().argName("dest").desc("Output installator file path.").build();
			Option info = Option.builder("i").hasArg(false).desc("Prints info without executing wizard.").build();
			Option f = Option.builder("f").hasArg(false)
					.desc("Force creating installer jar when file with given name exists.").build();
			options.addOption(src);
			options.addOption(dest);
			options.addOption(info);
			options.addOption(f);
			CommandLine parsedCl = parser.parse(options, args);
			new WebAppInstallationWizard(parsedCl, options);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
			logger.error("Check -src and -dest arugments, and try again.");
		}
	}

}
