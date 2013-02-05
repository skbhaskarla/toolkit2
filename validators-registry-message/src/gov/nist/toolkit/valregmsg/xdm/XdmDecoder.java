package gov.nist.toolkit.valregmsg.xdm;

import gov.nist.toolkit.MessageValidatorFactory2.MessageValidatorFactoryFactory;
import gov.nist.toolkit.errorrecording.ErrorRecorder;
import gov.nist.toolkit.errorrecording.client.XdsErrorCode.Code;
import gov.nist.toolkit.errorrecording.factories.ErrorRecorderBuilder;
import gov.nist.toolkit.errorrecording.factories.TextErrorRecorderBuilder;
import gov.nist.toolkit.registrymetadata.Metadata;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.valccda.CdaDetector;
import gov.nist.toolkit.valregmsg.validation.factories.MessageValidatorFactory;
import gov.nist.toolkit.valsupport.client.ValidationContext;
import gov.nist.toolkit.valsupport.engine.MessageValidatorEngine;
import gov.nist.toolkit.valsupport.message.MessageValidator;
import gov.nist.toolkit.xdsexception.ExceptionUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

import org.apache.axiom.om.OMElement;

public class XdmDecoder extends MessageValidator {
	InputStream in;
	ErrorRecorderBuilder erBuilder;
	
	public XdmDecoder(ValidationContext vc, ErrorRecorderBuilder erBuilder, InputStream zipInputStream) {
		super(vc);
		this.erBuilder = erBuilder;
		in = zipInputStream;
	}
	
	OMap contents;
	
	boolean showContents = false;
	
