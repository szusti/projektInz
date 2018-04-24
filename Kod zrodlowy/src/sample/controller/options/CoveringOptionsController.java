package sample.controller.options;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sample.view.Allert;
import sample.view.InformationDialog;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by DD on 2017-11-28.
 */
public class CoveringOptionsController {
    @FXML TextField textFieldCoverage, textFieldSearch, textFieldMargin;
    private Stage stage;
    private Properties prop;
    private boolean cancel = true;

    public boolean show(Properties prop){
        try{
            stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample/view/options/CoveringOptionsView.fxml"));
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

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        }catch (IOException e){
            Allert.showError(e);
        }
        return this.cancel;
    }

    private void onLoadView(){
        textFieldCoverage.setText(prop.getProperty("coverage"));
        textFieldMargin.setText(prop.getProperty("margin"));
        textFieldSearch.setText(prop.getProperty("searchWidth"));
    }
    @FXML private void helpCoverage(){
        InformationDialog.showInfo("Treshold for coverage of a training table by rules (between 0 and 1)");
    }
    @FXML private void helpSearch(){
        InformationDialog.showInfo("Width of searching beam in one rule generator");
    }

    @FXML private void confirm() {
        double numberCoverage = stringToDouble(textFieldCoverage.getText());
        double numberMargin = stringToDouble(textFieldMargin.getText());
        int numberSearch = stringToInt(textFieldSearch.getText());
        //System.out.println(numberCoverage+"<"+numberMargin+">"+numberSearch);
        if(numberCoverage>1 || 0>numberCoverage){
            InformationDialog.showInfo("Coverage should be between 0.0 and 1.0");
        }
        else if(!Double.isNaN(numberCoverage) && !Double.isNaN(numberMargin)  && numberSearch!=-666 ){
            cancel = false;
            prop.setProperty("coverage", String.valueOf(numberCoverage));
            prop.setProperty("margin", String.valueOf(numberMargin));
            prop.setProperty("searchWidth", String.valueOf(numberSearch));
            cancel();
        }
    }

    private double stringToDouble(String text){
        try{
            double number = Double.parseDouble(text);
            return number;
        }catch(NumberFormatException e){
            Allert.showError(e);
            return Double.NaN;
        }

    }
    private int stringToInt(String text){
        try{
            int number = Integer.parseInt(text);
            return number;
        }catch(NumberFormatException e){
            Allert.showError(e);
            return -666;
        }

    }


    @FXML private void cancel() {
        stage.close();
    }
}


