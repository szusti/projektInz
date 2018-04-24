package sample.model;

import rseslib.structure.attribute.Header;
import rseslib.structure.data.DoubleData;
import rseslib.structure.data.DoubleDataWithDecision;
import rseslib.structure.rule.Rule;
import rseslib.structure.table.DoubleDataTable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

public class DataIntegration {
    private LinkedList<LinkedList<Integer>> sameObjectDiffDec;
    private LinkedList<LinkedList<Integer>> sameObject;

    public DataIntegration(DoubleDataTable data) {
        sameObjectDiffDec = new LinkedList<LinkedList<Integer>>();
        sameObject = new LinkedList<LinkedList<Integer>>();
        Header m_Header = data.attributes();
        boolean same;
        for (int i = 0; i < data.noOfObjects(); i++) {
            LinkedList<Integer> sameObjectDiffDecTemp = new LinkedList<>();
            LinkedList<Integer> sameObjectTemp = new LinkedList<>();
            sameObjectDiffDecTemp.add(i);
            sameObjectTemp.add(i);
            DoubleDataWithDecision singleData1 = (DoubleDataWithDecision) data.getDataObjects().get(i);
            for (int j = i; j < data.noOfObjects(); j++) {
                if (i != j) {
                    DoubleDataWithDecision singleData2 = (DoubleDataWithDecision) data.getDataObjects().get(j);
                    same = true;
                    for (int attribute = 0; attribute < m_Header.noOfAttr(); attribute++) {
                        if (!m_Header.isDecision(attribute) && (singleData1.get(attribute) != singleData2.get(attribute))) {
                            same = false;
                        }
                    }
                    if (same) {
                        if (singleData1.getDecision() == singleData2.getDecision()) {
                            sameObjectTemp.add(j);
                        } else {
                            sameObjectDiffDecTemp.add(j);
                        }
                    }
                }
            }
            if (sameObjectTemp.size() > 1){
                checkIfExists(sameObjectTemp,false);
            }
            if (sameObjectDiffDecTemp.size() > 1){
                checkIfExists(sameObjectDiffDecTemp,true);
            }

        }

    }

    private void checkIfExists(LinkedList<Integer> sameObjectData, boolean diffDec){
        LinkedList<LinkedList<Integer>> referenceMain;
        if(diffDec){
            referenceMain = sameObjectDiffDec;
        }else{
            referenceMain = sameObject;
        }
        boolean exists = false;
        Collections.sort(sameObjectData);
        for(LinkedList<Integer> subSameList : referenceMain) {
            if (!Collections.disjoint(subSameList, sameObjectData)) {
                exists = true;
                LinkedList<Integer> notPresent = new LinkedList<>(sameObjectData);
                notPresent.removeAll(subSameList);
                subSameList.addAll(notPresent);
                Collections.sort(subSameList);
            }
        }
        if(!exists){
            referenceMain.add(sameObjectData);
        }
    }
    public LinkedList<LinkedList<Integer>> getSameObject() {
        return sameObject;
    }

    public LinkedList<LinkedList<Integer>> getSameObjectDiffDec() {
        return sameObjectDiffDec;
    }

    public int getNumberOfSameObjects(){
    int nOfObject = 0;
    if(!sameObject.isEmpty()) {
        for (LinkedList<Integer> subList : sameObject) {
            nOfObject += subList.size();
        }
    }
        return nOfObject;
    }
    public int getNumberOfSameObjectsDiffDec(){
        int nOfObject = 0;
        if(!sameObjectDiffDec.isEmpty()) {
            for (LinkedList<Integer> subList : sameObjectDiffDec) {
                nOfObject += subList.size();
            }
        }
        return nOfObject;
    }

    public void deleteObject(int index){
        boolean foundObject;
        int group = 0;
        for(int i=0;i<sameObjectDiffDec.size();i++){
            foundObject = false;
            for(int j=0;j<sameObjectDiffDec.get(i).size();j++){
                if(sameObjectDiffDec.get(i).get(j)==index){
                    if(sameObjectDiffDec.get(i).size()==2){
                        sameObjectDiffDec.remove(i);
                    }else{
                        sameObjectDiffDec.get(i).remove(j);
                    }
                    foundObject = true;
                    break;
                }
            }
            if(foundObject) break;
        }

        for(int i=0;i<sameObject.size();i++){
            foundObject = false;
            for(int j=0;j<sameObject.get(i).size();j++){
                if(sameObject.get(i).get(j)==index){
                    if(sameObject.get(i).size()==2){
                        sameObject.remove(i);
                    }else{
                        sameObject.get(i).remove(j);
                    }
                    foundObject = true;
                    break;
                }
            }
            if(foundObject) break;
        }
    }

    public static int[] indexOfFirstShow(DoubleDataTable dataTable, DoubleData newData){
        int[] tab = {0,-1};
        Header head = dataTable.attributes();
        for(int attrib=0;attrib<head.noOfAttr();attrib++){
            if(newData.get(attrib)==Double.NaN){
                return tab;
            }
        }
        //int different = -1;
        boolean same = true;
        for(int i =0;i<dataTable.getDataObjects().size();i++){
            same = true;
            for(int j=0;j<head.noOfAttr();j++){
                if(!head.attribute(j).isDecision()) {
                    if(dataTable.getDataObjects().get(i).get(j)!=newData.get(j)) same=false;
                }
            }
            if(same){
                tab[0]++;
                if(tab[1]==-1){
                    tab[1]=i;
                }
            }
        }
        return tab;
    }



    public int checkifExist(int index){
        int exist = -1;
        if(!sameObject.isEmpty()){
            boolean foundObject;
            for(int i=0;i<sameObject.size();i++){
            foundObject = false;
            for(int j=0;j<sameObject.get(i).size();j++){
                if(sameObject.get(i).get(j)==index){
                    foundObject = true;
                    exist = 0;
                    break;
                }
            }
            if(foundObject){break;}
            }
        }

        if(!sameObjectDiffDec.isEmpty()){
            boolean foundObject;
            for(int i=0;i<sameObjectDiffDec.size();i++){
                foundObject = false;
                for(int j=0;j<sameObjectDiffDec.get(i).size();j++){
                    if(sameObjectDiffDec.get(i).get(j)==index){
                        foundObject = true;
                        exist = 1;
                        break;
                    }
                }
                if(foundObject){break;}
            }
        }

        return exist;
    }
}
