package pl.edu.icm.cermine.tools.classification.svm;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.apache.commons.collections.iterators.ArrayIterator;

import pl.edu.icm.cermine.structure.model.BxPage;
import pl.edu.icm.cermine.structure.model.BxZone;
import pl.edu.icm.cermine.tools.classification.features.FeatureVector;
import pl.edu.icm.cermine.tools.classification.features.FeatureVectorBuilder;
import pl.edu.icm.cermine.tools.classification.general.FeatureVectorScaler;
import pl.edu.icm.cermine.tools.classification.general.FeatureVectorScalerImpl;
import pl.edu.icm.cermine.tools.classification.general.FeatureVectorScalerNoOp;
import pl.edu.icm.cermine.tools.classification.general.LinearScaling;
import pl.edu.icm.cermine.tools.classification.general.TrainingSample;

/**
 * @author Pawel Szostek (p.szostek@
 *
 * @param <S> classified object's class
 * @param <T> context class
 * @param <E> target enumeration for labels
 */
public abstract class SVMClassifier<S, T, E extends Enum<E>> {
	final static protected svm_parameter defaultParameter = new svm_parameter();		
	static {
		// default values
		defaultParameter.svm_type = svm_parameter.C_SVC;
		defaultParameter.C = 8;
		defaultParameter.kernel_type = svm_parameter.POLY;
		defaultParameter.degree = 3;
		defaultParameter.gamma = 1.0/8.0; // 1/k
		defaultParameter.coef0 = 0.5;
		defaultParameter.nu = 0.5;
		defaultParameter.cache_size = 100;
		defaultParameter.eps = 1e-3;
		defaultParameter.p = 0.1;
		defaultParameter.shrinking = 1;
		defaultParameter.probability = 0;
		defaultParameter.nr_weight = 0;
		defaultParameter.weight_label = new int[0];
		defaultParameter.weight = new double[0];
	}
	protected FeatureVectorBuilder<S, T> featureVectorBuilder;
	protected FeatureVectorScaler scaler;
	protected String[] featuresNames;
		
	protected svm_parameter param;
	protected svm_problem problem;
	protected svm_model model;
	
	protected Class<E> enumClassObj;
	
	public SVMClassifier(FeatureVectorBuilder<S, T> featureVectorBuilder, Class<E> enumClassObj) 
	{
		this.featureVectorBuilder = featureVectorBuilder;
		this.enumClassObj = enumClassObj;
		Integer dimensions = featureVectorBuilder.size();
		
		Double scaledLowerBound = 0.0;
		Double scaledUpperBound = 1.0;
        FeatureVectorScalerImpl scaler = new FeatureVectorScalerImpl(dimensions, scaledLowerBound, scaledUpperBound);
		scaler.setStrategy(new LinearScaling());
		this.scaler = scaler;

		featuresNames = (String[])featureVectorBuilder.getFeatureNames().toArray(new String[0]);
		
		param = getDefaultParam();
	}
	
	protected static svm_parameter clone(svm_parameter param) {
		svm_parameter ret = new svm_parameter();
		// default values
		ret.svm_type = param.svm_type;
		ret.C = param.C;
		ret.kernel_type = param.kernel_type;
		ret.degree = param.degree;
		ret.gamma = param.gamma; // 1/k
		ret.coef0 = param.coef0;
		ret.nu = param.nu;
		ret.cache_size = param.cache_size;
		ret.eps = param.eps;
		ret.p = param.p;
		ret.shrinking = param.shrinking;
		ret.probability = param.probability;
		ret.nr_weight = param.nr_weight;
		ret.weight_label = param.weight_label;
		ret.weight = param.weight;
		return ret;
	}
	
	public static svm_parameter getDefaultParam() {
		return clone(defaultParameter);
	}
	
	public void buildClassifier(List<TrainingSample<E>> trainingElements) 
	{
		assert trainingElements.size() > 0;
//		for(TrainingSample<E> sample: trainingElements) {
//			System.out.println(sample.getLabel() + " "+ Arrays.asList(sample.getFeatures().getFeatures()));
//		}
		scaler.calculateFeatureLimits(trainingElements);
		problem = buildDatasetForTraining(trainingElements);
//		System.out.println(Arrays.toString(problem.x));
//		System.out.println(Arrays.toString(problem.y));
		model = libsvm.svm.svm_train(problem, param);
	}
	
	public E predictLabel(S object, T context) {
		svm_node[] instance = buildDatasetForClassification(object, context);
		Integer predictedVal = ((Double)svm.svm_predict(model, instance)).intValue();
		return enumClassObj.getEnumConstants()[predictedVal];
	}
	
	public E predictLabel(TrainingSample<E> sample) {
		svm_node[] instance = buildDatasetForClassification(sample.getFeatureVector());
		Integer predictedVal = ((Double)svm.svm_predict(model, instance)).intValue();
		return enumClassObj.getEnumConstants()[predictedVal];
	}

