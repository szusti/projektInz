package sample.view;


import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import rseslib.structure.rule.Rule;

import java.util.Collection;

public class ShowRules {
    public static void show(Collection<Rule> rules){
        Stage showFileStage = new Stage();
        AnchorPane pane = new AnchorPane();
        ListView<Rule> list = new ListView<Rule>();
        list.getItems().addAll(rules);

        pane.setTopAnchor(list, 0.0);
        pane.setRightAnchor(list, 0.0);
        pane.setLeftAnchor(list, 0.0);
        pane.setBottomAnchor(list, 0.0);

        pane.getChildren().add(list);
        Scene sceneShowFIle = new Scene(pane,700,600);
        showFileStage.setScene(sceneShowFIle);
        showFileStage.show();
    }
}
