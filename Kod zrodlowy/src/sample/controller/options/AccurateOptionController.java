package sample.controller.options;

import sample.view.Allert;

import java.util.Properties;

/**
 * Created by DD on 2017-11-28.
 */
public class AccurateOptionController {
    public static boolean options(Properties prop){
        String title = "Options";
        String header = "Option for accurate rule generator";
        String content = "Maximal number of rules used in the classifier";
        String defaultNumber = prop.getProperty("maxNumberOfRules");
        int number = Allert.alertConfirmPutNumber(title, header, content, defaultNumber );
        if (number != -1) {
            prop.setProperty("maxNumberOfRules", String.valueOf(number));
            return false;
        }
        return true;

    }
}
