package sample.model;

import rseslib.structure.attribute.Header;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.DoubleData;
import rseslib.structure.table.DoubleDataTable;

public class DataInfo {
    public static String toStringAttributeInfo(DoubleDataTable data, int indexAttr){
        StringBuilder stringB = new StringBuilder();
        Header head = data.attributes();
        stringB.append(head.attribute(indexAttr) + "\n \n");
        if(head.isNominal(indexAttr)){
            int[] valueDistr = getValueDistribution(data, indexAttr);
//            int nOAtt = 0;
//            for (int numer : valueDistr) {
//                nOAtt += numer;
//            }
            stringB.append("Number of appearance: \n");
            NominalAttribute decAttr = (NominalAttribute) data.attributes().attribute(indexAttr);
            for (int j = 0; j < decAttr.noOfValues(); j++) {
                stringB.append("'" + NominalAttribute.stringValue(decAttr.globalValueCode(j)) + "' = " + valueDistr[j] + " \n");
            }
        }else if(head.isNumeric(indexAttr)){
            stringB.append(data.getNumericalStatistics(indexAttr) + "\n");
        }
        return stringB.toString();
    }


    public static int[] getValueDistribution(DoubleDataTable data, int attrInd) {
        Header head = data.attributes();
        if (!head.isNominal(attrInd)) return null;
        int[][] mValueDistribution = new int[head.noOfAttr()][];
        if (mValueDistribution[attrInd]==null)
        {
            NominalAttribute attr = (NominalAttribute)head.attribute(attrInd);
            mValueDistribution[attrInd] = new int[attr.noOfValues()];
            for (DoubleData dObj : data.getDataObjects()) {
                if (attr.localValueCode(dObj.get(attrInd)) != -1) {
                    mValueDistribution[attrInd][attr.localValueCode(dObj.get(attrInd))]++;
                }
            }
        }
        return mValueDistribution[attrInd];
    }

    public static String toStringAttributeInfoFull(DoubleDataTable data){
        StringBuilder stringB = new StringBuilder();
        Header head = data.attributes();
        for(int i = 0; i<head.noOfAttr();i++){
            stringB.append(toStringAttributeInfo(data,i));
            stringB.append("~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
        }
        return stringB.toString();
    }

    public static String attributePercentage(DoubleDataTable data, int indexAttr, DataMissingStatistics dataMissingStatistics){
        StringBuilder stringB = new StringBuilder();
        Header head = data.attributes();
        stringB.append(head.attribute(indexAttr) + "\n\n" );

        if(dataMissingStatistics!=null){
            int[] missing = dataMissingStatistics.nOfMissingAttrib();
            int numberOfMissing = missing[indexAttr];
            if(numberOfMissing>0){
                stringB.append("Number of objects that missing this attribute: " + numberOfMissing + "("+ roundPercent(data.noOfObjects(), numberOfMissing) + "%)\n\n");
            }
        }
        if(head.isNominal(indexAttr)){
            int[] valueDistr = getValueDistribution(data, indexAttr);

            stringB.append("Number of appearance: \n");
            NominalAttribute decAttr = (NominalAttribute) data.attributes().attribute(indexAttr);
            for (int j = 0; j < decAttr.noOfValues(); j++) {
                stringB.append("'" + NominalAttribute.stringValue(decAttr.globalValueCode(j)) + "' = " + valueDistr[j] + " (" +roundPercent(data.noOfObjects(), valueDistr[j]) + "%) \n");
            }
        }
        else if(head.isNumeric(indexAttr)) {
            stringB.append(data.getNumericalStatistics(indexAttr) + "\n");
        }
        return stringB.toString();
    }

    private static int roundPercent(double numberOfObject, double numberOfValues){
        double percent = (numberOfValues/numberOfObject)*100;
        int roundedPercent = (int)Math.round(percent);
        if(roundedPercent == 0) roundedPercent=1;
        if(roundedPercent == numberOfObject) roundedPercent-=1;
        return roundedPercent;
    }
}
