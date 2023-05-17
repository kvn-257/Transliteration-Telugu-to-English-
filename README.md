# Transliteration from Telugu to English using GRUs

This project aims to develop a transliteration system that converts Telugu text to English using Gated Recurrent Units (GRUs) and an Android app. The system utilizes the PyTorch framework for training the GRU model and integrates it with the PyTesseract OCR library for extracting Telugu text from images.

## Codes to understand and apply 

1. Main Code for training the model and for inferenece in the notebook [Project(Transliteration).ipynb](Project(Transliteration).ipnb).
2. Conversion of trained model to android compatible model in notebook [Demo.ipynb](Demo.ipynb). 
3. Main code of backend of app in [MainActivity.java](app/src/main/java/com/example/transliteration/MainActivity.java).
4. For front end of app, refer [layouts](app/src/main/res/layout/).

## Project Overview

The project follows the following steps:

1. **Data Collection**: Telugu-English parallel words are collected from Wikititles.
2. **Data Cleaning**: Incorrect pairs are removed from the dataset.
3. **Data Preprocessing**: The dataset is converted into one-hot encoding for model input.
4. **Data Splitting**: The dataset is split into training and testing data using `train_test_split` from the sklearn library.
5. **Model Architecture**: A GRU model with two GRU layers and an attention mechanism is implemented using PyTorch.
6. **Model Training**: The model is trained using the Adam optimizer and NNLL loss function. Hyperparameters are tuned using MLflow for better performance.
7. **Model Evaluation**: The trained model's accuracy is evaluated on the test data.
8. **Model Deployment**: The trained model is converted into an Android-compatible model. Using this, android app is developed in Android Studio that utilizes the PyTesseract and PyTorch dependencies for transliteration.

## Requirements

To run this project, you will need the following libraries:

- PyTorch
- sklearn
- numpy
- matplotlib
- mlflow
- pytesseract
- Android Studio (for app development)

## Usage

1. Clone the repository.
2. Install the required libraries mentioned above.
3. Run the [Project(Transliteration).ipynb](Project(Transliteration).ipynb) to train the GRU model.
4. To convert the trained model into an Android-compatible model run [Demo.ipynb](Demo.ipynb).
5. Import the [Android project](app) into Android Studio.
6. Install the app on an Android device or emulator.
7. Launch the app, capture or select an image with Telugu text, and observe the transliterated output.

## Contribution

Contributions to this project are welcome. If you find any issues or have suggestions for improvements, please feel free to create a pull request.






