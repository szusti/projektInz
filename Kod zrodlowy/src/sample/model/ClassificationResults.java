package sample.model;

import rseslib.processing.classification.MultipleTestResult;
import rseslib.processing.classification.TestResult;
import rseslib.structure.attribute.Header;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.DoubleData;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class ClassificationResults {
    private NonDeterClassifier nonDeterClass;
    private TestResult testResult;
    private MultipleTestResult multipleTestResults;
    private Instant time;
    private String ruleGeneratorType;
    private String classificationType;
    private String fileName;
    private Properties prop;
    private Header m_head;

    public ClassificationResults(NonDeterClassifier nonDeterClass, TestResult testResult, MultipleTestResult multipleTestResults,Instant time, Properties prop, String classificationType, String fileName, Header head){
        this.nonDeterClass = nonDeterClass;
        this.testResult = testResult;
        this.multipleTestResults = multipleTestResults;
        this.time = time;
        this.ruleGeneratorType = prop.getProperty("ruleGenerator");
        this.classificationType = classificationType;
        this.prop = new Properties();
        this.prop.putAll(prop);
        this.fileName = fileName;
        this.m_head = head;
    }

    public NonDeterClassifier getNonDeterClass() {
        return nonDeterClass;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public MultipleTestResult getMultipleTestResults() {
        return multipleTestResults;
    }

    public Instant getTime() {
        return time;
    }

    public String getRuleGeneratorType() {
        return ruleGeneratorType;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public Properties getProp() {return prop;}

    @Override
    public String toString(){
        StringBuilder stringB = new StringBuilder();
        toStringGenerally(stringB);
        toStringResult(stringB);
        toStringConfusionMatrix(stringB);
        toStringOptions(stringB);

        return stringB.toString();
    }

    private void toStringGenerally(StringBuilder stringB){
        stringB.append(fileName +"\n");
        stringB.append("Used deterministic rule generator - "+ruleGeneratorType +"\n");
        stringB.append("Used classifier test type - "+ classificationType +"\n");
        LocalDateTime ldt = LocalDateTime.ofInstant(time, ZoneId.systemDefault());
        String timeString =  String.format("%d %s %d at %d:%d%n", ldt.getDayOfMonth(), ldt.getMonth(),ldt.getYear(), ldt.getHour(), ldt.getMinute());
        stringB.append("Date - " + timeString +"\nResults \n");
    }
    private void toStringResult(StringBuilder stringB){
        if(testResult!=null){
            stringB.append(testResult.toString() + "\n");
            for (Map.Entry<?, ?> entry: testResult.getStatistics().entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                stringB.append(key+ " "+ value + "\n");

            }
        }else if(multipleTestResults!=null){
            stringB.append(multipleTestResults.toString() +"\n");
        }else{
            stringB.append("--------------------------------ERROR------------------------------- \n");
        }
    }
    public void toStringConfusionMatrix(StringBuilder stringB){
        boolean test = testResult!=null;
        if(testResult!=null || multipleTestResults!=null){
            Header head = m_head;
            stringB.append("\n -------------------Confusion Matrix------------\n");
            char names = 'a';
            int maxNODigits = 0;

            for(int i=0;i<head.nominalDecisionAttribute().noOfValues();i++){
                for(int j=0;j<head.nominalDecisionAttribute().noOfValues();j++) {
                    int number;
                    if(test) {
                        number = testResult.getNoOfObjects(head.nominalDecisionAttribute().globalValueCode(i), head.nominalDecisionAttribute().globalValueCode(j));
                    }else{
                        number = multipleTestResults.getNoOfObjects(head.nominalDecisionAttribute().globalValueCode(i), head.nominalDecisionAttribute().globalValueCode(j));
                    }
                    int length = String.valueOf(number).length();
                    if(length>maxNODigits) maxNODigits=length;
                }
            }

            maxNODigits+=7;
            for(int i=0;i<head.nominalDecisionAttribute().noOfValues();i++){
                stringB.append(String.format("%"+(maxNODigits-2)+"s", names));
                names++;
            }
            names = 'a';
            stringB.append("\n");

            for(int i=0;i<head.nominalDecisionAttribute().noOfValues();i++){
                for(int j=0;j<head.nominalDecisionAttribute().noOfValues();j++){
                    int number;
                    if(test) {
                        number = testResult.getNoOfObjects(head.nominalDecisionAttribute().globalValueCode(i), head.nominalDecisionAttribute().globalValueCode(j));
                    }else{
                        number = multipleTestResults.getNoOfObjects(head.nominalDecisionAttribute().globalValueCode(i), head.nominalDecisionAttribute().globalValueCode(j));
                    }
                    int length = String.valueOf(number).length();
                    stringB.append(String.format("%"+(maxNODigits-length)+"s|",number));
                }
                stringB.append(" "+names +" = "+NominalAttribute.stringValue(head.nominalDecisionAttribute().globalValueCode(i)) + "\n");
                names++;
            }

            stringB.append("\n");
        }
    }
    private void toStringOptions(StringBuilder stringB){
        stringB.append("\n\nUsed options:\n");
        stringB.append("~~~~Non-deterministic rule generator\n");
        stringB.append("maxNumberOfDecValues " +prop.get("maxNumberOfDecValues") +"\n");
        stringB.append("Alfa " +prop.get("confidence") +"\n");
        if(ruleGeneratorType.equals("Covering")){
            stringB.append("~~~~Covering rule generator\n");
            stringB.append("coverage " +prop.get("coverage") +"\n");
            stringB.append("searchWidth " +prop.get("searchWidth") +"\n");
            stringB.append("margin " +prop.get("margin") +"\n");
        }else if(ruleGeneratorType.equals("Accurate")){
            stringB.append("~~~~Accurate rule generator\n");
            stringB.append("maxNumberOfRules " +prop.get("maxNumberOfRules") +"\n");
        }else if(ruleGeneratorType.equals("JohnsonReducts") || ruleGeneratorType.equals("GlobalReducts") || ruleGeneratorType.equals("LocalReducts") ){
            stringB.append("~~~~"+prop.getProperty("ruleGenerator")+" rule generator\n");
            stringB.append("IndiscernibilityForMissing " +prop.get("IndiscernibilityForMissing") +"\n");
            stringB.append("DiscernibilityMethod " +prop.get("DiscernibilityMethod") +"\n");
            stringB.append("GeneralizedDecisionTransitiveClosure " +prop.get("GeneralizedDecisionTransitiveClosure") +"\n");
            stringB.append("MissingValueDescriptorsInRules " +prop.get("MissingValueDescriptorsInRules") +"\n");
            if(ruleGeneratorType.equals("JohnsonReducts")){
                stringB.append("JohnsonReducts " +prop.get("JohnsonReducts") +"\n");
            }
        }
        if(classificationType.equals("Cross-Validation")){
            stringB.append("~~~~Cross-valid\n");
            stringB.append("noOfFolds " +prop.get("noOfFolds") +"\n");
        }
//        for (Map.Entry<?, ?> entry: prop.entrySet()) {
//            String key = (String) entry.getKey();
//            String value = (String) entry.getValue();
//            stringB.append(key+ " "+ value + "\n");
//
//        }

    }
}
