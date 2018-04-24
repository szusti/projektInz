package sample.controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import rseslib.structure.attribute.Attribute;
import rseslib.structure.attribute.Header;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.DoubleData;
import rseslib.structure.data.DoubleDataObject;
import rseslib.structure.data.DoubleDataWithDecision;
import rseslib.structure.table.ArrayListDoubleDataTable;
import rseslib.structure.table.DoubleDataTable;
import rseslib.structure.table.NumericalStatistics;
import rseslib.system.PropertyConfigurationException;
import sample.view.Allert;
import sample.view.InformationDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;


public class RulesController {

    @FXML private ListView<DoubleData> listViewObjects;
    @FXML private ListView<String> listViewAttributes;
    @FXML private ComboBox comboBoxValue;
    @FXML private Spinner<Integer> spinnerNOfObj;
    @FXML private CheckBox checkBoxRandom;

    private MainController mainController;
    private DoubleDataTable testingData;
    private int choosedDoubleData = -1;
    private int choosedAttribute = -1;


    public void initialize(){
        testingData = null;
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
    }
    public void injectMainController(MainController mainController){
        this.mainController = mainController;
    }
    @FXML private void deleteObject(){
        if(choosedDoubleData!=-1){
            DoubleData objectToDelete = testingData.getDataObjects().get(choosedDoubleData);
            choosedDoubleData = -1;
            listViewObjects.getSelectionModel().clearSelection();
            testingData.getDataObjects().remove(objectToDelete);
            listViewObjects.getItems().remove(objectToDelete);
        }
    }
    @FXML private void addObjects(){
        if(testingData!=null) {
            int numOfObj = spinnerNOfObj.getValue();
            boolean random = checkBoxRandom.selectedProperty().get();
            for (int i = 0; i < numOfObj; i++) {
                DoubleDataObject newData = new DoubleDataObject(testingData.attributes());
                if (random) {
                    fillRandomlyData(newData);
                } else {
                    for (int j = 0; j < testingData.attributes().noOfAttr(); j++) {
                        newData.set(j, Double.NaN);
                    }
                }
                testingData.getDataObjects().add(newData);
                listViewObjects.getItems().add(newData);
            }
        }
    }
    @FXML private void setValue(){
        if(testingData!=null) {
            if (choosedAttribute != -1 && choosedDoubleData != -1 && comboBoxValue.getValue() != null) {
                if (testingData.attributes().attribute(choosedAttribute).isNominal()) {
                    NominalAttribute nomAttrib = (NominalAttribute) testingData.attributes().attribute(choosedAttribute);
                    testingData.getDataObjects().get(choosedDoubleData).set(choosedAttribute, nomAttrib.globalValueCode((String) comboBoxValue.getValue()));
                    listViewObjects.refresh();
                } else if (testingData.attributes().attribute(choosedAttribute).isNumeric()) {
                    try {
                        double value = Double.parseDouble((String) comboBoxValue.getValue());
                        testingData.getDataObjects().get(choosedDoubleData).set(choosedAttribute, value);
                        listViewObjects.refresh();
                    } catch (NumberFormatException e) {
                        Allert.showError(e);
                    }
                }
            }
        }
    }
    @FXML private void deleteObjectByKey(KeyEvent key){if(key.getCode().equals(KeyCode.DELETE)) deleteObject();}
    @FXML private void predict(){
        if(mainController.getNonDeterClassifier()!=null){
            try {
                for (DoubleData data : testingData.getDataObjects()) {
                    ((DoubleDataWithDecision)data).setDecision(mainController.getNonDeterClassifier().classify(data));
                    listViewObjects.refresh();
                }
            }catch (PropertyConfigurationException pce){
                Allert.showError(pce);
            }catch (IndexOutOfBoundsException ite){
                InformationDialog.showInfo("Wrong builded classifier");
            }

        }else{
            InformationDialog.showInfo("Didn't found rules.\nMake sure you have generated classificator in Classification tab (except cross-validation)");
        }

    }

    private void setChoosedAttribute(int attrib){
        comboBoxValue.getItems().clear();
        Attribute attribute = testingData.attributes().attribute(choosedAttribute);
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

    public void loadedNewData(Header head){
        if (head != null) {
            testingData = new ArrayListDoubleDataTable(head);
            ObservableList<String> observableListAttrib = FXCollections.observableArrayList();
            for (int i = 0; i < head.noOfAttr(); i++) {
                Attribute attrib = head.attribute(i);
                if (!attrib.isDecision()) {
                    observableListAttrib.add(attrib.name());
                }
            }
            listViewAttributes.setItems(observableListAttrib);
            listViewObjects.getItems().addAll(testingData.getDataObjects());
        }
    }

    private void fillRandomlyData(DoubleDataObject data){
        Random rand = new Random();
        for(int i=0;i<testingData.attributes().noOfAttr();i++){
            if(testingData.attributes().attribute(i).isNominal()){
                NominalAttribute nomAttrib = (NominalAttribute) testingData.attributes().attribute(i);
                double attribValue;
                if(!nomAttrib.isDecision()) {
                    int nOfValues = nomAttrib.noOfValues();
                    attribValue = nomAttrib.globalValueCode(rand.nextInt(nOfValues));
                }else {
                    attribValue= Double.NaN;
                }
                data.set(i, attribValue);
            }else{
                double valueNumAtt;
                NumericalStatistics numStat = testingData.getNumericalStatistics(i);
                if(testingData.attributes().attribute(i).isDecision()){
                    valueNumAtt = Double.NaN;
                }else{
                    valueNumAtt = Math.round((rand.nextDouble() * (numStat.getMaximum() - numStat.getMinimum()) + numStat.getMinimum()) * 100);
                    valueNumAtt = valueNumAtt/100;
                }
                //System.out.println(numStat.getMinimum() + " and max" + numStat.getMaximum());
                data.set(i, valueNumAtt);
            }
        }
    }

    @FXML private void deleteAll(){
        testingData.getDataObjects().clear();
        choosedDoubleData = -1;
        listViewObjects.getSelectionModel().clearSelection();
        listViewObjects.getItems().clear();
    }





}

