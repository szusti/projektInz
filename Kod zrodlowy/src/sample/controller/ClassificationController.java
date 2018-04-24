package sample.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rseslib.processing.classification.*;
import rseslib.structure.attribute.Header;
import rseslib.structure.data.DoubleData;
import rseslib.structure.rule.Rule;
import rseslib.structure.table.ArrayListDoubleDataTable;
import rseslib.structure.table.DoubleDataTable;
import rseslib.system.Configuration;
import rseslib.system.PropertyConfigurationException;
import rseslib.system.progress.StdOutProgress;
import sample.controller.options.*;
import sample.model.ClassificationResults;
import sample.model.NonDeterClassifier;
import sample.view.Allert;
import sample.view.InformationDialog;
import sample.view.ProcessingProgress;
import sample.view.ShowRules;

import java.io.File;
import java.time.Instant;
import java.util.*;

public class ClassificationController {
    @FXML private ComboBox<String> comboBoxDeterministicRule;
    @FXML private RadioButton radioButtonTrainingSetOnly, radioButtonSplit, radioButtonCrossValid, radioButtonTestSet;
    @FXML private ToggleGroup howClassify;
    @FXML private Spinner<Integer> spinnerTrain, spinnerCrossValid;
    @FXML private Button buttonLoadTestSet, buttonShowNDRules, buttonShowRules;
    @FXML private TextArea textAreaClassificationResult;
    @FXML private Label labelTestPercent;
    @FXML private ListView<ClassificationResults> listViewResutls;

    private MainController mainController;
    private DoubleDataTable testDataTable;
    private Properties prop;
    private NonDeterClassifier nonDeterClass = null;