	protected svm_problem buildDatasetForTraining(List<TrainingSample<E>> trainingElements)
	{
		svm_problem problem = new svm_problem();
		problem.l = trainingElements.size();
		problem.x = new svm_node[problem.l][trainingElements.get(0).getFeatureVector().size()];
		problem.y = new double[problem.l];
		
		Integer elemIdx = 0;
		for(TrainingSample<E> trainingElem : trainingElements) {
			FeatureVector scaledFV = scaler.scaleFeatureVector(trainingElem.getFeatureVector());
			Integer featureIdx = 0;
			for(Double val: scaledFV.getFeatureValues()) {
				svm_node cur = new svm_node();
				cur.index = featureIdx;
				cur.value = val;
				problem.x[elemIdx][featureIdx] = cur;
				++featureIdx;
			}
			problem.y[elemIdx] = trainingElem.getLabel().ordinal();
			++elemIdx;
		}
		return problem;
	}
	
	protected svm_node[] buildDatasetForClassification(FeatureVector fv) {
		FeatureVector scaled = scaler.scaleFeatureVector(fv);
		svm_node[] ret = new svm_node[featureVectorBuilder.size()];
		Integer featureIdx = 0;
		for(Double val: scaled.getFeatureValues()) {
			svm_node cur = new svm_node();
			cur.index = featureIdx;
			cur.value = val;
			ret[featureIdx] = cur;
			++featureIdx;
		}
		return ret;
	}
	
	protected svm_node[] buildDatasetForClassification(S object, T context)
	{
		return buildDatasetForClassification(featureVectorBuilder.getFeatureVector(object, context));
	}

	public double[] getWeights() {
		double[][] coef = model.sv_coef;

		double[][] prob = new double[model.SV.length][featureVectorBuilder.size()];
		for (int i = 0; i < model.SV.length; i++) {
			for (int j = 0; j < model.SV[i].length; j++) {
				prob[i][j] = model.SV[i][j].value;
			}
		}
		double w_list[][][] = new double[model.nr_class][model.nr_class - 1][model.SV[0].length];

		for (int i = 0; i < model.SV[0].length; ++i) {
			for (int j = 0; j < model.nr_class - 1; ++j) {
				int index = 0;
				int end = 0;
				double acc;
				for (int k = 0; k < model.nr_class; ++k) {
					acc = 0.0;
					index += (k == 0) ? 0 : model.nSV[k - 1];
					end = index + model.nSV[k];
					for (int m = index; m < end; ++m) {
						acc += coef[j][m] * prob[m][i];
					}
					w_list[k][j][i] = acc;
				}
			}
		}

		double[] weights = new double[model.SV[0].length];
		for (int i = 0; i < model.nr_class - 1; ++i) {
			for (int j = i + 1, k = i; j < model.nr_class; ++j, ++k) {
				for (int m = 0; m < model.SV[0].length; ++m) {
					weights[m] = (w_list[i][k][m] + w_list[j][i][m]);

				}
			}
		}
		return weights;
	}

	public void loadModelFromResources(String modelFilePath, String rangeFilePath) throws IOException
	{
		InputStreamReader modelISR = new InputStreamReader(this.getClass().getResourceAsStream(modelFilePath));
		BufferedReader modelFile = new BufferedReader(modelISR);
		
		InputStreamReader rangeISR = new InputStreamReader(this.getClass().getResourceAsStream(rangeFilePath));
		BufferedReader rangeFile = new BufferedReader(rangeISR);
		loadModelFromFile(modelFile, rangeFile);
	}

	public void loadModelFromFile(String modelFilePath, String rangeFilePath) throws IOException
	{
		BufferedReader modelFile = new BufferedReader(new InputStreamReader(new FileInputStream(modelFilePath)));
		BufferedReader rangeFile = null;
        if (rangeFilePath != null) {
            rangeFile = new BufferedReader(new InputStreamReader(new FileInputStream(rangeFilePath)));
        }
		loadModelFromFile(modelFile, rangeFile);
	}
	
	public void loadModelFromFile(BufferedReader modelFile, BufferedReader rangeFile) throws IOException
	{
        if (rangeFile == null) {
            this.scaler = new FeatureVectorScalerNoOp();
        } else {
            FeatureVectorScalerImpl scaler =
                    FeatureVectorScalerImpl.fromRangeReader(rangeFile);

            if(scaler.getLimits().length != featureVectorBuilder.size()) {
                throw new IllegalArgumentException("Supplied .range file has "
                        + "wrong number of features (got " + scaler.getLimits().length
                        + ", expected " + featureVectorBuilder.size() + " )");
            }

			this.scaler = scaler;
		}

		this.model = svm.svm_load_model(modelFile);
	}

	public void saveModel(String modelPath) throws IOException
	{
		scaler.saveRangeFile(modelPath + ".range");
		svm.svm_save_model(modelPath, model);
	}
	
	public void printWeigths(FeatureVectorBuilder<BxZone, BxPage> vectorBuilder)
	{
		List<String> fnames = featureVectorBuilder.getFeatureNames();
		Iterator<String> namesIt = fnames.iterator();
		Iterator<Double> valueIt = (Iterator<Double>)new ArrayIterator(getWeights());

		assert fnames.size() == getWeights().length;
		
        while(namesIt.hasNext() && valueIt.hasNext()) {
        	String name = namesIt.next();
        	Double val = valueIt.next();
        	System.out.println(name + " " + val);
        }
	}

	public void setParameter(svm_parameter param) {
		this.param = param;
	}


}
