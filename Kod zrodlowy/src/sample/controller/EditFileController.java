package sample.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rseslib.structure.attribute.Attribute;
import rseslib.structure.attribute.Header;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.DoubleData;
import rseslib.structure.data.DoubleDataObject;
import rseslib.structure.table.DoubleDataTable;
import rseslib.structure.table.NumericalStatistics;
import sample.model.DataInfo;
import sample.model.DataIntegration;
import sample.view.Allert;
import sample.view.ProcessingProgress;

import java.util.Random;

public class EditFileController {
    @FXML private ListView<DoubleData> listViewObjects;
    @FXML private ListView<String> listViewAttributes;
    @FXML private TextArea textAreaAttribInfo;
    @FXML private ComboBox comboBoxValue;
    @FXML private Spinner<Integer> spinnerNOfObj;
    @FXML private CheckBox checkBoxRandom;

    private DoubleDataTable copyDataTable;
    private DoubleDataTable originalDataTable;
    private Header header;
    private Stage stage;
    private boolean somethingWasEdited = false;
    private int choosedDoubleData = -1;
    private int choosedAttribute = -1;

    private static final String DEFAULT_CONTROL_INNER_BACKGROUND = "derive(-fx-base,80%)";
    private static final String HIGHLIGHTED_CONTROL_INNER_BACKGROUND_SAME_OBJECTS = "derive(palegreen, 50%)";
    private static final String HIGHLIGHTED_CONTROL_INNER_BACKGROUND_INCONSISTENCY_OBJECTS = "derive(red, 50%)";

