package sample.controller.options;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sample.view.Allert;
import sample.view.InformationDialog;

import java.io.IOException;
import java.util.Properties;

public class ClassificatorOptionsController {
    @FXML private TextField textFieldAlfa;
    @FXML private Spinner<Integer> spinnerNODec;

    private Stage stage;
    private Properties prop;
    private boolean cancel = true;

    public boolean show(Properties prop, int numberOfDec){
        try{
            stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample/view/options/ClassificatorOptionsView.fxml"));
            loader.setController(this);
            Parent root1 = (Parent) loader.load();

            stage.setTitle("Options");
            stage.setScene(new Scene(root1, 322, 349));

            stage.setOnCloseRequest(e -> {
                e.consume();
                cancel();
            });

            this.prop = prop;
            onLoadView(numberOfDec);

            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();
        }catch (IOException e){
            Allert.showError(e);
        }
        return this.cancel;
    }
    private void onLoadView(int numberOfDec){
        textFieldAlfa.setText(prop.getProperty("confidence"));
        SpinnerValueFactory<Integer> valueFactoryTest = null;
        if(numberOfDec==0){

        }else {
            valueFactoryTest = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, numberOfDec, Integer.parseInt(prop.getProperty("maxNumberOfDecValues")));
        }
        spinnerNODec.setValueFactory(valueFactoryTest);
    }

    @FXML private void confirm() {
        double numberConfi = stringToDouble(textFieldAlfa.getText());
        if(numberConfi>1 || 0.5> numberConfi){
            InformationDialog.showInfo("Alfa should be between 0.5 and 1");
        }
        else if(!Double.isNaN(numberConfi)){
            cancel = false;
            prop.setProperty("confidence", String.valueOf(numberConfi));
            //System.out.println(spinnerNODec.getValueFactory()==null);
            if(spinnerNODec.getValueFactory()!=null) {
                prop.setProperty("maxNumberOfDecValues", String.valueOf(spinnerNODec.getValue()));
            }
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

    @FXML private void cancel() {
        stage.close();
    }
}
