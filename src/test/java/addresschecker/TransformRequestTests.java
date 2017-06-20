package addresschecker;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;

import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.python.jline.internal.InputStreamReader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

public class TransformRequestTests {
    
    @Test
    public void testBasicAddress01() {
    	test("basic-address-01");
    }
    
    @Test
    public void testBasicAddress02() {
    	test("basic-address-02");
    }
    
    @Test
    public void testBasicAddress03() {
    	test("basic-address-03");
    }
    
    @Test
    public void testAddressWithUnit() {
    	test("address-with-unit");
    }
    
    private void test(String scenario) {
    	// get the input for the scenario
    	String input = getInput(scenario);
    	// simulate context for groovy script
    	Binding b = new Binding();
    	b.setProperty("payload", input);
    	script.setBinding(b);
    	// run script to generate output
    	String actualOutput = (String) script.run();
    	// compare expected output to actual output
    	assertEquals(getExpectedOutput(scenario), canonicalizeAndFormatXml(actualOutput));
    }
    
    private String getInput(String scenario) {
    	// load input from intput file
    	String inputFile = "/messages/input/" + scenario + ".json";
    	InputStream iis = TransformRequestTests.class.getResourceAsStream(inputFile);
    	if (iis == null) {
    		throw new RuntimeException(inputFile + " not found");
    	}
    	try {
			return IOUtils.toString(iis);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
    
    private String getExpectedOutput(String scenario) {
    	// load expected output from file
    	String expectedOutputFile = "/messages/output/" + scenario + ".xml";
    	InputStream eois = TransformRequestTests.class.getResourceAsStream(expectedOutputFile);
    	if (eois == null) {
    		return expectedOutputFile + " not found";
    	} else {
    		try {
				return canonicalizeAndFormatXml(IOUtils.toString(eois));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    private static Script script;
	
    @BeforeClass
    public static void beforeClass() throws InvalidCanonicalizerException {
    	// call groovy expressions from Java code
    	Binding binding = new Binding();
    	binding.setVariable("payload", new Integer(2));
    	InputStream stream = TransformRequestTests.class.getResourceAsStream("/addresschecker/transformRequest.groovy");
    	script = new GroovyShell().parse(new GroovyCodeSource(new InputStreamReader(stream), "addresschecker/transformRequest.groovy", ""));
    }
    
    private static Canonicalizer canonicalizer;
    
    @BeforeClass
    public static void initialiseCanonicalizer() throws InvalidCanonicalizerException {
    	Init.init();
    	canonicalizer = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS);
    }
    
    private static String canonicalizeAndFormatXml(String xml) {
		try {
			return new String(canonicalizer.canonicalize(toPrettyString(xml).getBytes()));
		} catch (CanonicalizationException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
    }

	// derived from http://stackoverflow.com/a/33541820/363573
    private static String toPrettyString(String xml) {
        try {
            // Turn xml string into a document
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

            // Remove whitespaces outside tags
            document.normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                                                          document,
                                                          XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                node.getParentNode().removeChild(node);
            }

            // Setup pretty print options
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // Return pretty print xml string
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}
