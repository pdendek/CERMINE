package pl.edu.icm.cermine.evaluation;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.DOMOutputter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import pl.edu.icm.cermine.PdfNLMMetadataExtractor;
import pl.edu.icm.cermine.evaluation.tools.CosineDistance;
import pl.edu.icm.cermine.evaluation.tools.DateComparator;
import pl.edu.icm.cermine.evaluation.tools.PdfNlmIterator;
import pl.edu.icm.cermine.evaluation.tools.PdfNlmPair;
import pl.edu.icm.cermine.evaluation.tools.SmithWatermanDistance;
import pl.edu.icm.cermine.evaluation.tools.StringTools;
import pl.edu.icm.cermine.evaluation.tools.XMLTools;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.exception.TransformationException;
/**
 *
 * @author Pawel Szostek (p.szostek@icm.edu.pl)
 */

public final class FinalMetadataExtractionEvaluation {
	
	private Boolean verbose = false;
	
	public FinalMetadataExtractionEvaluation(Boolean verbose) {
		this.verbose = verbose;
	}
	
	private void printVerbose(String text) {
		if(verbose) {
			System.out.println(text);
		}
	}
	
	private static class CorrectAllPair {
		public CorrectAllPair() {
			correct = 0;
			all = 0;
		}
		public Integer correct;
		public Integer all;
		
		@Override
		public String toString() {
			return "[Correct: " + correct + ", all: " + all + "]";
		}
		
		public Double calculateAccuracy() {
			if(all == 0) {
				return null;
			} else {
				return (double)correct/all;
			}
		}
	}
	
