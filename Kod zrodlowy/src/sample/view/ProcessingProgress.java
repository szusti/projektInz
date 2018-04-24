package sample.view;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class ProcessingProgress {
    static Stage stage;
    public void show(){
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        ProgressIndicator indicator = new ProgressIndicator();
        Scene scene = new Scene(indicator);
        stage.setScene(scene);
        stage.show();

    }

    public void close(){
        stage.close();
    }
}
