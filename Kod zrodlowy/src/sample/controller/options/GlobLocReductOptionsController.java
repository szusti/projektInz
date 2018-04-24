package sample.controller.options;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sample.view.Allert;
import sample.view.InformationDialog;

import java.io.IOException;
import java.util.Properties;

public class GlobLocReductOptionsController {
    @FXML private ComboBox<String> comboBIndiscernibility, comboBDiscernibility;
    @FXML private CheckBox checkBGeneralized, checkBMissing;
    protected boolean cancel = true;
    private Stage stage;
    protected Properties prop;

    public boolean show(Properties prop) {
        try{
        stage = new Stage();
        FXMLLoader loader = loader();
        loader.setController(this);
        Parent root1 = (Parent) loader.load();

        stage.setTitle("Options");
        stage.setScene(new Scene(root1, 322, 349));

        stage.setOnCloseRequest(e -> {
            e.consume();
            cancel();
            });

            this.prop = prop;
            onLoadView();
            comboBDiscernibility.getSelectionModel().selectedIndexProperty().addListener(e->checkBGeneralizedEnable());

            stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }catch (IOException e){
            Allert.showError(e);
        }
        return this.cancel;
    }

    protected FXMLLoader loader(){
        return new FXMLLoader(getClass().getResource("/sample/view/options/GlobLocReductOptionsView.fxml"));

    }
    protected void onLoadView(){
        comboBIndiscernibility.getItems().addAll("DiscernFromValue", "DiscernFromValueOneWay", "DontDiscernFromValue");
        comboBIndiscernibility.setValue(prop.getProperty("IndiscernibilityForMissing"));
        comboBDiscernibility.getItems().addAll("All", "OrdinaryDecisionAndInconsistenciesOmitted",
                "GeneralizedDecision", "GeneralizedDecisionAndOrdinaryChecked");
        comboBDiscernibility.setValue(prop.getProperty("DiscernibilityMethod"));
        checkBGeneralizedEnable();
        if(prop.getProperty("MissingValueDescriptorsInRules").equals("TRUE")){
            checkBMissing.selectedProperty().setValue(true);
        }
    }
    private void checkBGeneralizedEnable(){
        String choosed = comboBDiscernibility.getSelectionModel().getSelectedItem();
        if(choosed.equals("GeneralizedDecision") || choosed.equals("GeneralizedDecisionAndOrdinaryChecked")){
            checkBGeneralized.disableProperty().setValue(false);
            if(prop.getProperty("GeneralizedDecisionTransitiveClosure").equals("TRUE")) checkBGeneralized.selectedProperty().setValue(true);
        }else {
            checkBGeneralized.disableProperty().setValue(true);
            if(!prop.getProperty("GeneralizedDecisionTransitiveClosure").equals("TRUE")) checkBGeneralized.selectedProperty().setValue(false);

        }
    }
    @FXML protected void confirm(){
        cancel = false;

        makeChanges();

        cancel();
    }

    protected void makeChanges(){
        cancel = false;

        prop.setProperty("IndiscernibilityForMissing", comboBIndiscernibility.getValue());
        prop.setProperty("DiscernibilityMethod", comboBDiscernibility.getValue());

        if(!checkBGeneralized.disableProperty().get() && checkBGeneralized.isSelected()) {
            prop.setProperty("GeneralizedDecisionTransitiveClosure", "TRUE");
        }else prop.setProperty("GeneralizedDecisionTransitiveClosure", "FALSE");

        if(checkBMissing.isSelected()) {
            prop.setProperty("MissingValueDescriptorsInRules", "TRUE");
        }else  prop.setProperty("MissingValueDescriptorsInRules", "FALSE");

    }
    @FXML protected void cancel(){
        stage.close();
    }

    @FXML private void helpIndiscernibility(){
        InformationDialog.showInfo("The method of discerning missing values from others\n" +
                "Values: DiscernFromValue, DiscernFromValueOneWay, DontDiscernFromValue\n" +
                "or individually for each attribute ");
    }
    @FXML private void helpDiscernibility(){
        InformationDialog.showInfo("Discernibility matrix building method,\n" +
                "it defines what is to be discerned. \n" +
                "All, OrdinaryDecisionAndInconsistenciesOmitted,\n" +
                "GeneralizedDecision, GeneralizedDecisionAndOrdinaryChecked)");
    }
    @FXML private void helpGeneralized(){
        InformationDialog.showInfo("Used only if DiscernibilityMethod set to one of:\n" +
                "GeneralizedDecision or GeneralizedDecisionAndOrdinaryChecked.\n" +
                "In case of missing values in data and IndiscernibilityForMissing<>DiscernFromValue\n" +
                "the generalized decision does not define an equivalence relation.\n" +
                "Switch indicates whether the generalized decision is transitively closed\n" +
                "before it is used to build discernibility matrix.   ");
    }
    @FXML private void helpMissing(){
        InformationDialog.showInfo("Switch indicating whether descriptors with missing values\n" +
                "are enabled in rules");
    }
}
