package pl.edu.icm.cermine.libsvm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import pl.edu.icm.cermine.evaluation.tools.EvaluationUtils;
import pl.edu.icm.cermine.evaluation.tools.EvaluationUtils.DocumentsIterator;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.exception.TransformationException;
import pl.edu.icm.cermine.structure.HierarchicalReadingOrderResolver;
import pl.edu.icm.cermine.structure.SVMInitialZoneClassifier;
import pl.edu.icm.cermine.structure.SVMMetadataZoneClassifier;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.model.BxPage;
import pl.edu.icm.cermine.structure.model.BxZone;
import pl.edu.icm.cermine.structure.model.BxZoneLabel;
import pl.edu.icm.cermine.structure.model.BxZoneLabelCategory;
import pl.edu.icm.cermine.tools.classification.features.FeatureVectorBuilder;
import pl.edu.icm.cermine.tools.classification.general.BxDocsToTrainingSamplesConverter;
import pl.edu.icm.cermine.tools.classification.general.TrainingSample;
import pl.edu.icm.cermine.tools.classification.sampleselection.SampleFilter;

public class LibSVMExporter {

    public static void toLibSVM(TrainingSample<BxZoneLabel> trainingElement, BufferedWriter fileWriter) throws IOException {
        try {
        	if(trainingElement.getLabel() == null) {
        		return;
        	}
        	fileWriter.write(String.valueOf(trainingElement.getLabel().ordinal()));
        	fileWriter.write(" ");
        	
        	Integer featureCounter = 1;
        	for (Double value : trainingElement.getFeatureVector().getFeatureValues()) {
        		StringBuilder sb = new StringBuilder();
        		Formatter formatter = new Formatter(sb, Locale.US);
        		formatter.format("%d:%.5f", featureCounter++, value);
        		fileWriter.write(sb.toString());
        		fileWriter.write(" ");
        	}
        	fileWriter.write("\n");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
    }
    public static void toLibSVM(List<TrainingSample<BxZoneLabel>> trainingElements, String filePath) throws IOException {
    	BufferedWriter svmDataFile = null;
        try {
            FileWriter fstream = new FileWriter(filePath);
            svmDataFile = new BufferedWriter(fstream);
            for (TrainingSample<BxZoneLabel> elem : trainingElements) {
            	if(elem.getLabel() == null) {
            		continue;
            	}
                svmDataFile.write(String.valueOf(elem.getLabel().ordinal()));
                svmDataFile.write(" ");

                Integer featureCounter = 1;
                for (Double value : elem.getFeatureVector().getFeatureValues()) {
                    StringBuilder sb = new StringBuilder();
                    Formatter formatter = new Formatter(sb, Locale.US);
                    formatter.format("%d:%.5f", featureCounter++, value);
                    svmDataFile.write(sb.toString());
                    svmDataFile.write(" ");
                }
                svmDataFile.write("\n");
            }
            svmDataFile.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return;
        } finally {
        	if(svmDataFile != null) {
        		svmDataFile.close();
        	}
        }
        
        System.out.println("Done.");
    }
    
    public static void main(String[] args) throws ParseException, IOException, TransformationException, AnalysisException, CloneNotSupportedException {
        Options options = new Options();
        options.addOption("under", false, "use undersampling for data selection");
        options.addOption("over", false, "use oversampling for data selection");
        options.addOption("normal", false, "don't use any special strategy for data selection");

        CommandLineParser parser = new GnuParser();
        CommandLine line = parser.parse(options, args);

        if (args.length != 2 ||  !(line.hasOption("under") ^ line.hasOption("over") ^ line.hasOption("normal"))) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" [-options] input-directory", options);
            System.exit(1); 
        }
        String inputDirPath = line.getArgs()[0];
        File inputDirFile = new File(inputDirPath);

		// SampleSelector<BxZoneLabel> sampler = null;
		// if (line.hasOption("over")) {
		// sampler = new OversamplingSampler<BxZoneLabel>(1.0);
		// } else if (line.hasOption("under")) {
		// sampler = new UndersamplingSelector<BxZoneLabel>(1.0);
		// } else if (line.hasOption("normal")) {
		// sampler = new NormalSelector<BxZoneLabel>();
		// } else {
		// System.err.println("Sampling pattern is not specified!");
		// System.exit(1);
		// }

		// List<TrainingSample<BxZoneLabel>> initialTrainingElements = new
		// ArrayList<TrainingSample<BxZoneLabel>>();
		// List<TrainingSample<BxZoneLabel>> metaTrainingElements = new
		// ArrayList<TrainingSample<BxZoneLabel>>();
        
        Integer docIdx = 0;

        HierarchicalReadingOrderResolver ror = new HierarchicalReadingOrderResolver();
        EvaluationUtils.DocumentsIterator iter = new DocumentsIterator(inputDirPath);

        FeatureVectorBuilder<BxZone, BxPage> metaVectorBuilder = SVMMetadataZoneClassifier.getFeatureVectorBuilder();
        FeatureVectorBuilder<BxZone, BxPage> initialVectorBuilder = SVMInitialZoneClassifier.getFeatureVectorBuilder();

		SampleFilter metaSamplesFilter = new SampleFilter(
				BxZoneLabelCategory.CAT_METADATA);

		FileWriter initialStream = new FileWriter("initial_"
				+ inputDirFile.getName() + ".dat");
		BufferedWriter svmInitialFile = new BufferedWriter(initialStream);

		FileWriter metaStream = new FileWriter("meta_" + inputDirFile.getName()
				+ ".dat");
		BufferedWriter svmMetaFile = new BufferedWriter(metaStream);

        for(BxDocument doc: iter) {
        	System.out.println(docIdx + ": " + doc.getFilename());
        	String filename = doc.getFilename();
        	doc = ror.resolve(doc);
        	doc.setFilename(filename);
        	////
        	for (BxZone zone : doc.asZones()) {
        		if (zone.getLabel() != null) {
        			if (zone.getLabel().getCategory() != BxZoneLabelCategory.CAT_METADATA) {
        				zone.setLabel(zone.getLabel().getGeneralLabel());
        			}
        		}
        		else {
        			zone.setLabel(BxZoneLabel.OTH_UNKNOWN);
        		}
        	}
			List<TrainingSample<BxZoneLabel>> newMetaSamples = BxDocsToTrainingSamplesConverter
					.getZoneTrainingSamples(doc, metaVectorBuilder,
							BxZoneLabel.getIdentityMap());
			newMetaSamples = metaSamplesFilter.pickElements(newMetaSamples);
        	
        	////
			List<TrainingSample<BxZoneLabel>> newInitialSamples = BxDocsToTrainingSamplesConverter
					.getZoneTrainingSamples(doc, initialVectorBuilder,
							BxZoneLabel.getLabelToGeneralMap());
			// /
			for (TrainingSample<BxZoneLabel> sample : newMetaSamples) {
				toLibSVM(sample, svmMetaFile);
			}
			for (TrainingSample<BxZoneLabel> sample : newInitialSamples) {
				toLibSVM(sample, svmInitialFile);
			}
        	++docIdx;
        }
		svmInitialFile.close();
		svmMetaFile.close();

		// initialTrainingElements =
		// sampler.pickElements(initialTrainingElements);
		// metaTrainingElements = sampler.pickElements(metaTrainingElements);

		// toLibSVM(initialTrainingElements, "initial_" + inputDirFile.getName()
		// + ".dat");
		// toLibSVM(metaTrainingElements, "meta_" + inputDirFile.getName() +
		// ".dat");
    }
}