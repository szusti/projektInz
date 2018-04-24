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

import rseslib.processing.missing.RoughSetNonInvasiveImputation;
import rseslib.structure.attribute.Attribute;
import rseslib.structure.attribute.Header;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.DoubleData;
import rseslib.structure.table.DoubleDataTable;
import rseslib.structure.table.NumericalStatistics;
import sample.model.DataInfo;
import sample.model.DataMissingStatistics;
import sample.view.Allert;
import sample.view.ProcessingProgress;

import java.util.List;
import java.util.Properties;
import java.util.Random;


public class PreprocessMissingController {
    @FXML private ComboBox<String> fillAutomaticallyCB;
    @FXML private ListView<DoubleData> listViewMissingObjects;
    @FXML private ListView<String> listViewAttributes;
    @FXML private TextArea textAreaAttribInfo;
    @FXML private ComboBox comboBoxValue;

    private DoubleDataTable copyDataTable;
    private DoubleDataTable originalDataTable;
    private int choosedDoubleData, choosedAttribute;
    private DataMissingStatistics dataTableStatistics;
    private Stage stage;
    private boolean somethingWasEdited = false;



    public boolean show(DoubleDataTable dataTable, DataMissingStatistics dataTableStatistics){
        try {
            stage = new Stage();
            stage.setResizable(false);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample/view/PreprocessMissingWindow.fxml"));
            loader.setController(this);
            Parent root1 = (Parent) loader.load();

            stage.setTitle("Missing");
            stage.setScene(new Scene(root1, 720, 680));
            stage.setOnCloseRequest(e->{
            e.consume();
            cancel();
                }
            );

            this.copyDataTable = (DoubleDataTable) dataTable.clone();
            this.originalDataTable = dataTable;
            this.dataTableStatistics = dataTableStatistics;

            fillAutomaticallyCB.getItems().add("Majority");
            fillAutomaticallyCB.getItems().add("Minority");
            fillAutomaticallyCB.getItems().add("RSNonInvasive");
            fillAutomaticallyCB.setValue("Majority");
            choosedDoubleData= -1;
            choosedAttribute = -1;

            listViewMissingObjects.setCellFactory(lv -> new ListCell<DoubleData>() {
                @Override
                public void updateItem(DoubleData item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                    } else {
                        String text = dataTableStatistics.getIndexOfObjectWithMissing().get(this.getIndex()) +" "+ item.toString(); // get text from item
                        setText(text);
                    }
                }
            });
            setlistViewMissingObjects();

            listViewMissingObjects.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
                if(newValue!=null) {
                    setListViewAttributes(listViewMissingObjects.getSelectionModel().getSelectedIndex());
                }
            });

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();


        }catch (Exception e){
            Allert.showError(e);
        }

        return somethingWasEdited;
    }
    private void setlistViewMissingObjects(){
        listViewMissingObjects.getItems().clear();
        ObservableList<DoubleData> observableList = FXCollections.observableArrayList();
        observableList.addAll(dataTableStatistics.getObjectWithMissing(copyDataTable));
        listViewMissingObjects.setItems(observableList);

    }
    private void setListViewAttributes(int objectIndex){
        comboBoxValue.getItems().clear();
        comboBoxValue.valueProperty().setValue(null);
        comboBoxValue.editableProperty().set(false);
        textAreaAttribInfo.clear();
        choosedAttribute = -1;
        listViewAttributes.getSelectionModel().clearSelection();

        ObservableList<String> observableListAttrib = FXCollections.observableArrayList();
        for(int i=0;i<dataTableStatistics.getMissingAttrib().get(objectIndex).size();i++){
            Attribute attrib = copyDataTable.attributes().attribute(dataTableStatistics.getMissingAttrib().get(objectIndex).get(i));
            observableListAttrib.add(attrib.name());
        }
        choosedDoubleData = objectIndex;
        listViewAttributes.setItems(observableListAttrib);

    }

    @FXML private void setTextAreaAttribInfo(){
        if(listViewAttributes.getSelectionModel().getSelectedItem()!=null) {
            comboBoxValue.getItems().clear();
            choosedAttribute =  dataTableStatistics.getMissingAttrib().get(choosedDoubleData).get(listViewAttributes.getSelectionModel().getSelectedIndex());
            textAreaAttribInfo.setText(DataInfo.attributePercentage(copyDataTable, choosedAttribute, dataTableStatistics));

            Attribute attrib = copyDataTable.attributes().attribute(choosedAttribute);
            if(attrib.isNominal()){
                comboBoxValue.valueProperty().setValue(null);
                comboBoxValue.editableProperty().set(false);
                NominalAttribute nomAttrib = (NominalAttribute)attrib;
                for(int i=0;i<nomAttrib.noOfValues();i++){
                    comboBoxValue.getItems().add(NominalAttribute.stringValue(nomAttrib.globalValueCode(i)));
                }
            }else if(attrib.isNumeric()){
                comboBoxValue.editableProperty().set(true);
            }
        }

    }

    @FXML private void setValue(){
        if(choosedAttribute!=-1 && choosedDoubleData!=-1 && comboBoxValue.getValue()!=null){
            if(copyDataTable.attributes().attribute(choosedAttribute).isNominal()) {
                NominalAttribute nomAttrib = (NominalAttribute) copyDataTable.attributes().attribute(choosedAttribute);
                listViewMissingObjects.getItems().get(choosedDoubleData).set(choosedAttribute, nomAttrib.globalValueCode((String)comboBoxValue.getValue()));
                somethingWasEdited = true;
                listViewMissingObjects.refresh();
                textAreaAttribInfo.setText(DataInfo.attributePercentage(copyDataTable, choosedAttribute, dataTableStatistics));
            }else if(copyDataTable.attributes().attribute(choosedAttribute).isNumeric()){
                try {
                    double value = Double.parseDouble((String)comboBoxValue.getValue());
                    listViewMissingObjects.getItems().get(choosedDoubleData).set(choosedAttribute, value);
                    somethingWasEdited = true;
                    listViewMissingObjects.refresh();
                    textAreaAttribInfo.setText(DataInfo.toStringAttributeInfo(copyDataTable, choosedAttribute));
                }
                catch(NumberFormatException e) {
                    Allert.showError(e);
                }
            }
        }
    }

    @FXML private void deleteObject(){
        if(choosedDoubleData!=-1){
            DoubleData objectToDelete = listViewMissingObjects.getItems().get(choosedDoubleData);
            dataTableStatistics.deleteObjectFromList(choosedDoubleData);

            choosedDoubleData = choosedAttribute = -1;
            somethingWasEdited = true;
            listViewAttributes.getItems().clear();
            listViewAttributes.getSelectionModel().clearSelection();
            listViewMissingObjects.getSelectionModel().clearSelection();
            textAreaAttribInfo.clear();
            comboBoxValue.getItems().clear();

            copyDataTable.getDataObjects().remove(objectToDelete);
            listViewMissingObjects.getItems().remove(objectToDelete);
        }

    }
    @FXML private void deleteObjectByKey(KeyEvent key){
        if(key.getCode().equals(KeyCode.DELETE)) deleteObject();
    }

    @FXML private void filterMissingValues() {
            boolean confirm = Allert.alertConfirm("Confirmation", "Filtering missing values.", "This will delete all objects with missing values.");
            if (confirm) {
                ProcessingProgress progress = new ProcessingProgress();
                progress.show();
                Task<Integer> task = new Task<Integer>() {
                    @Override protected Integer call() throws Exception {
                        filterMissingValuesbyAll();
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
    }
        private void roughSetNonInvasiveImputation(int text){
            Properties rSNonInvasive = new Properties();
            rSNonInvasive.setProperty("MAX_ITERATION_LIMIT", Integer.toString(text));
            try {
                RoughSetNonInvasiveImputation nonInvasiveFilter = new RoughSetNonInvasiveImputation(rSNonInvasive);
                copyDataTable = nonInvasiveFilter.imputation(originalDataTable);
                makeChangesToOryginal();
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    private void filterMissingValuesbyAll() {
        for (int i = dataTableStatistics.getCopyIndexOfObjectWithMissing().size() - 1 ; i >= 0 ; i--) {
            int index = dataTableStatistics.getCopyIndexOfObjectWithMissing().get(i);
            originalDataTable.getDataObjects().remove(index);
        }
    }


    @FXML private void fillAutomatically(){
        String fillMethod = fillAutomaticallyCB.getValue();

        if(fillMethod.equals("Majority") || fillMethod.equals("Minority")){
            boolean majority = true;
            String whichOne = "majority";
            if(fillMethod.equals("Minority")){
                majority = false;
                whichOne = "minority";
            }
            boolean confirm = Allert.alertConfirm("Confirmation", "Fill attributes by " +whichOne+" of values", "You will fill all missing values by "+ whichOne +" value for each attribute (for numeric average).");
            if (confirm){
                ProcessingProgress progress = new ProcessingProgress();
                progress.show();
                Task<Integer> task = null;
                if(majority) {
                    task = new Task<Integer>() {
                        @Override
                        protected Integer call() throws Exception {
                            fillMissing(true);
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            super.succeeded();
                            progress.close();
                            somethingWasEdited = true;
                            stage.close();
                        }
                    };
                }else{
                    task = new Task<Integer>() {
                        @Override
                        protected Integer call() throws Exception {
                            fillMissing(false);
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            super.succeeded();
                            progress.close();
                            somethingWasEdited = true;
                            stage.close();
                        }
                    };
                }
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();
            }
        }else if(fillMethod.equals("RSNonInvasive")) {
            String title = "Confirmation";
            String header = "Filtering missing values.";
            String content = "This will filter all objects with missing values by non-invasive data imputation. \n  See: G. Gediga, I. Duentsch, \"Rough Set Data Analysis (RDA): Non invasive data imputation\". \n \n Write max iteration limit";
            String defaultNumber = "15";
            int number = Allert.alertConfirmPutNumber(title, header, content, defaultNumber);
            if (number != -1) {
                roughSetNonInvasiveImputation(number);
            }

        }
    }
    private void fillMissing(boolean majority){
        Header head = originalDataTable.attributes();
        List<DoubleData> objectsWithMissing = dataTableStatistics.getCopyObjectWithMissing(originalDataTable);
        for(int i =0; i<objectsWithMissing.size();i++){
            fillObject(i, head, true, majority);
        }
    }

    private void fillObject(int choosedDoubleData, Header head, boolean onOriginal, boolean highest){
        DoubleData data;
            if(onOriginal) {
                data = dataTableStatistics.getCopyObjectWithMissing(originalDataTable).get(choosedDoubleData);
            }else data = dataTableStatistics.getCopyObjectWithMissing(copyDataTable).get(choosedDoubleData);
                for(int missingAttrib:dataTableStatistics.getMissingAttrib().get(choosedDoubleData)){
                    Attribute attrib = head.attribute(missingAttrib);
                    if(attrib.isNominal()){
                        int[] distribution;
                        if(onOriginal) {
                            distribution = DataInfo.getValueDistribution(originalDataTable, missingAttrib);
                        } else distribution = DataInfo.getValueDistribution(copyDataTable, missingAttrib);
                        int bestLocalIndx = 0;
                        int bestDistrib = 0;
                        if(highest) {
                            for (int localAttrib = 0; localAttrib < distribution.length; localAttrib++) {
                                if (distribution[localAttrib] > bestDistrib) {
                                    bestDistrib = distribution[localAttrib];
                                    bestLocalIndx = localAttrib;
                                }
                            }
                        }else{
                            bestDistrib = Integer.MAX_VALUE;
                            for (int localAttrib = 0; localAttrib < distribution.length; localAttrib++) {
                                if (distribution[localAttrib] < bestDistrib) {
                                    bestDistrib = distribution[localAttrib];
                                    bestLocalIndx = localAttrib;
                                }
                            }
                            }
                        data.set(missingAttrib, ((NominalAttribute)attrib).globalValueCode(bestLocalIndx));
                    }else if(attrib.isNumeric()){
                        if(onOriginal) {
                            data.set(missingAttrib, originalDataTable.getNumericalStatistics(missingAttrib).getAverage());
                        }else data.set(missingAttrib, copyDataTable.getNumericalStatistics(missingAttrib).getAverage());
                    }
                }
    }

    @FXML private void fillOneObject(){
        if(choosedDoubleData!=-1) {
            Random rand = new Random();
            DoubleData data = dataTableStatistics.getCopyObjectWithMissing(copyDataTable).get(choosedDoubleData);
            for(int missingAttrib:dataTableStatistics.getMissingAttrib().get(choosedDoubleData)){
                if (Double.isNaN(data.get(missingAttrib))) {
                    if (copyDataTable.attributes().attribute(missingAttrib).isNominal()) {
                        NominalAttribute nomAttrib = (NominalAttribute) data.attributes().attribute(missingAttrib);
                        double attribValue;
                        if (!nomAttrib.isDecision()) {
                            int nOfValues = nomAttrib.noOfValues();
                            attribValue = nomAttrib.globalValueCode(rand.nextInt(nOfValues));
                        } else {
                            attribValue = Double.NaN;
                        }
                        data.set(missingAttrib, attribValue);
                    } else {
                        double valueNumAtt;
                        NumericalStatistics numStat = copyDataTable.getNumericalStatistics(missingAttrib);
                        if (copyDataTable.attributes().attribute(missingAttrib).isDecision()) {
                            valueNumAtt = Double.NaN;
                        } else {
                            valueNumAtt = Math.round((rand.nextDouble() * (numStat.getMaximum() - numStat.getMinimum()) + numStat.getMinimum()) * 100);
                            valueNumAtt = valueNumAtt / 100;
                        }
                        data.set(missingAttrib, valueNumAtt);
                    }
                }
            }
            listViewMissingObjects.refresh();
        }
    }

    @FXML private void cancel(){
        if(somethingWasEdited) {
            boolean confirm = Allert.alertConfirm("Cancel", "Cancel changes", "Click Confirm to cancel all your changes" );
            if(confirm){
                stage.close();
            }
        }else{
            stage.close();
        }


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
                if(copyDataTable.noOfObjects()==originalDataTable.noOfObjects()) {
                    for (int objIndex : dataTableStatistics.getIndexOfObjectWithMissing()) {
                        originalDataTable.getDataObjects().set(objIndex, copyDataTable.getDataObjects().get(objIndex));
                    }
                }else{
                    originalDataTable.getDataObjects().clear();
                    originalDataTable.getDataObjects().addAll(copyDataTable.getDataObjects());
                }
                somethingWasEdited = true;
                return  null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                progress.close();
                stage.close();
            }
        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

}