	public static void main(String[] args) {
		try {
			InputStream is = Io.getInputStreamFromFile(new File(args[0]));
			ValidationContext vc = new ValidationContext();
			vc.isXDM = true;
			ErrorRecorderBuilder erBuilder = new TextErrorRecorderBuilder();
			ErrorRecorder er = erBuilder.buildNewErrorRecorder();
			MessageValidatorEngine mvc = new MessageValidatorEngine();
			
			XdmDecoder xd = new XdmDecoder(vc, erBuilder, is);
			xd.showContents = true;
			xd.run(er, mvc);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Does this InputStream hold an XDM?
	 * This will typically be called as:
	 *     new XdmDecoder().detect(is);
	 * and react to it by catching these three exceptions.
	 * @param is InputStream to test
	 * @throws ZipException - fails ZIP decoding
	 * @throws IOException - really bad
	 * @throws XDMException - ZIP decoding ok, critical XDM elements missing - probably ZIP of some other content
	 */
	public void detect(InputStream is) throws ZipException, IOException, XDMException {
		contents = new ZipDecoder().parse(is);
		if (!isXDM())
			throw new XDMException("ZIP does not contain XDM content");
	}
	
	public void run(ErrorRecorder er, MessageValidatorEngine mvc) {

		er.challenge("Decoding ZIP");
		try {
			contents = new ZipDecoder().parse(in);
		} 
		catch (ZipException e) {
			er.err(Code.NoCode, e);
			return;
		}
		catch (IOException e) {
			er.err(Code.NoCode, e);
			return;
		}
		
		if (showContents) {
			for (Path path : contents.keySet()) {
				System.out.println(path);
			}
		}
		
		er.challenge("Looking for INDEX.HTM");
		if (!hasIndexHtm()) {
			er.err(Code.NoCode, "File INDEX.HTM not found", "","");
		}
		
		er.challenge("Looking for README.TXT");
		if (!hasReadmeTxt()) {
			er.err(Code.NoCode, "File README.TXT not found", "","");
		}
		
		er.challenge("Looking for directory IHE_XDM");
		if (!hasIheXdm()) {
			er.err(Code.NoCode, "Directory IHE_XDM not found", "","");
			return;
		}
		
		List<Path> subsetDirs = getSubsetDirs();
		er.detail("SubmissionSet dirs are " + subsetDirs);
		
		for (Path subsetDir : subsetDirs) {
			if (!hasMetadata(subsetDir)) {
				er.err(Code.NoCode, "SubmissionSet directory <" + subsetDir + "> has no METADATA.XML file", "","");
			} else {
				Path metadataFilename = new Path(subsetDir + "METADATA.XML");
				er.challenge("Parsing metadata from " + metadataFilename);
				Metadata m = null;
				try {
					ByteArray ba = contents.get(metadataFilename);
					OMElement ele = Util.parse_xml(ba.getInputStream());
					
					ValidationContext vc = new ValidationContext();
					vc.hasHttp = false;
					vc.hasSaml = false;
					vc.hasSoap = false;
					vc.isAsync = false;
					vc.isR = true;
					vc.isXDM = true;
					vc.isRequest = true;
					vc.xds_b = true;
					vc.setCodesFilename(this.vc.getCodesFilename());
					MessageValidatorFactory.validateBasedOnValidationContext(
							erBuilder,
							ele,
							mvc,
							vc,
							null
							);
					
					m = new Metadata(ele);
					System.out.println("Metadata structure is " + m.structure());
					er.detail("Has " + m.getExtrinsicObjectIds().size() + " ExtrinsicObjects");
					for (OMElement eo : m.getExtrinsicObjects()) {
						er.detail("For ExtrinsicObject " + m.getId(eo));
						
						String hash = m.getSlotValue(eo, "hash", 0);
						String size = m.getSlotValue(eo, "size", 0);
						String uri  = m.getSlotValue(eo, "URI", 0);
						
						if (hash == null)
							er.err(Code.NoCode, "hash attribute not found", subsetDir,"");
						else
							er.detail("hash attribute found");
						if (size == null)
							er.err(Code.NoCode, "size attribute not found", subsetDir,"");
						else
							er.detail("size attribute found");
						if (uri == null) {
							er.err(Code.NoCode, "URI attribute not found", subsetDir,"");
							return;
						}
						else
							er.detail("uri attribute found");
						
						Path uriPath = new Path(subsetDir + uri);
						if (!contents.containsKey(uriPath)) {
							er.err(Code.NoCode, "URI attribute is " + uri + " but file does not exist", subsetDir,"");
							return;
						}
						else
							er.detail("document found for uri");
						
						ByteArray doc = contents.get(uriPath);
						
						if (size != null && !size.equals(doc.getSizeAsString()))
							er.err(Code.NoCode, "Metadata size is " + size + " but document size is " + doc.getSizeAsString(), subsetDir,"");
						else
							er.detail("size matches");
						if (hash != null && !hash.equalsIgnoreCase(doc.getSha1()))
							er.err(Code.NoCode, "Metadata hash is " + hash + " but document hash is " + doc.getSha1(), subsetDir,"");
						else
							er.detail("hash matches");
						
						// Attempt validation of document but only if it is a CDA R2 (and hopefully a CCDA)
						byte[] contents = doc.ba;

						if (new CdaDetector().isCDA(contents)) {
							er.detail("Input is CDA R2, try validation as CCDA");
							ValidationContext docVC = new ValidationContext();
							docVC.clone(vc);  // this leaves ccdaType in place since that is what is setting the expectations
							docVC.isDIRECT = false;
							docVC.isCCDA = true;

							MessageValidatorEngine mve = MessageValidatorFactoryFactory.messageValidatorFactory2I.getValidator((ErrorRecorderBuilder)er, contents, null, docVC, null);
							mve.run();
						} else {
							er.detail("Is not a CDA R2 so no validation attempted");
						}
						
					}
				} catch (Exception e) {
					er.err(Code.NoCode, "Error reading metadata from " + metadataFilename + "\n" + ExceptionUtil.exception_details(e), subsetDir,"");
					return;
				}
			}
		}
		
		mvc.run();
	}
	
	String simplifyPath(String path) {
		String[] parts = path.split(File.separator);
		rerun:
		for (int i=0; i<parts.length; i++) {
			if (parts[i].equals("..")) {
				if (i == 0)
					return path;
				int oldSize = parts.length;
				parts = deleteEntry(parts, i);
				parts = deleteEntry(parts, i-1);
				int newSize = parts.length;
				if (newSize < oldSize)
					continue rerun;
			}
		}
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<parts.length; i++) {
			if (buf.length() > 0) 
				buf.append(File.separator);
			buf.append(parts[i]);
		}
		return buf.toString();
	}
	
	List<String> asList(String[] parts) {
		List<String> lst = new ArrayList(parts.length);
		for (int i=0; i<parts.length; i++)
			lst.add(parts[i]);
		return lst;
	}
	
	String[] deleteEntry(String[] parts, int entry) {
		
		List<String> lst = asList(parts);
		lst.remove(entry);
		
		String[] ret = new String[lst.size()];
		for (int i=0; i<lst.size(); i++)	
			ret[i] = lst.get(i);
		
		return ret;
	}
	
	static String[] testdata = {
			"foo", "foo",
			"foo/../bar", "bar",
			"../foo", "../foo",
			"foo/bar/../baz/z/../x", "foo/baz/x",
			"", ""
	};
	
	XdmDecoder() {
		super(null);
	}
	
//	public static void main(String[] args) {
//		XdmDecoder x = new XdmDecoder();
//		for (int i=0; i<testdata.length; i+=2) {
//			String from = testdata[i];
//			String to = testdata[i+1];
//			String n = x.simplifyPath(from);
//			if (!to.equals(n)) {
//				System.out.println(to + " not equals " + n);
//			}
//		}
//	}
	
	public boolean isXDM() {
		if (!hasIheXdm()) return false;
		for (Path path: getSubsetDirs()) {
			if (hasMetadata(path))
				return true;
		}
		return false;
	}
	
	boolean hasMetadata(Path subsetDir) {
		Path x = subsetDir.withFile("METADATA.XML");
		return contents.containsKey(x);
	}
	
	boolean hasIheXdm() {
		return contents.containsKey(new Path("IHE_XDM/"));
	}
	
	boolean hasIndexHtm() {
		return contents.containsKey(new Path("INDEX.HTM"));
	}

	boolean hasReadmeTxt() {
		return contents.containsKey(new Path("README.TXT"));
	}
	
	List<Path> getSubsetDirs() {
		List<Path> dirs = new ArrayList<Path>();
		
		for (Path path : contents.keySet()) {
			if ("IHE_XDM".equals(path.getDir(0))) {
				if (path.getDir(1) != null && path.dirSize() == 2 && !path.hasFile())
					dirs.add(path);
			}
		}
		
		return dirs;
	}

	
	boolean splitMatch(String bigString, int index, String littleString) {
		String[] parts = bigString.split("/");
		if (index >= parts.length)
			return false;
		return littleString.equals(parts[index]);
	}
	
	int dirLevels(String path) {
		String[] parts = path.split("/");
		return parts.length;
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		for (Path key : contents.keySet())
			buf.append(key).append("\n");
		
		return buf.toString();
	}

}
