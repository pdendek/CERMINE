package pl.edu.icm.cermine.structure;

import java.io.*;
import java.util.Arrays;
import pl.edu.icm.cermine.PdfBxStructureExtractor;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.exception.TransformationException;
import pl.edu.icm.cermine.metadata.zoneclassification.features.*;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.model.BxPage;
import pl.edu.icm.cermine.structure.model.BxZone;
import pl.edu.icm.cermine.structure.model.BxZoneLabelCategory;
import pl.edu.icm.cermine.structure.transformers.BxDocumentToTrueVizWriter;
import pl.edu.icm.cermine.tools.classification.features.FeatureCalculator;
import pl.edu.icm.cermine.tools.classification.features.FeatureVectorBuilder;
import pl.edu.icm.cermine.tools.classification.svm.SVMZoneClassifier;

/**
 * SVM-based metadata zone classifier.
 * 
 * @author Paweł Szostek
 */
public class SVMMetadataZoneClassifier extends SVMZoneClassifier {
	private final static String MODEL_FILE_PATH = "/pl/edu/icm/cermine/structure/svm_meta_classifier";
	private final static String RANGE_FILE_PATH = "/pl/edu/icm/cermine/structure/svm_meta_classifier.range";
	
	public SVMMetadataZoneClassifier() throws AnalysisException {
		super(getFeatureVectorBuilder());
        try {
            loadModelFromResources(MODEL_FILE_PATH, RANGE_FILE_PATH);
        } catch (IOException ex) {
            throw new AnalysisException("Cannot create SVM classifier!", ex);
        }
	}
	
	public SVMMetadataZoneClassifier(BufferedReader modelFile, BufferedReader rangeFile) throws AnalysisException {
		super(getFeatureVectorBuilder());
        try {
            loadModelFromFile(modelFile, rangeFile);
        } catch (IOException ex) {
            throw new AnalysisException("Cannot create SVM classifier!", ex);
        }
	}

	public SVMMetadataZoneClassifier(String modelFilePath, String rangeFilePath) throws AnalysisException {
		super(getFeatureVectorBuilder());
        try {
            loadModelFromFile(modelFilePath, rangeFilePath);
        } catch (IOException ex) {
            throw new AnalysisException("Cannot create SVM classifier!", ex);
        }
	}

	public static FeatureVectorBuilder<BxZone, BxPage> getFeatureVectorBuilder() {
		FeatureVectorBuilder<BxZone, BxPage> vectorBuilder = new FeatureVectorBuilder<BxZone, BxPage>();
		vectorBuilder.setFeatureCalculators(Arrays
				.<FeatureCalculator<BxZone, BxPage>> asList(
						new AbstractFeature(),
						new AffiliationFeature(),
						new AuthorFeature(),
						new AuthorNameRelativeFeature(),
						new BibinfoFeature(),
						new CharCountFeature(),
						new CharCountRelativeFeature(),
						new ContributionFeature(),
						new DateFeature(),
						new DistanceFromNearestNeighbourFeature(),
						new DotCountFeature(),
						new DotRelativeCountFeature(),
						new EmailFeature(),
						new EmptySpaceRelativeFeature(),
						new FontHeightMeanFeature(),
						new FreeSpaceWithinZoneFeature(),
						new FullWordsRelativeFeature(),
						new HeightFeature(),
						new HeightRelativeFeature(),
						new HorizontalRelativeProminenceFeature(),
						new IsAfterMetTitleFeature(),
						new IsFontBiggerThanNeighboursFeature(),
						new IsGreatestFontOnPageFeature(),
						new IsWidestOnThePageFeature(),
						new KeywordsFeature(),
						new LastButOneZoneFeature(),
						new LineCountFeature(),
						new LineRelativeCountFeature(),
						new LineHeightMeanFeature(),
						new LineWidthMeanFeature(),
						new LineXPositionMeanFeature(),
						new LineXWidthPositionDiffFeature(),
						new LetterCountFeature(),
						new LetterRelativeCountFeature(),
						new LowercaseCountFeature(),
						new LowercaseRelativeCountFeature(),
						new PreviousZoneFeature(),
						new ProportionsFeature(),
						new PunctuationRelativeCountFeature(),
						new UppercaseCountFeature(),
						new UppercaseRelativeCountFeature(),
						new UppercaseWordCountFeature(),
						new UppercaseWordRelativeCountFeature(),
						new VerticalProminenceFeature(),
						new WidthFeature(),
						new WordCountFeature(),
						new WordCountRelativeFeature(),
						new WordWidthMeanFeature(),
						new WordLengthMeanFeature(),
						new WordLengthMedianFeature(),
						new WhitespaceCountFeature(),
						new WhitespaceRelativeCountLogFeature(),
						new WidthRelativeFeature(),
						new XPositionFeature(),
						new XPositionRelativeFeature(),
						new YPositionFeature(),
						new YPositionRelativeFeature(),
						new YearFeature()
						)
				);
		return vectorBuilder;
	}
    
    @Override
    public BxDocument classifyZones(BxDocument document) throws AnalysisException {
        for (BxPage page : document.getPages()) {
            for (BxZone zone : page.getZones()) {
                zone.setParent(page);
            }
        }
        for (BxZone zone: document.asZones()) {
            if (zone.getLabel().isOfCategoryOrGeneral(BxZoneLabelCategory.CAT_METADATA)) {
                zone.setLabel(predictLabel(zone, zone.getParent()));
            }
		}
        return document;
    }

	public static void main(String[] args) throws AnalysisException, IOException, TransformationException {
    	if(args.length != 1){
    		System.err.println("USAGE: program DIR_PATH");
    		System.exit(1);
    	}
    	PdfBxStructureExtractor structureExtractor = new PdfBxStructureExtractor();
    	SVMMetadataZoneClassifier metaClassifier = new SVMMetadataZoneClassifier();
    	File dir = new File(args[0]);
    	for(File pdf: dir.listFiles()) {
    		BxDocument result = structureExtractor.extractStructure(new FileInputStream(pdf));
    		result = metaClassifier.classifyZones(result);
    		FileWriter fstream = new FileWriter(pdf.getName() + ".xml");
            BufferedWriter out = new BufferedWriter(fstream);
            try {
                BxDocumentToTrueVizWriter writer = new BxDocumentToTrueVizWriter();
                out.write(writer.write(result.getPages()));
                writer.write(result.getPages());
            } finally {
                out.close();
            }
    	}
	}
}