    public void injectMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void initialize() {
        try {
            prop = Configuration.loadDefaultProperties(NonDeterClassifier.class);
        }catch (Exception e){
            Allert.showError(e);
        }
        prop.setProperty("noOfFolds", "5");
        prop.setProperty("noOfTests", "10");
        prop.setProperty("noOfPartsForTraining", "2");
        prop.setProperty("noOfPartsForTesting", "1");
        comboBoxDeterministicRule.getItems().addAll("LocalReducts", "GlobalReducts", "JohnsonReducts", "Accurate", "Covering");
        comboBoxDeterministicRule.setValue(prop.getProperty("ruleGenerator"));

        SpinnerValueFactory<Integer> valueFactoryTrain = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 70);
        spinnerTrain.setValueFactory(valueFactoryTrain);
        SpinnerValueFactory<Integer> valueFactoryCrossValid = new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 50, 5);
        spinnerCrossValid.setValueFactory(valueFactoryCrossValid);

        spinnerTrain.valueProperty().addListener((obs, oldValue, newValue) -> labelTestPercent.setText(String.valueOf(100-newValue)));

        testDataTable = null;

        listViewResutls.setCellFactory(lv -> new ListCell<ClassificationResults>() {
            @Override
            public void updateItem(ClassificationResults item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    if(item.getMultipleTestResults()==null&&item.getTestResult()==null){
                        listViewResutls.getItems().remove(item);
                    }else {
                        String text = item.getTime().toString();
                        setText(text);
                    }
                }
            }
        });

        comboBoxDeterministicRule.valueProperty().addListener((v, old, newVal)->prop.setProperty("ruleGenerator", comboBoxDeterministicRule.getValue()));

    }

    @FXML private void classifyData(){
            DoubleDataTable trainDataTable = mainController.getDoubleData();
            if (trainDataTable != null && !mainController.missingExists()) {
                try {
                    if (howClassify.getSelectedToggle().equals(radioButtonTrainingSetOnly)) {
                        classifyTrainingSetOnly(trainDataTable);
                    } else if (howClassify.getSelectedToggle().equals(radioButtonSplit)) {
                        classifySplit(trainDataTable);

                    } else if (howClassify.getSelectedToggle().equals(radioButtonCrossValid)) {
                        classifyCrossValid(trainDataTable);

                    } else if (howClassify.getSelectedToggle().equals(radioButtonTestSet)) {
                        classifyTestSet(trainDataTable);

                    }

                } catch (Exception e) {
                    Allert.showError(e);
                }

            }
            if (mainController.missingExists()) {
                InformationDialog.showInfo("You need to deal with missing values before building classifier");
            }
    }

    private void classifyTrainingSetOnly(DoubleDataTable trainDataTable) throws PropertyConfigurationException, InterruptedException{
        ProcessingProgress progress = new ProcessingProgress();
        progress.show();
        Task<Integer> task = new Task<Integer>() {
            @Override protected Integer call() throws Exception {
                Platform.runLater(()->textAreaClassificationResult.setText("Counting... Please wait"));
                Classifier ruleBased = new NonDeterClassifier(prop, trainDataTable, new StdOutProgress());
                TestResult tr = new SingleClassifierTest().classify(ruleBased, trainDataTable, new StdOutProgress());
                nonDeterClass = (NonDeterClassifier)ruleBased;
                Platform.runLater(()->showResutls(tr, "Training set only", trainDataTable.attributes()));
                return  null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                progress.close();
            }
        };
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                progress.close();
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
                Allert.showError(ex);
            }
        });
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                Allert.showError(ex);
                progress.close();
            }
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void classifySplit(DoubleDataTable trainDataTable) throws PropertyConfigurationException, InterruptedException{
        ProcessingProgress progress = new ProcessingProgress();
        progress.show();
        Task<Integer> task = new Task<Integer>() {
            @Override protected Integer call() throws Exception {
                Platform.runLater(()->textAreaClassificationResult.setText("Counting... Please wait"));
                int nOfObjectTrain= roundPercent(trainDataTable.noOfObjects());
                ArrayList<DoubleData>[] parts = trainDataTable.randomSplit(nOfObjectTrain, trainDataTable.noOfObjects()- nOfObjectTrain);
                DoubleDataTable trainTable = new ArrayListDoubleDataTable(parts[0]);
                DoubleDataTable testTable = new ArrayListDoubleDataTable(parts[1]);
                Classifier ruleBased = new NonDeterClassifier(prop, trainTable, new StdOutProgress());
                TestResult tr = new SingleClassifierTest().classify(ruleBased, testTable, new StdOutProgress());
                nonDeterClass = (NonDeterClassifier)ruleBased;
                Platform.runLater(()->showResutls(tr, ("Training split Train: \nTrain " + spinnerTrain.getValue() +"% (number of objects "+trainTable.noOfObjects() +") Test "+labelTestPercent.getText()) + "% (number of objects "+testTable.noOfObjects()+")", trainDataTable.attributes()));
                return  null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                progress.close();
            }
            @Override protected void failed() {
                super.failed();
                progress.close();
            }
        };
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                Allert.showError(ex);
                progress.close();
            }
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void classifyCrossValid(DoubleDataTable trainDataTable) throws PropertyConfigurationException, InterruptedException{
        ProcessingProgress progress = new ProcessingProgress();
        progress.show();
        Task<Integer> task = new Task<Integer>() {
            @Override protected Integer call() throws Exception {
                Platform.runLater(()->textAreaClassificationResult.setText("Counting... Please wait"));
                prop.setProperty("noOfFolds", String.valueOf(spinnerCrossValid.getValue()));
                ClassifierSet classifierSet = new ClassifierSet();
                classifierSet.addClassifier("test", NonDeterClassifier.class, prop);
                CrossValidationTest crossValidTest = new CrossValidationTest(prop, classifierSet);
                Map<String,MultipleTestResult> results = crossValidTest.test(trainDataTable, new StdOutProgress());
                MultipleTestResult multipletr = results.get("test");
                Platform.runLater(()->{
                    showResultsCross(multipletr, trainDataTable.attributes());
                });
                return  null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                progress.close();
            }
            @Override protected void failed() {
                super.failed();
                progress.close();
            }
        };
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                Allert.showError(ex);
                progress.close();
            }
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void classifyTestSet(DoubleDataTable trainDataTable) throws PropertyConfigurationException, InterruptedException{
        if(testDataTable!=null) {
            ProcessingProgress progress = new ProcessingProgress();
            progress.show();
            Task<Integer> task = new Task<Integer>() {
                @Override protected Integer call() throws Exception {
                    Platform.runLater(()->textAreaClassificationResult.setText("Counting... Please wait"));
                    Classifier ruleBased = new NonDeterClassifier(prop, trainDataTable, new StdOutProgress());
                    TestResult tr = new SingleClassifierTest().classify(ruleBased, testDataTable, new StdOutProgress());
                    nonDeterClass = (NonDeterClassifier)ruleBased;
                    Platform.runLater(()->showResutls(tr, "Train with separated test.\n" +("Number of train objects "+trainDataTable.noOfObjects() +" Test objects "+ testDataTable.noOfObjects()), trainDataTable.attributes()));
                    //listViewResutls.getItems().add(new ClassificationResults((NonDeterClassifier)ruleBased, tr, null, Instant.now()));
                    return  null;
                }
                @Override protected void succeeded() {
                    super.succeeded();
                    progress.close();
                }
                @Override protected void failed() {
                    super.failed();
                    progress.close();
                }
            };
            task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                if(newValue != null) {
                    Exception ex = (Exception) newValue;
                    Allert.showError(ex);
                    progress.close();
                }
            });
            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        }else InformationDialog.showInfo("Didn't found test set");

    }

    private void showResutls(TestResult tr, String classifType, Header head){
        buttonShowNDRules.disableProperty().setValue(false);
        buttonShowRules.disableProperty().setValue(false);
        listViewResutls.getItems().add(new ClassificationResults(nonDeterClass, tr, null, Instant.now(), prop, classifType,  ("File \""+mainController.getFileName() + "\". Number of objects " + mainController.getDoubleData().noOfObjects()), head));
        textAreaClassificationResult.setText(listViewResutls.getItems().get(listViewResutls.getItems().size()-1).toString());
    }
    private void showResultsCross(MultipleTestResult multipletr, Header head){
        nonDeterClass = null;
        listViewResutls.getItems().add(new ClassificationResults(null, null, multipletr, Instant.now(),prop, "Cross-Validation", ("File \""+mainController.getFileName() + "\". Number of objects " + mainController.getDoubleData().noOfObjects()), head));
        if(multipletr!=null) {
            textAreaClassificationResult.setText(listViewResutls.getItems().get(listViewResutls.getItems().size() - 1).toString());
        }else textAreaClassificationResult.setText("error");

        buttonShowNDRules.disableProperty().setValue(true);
        buttonShowRules.disableProperty().setValue(true);

    }

    @FXML private void classificatorOptions(){
        Properties newProp = new Properties();
        newProp.putAll(prop);
        ClassificatorOptionsController classOption = new ClassificatorOptionsController();
        int numberOfValues = 0;
        if(mainController.getDoubleData()!=null) numberOfValues = mainController.getDoubleData().attributes().nominalDecisionAttribute().noOfValues();
        boolean cancel = classOption.show(newProp, numberOfValues);
        if(!cancel){
            prop.putAll(newProp);
        }
    }


    @FXML private void deterRuleGeneratorOptions(){
        String whichRuleGenerator = comboBoxDeterministicRule.getValue();
        Properties newProp = new Properties();
        newProp.putAll(prop);
        boolean cancel = true;
        if(whichRuleGenerator.equals("Accurate")){
            cancel = AccurateOptionController.options(newProp);
        }else{
            if(whichRuleGenerator.equals("LocalReducts") || whichRuleGenerator.equals("GlobalReducts")){
                GlobLocReductOptionsController glOptionController = new GlobLocReductOptionsController();
                cancel = glOptionController.show(newProp);
            }else if(whichRuleGenerator.equals("JohnsonReducts")){
                JohnsonReductOptionsController jOptionController = new JohnsonReductOptionsController();
                cancel = jOptionController.show(newProp);
            }else if(whichRuleGenerator.equals("Covering")){
                CoveringOptionsController cOptionsController = new CoveringOptionsController();
                cancel = cOptionsController.show(newProp);

            }
        }
        if(!cancel){
            prop.putAll(newProp);
        }
    }
    @FXML  private void loadTestSet() throws Exception {
        Stage primaryStage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load File");
        //fileChooser.setInitialDirectory(new File("C:\\Users\\DD\\Desktop\\testy\\out\\production\\testy\\lib"));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("ARFF (*.arff)", "*.arff");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            try {
                testDataTable = new ArrayListDoubleDataTable(
                        new File(file.toString()),
                        new StdOutProgress());
                buttonLoadTestSet.setText(file.getName());

            }catch(Exception e){
                radioButtonTrainingSetOnly.selectedProperty().setValue(true);
                Allert.showLoadFileError(e);
                buttonLoadTestSet.setText("ERROR");
            }
        }
    }

    private int roundPercent(double numberOfObject){
        double percent = ((double)spinnerTrain.getValue()/100)*numberOfObject;
        int roundedPercent = (int)Math.round(percent);
        if(roundedPercent == 0) roundedPercent=1;
        if(roundedPercent == numberOfObject) roundedPercent-=1;
        return roundedPercent;
    }

    @FXML private void showDRules(){
        showRules(true);
    }
    @FXML private void showNDRules(){
        showRules(false);
    };
    private void showRules(boolean deterRules){
        Collection<Rule> rules = null;
        if(deterRules){
            rules = nonDeterClass.getDecisionRules();
        }else rules = nonDeterClass.getNDDecisionRules();
        ShowRules.show(rules);

    }
    @FXML private void selectedClassResult(){
        if(!listViewResutls.getItems().isEmpty()) {
            ClassificationResults resultClass = listViewResutls.getSelectionModel().getSelectedItem();
            textAreaClassificationResult.setText(listViewResutls.getSelectionModel().getSelectedItem().toString());
            if (resultClass.getMultipleTestResults() == null) {
                nonDeterClass = resultClass.getNonDeterClass();
                buttonShowNDRules.disableProperty().setValue(false);
                buttonShowRules.disableProperty().setValue(false);

            } else {
                buttonShowNDRules.disableProperty().setValue(true);
                buttonShowRules.disableProperty().setValue(true);

            }
        }
    }
    public NonDeterClassifier getNonDeterClass(){return nonDeterClass;}
    @FXML private void deleteResultByKey(KeyEvent key){
        if(!listViewResutls.getItems().isEmpty()) {
            if (key.getCode().equals(KeyCode.DELETE)) {
                listViewResutls.getItems().remove(listViewResutls.getSelectionModel().getSelectedIndex());
            };
        }
    }
}
