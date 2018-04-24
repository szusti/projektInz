package sample.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import rseslib.structure.data.DoubleData;
import rseslib.structure.table.DoubleDataTable;
import sample.model.DataIntegration;

public class ShowFile {
    private static final String DEFAULT_CONTROL_INNER_BACKGROUND = "derive(-fx-base,80%)";
    private static final String HIGHLIGHTED_CONTROL_INNER_BACKGROUND_SAME_OBJECTS = "derive(palegreen, 50%)";
    private static final String HIGHLIGHTED_CONTROL_INNER_BACKGROUND_INCONSISTENCY_OBJECTS = "derive(red, 50%)";
    public static void showFile(DoubleDataTable dataTable){
        Stage showFileStage = new Stage();
        showFileStage.initModality(Modality.APPLICATION_MODAL);
        showFileStage.setMinWidth(300);
        showFileStage.setMinHeight(400);
        AnchorPane pane = new AnchorPane();
        ListView<DoubleData> list = new ListView<DoubleData>();

        list.setCellFactory(lv -> new ListCell<DoubleData>() {
            @Override
            public void updateItem(DoubleData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                }
                else{
                    int[] index = DataIntegration.indexOfFirstShow(dataTable, item);
                    setText(index[1] + " " + item.toString());
                    if(index[1]==-1){
                        setStyle("-fx-control-inner-background: " + HIGHLIGHTED_CONTROL_INNER_BACKGROUND_SAME_OBJECTS + ";");
                    }else if (index[0]>1) {
                        setStyle("-fx-control-inner-background: " + HIGHLIGHTED_CONTROL_INNER_BACKGROUND_INCONSISTENCY_OBJECTS + ";");
                    }
                    else {
                        setStyle("-fx-control-inner-background: " + DEFAULT_CONTROL_INNER_BACKGROUND + ";");
                    }
                }
            }
        });

        ObservableList<DoubleData> items = FXCollections.observableArrayList ();
        items.addAll(dataTable.getDataObjects());

        list.setItems(items);

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
