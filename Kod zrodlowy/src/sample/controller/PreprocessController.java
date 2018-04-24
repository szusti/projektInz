package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import rseslib.structure.attribute.Attribute;
import rseslib.structure.table.ArrayListDoubleDataTable;
import rseslib.structure.table.DoubleDataTable;
import rseslib.system.progress.StdOutProgress;
import sample.model.DataInfo;
import sample.model.DataIntegration;
import sample.model.DataMissingStatistics;
import sample.view.Allert;
import sample.view.ProcessingProgress;
import sample.view.ShowFile;

import java.io.File;
import java.util.LinkedList;

public class PreprocessController {
    private DoubleDataTable dataTable;
    private DataMissingStatistics dataMissingStatistics;
    private DataIntegration dataIntegration;
    @FXML private Label labelFileNotLoaded;
    @FXML private Button buttonSeeFile, buttonSeeMissing, buttonEdit, buttonSaveFile;
    @FXML private TextArea textAreaSummDataSpecifics, textAreaSummDataSumm;
    @FXML ListView<String> listViewAttributes;
    private MainController mainController;


    public void initialize(){
        dataTable = null;
        dataMissingStatistics = null;
        dataIntegration = null;

        //textAreaSummDataSumm.setStyle("-fx-focus-color: transparent;");
        //textAreaSummDataSpecifics.setStyle("-fx-focus-color: transparent;");

        listViewAttributes.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
            if(newValue!=null) {
                attributeInfo(listViewAttributes.getSelectionModel().getSelectedIndex());
            }
        });
        textAreaSummDataSpecifics.textProperty().addListener((v, oldValue, newValue)->{
            if(!oldValue.equals(newValue)) {
                textAreaSummDataSpecifics.setScrollTop(Double.MIN_VALUE);
            }
        });
    }
    public void injectMainController(MainController mainController){
        this.mainController = mainController;
    }

    @FXML private void loadFile(){

        Stage primaryStage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load File");
        //fileChooser.setInitialDirectory(new File("C:\\Users\\DD\\Desktop\\testy\\out\\production\\testy\\lib"));
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("ARFF (*.arff)", "*.arff");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            try {
                ProcessingProgress progress = new ProcessingProgress();
                progress.show();
                Task<Integer> task = new Task<Integer>() {
                    @Override protected Integer call() throws Exception {
                               dataTable = new ArrayListDoubleDataTable(
                                       new File(file.toString()),
                                       new StdOutProgress());

                        return  null;
                    }
                    @Override protected void succeeded() {
                        super.succeeded();
                        progress.close();
                        String stringFileName = file.getName().substring(0, file.getName().indexOf('.'));
                        labelFileNotLoaded.setText(stringFileName.replaceAll("\\s+",""));
                        loadData(dataTable);
                        primaryStage.close();
                    }
//                    @Override protected void failed() {
//                        super.failed();
//                        progress.close();
//                        primaryStage.close();
//
//                    }
                };
                task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
                    if(newValue != null) {
                        Exception ex = (Exception) newValue;
                        loadFailed(ex);
                        progress.close();
                        primaryStage.close();
                    }
                });
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }catch (Exception e){
                loadFailed(e);
            }
        }
    }

    private void loadData(DoubleDataTable dataTable){
        ProcessingProgress progress = new ProcessingProgress();
        progress.show();
        Task<Integer> task = new Task<Integer>() {
            @Override protected Integer call() throws Exception {
               checkDataIntegration();
                return  null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                progress.close();
                mainController.setTestData(dataTable.attributes());
                setLoadedDataUI();
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
    private void checkDataIntegration(){
        DoubleDataTable dt = new ArrayListDoubleDataTable(dataTable.getDataObjects());
        dataTable= (DoubleDataTable) dt.clone();
        dataMissingStatistics = new DataMissingStatistics(dataTable);
        dataIntegration = new DataIntegration(dataTable);
    }
    private void setLoadedDataUI(){
        textAreaSummDataSpecifics.clear();
        textAreaSummDataSumm.setText("Number of objects = "+ dataTable.noOfObjects() +"\n");
        if(dataMissingStatistics!= null && !dataMissingStatistics.getIndexOfObjectWithMissing().isEmpty()) {
            textAreaSummDataSumm.appendText("Number of objects with missing values: "+ dataMissingStatistics.getCopyIndexOfObjectWithMissing().size() +"\n");
            buttonSeeMissing.disableProperty().set(false);
            textAreaSummDataSpecifics.appendText("Object with missing values \n");
            textAreaSummDataSpecifics.appendText(dataMissingStatistics.getIndexOfObjectWithMissing() + "\n");
        }else{
            dataMissingStatistics = null;
            buttonSeeMissing.disableProperty().set(true);
        }
        if(dataIntegration!=null) {
            if (dataIntegration.getNumberOfSameObjects() != 0) {
                textAreaSummDataSumm.appendText("Objects indiscernibility: " + dataIntegration.getNumberOfSameObjects() + "\n");
                textAreaSummDataSpecifics.appendText("Objects indiscernibility: \n");
                LinkedList<LinkedList<Integer>> sameObjectDiffDec = dataIntegration.getSameObject();
                for (LinkedList<Integer> subList : sameObjectDiffDec) {
                    textAreaSummDataSpecifics.appendText(subList.toString() + "\n");
                }
            }
            if (dataIntegration.getNumberOfSameObjectsDiffDec() != 0) {
                textAreaSummDataSumm.appendText("Inconsistency objects: " + dataIntegration.getNumberOfSameObjectsDiffDec() + "\n");
                textAreaSummDataSpecifics.appendText("Inconsistency objects: \n");
                LinkedList<LinkedList<Integer>> sameObjectDiffDec = dataIntegration.getSameObjectDiffDec();
                for (LinkedList<Integer> subList : sameObjectDiffDec) {
                    textAreaSummDataSpecifics.appendText(subList.toString() + "\n");
                }
            }
            if (dataIntegration.getNumberOfSameObjects() == 0 && dataIntegration.getNumberOfSameObjectsDiffDec() == 0)
                dataIntegration = null;
        }
        setTextAreaSummDataSpecifics();
        textAreaSummDataSumm.appendText(dataTable.attributes().toString());
        setAttributesList();

        buttonSeeFile.disableProperty().set(false);
        buttonSaveFile.disableProperty().set(false);
        buttonEdit.disableProperty().set(false);

    }
    private void setTextAreaSummDataSpecifics(){
        textAreaSummDataSpecifics.appendText(DataInfo.toStringAttributeInfoFull(dataTable));
    }

    private void setAttributesList(){
        ObservableList<String> observableListAttrib = FXCollections.observableArrayList();
        for(int i=0;i<dataTable.attributes().noOfAttr();i++){
            Attribute attrib = dataTable.attributes().attribute(i);
            observableListAttrib.add(attrib.name());
        }
        listViewAttributes.setItems(observableListAttrib);
    }

    @FXML private void showFile(){
        ShowFile.showFile(dataTable);
    }


    @FXML private void seeMissingInData(){
         PreprocessMissingController ppMissingController = new PreprocessMissingController();
         boolean somethingWasEdited = ppMissingController.show(dataTable, dataMissingStatistics);
         if(somethingWasEdited) {
             if(dataTable.noOfObjects()!=0){
             loadData(dataTable);
             }else {
                 zeroObjects();
             }
         }
    }

    private void zeroObjects(){
        textAreaSummDataSpecifics.setText("none");
        textAreaSummDataSumm.setText("none");
        listViewAttributes.getItems().clear();
        dataMissingStatistics = null;
        dataIntegration = null;
        buttonSeeFile.disableProperty().set(true);
        buttonSaveFile.disableProperty().set(true);
        buttonSeeMissing.disableProperty().set(true);
    }

    private void loadFailed(Exception e){
        Allert.showLoadFileError(e);
        mainController.setTestData(null);
        labelFileNotLoaded.setText("ERROR");
        dataTable = null;
        textAreaSummDataSumm.clear();
        textAreaSummDataSpecifics.clear();
        listViewAttributes.getItems().clear();
        buttonSeeFile.disableProperty().set(true);
        buttonEdit.disableProperty().set(true);
        buttonSeeMissing.disableProperty().set(true);
        buttonSaveFile.disableProperty().set(true);
    }
    @FXML private void editFile() {
        EditFileController eFileController = new EditFileController();
        boolean somethingWasEdited = eFileController.show(dataTable, dataIntegration);
        if (somethingWasEdited) {
            if(dataTable.noOfObjects()!=0){
                loadData(dataTable);
            }else {
                zeroObjects();
            }
        }
    }
    @FXML private void attributesSummary(){
        if(dataTable!=null){
            listViewAttributes.getSelectionModel().clearSelection();
            setLoadedDataUI();
        }
    }
    private void  attributeInfo(int index){
        if(dataTable.attributes().attribute(index).isDecision()) {
            textAreaSummDataSpecifics.setText("Decision attribute \n");
        }else {textAreaSummDataSpecifics.setText("Conditional attribute \n");}
        textAreaSummDataSpecifics.appendText(DataInfo.attributePercentage(dataTable, index, dataMissingStatistics));


    }

    public DoubleDataTable getDataTable(){
        return dataTable;
    }
    public DataIntegration getDataIntegration(){return dataIntegration;}
    public boolean missingExists(){
        if(dataMissingStatistics == null){
            return false;
        }else return true;
    }


    @FXML private void saveFile(){
        Stage primaryStage = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(labelFileNotLoaded.getText());
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("ARFF (*.arff)", "*.arff");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(primaryStage);
        if(file != null){
            SaveFile(dataTable, file);
        }

    }
    private void SaveFile(DoubleDataTable data, File file){
        try {
            dataTable.storeArff(file.getName(), file, new StdOutProgress());
        } catch (Exception ex) {
            Allert.showError(ex);
        }

    }

    public String getFileName(){
        return labelFileNotLoaded.getText();
    }


}
