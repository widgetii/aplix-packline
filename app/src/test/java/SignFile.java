import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Signature;

import org.apache.commons.codec.binary.Base64;

public class SignFile {

	private static final String ALGORITHM = "MD5withRSA";

	private String keystore;
	private String keystorePass;
	private String alias;
	private String fileToSign;
	private String signature;
	private KeyStore.PrivateKeyEntry pkEntry;

	public static void main(String[] args) {
		try {
			SignFile sf = new SignFile();
			if (sf.parseCommandLine(args)) {
				sf.sign();
				sf.verify();
			}
		} catch (Exception e) {
			System.out.print("\n");
			e.printStackTrace();
		}
	}

	private boolean parseCommandLine(String[] args) {
		if (args == null || args.length < 5) {
			System.out.print(getUsage());
			return false;
		}

		int i = 0;
		while (i < args.length - 1) {
			String arg = args[i];

			if ("-keystore".equalsIgnoreCase(arg)) {
				keystore = args[++i];
			} else if ("-keystorepass".equalsIgnoreCase(arg)) {
				keystorePass = args[++i];
			} else if ("-alias".equalsIgnoreCase(arg)) {
				alias = args[++i];
			} else if ("-file".equalsIgnoreCase(arg)) {
				fileToSign = args[++i];
			}

			i++;
		}

		if (keystore == null || keystorePass == null || alias == null || fileToSign == null) {
			System.out.print(getUsage());
			return false;
		}

		return true;
	}

	private void sign() throws Exception {
		// Loading keystore
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream fis = null;
		try {
			System.out.print(String.format("Loading keystore '%s'.....", keystore));
			fis = new FileInputStream(keystore);
			ks.load(fis, keystorePass.toCharArray());
			System.out.println("Ok");
		} finally {
			if (fis != null) {
				fis.close();
			}
		}

		// Read existing entries from the keystore
		KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keystorePass.toCharArray());

		// get my private key
		System.out.print(String.format("Reading keystore entry '%s'.....", alias));
		pkEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, protParam);
		if (pkEntry == null) {
			throw new Exception(String.format("Entry '%s' does not exist", alias));
		}
		System.out.println("Ok");

		// Signing file
		System.out.print(String.format("Signing '%s'.....", fileToSign));
		Signature sig = Signature.getInstance(ALGORITHM);
		sig.initSign(pkEntry.getPrivateKey());

		fis = new FileInputStream(fileToSign);
		BufferedInputStream bufin = new BufferedInputStream(fis);
		byte[] buffer = new byte[1024];
		int len;
		while ((len = bufin.read(buffer)) >= 0) {
			sig.update(buffer, 0, len);
		}
		bufin.close();
		byte[] result = sig.sign();
		System.out.println("Ok");

		signature = Base64.encodeBase64String(result);
		System.out.println(String.format("The signature is:\n%s", signature));
	}

	private void verify() throws Exception {
		// Verifying signature
		System.out.print("Verifying signature.....");

		Signature sig = Signature.getInstance(ALGORITHM);
		sig.initVerify(pkEntry.getCertificate().getPublicKey());

		FileInputStream fis = new FileInputStream(fileToSign);
		BufferedInputStream bufin = new BufferedInputStream(fis);
		byte[] buffer = new byte[1024];
		int len;
		while ((len = bufin.read(buffer)) >= 0) {
			sig.update(buffer, 0, len);
		}
		bufin.close();

		boolean result = sig.verify(Base64.decodeBase64(signature));
		System.out.println(result ? "Ok" : "FALSE");
	}

	private String getUsage() {
		return "Usage: SignFile -keystore <path-to-keystore> -keystorepass <password> -file <file-to-sign>";
	}
}
