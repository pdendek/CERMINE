package pl.edu.icm.cermine;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.custommonkey.xmlunit.Diff;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

import pl.edu.icm.cermine.exception.AnalysisException;

/**
 *
 * @author Dominika Tkaczyk
 */
public class PdfNLMContentExtractorTest {
    static final private String TEST_FILE = "/pl/edu/icm/cermine/test2.pdf";
    static final private String EXP_FILE = "/pl/edu/icm/cermine/test2-cont.xml";
    
    private DocumentContentExtractor<Element> extractor;
    
    @Before
    public void setUp() throws AnalysisException, IOException {
        extractor = new PdfNLMContentExtractor();
    }
    
    @Ignore
    @Test
    public void metadataExtractionTest() throws AnalysisException, JDOMException, IOException, SAXException {
        InputStream testStream = this.getClass().getResourceAsStream(TEST_FILE);
        Element testContent;
        try {
            testContent = extractor.extractContent(testStream);
        } finally {
            testStream.close();
        }
        
        InputStream expStream = this.getClass().getResourceAsStream(EXP_FILE);
        InputStreamReader expReader = new InputStreamReader(expStream);
        SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        Document dom;
        try {
            dom = saxBuilder.build(expReader);
        } finally {
            expStream.close();
            expReader.close();
        }
        Element expContent = dom.getRootElement();
        
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        Diff diff = new Diff(outputter.outputString(expContent), outputter.outputString(testContent));
        System.out.println(outputter.outputString(testContent));
        System.out.println(">>>>>");
        System.out.println(outputter.outputString(expContent));
        assertTrue(diff.similar());
        
    }
}
