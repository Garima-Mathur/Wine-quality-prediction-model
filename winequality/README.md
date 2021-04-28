# Wine Quality Prediction | Assignment 2

Goal: The purpose of this individual assignment is to learn how to develop parallel machine learning (ML) applications in Amazon AWS cloud platform. Specifically, you will learn: (1) how to use Apache Spark to train an ML model in parallel on multiple EC2 instances; (2) how to use Spark’s MLlib to develop and use an ML model in the cloud; (3) How to use Docker to create a container for your ML model to simplify model deployment.

## Full Readme 
* [Readme word](https://github.com/Garima-njit/Wine-quality-prediction-model/main/winequality/ReadMe_doc.docx)

## Docker image link
* [Docker image : garima/wine-prediction-mvn:1.0](https://hub.docker.com/repository/docker/garima/wine-prediction-mvn)


## Before running Requirements :
* Input file must be copied under data/ and with same name TestDataset.csv
* Input file must have save format as of TrainingDataset.csv and ValidationDataset.csv. Column names in double quotes with exact name number of column with same names and separator ';'




## Docker run instructions
````
docker pull garima/wine-prediction-mvn:1.0

Docker run -v [fullLocalPath of TestDataset.csv: data/TestDataset.csv ] garima/wine-prediction-mvn:1.0

for e.g.

docker run -v /Users/garima/Desktop/docker-attempt/TestDataset.csv:/data/TestDataset.csv garima/wine-prediction-mvn:1.0
```` 


