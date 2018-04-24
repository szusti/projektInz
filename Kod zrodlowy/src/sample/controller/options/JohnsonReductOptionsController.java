package sample.controller.options;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import sample.view.InformationDialog;

/**
 * Created by DD on 2017-11-28.
 */
public class JohnsonReductOptionsController extends GlobLocReductOptionsController{
    @FXML private ComboBox<String> comboBJohnReducts;

    @Override
    protected FXMLLoader loader(){
        return new FXMLLoader(getClass().getResource("/sample/view/options/JohnsonReductOptionsView.fxml"));

    }

    @Override
    protected void onLoadView() {
        super.onLoadView();
        comboBJohnReducts.getItems().addAll("One", "All");
        comboBJohnReducts.setValue(prop.getProperty("JohnsonReducts"));

    }

    @Override
    protected void confirm() {
        cancel = false;
        makeChanges();
        prop.setProperty("JohnsonReducts", comboBJohnReducts.getValue());
        cancel();
    }

    @FXML private void helpJohnReducts(){
        InformationDialog.showInfo("Switch defining whether Johnson's method generates\n" +
                "one or all possible Johnson's reducts\n" +
                "Values: One, All");
    }


}