    public boolean show(DoubleDataTable dataTable, DataIntegration dataIntegration){
        try {
            stage = new Stage();
            stage.setResizable(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample/view/EditFileWindow.fxml"));
            loader.setController(this);
            Parent root1 = (Parent) loader.load();

            stage.setTitle("Edit Data");
            stage.setScene(new Scene(root1, 720, 680));
            stage.setOnCloseRequest(e->{
                        e.consume();
                        cancel();
                    }
            );

            this.copyDataTable = (DoubleDataTable) dataTable.clone();
            this.originalDataTable = dataTable;
            this.header = dataTable.attributes();

            listViewObjects.setCellFactory(lv -> new ListCell<DoubleData>() {
                @Override
                public void updateItem(DoubleData item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    }
                    else{
                        int[] index = DataIntegration.indexOfFirstShow(copyDataTable, item);
                        setText(index[1] + " " + item.toString());
                        if(index[1]==-1){
                            setStyle("-fx-control-inner-background: " + HIGHLIGHTED_CONTROL_INNER_BACKGROUND_SAME_OBJECTS + ";");
                        }else if (index[0]>1) {
                            setStyle("-fx-control-inner-background: " + HIGHLIGHTED_CONTROL_INNER_BACKGROUND_INCONSISTENCY_OBJECTS + ";");
                        }
                        else {
                            setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                        }
                    }
                }
            });

            setlistViewObjects();
            setListViewAttributes();
            SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
            spinnerNOfObj.setValueFactory(valueFactory);

            listViewObjects.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
                if(newValue!=null) {
                    choosedDoubleData = listViewObjects.getSelectionModel().getSelectedIndex();
                }
            });
            listViewAttributes.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
                if(newValue!=null) {
                    choosedAttribute = listViewAttributes.getSelectionModel().getSelectedIndex();
                    setChoosedAttribute(choosedAttribute);
                }
            });

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();


        }catch (Exception e){
            Allert.showError(e);
        }
        return somethingWasEdited;
    }

    private void setlistViewObjects(){
        ObservableList<DoubleData> observableList = FXCollections.observableArrayList();
        observableList.addAll(copyDataTable.getDataObjects());
        listViewObjects.setItems(observableList);
    }

    private void setListViewAttributes(){
        ObservableList<String> observableListAttrib = FXCollections.observableArrayList();
        for(int i=0;i<header.noOfAttr();i++){
            Attribute attrib = header.attribute(i);
            observableListAttrib.add(attrib.name());
        }
        listViewAttributes.setItems(observableListAttrib);
    }
    private void setChoosedAttribute(int attrib){
        comboBoxValue.getItems().clear();
        textAreaAttribInfo.clear();

        updateTextArea();

        Attribute attribute = header.attribute(choosedAttribute);
        if(attribute.isNominal()){
            comboBoxValue.valueProperty().setValue(null);
            comboBoxValue.editableProperty().set(false);
            NominalAttribute nomAttrib = (NominalAttribute)attribute;
            for(int i=0;i<nomAttrib.noOfValues();i++){
                comboBoxValue.getItems().add(NominalAttribute.stringValue(nomAttrib.globalValueCode(i)));
            }
        }else if(attribute.isNumeric()){
            comboBoxValue.editableProperty().set(true);
        }
    }

    @FXML private void deleteObject(){
        if(choosedDoubleData!=-1){
            DoubleData objectToDelete = copyDataTable.getDataObjects().get(choosedDoubleData);
            choosedDoubleData = -1;
            somethingWasEdited = true;
            listViewObjects.getSelectionModel().clearSelection();

            copyDataTable.getDataObjects().remove(objectToDelete);
            listViewObjects.getItems().remove(objectToDelete);
            updateTextArea();
        }
    }

    @FXML private void addObjects(){
        int numOfObj = spinnerNOfObj.getValue();
        boolean random = checkBoxRandom.selectedProperty().get();

        for(int i =0; i<numOfObj;i++){
            DoubleDataObject newData = new DoubleDataObject(header);
            if(random){
                fillRandomlyData(newData);
            }else{
                for(int j=0;j<header.noOfAttr();j++){
                    newData.set(j,Double.NaN);
                }
            }
            copyDataTable.getDataObjects().add(newData);
            listViewObjects.getItems().add(newData);
        }
        updateTextArea();
        somethingWasEdited = true;
    }

    private void fillRandomlyData(DoubleDataObject data){
        Random rand = new Random();
        for(int i=0;i<header.noOfAttr();i++){
            if(header.attribute(i).isNominal()){
                NominalAttribute nomAttrib = (NominalAttribute) header.attribute(i);
                int nOfValues = nomAttrib.noOfValues();
                double attribValue = nomAttrib.globalValueCode(rand.nextInt(nOfValues));
                data.set(i, attribValue);
            }else{
                NumericalStatistics numStat = originalDataTable.getNumericalStatistics(i);
                double valueNumAtt = Math.round((rand.nextDouble() * (numStat.getMaximum() - numStat.getMinimum()) + numStat.getMinimum()) * 100);
                valueNumAtt = valueNumAtt/100;
                data.set(i, valueNumAtt);

            }
        }
    }
    @FXML private void setValue(){
        if(choosedAttribute!=-1 && choosedDoubleData!=-1 && comboBoxValue.getValue()!=null){
            if(header.attribute(choosedAttribute).isNominal()) {
                NominalAttribute nomAttrib = (NominalAttribute) header.attribute(choosedAttribute);
                copyDataTable.getDataObjects().get(choosedDoubleData).set(choosedAttribute, nomAttrib.globalValueCode((String)comboBoxValue.getValue()));
                somethingWasEdited = true;
                listViewObjects.refresh();
                updateTextArea();
            }else if(copyDataTable.attributes().attribute(choosedAttribute).isNumeric()){
                try {
                    double value = Double.parseDouble((String)comboBoxValue.getValue());
                    copyDataTable.getDataObjects().get(choosedDoubleData).set(choosedAttribute, value);
                    somethingWasEdited = true;
                    listViewObjects.refresh();
                    //updateTextArea();
                }
                catch(NumberFormatException e) {
                    Allert.showError(e);

                }
            }
        }
    }
    @FXML private void deleteObjectByKey(KeyEvent key){
        if(key.getCode().equals(KeyCode.DELETE)) deleteObject();
    }

    @FXML private void confirm(){
        if(somethingWasEdited) {
            boolean confirm = Allert.alertConfirm("Confirmation", "Confirm changes", "Click Confirm to confirm all your changes to data" );
            if(confirm){
                makeChangesToOryginal();
            }
        }else{
            stage.close();
        }
    }

    private void makeChangesToOryginal(){
        ProcessingProgress progress = new ProcessingProgress();
        progress.show();
        Task<Integer> task = new Task<Integer>() {
            @Override protected Integer call() throws Exception {
                originalDataTable.getDataObjects().clear();
                originalDataTable.getDataObjects().addAll(copyDataTable.getDataObjects());
                return  null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                progress.close();
                somethingWasEdited = true;
                stage.close();
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    @FXML private void cancel() {
        if(somethingWasEdited) {
            boolean confirm = Allert.alertConfirm("Cancel", "Cancel changes", "Click Confirm to cancel all your changes" );
            if(confirm){
                stage.close();
            }
        }else{
            stage.close();
        }
    }
    private void updateTextArea(){
        if(choosedAttribute!=-1) {
            textAreaAttribInfo.setText(DataInfo.attributePercentage(copyDataTable, choosedAttribute, null));
        }
    }

}