	public void evaluate(PdfNlmIterator iter, BufferedReader initialModel, BufferedReader initialRange, BufferedReader metaModel, BufferedReader metaRange) throws AnalysisException, IOException, TransformationException, ParserConfigurationException, SAXException,JDOMException, XPathExpressionException, TransformerException {
		PdfNLMMetadataExtractor metadataExtractor = new PdfNLMMetadataExtractor(initialModel, initialRange, metaModel, metaRange);
        
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setFeature("http://xml.org/sax/features/namespaces", false);
        dbf.setFeature("http://xml.org/sax/features/validation", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        javax.xml.parsers.DocumentBuilder documentBuilder = null;

    	try {
    		documentBuilder = dbf.newDocumentBuilder();
    	}
    	catch (javax.xml.parsers.ParserConfigurationException ex) {
    		ex.printStackTrace();
    		System.exit(1);
    	}

        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        builder.setValidation(false);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        CorrectAllPair issn = new CorrectAllPair();
        CorrectAllPair doi = new CorrectAllPair();
        CorrectAllPair urn = new CorrectAllPair();
        CorrectAllPair volume = new CorrectAllPair();
        CorrectAllPair issue = new CorrectAllPair();
        CorrectAllPair pages = new CorrectAllPair();
        CorrectAllPair dateYear = new CorrectAllPair();
        CorrectAllPair dateFull = new CorrectAllPair();
        CorrectAllPair title = new CorrectAllPair();

        List<Double> abstractRates = new ArrayList<Double>(iter.size());
        List<Double> titleRates = new ArrayList<Double>(iter.size());
        List<Double> journalTitleRates = new ArrayList<Double>(iter.size());
        List<Double> publisherNameRates = new ArrayList<Double>(iter.size());
        
        List<Double> keywordPrecisions = new ArrayList<Double>(iter.size());
        List<Double> keywordRecalls = new ArrayList<Double>(iter.size());

        List<Double> authorsPrecisions = new ArrayList<Double>(iter.size());
        List<Double> authorsRecalls = new ArrayList<Double>(iter.size());


        for (PdfNlmPair pair : iter) {
        	printVerbose(">>>>>>>>> " + pair.getPdf().getName());

        	org.w3c.dom.Document originalNlm = documentBuilder.parse(new FileInputStream(pair.getNlm()));

        	org.jdom.Element metaElement = metadataExtractor.extractMetadata(new FileInputStream(pair.getPdf()));
        	org.w3c.dom.Document extractedNlm = ElementToW3CDocument(metaElement);
            
//        	print(outputDoc(originalNlm));
//        	print(outputDoc(extractedNlm));
        	
            String expectedTitle = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/title-group/article-title");
            String extractedTitle = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/title-group/article-title");
            
            List<String> expectedAuthors = XMLTools.extractTextAsList(originalNlm, "/article/front/article-meta/contrib-group/contrib//name");
            List<String> extractedAuthors = XMLTools.extractTextAsList(extractedNlm, "/article/front/article-meta/contrib-group/contrib[@contrib-type='author']/string-name");

            List<String> expectedKeywords = XMLTools.extractTextAsList(originalNlm, "/article/front/article-meta/kwd-group/kwd");
            List<String> extractedKeywords = XMLTools.extractTextAsList(extractedNlm, "/article/front/article-meta/kwd-group/kwd");
            
            String expectedJournalTitle = XMLTools.extractTextFromNode(originalNlm, "/article/front/journal-meta/journal-title-group/journal-title");
            String extractedJournalTitle = XMLTools.extractTextFromNode(extractedNlm, "/article/front/journal-meta/journal-title-group/journal-title");
            
            String expectedPublisherName = XMLTools.extractTextFromNode(originalNlm, "/article/front/journal-meta/publisher/publisher-name");
            String extractedPublisherName = XMLTools.extractTextFromNode(extractedNlm, "/article/front/journal-meta/publisher/publisher-name");
            
            String expectedAbstract = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/abstract");
            String extractedAbstract = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/abstract");
            		
            String expectedDoi =  XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/article-id[@pub-id-type='doi']");
            String extractedDoi =  XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/article-id[@pub-id-type='doi']");
            
            String expectedISSN =  XMLTools.extractTextFromNode(originalNlm, "/article/front/journal-meta/issn[@pub-type='ppub']");
            String extractedISSN =  XMLTools.extractTextFromNode(extractedNlm, "/article/front/journal-meta/issn[@pub-type='ppub']");
            
            String expectedURN = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/article-id[@pub-id-type='urn']");
            String extractedURN = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/article-id[@pub-id-type='urn']");

            String expectedVolume = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/volume");
            String extractedVolume = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/volume");
            
            String expectedIssue = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/issue");
            String extractedIssue = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/issue");
            
            String expectedFPage = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/fpage");
            String extractedFPage = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/fpage");
            
            String expectedLPage = XMLTools.extractTextFromNode(originalNlm, "/article/front/article-meta/lpage") ;
            String extractedLPage = XMLTools.extractTextFromNode(extractedNlm, "/article/front/article-meta/lpage") ;
            
            List<String> expectedPubDate = XMLTools.extractTextAsList(originalNlm, "/article/front/article-meta/pub-date");
            expectedPubDate = removeLeadingZerosFromDate(expectedPubDate);
            List<String> extractedPubDate = XMLTools.extractTextAsList(extractedNlm, "/article/front/article-meta/pub-date");
            extractedPubDate = removeLeadingZerosFromDate(extractedPubDate);
            
            
            //equality measures
            if(!expectedVolume.isEmpty()) {
            	if(expectedVolume.equals(extractedVolume)) {
            		++volume.correct;
            	}
            	++volume.all;
            }
            if(!expectedIssue.isEmpty()) {
            	if(expectedIssue.equals(extractedIssue)) {
            		++issue.correct;
            	}
            	++issue.all;
            }
            if(!expectedISSN.isEmpty()) {
            	if(extractedISSN.equals(expectedISSN)) {
            		++issn.correct;
            	}
            	++issn.all;
            }
            if(!expectedDoi.isEmpty()) {
	            if(expectedDoi.equals(extractedDoi)) {
	            	++doi.correct;
	            }
	            ++doi.all;
            }
            if(!expectedURN.isEmpty()) {
	            if(expectedURN.equals(extractedURN)) {
	            	++urn.correct;
	            }
	            ++urn.all;
            }
            if(!expectedTitle.isEmpty()) {
	            if(new CosineDistance().compare(StringTools.tokenize(expectedTitle), StringTools.tokenize(extractedTitle)) > 0.8) {
	            	++urn.correct;
	            }
	            ++urn.all;
            }
            if(!expectedFPage.isEmpty() && !expectedLPage.isEmpty()) {
                if(expectedFPage.equals(extractedFPage) && expectedLPage.equals(extractedLPage)) {
                	++pages.correct;
                }
                ++pages.all;
            }

            
            if(!expectedPubDate.isEmpty()) {
            	Boolean yearsMatch = DateComparator.yearsMatch(expectedPubDate, extractedPubDate);
            	if(extractedPubDate != null && yearsMatch != null) {
	            	if(yearsMatch) {
	            		++dateYear.correct;
	            	}
	            	++dateYear.all;
            	}
            	Boolean datesMatch = DateComparator.datesMatch(expectedPubDate, extractedPubDate);
            	if(extractedPubDate!= null &&datesMatch != null) {
	            	if(datesMatch) {
	            		++dateFull.correct;
	            	}
	            	++dateFull.all;
            	}
            }
            
            //Smith-Waterman distance measures
            if(expectedPublisherName.length() > 0) {
            	publisherNameRates.add(compareStringsSW(expectedPublisherName, extractedPublisherName));
            } else {
            	publisherNameRates.add(null);
            }
            if(expectedAbstract.length() > 0) {
            	abstractRates.add(compareStringsSW(expectedAbstract, extractedAbstract));
            } else {
            	abstractRates.add(null);
            }
            if(expectedTitle.length() > 0) {
            	abstractRates.add(compareStringsSW(expectedTitle, extractedTitle));
            } else {
            	abstractRates.add(null);
            }
            if(expectedJournalTitle.length() > 0) {
            	journalTitleRates.add(compareStringsSW(expectedTitle, extractedTitle));
            } else {
            	journalTitleRates.add(null);
            }
            
            //precision + recall
            if(expectedAuthors.size() > 0) {
            	authorsPrecisions.add(calculatePrecision(expectedAuthors, extractedAuthors));
            	authorsRecalls.add(calculateRecall(expectedAuthors, extractedAuthors));
            } else {
            	authorsPrecisions.add(null);
            	authorsRecalls.add(null);
            }
            if(expectedKeywords.size() > 0) {
            	keywordPrecisions.add(calculatePrecision(expectedKeywords, extractedKeywords));
            	keywordRecalls.add(calculateRecall(expectedKeywords, extractedKeywords));
            } else {
            	keywordPrecisions.add(null);
            	keywordRecalls.add(null);
            }
            
            printVerbose(">>> Expected authors: ");
            for(String author: expectedAuthors) {
            	printVerbose(author);
            }
            
            printVerbose(">>> Extracted authors: ");
            for(String author: extractedAuthors) {
            	printVerbose(author);
            }
            
            printVerbose(">>> Expected keywords: ");
            for(String keyword: expectedKeywords) {
            	printVerbose(keyword);
            }
            
            printVerbose(">>> Extracted keywords: ");
            for(String keyword: extractedKeywords) {
            	printVerbose(keyword);
            }
            
            
            printVerbose(">>> Expected journal title: " + expectedJournalTitle);
            printVerbose(">>> Extracted journal title: " + extractedJournalTitle);
            
            printVerbose(">>> Expected publisher name: " + expectedPublisherName);
            printVerbose(">>> Extracted publisher name: " + extractedPublisherName);
            
            printVerbose(">>> Expected article title: " + expectedTitle);
            printVerbose(">>> Extracted article title: " + extractedTitle);

            printVerbose(">>> Expected article abstract: " + expectedAbstract);
            printVerbose(">>> Extracted article abstract: " + extractedAbstract);
            
            printVerbose(">>> Expected doi: " + expectedDoi);
            printVerbose(">>> Extracted doi: " + extractedDoi);
            
            printVerbose(">>> Expected abstract: " + expectedAbstract);
            printVerbose(">>> Extracted abstract: " + extractedAbstract);
            
            printVerbose(">>> Expected date: ");
            for(String date: expectedPubDate) {
            	printVerbose(date);
            }

            printVerbose(">>> Extracted date: ");
            for(String date: extractedPubDate) {
            	printVerbose(date);
            }
            printVerbose(">>> Expected pages" + expectedFPage + expectedLPage);
            printVerbose(">>> Extracted pages" + extractedFPage + extractedLPage);
            printVerbose("abstract " + abstractRates);
            printVerbose("title " + title);
            printVerbose("journal title " + journalTitleRates);
            printVerbose("publisher name rates " + publisherNameRates);
            printVerbose("namesP " + authorsPrecisions);
            printVerbose("namesR " + authorsRecalls);
            printVerbose("keywordsP " + keywordPrecisions);
            printVerbose("keywordsR " + keywordRecalls);
            printVerbose("date years" + dateYear);
            printVerbose("date full" + dateFull);
            printVerbose("doi" + doi);
            printVerbose("URN" + urn);
            printVerbose("pages" + pages);
        }
        
        Double value;
        System.out.println("==== Summary ("+ iter.size() +" docs)====");
        if((value = calculateAverage(abstractRates)) != null)
        	System.out.printf("abstract avg (SW) \t\t%4.2f\n", 100*value);
        if((value = calculateAverage(journalTitleRates)) != null)
        	System.out.printf("journal title avg (SW) \t\t%4.2f\n", 100*value);
        if((value = calculateAverage(publisherNameRates)) != null)
        	System.out.printf("publisher name (SW) \t\t%4.2f\n", 100*value);
        if((value = calculateAverage(titleRates)) != null)
        	System.out.printf("title avg (SW)\t%4.2f\n", 100*value);
        if((value = calculateAverage(titleRates)) != null)
        	System.out.printf("names precision avg (EQ)\t%4.2f\n", 100*value);
        if((value = calculateAverage(authorsRecalls)) != null)
        	System.out.printf("names recall avg (EQ)\t\t%4.2f\n", 100*value);
        if((value = calculateAverage(keywordPrecisions)) != null)
        	System.out.printf("keywords precision avg (EQ)\t%4.2f\n", 100*value);
        if((value = calculateAverage(keywordRecalls)) != null)
        	System.out.printf("keywords recall" +
        			" avg (EQ)\t%4.2f\n", 100*value);
        if((value = title.calculateAccuracy()) != null)
        	System.out.printf("title accuracy avg\t\t%4.2f\n", 100*value);
        if((value = dateYear.calculateAccuracy()) != null)
        	System.out.printf("date year accuracy avg\t\t%4.2f\n", 100*value);
        if((value = dateFull.calculateAccuracy()) != null)
        	System.out.printf("date full accuracy avg\t\t%4.2f\n", 100*value);
        if((value = doi.calculateAccuracy()) != null)
        	System.out.printf("doi accuracy avg\t\t%4.2f\n", 100*value);
        if((value = urn.calculateAccuracy()) != null) 
        	System.out.printf("URN accuracy avg\t\t%4.2f\n", 100*value);
        if((value = issue.calculateAccuracy()) != null) 
        	System.out.printf("issue accuracy avg\t\t%4.2f\n", 100*value);
        if((value = volume.calculateAccuracy()) != null) 
        	System.out.printf("volume accuracy avg\t\t%4.2f\n", 100*value);
        if((value = pages.calculateAccuracy()) != null)
        	System.out.printf("pages accuracy avg\t\t%4.2f\n", 100*value);
	}


    private static Double calculateAverage(List<Double> values) {
    	Integer all = 0;
    	Double sum = .0;
    	for(Double value: values) {
    		if(value != null) {
    			++all;
    			sum += value;
    		}
    	}
    	return sum/all;
    }
    
    private static Double calculatePrecision(List<String> expected, List<String> extracted) {
    	if(extracted.size() == 0) {
    		return .0;
    	}
    	Integer correct = 0;
    	CosineDistance cos = new CosineDistance();
    	external: for(String partExt: extracted) {
    				 for(String partExp: expected) {
    					if(cos.compare(StringTools.tokenize(partExt), StringTools.tokenize(partExp)) > Math.sqrt(2)/2) {
    						++correct;
    						continue external;
    					}
    				 }
    	}
    	return (double)correct/extracted.size();
    }
   
    private static Double calculateRecall(List<String> expected, List<String> extracted) {
    	Integer correct = 0;
    	CosineDistance cos = new CosineDistance();
    	external: for(String partExt: extracted) {
    		internal: for(String partExp: expected) {
    			if(cos.compare(StringTools.tokenize(partExt), StringTools.tokenize(partExp)) > Math.sqrt(2)/2) {
    				++correct;
    				continue external;
    			}
    		}
    	}
    	return (double)correct/expected.size();
    }
    
    private static Double compareStringsSW(String expectedText, String extractedText) {
		List<String> expectedTokens = StringTools.tokenize(expectedText);
		List<String> extractedTokens = StringTools.tokenize(extractedText);
		SmithWatermanDistance distanceFunc = new SmithWatermanDistance(.0, .0);
		Double distance = distanceFunc.compare(expectedTokens, extractedTokens);
		return distance/(double)expectedTokens.size();
	}

	static List<String> removeLeadingZerosFromDate(List<String> strings) {
    	List<String> ret = new ArrayList<String>();
    	for(String string: strings) {
    		String[] parts = string.split("\\s");
    		if(parts.length > 1) {
	    		List<String> newDate = new ArrayList<String>();
	    		for(String part: parts) {
	    			newDate.add(part.replaceFirst("^0+(?!$)", ""));
	    		}
	    		ret.add(StringUtils.join(newDate, " "));
    		} else {
    			ret.add(string);
    		}
    	}
    	return ret;
    }
    
    static org.w3c.dom.Document ElementToW3CDocument(org.jdom.Element elem) throws JDOMException {
        org.jdom.Document metaDoc = new org.jdom.Document();
        metaDoc.setRootElement(elem);
        org.jdom.output.DOMOutputter domOutputter = new DOMOutputter();
        org.w3c.dom.Document document = domOutputter.output(metaDoc);
        return document;
    }
    
      static String outputDoc(Document document) throws IOException, TransformerException {
    	  OutputFormat format = new OutputFormat(document); //document is an instance of org.w3c.dom.Document
    	  format.setLineWidth(65);
    	  format.setIndenting(true);
    	  format.setIndent(2);
    	  Writer out = new StringWriter();
    	  XMLSerializer serializer = new XMLSerializer(out, format);
    	  serializer.serialize(document);
    	  return out.toString();
      }
      

      public static void main(String[] args) throws AnalysisException, IOException, TransformationException, ParserConfigurationException, SAXException,JDOMException, XPathExpressionException, TransformerException {
          if (args.length < 5 || args.length > 6) {
              System.out.println("Usage: FinalEffectEvaluator [-v] <input path> <initial-model> <initial-range> <meta-model> <meta-range>");
              return;
          }
          Boolean verbose = false;
          String directory = null;
          Integer base = 0;
          if(args[0].equals("-v")) {
          	verbose = true;
          	directory = args[1];
          	base=2;
          } else {
        	  directory = args[0];
        	  base = 1;
          }
          
          BufferedReader initialModel= new BufferedReader(new InputStreamReader(new FileInputStream(args[0+base])));
          BufferedReader initialRange= new BufferedReader(new InputStreamReader(new FileInputStream(args[1+base])));
          BufferedReader metaModel= new BufferedReader(new InputStreamReader(new FileInputStream(args[2+base])));
          BufferedReader metaRange= new BufferedReader(new InputStreamReader(new FileInputStream(args[3+base])));
          FinalMetadataExtractionEvaluation e = new FinalMetadataExtractionEvaluation(verbose);
          PdfNlmIterator iter = new PdfNlmIterator(directory);
          e.evaluate(iter, initialModel, initialRange, metaModel, metaRange);
      }
}
