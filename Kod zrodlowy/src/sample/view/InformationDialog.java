package sample.view;

import javafx.scene.control.Alert;

/**
 * Created by DD on 2017-11-27.
 */
public class InformationDialog {
    public static void showInfo(String content){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText(content);

        alert.showAndWait();
    }
}
