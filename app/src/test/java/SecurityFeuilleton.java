import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;

public class SecurityFeuilleton {

	private String certificate;
	private String fileToUpdate;
	private String oldSequenceFile;

	public static void main(String[] args) {
		try {
			SecurityFeuilleton sf = new SecurityFeuilleton();
			if (sf.parseCommandLine(args)) {
				sf.updateFile();
			}
		} catch (Exception e) {
			System.out.print("\n");
			e.printStackTrace();
		}
	}

	private boolean parseCommandLine(String[] args) {
		if (args == null || args.length < 3) {
			System.out.print(getUsage());
			return false;
		}

		int i = 0;
		while (i < args.length - 1) {
			String arg = args[i];

			if ("-certificate".equalsIgnoreCase(arg)) {
				certificate = args[++i];
			} else if ("-file".equalsIgnoreCase(arg)) {
				fileToUpdate = args[++i];
			} else if ("-oldSequence".equalsIgnoreCase(arg)) {
				oldSequenceFile = args[++i];
			}

			i++;
		}

		if (certificate == null || fileToUpdate == null || oldSequenceFile == null) {
			System.out.print(getUsage());
			return false;
		}

		return true;
	}

	private void updateFile() throws Exception {
		// Loading certificate
		System.out.print(String.format("Loading certificate '%s'.....", certificate));
		InputStream certFileIs = new FileInputStream(certificate);
		CertificateFactory cf = CertificateFactory.getInstance("X509");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(certFileIs);
		System.out.println("Ok");

		// Seek for old sequence
		byte[] fileContents = IOUtils.toByteArray(new FileInputStream(fileToUpdate));
		byte[] oldSequence = IOUtils.toByteArray(new FileInputStream(oldSequenceFile));
		byte[] newSequence = cert.getPublicKey().getEncoded();

		if (oldSequence.length != newSequence.length) {
			System.out.println("Old and new sequences have different length, update failed");
			return;
		}

		int pos = indexOf(fileContents, oldSequence);
		if (pos == -1) {
			System.out.println(String.format("Old sequence not found in '%s'", fileToUpdate));
			return;
		}

		System.out.println(String.format("Old sequence was found on posistion 0x%x", pos));

		// Update byte buffer with new sequence
		System.out.print("Updating sequence at the given position.....");
		for (int i = 0; i < newSequence.length; i++) {
			fileContents[pos + i] = newSequence[i];
		}
		System.out.println("Ok");

		// Renaming file to update

		System.out.print("Backuping file.....");
		File originalFile = new File(fileToUpdate);
		File destinationFile = new File(String.format("%s.bak", fileToUpdate));
		if (!originalFile.renameTo(destinationFile)) {
			System.out.println("FAILED");
		} else {
			System.out.println("Ok");
		}

		// Saving changes
		System.out.print(String.format("Saving changes back to '%s'.....", fileToUpdate));
		FileOutputStream fos = new FileOutputStream(fileToUpdate);
		try {
			IOUtils.write(fileContents, fos);
		} finally {
			fos.close();
		}
		System.out.println("Ok");
	}

	/**
	 * Finds the first occurrence of the pattern in the text.
	 */
	public static int indexOf(byte[] data, byte[] pattern) {
		int[] failure = computeFailure(pattern);

		int j = 0;
		if (data.length == 0)
			return -1;

		for (int i = 0; i < data.length; i++) {
			while (j > 0 && pattern[j] != data[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == data[i]) {
				j++;
			}
			if (j == pattern.length) {
				return i - pattern.length + 1;
			}
		}
		return -1;
	}

	/**
	 * Computes the failure function using a boot-strapping process, where the
	 * pattern is matched against itself.
	 */
	private static int[] computeFailure(byte[] pattern) {
		int[] failure = new int[pattern.length];

		int j = 0;
		for (int i = 1; i < pattern.length; i++) {
			while (j > 0 && pattern[j] != pattern[i]) {
				j = failure[j - 1];
			}
			if (pattern[j] == pattern[i]) {
				j++;
			}
			failure[i] = j;
		}

		return failure;
	}

	private String getUsage() {
		return "Usage: SecurityFeuilleton -certificate <path-to-certificate> -file <file-to-update> -oldSequence <old-sequence-file>";
	}
}
