package gov.nist.toolkit.testengine.scripts;

import gov.nist.toolkit.registrysupport.MetadataSupport;
import gov.nist.toolkit.utilities.io.Io;
import gov.nist.toolkit.utilities.xml.OMFormatter;
import gov.nist.toolkit.utilities.xml.Util;
import gov.nist.toolkit.valregmetadata.coding.AllCodes;
import gov.nist.toolkit.valregmetadata.coding.Code;
import gov.nist.toolkit.valregmetadata.coding.CodesFactory;
import gov.nist.toolkit.valregmetadata.coding.Uuid;
import gov.nist.toolkit.xdsexception.XdsInternalException;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.FileUtils;

import javax.xml.namespace.QName;
import javax.xml.parsers.FactoryConfigurationError;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * This class is a script supposed to update all the metadata files living inside the testkit
 * for their codes to match the ones in the configuration file codes.xml.
 * Created by onh2 on 6/24/2015.
 */
public class UpdateCodes {
    File testkit;
    AllCodes allCodes=null;
    boolean error;
    static String sections[] = { "testdata", "tests", "examples"/*, "selftest"*/ };
    List<String> metadataFilesPaths=new ArrayList<String>();
    Map<String,Code> replacementMap= new HashMap<String,Code>();

    /**
     * This method scans the testkit for metadata files.
     */
    void scan() {
        error = false;
        for (int i=0; i<sections.length; i++) {
            String section = sections[i];

            File sectionFile = new File(testkit + File.separator + section);

            try {
                exploreTests(sectionFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method explores a test repository structure.
     *
     * @param testFile path to the testkit folder which contains the test section to explore.
     * @throws IOException
     */
    void exploreTests(File testFile) throws IOException {
        try {
            File[] dirs = testFile.listFiles();
            if (dirs == null) {
                System.out.println("No tests defined in " + dirs);
                error = true;
            }
            for (int i = 0; i < dirs.length; i++) {
                File testDir = dirs[i];
                if (testDir.getName().equals(".svn"))
                    continue;
                if (testDir.isDirectory()) {
                    exploreTests(testDir);
                } else {
                    if ("testplan.xml".equals(testDir.getName())) {
                        // read testplan.xml
                        String testplanContent = Io.stringFromFile(testDir);
                        OMElement testplanNode = Util.parse_xml(testplanContent);
                        // retrieve the TestStep nodes
                        Iterator<OMElement> steps = testplanNode.getChildrenWithName(new QName("TestStep"));
                        while (steps.hasNext()) {
                            // find transaction nodes among the nodes under exploration (Under a TestStep)
                            Iterator<OMElement> children = steps.next().getChildElements();
                            exploreChildren(children, testFile);
                        }
                    }
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }

    }

    private void processTestplans(File testFile) {
        try {
            File[] dirs = testFile.listFiles();
            if (dirs == null) {
                System.out.println("No tests defined in " + dirs);
                error = true;
            }
            for (int j = 0; j < dirs.length; j++) {
                File testDir = dirs[j];
                if (testDir.getName().equals(".svn"))
                    continue;
                if (testDir.isDirectory()) {
                    processTestplans(testDir);
                } else {
                    if ("testplan.xml".equals(testDir.getName())) {
                        // read testplan.xml
                        String testplanContent = Io.stringFromFile(testDir);
                        for (String oldCode : replacementMap.keySet()) {
                            if(oldCode.contains("DEMO-Summary^^1.3.6.1.4.1.21367.100.1"))
                            System.out.println(oldCode);
                            String oldCodeSplit[] = oldCode.split("\\^");
                            String code1 = oldCodeSplit[0];
                            String scheme1 = oldCodeSplit[1];
                            String name1 = oldCodeSplit[2];
                            testplanContent.replaceAll(code1, replacementMap.get(oldCode).getCode());
                            testplanContent.replaceAll(scheme1, replacementMap.get(oldCode).getScheme());
                            testplanContent.replaceAll(name1, replacementMap.get(oldCode).getDisplay());
                            if (testDir.getPath().contains("11897")&&testDir.getPath().contains("classcode_one")&&oldCode.contains("DEMO-Summary^^1.3.6.1.4.1.21367.100.1")) {
                                System.out.println(testDir);
                                System.out.println(testplanContent);
                            }
                        }
                        File backupFile = new File(testDir.toString() + ".bak");
                        if (!backupFile.exists()) {
                            // backup the unmodified file before updating
                            FileUtils.copyFile(testDir, backupFile);
                        }
                        Io.stringToFile(testDir, new OMFormatter(testplanContent).toString());
                    }
                }

            }
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     *  This method explores all the nodes under a set of nodes node in parameter to find MetadataFile nodes.
     *
     *  @param children Set of nodes to explore
     *  @param testFile Path to the folder being explored
     */
    private void exploreChildren(Iterator<OMElement> children,File testFile) {
        while(children!=null && children.hasNext()){
            OMElement node=children.next();
            // find transaction nodes among the nodes under exploration
            OMElement transaction = null;
            if (node.getLocalName().matches(".*Transaction")){
                transaction=node;
            }
            if (transaction!=null) {
                // getRetrievedDocumentsModel metadata file(s) name
                OMElement metadataFile=transaction.getFirstChildWithName(new QName("MetadataFile"));
                if (metadataFile!=null) {
                    metadataFilesPaths.add(testFile + "/" + metadataFile.getText());
                }
            }
            // the following is probably useless
            Iterator<OMElement> c = node.getChildElements();
            while (c.hasNext()){
                exploreChildren(c, testFile);
            }
        }
    }

    /**
     * This method method reads the different files found previously in the testkit to update non-conforming codes.
     */
    private void processCodes() {
        try {
            for (String filePath:metadataFilesPaths){
                File file=new File(filePath);
                if (file.exists()) {
                    File backupFile = new File(file.toString() + ".bak");
                    if (!backupFile.exists()) {
                        // backup the unmodified file before updating
                        FileUtils.copyFile(file, backupFile);
                    }
                    // read the file
                    OMElement metadataElement = Util.parse_xml(file);
                    List<OMElement> badCodes = findNonConformingCodes(metadataElement);
                    System.out.println(badCodes.size() + " bad codes to update in " + filePath);
                    // update bad codes
                    updateCodes(badCodes);
                    // update the file itself
                    Io.stringToFile(file, new OMFormatter(metadataElement).toString());
                }else{
                    System.err.println("WARNING: "+filePath+" file does not exist in Testkit where it should be.");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * This method explore a parsed document and looks for non-conforming codes with codes.xml file.
     * @param metadataElement parsed document to analyze
     * @return list of non-confirming code elements
     */
    private List<OMElement> findNonConformingCodes(OMElement metadataElement) {
        List<OMElement> badCodes = new ArrayList<OMElement>();
        List<OMElement> classifications = MetadataSupport.decendentsWithLocalName(metadataElement, "Classification");
        for (OMElement classification : classifications) {
            String classificationScheme = classification.getAttributeValue(MetadataSupport.classificationscheme_qname);
            if (classificationScheme == null || classificationScheme.equals(""))
                continue;
            Uuid classificationUuid = new Uuid(classificationScheme);
            if (!allCodes.isKnownClassification(classificationUuid))
                continue;
            Code code = getCode(classification);
            if (!allCodes.exists(classificationUuid, code))
                badCodes.add(classification);
        }
        return badCodes;
    }

    /**
     * This method returns a Code out of a classification element.
     * @param classificationElement classification element to extract
     * @return code object
     */
    public Code getCode(OMElement classificationElement){
        // getRetrievedDocumentsModel coding scheme
        String value = classificationElement.getAttributeValue(MetadataSupport.noderepresentation_qname);

        // getRetrievedDocumentsModel display name
        String displayName = null;
        OMElement nameElement = MetadataSupport.firstChildWithLocalName(classificationElement, "Name");
        OMElement localizedStringElement = MetadataSupport.firstChildWithLocalName(nameElement, "LocalizedString");
        if (nameElement == null || localizedStringElement == null) displayName="";
        displayName=localizedStringElement.getAttributeValue(MetadataSupport.value_qname);

        // getRetrievedDocumentsModel code
        String codeSystem = "";
        OMElement codeSystemElement;
        OMElement slot = MetadataSupport.firstChildWithLocalName(classificationElement, "Slot");
        OMElement valueList = MetadataSupport.firstChildWithLocalName(slot, "ValueList");
        OMElement v = MetadataSupport.firstChildWithLocalName(valueList, "Value");
        if (slot == null || valueList == null || v==null){
            codeSystemElement= null;
        } else{
            codeSystemElement= v;
        }

        // return the code object
        if (codeSystemElement == null) return new Code(value, codeSystem, displayName);
        codeSystem = codeSystemElement.getText();
        return new Code(value, codeSystem, displayName);
    }

    /**
     * This method takes care of updating the non-conforming codes.
     * @param badCodes list of non-conforming codes to update.
     * @throws XdsInternalException
     * @throws FactoryConfigurationError
     */
    void updateCodes(List<OMElement> badCodes) throws XdsInternalException, FactoryConfigurationError {
        for (OMElement classification : badCodes) {
            updateClassification(classification);
        }
    }

    /**
     * This method updates the classification of the metadata element itself.
     * @param classification classification to update.
     */
    void updateClassification(OMElement classification) {
        // create variables of the code to be replaced
        OMAttribute codeToReplace=classification.getAttribute(MetadataSupport.noderepresentation_qname)/*.setAttributeValue(code.getCode())*/;
        OMElement valueToReplace=null;
        OMElement slot = MetadataSupport.firstChildWithLocalName(classification, "Slot");
        if (slot != null) {
            OMElement valueList = MetadataSupport.firstChildWithLocalName(slot, "ValueList");
            if (valueList!=null) {
                valueToReplace = MetadataSupport.firstChildWithLocalName(valueList, "Value");
            }
        }
        OMElement nameElement = MetadataSupport.firstChildWithLocalName(classification, "Name");
        if (nameElement == null) return;
        OMElement localizedStringElement = MetadataSupport.firstChildWithLocalName(nameElement, "LocalizedString");
        if (localizedStringElement == null) return;
        OMAttribute nameToReplace = localizedStringElement.getAttribute(MetadataSupport.value_qname);
        Code oldCode=new Code(codeToReplace.getAttributeValue(),valueToReplace.getText(),nameToReplace.getAttributeValue());
        // check if the code to be replaced as already been changed before.
        Code replacementCode=replacementMap.get(oldCode.toString());
        if (replacementCode==null){
            // if not, assign a new code
            String classificationScheme = classification.getAttributeValue(MetadataSupport.classificationscheme_qname);
            Uuid classificationUuid = new Uuid(classificationScheme);
            // pick a new conforming code out of all the codes available in codes.xml
            Code newCode = allCodes.pick(classificationUuid);
            replacementCode=newCode;
            replacementMap.put(oldCode.toString(),replacementCode);
        }
        // replace the code
        codeToReplace.setAttributeValue(replacementCode.getCode());
        nameToReplace.setAttributeValue(replacementCode.getDisplay());
        valueToReplace.setText(replacementCode.getScheme());
    }

    public static void main(String[] args) {
        UpdateCodes uc=new UpdateCodes();
        // init codes
        uc.allCodes = new CodesFactory().load(new File(args[1]));
        // init testkit
        uc.testkit = new File(args[0]);
        uc.scan();
        uc.error = false;
        uc.processCodes();
        System.out.println(uc.replacementMap);
        for (int i=0; i<sections.length; i++) {
            String section = sections[i];

            File sectionFile = new File(uc.testkit + File.separator + section);


            uc.processTestplans(sectionFile);
        }
    }
}
