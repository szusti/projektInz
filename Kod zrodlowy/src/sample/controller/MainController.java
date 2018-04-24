package sample.controller;

import javafx.fxml.FXML;
import rseslib.structure.attribute.Header;
import rseslib.structure.table.DoubleDataTable;
import sample.model.DataIntegration;
import sample.model.NonDeterClassifier;

/**
 * Created by DD on 2017-10-16.
 */
public class MainController {
    @FXML private RulesController rulesTabController;
    @FXML private PreprocessController preprocessTabController;
    @FXML private ClassificationController classificationTabController;


//    public PreprocessController getPreprocess(){
//        return preprocessControllerTab;
//    }

    @FXML private void initialize() {
        rulesTabController.injectMainController(this);
        preprocessTabController.injectMainController(this);
        classificationTabController.injectMainController(this);
    }


    public boolean missingExists(){
        return preprocessTabController.missingExists();
    }
    public DoubleDataTable getDoubleData(){
        return preprocessTabController.getDataTable();
    }
    public DataIntegration getDataIntegration(){return preprocessTabController.getDataIntegration();}
    public String getFileName(){return  preprocessTabController.getFileName();}
    public void setTestData(Header head){rulesTabController.loadedNewData(head);}
    public NonDeterClassifier getNonDeterClassifier(){return classificationTabController.getNonDeterClass();}
}
