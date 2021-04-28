package com.nrd.wineq;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.*;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.io.File;
import java.io.IOException;

import static com.nrd.wineq.Constants.*;
import static org.apache.hadoop.fs.s3a.Constants.SECRET_KEY;


public class ModelTrainer {



    public static void main(String[] args) {


        Logger.getLogger("org").setLevel(Level.ERROR);
        Logger.getLogger("akka").setLevel(Level.ERROR);
        Logger.getLogger("breeze.optimize").setLevel(Level.ERROR);
        Logger.getLogger("com.amazonaws.auth").setLevel(Level.DEBUG);
        Logger.getLogger("com.github").setLevel(Level.ERROR);


        SparkSession spark = SparkSession.builder()
                .appName(APP_NAME)
                .master("local[*]")
                .config("spark.executor.memory", "2147480000")
                .config("spark.driver.memory", "2147480000")
                .config("spark.testing.memory", "2147480000")

                .getOrCreate();

        if (StringUtils.isNotEmpty(ACCESS_KEY_ID) && StringUtils.isNotEmpty(SECRET_KEY)) {
            spark.sparkContext().hadoopConfiguration().set("fs.s3a.access.key", ACCESS_KEY_ID);
            spark.sparkContext().hadoopConfiguration().set("fs.s3a.secret.key", SECRET_KEY);
        }

        File tempFile = new File(TRAINING_DATASET);
        boolean exists = tempFile.exists();
        if(exists){
            ModelTrainer parser = new ModelTrainer();
            parser.logisticRegression(spark);
        }else{
            System.out.print("TrainingDataset.csv doesn't exists");
        }


    }

    public void logisticRegression(SparkSession spark) {
        System.out.println();
        Dataset<Row> lblFeatureDf = getDataFrame(spark, true, TRAINING_DATASET).cache();
        LogisticRegression logReg = new LogisticRegression().setMaxIter(100).setRegParam(0.0);

        Pipeline pl1 = new Pipeline();
        pl1.setStages(new PipelineStage[]{logReg});

        PipelineModel model1 = pl1.fit(lblFeatureDf);


        LogisticRegressionModel lrModel = (LogisticRegressionModel) (model1.stages()[0]);
        // System.out.println("Learned LogisticRegressionModel:\n" + lrModel.summary().accuracy());
        LogisticRegressionTrainingSummary trainingSummary = lrModel.summary();
        double accuracy = trainingSummary.accuracy();
        double fMeasure = trainingSummary.weightedFMeasure();

        System.out.println();
        System.out.println("Training DataSet Metrics ");

        System.out.println("Accuracy: " + accuracy);
        System.out.println("F-measure: " + fMeasure);

        Dataset<Row> testingDf1 = getDataFrame(spark, true, VALIDATION_DATASET).cache();

        Dataset<Row> results = model1.transform(testingDf1);


        System.out.println("\n Validation Training Set Metrics");
        results.select("features", "label", "prediction").show(5, false);
        printMertics(results);

        try {
            model1.write().overwrite().save(MODEL_PATH);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void printMertics(Dataset<Row> predictions) {
        System.out.println();
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator();
        evaluator.setMetricName("accuracy");
        System.out.println("The accuracy of the model is " + evaluator.evaluate(predictions));
        evaluator.setMetricName("f1");
        double f1 = evaluator.evaluate(predictions);
        System.out.println("F1: " + f1);
    }

    public Dataset<Row> getDataFrame(SparkSession spark, boolean transform, String name) {

        Dataset<Row> validationDf = spark.read().format("csv").option("header", "true")
                .option("multiline", true).option("sep", ";").option("quote", "\"")
                .option("dateFormat", "M/d/y").option("inferSchema", true).load(name);


        validationDf = validationDf.withColumnRenamed("fixed acidity", "fixed_acidity")
                .withColumnRenamed("volatile acidity", "volatile_acidity")
                .withColumnRenamed("citric acid", "citric_acid")
                .withColumnRenamed("residual sugar", "residual_sugar")
                .withColumnRenamed("chlorides", "chlorides")
                .withColumnRenamed("free sulfur dioxide", "free_sulfur_dioxide")
                .withColumnRenamed("total sulfur dioxide", "total_sulfur_dioxide")
                .withColumnRenamed("density", "density").withColumnRenamed("pH", "pH")
                .withColumnRenamed("sulphates", "sulphates").withColumnRenamed("alcohol", "alcohol")
                .withColumnRenamed("quality", "label");

        validationDf.show(5);


        Dataset<Row> lblFeatureDf = validationDf.select("label", "alcohol", "sulphates", "pH",
                "density", "free_sulfur_dioxide", "total_sulfur_dioxide", "chlorides", "residual_sugar",
                "citric_acid", "volatile_acidity", "fixed_acidity");

        lblFeatureDf = lblFeatureDf.na().drop().cache();

        VectorAssembler assembler =
                new VectorAssembler().setInputCols(new String[]{"alcohol", "sulphates", "pH", "density",
                        "free_sulfur_dioxide", "total_sulfur_dioxide", "chlorides", "residual_sugar",
                        "citric_acid", "volatile_acidity", "fixed_acidity"}).setOutputCol("features");

        if (transform)
            lblFeatureDf = assembler.transform(lblFeatureDf).select("label", "features");


        return lblFeatureDf;
    }
}
