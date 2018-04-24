package sample.model;

import rseslib.structure.attribute.Header;
import rseslib.structure.data.DoubleData;
import rseslib.structure.table.DoubleDataTable;

import java.util.*;


public class DataMissingStatistics {

    private List<Integer> objectWithMissingValueIndex;
    private LinkedList<LinkedList<Integer>> missingAttrib;
    private final List<Integer> copyObjectWithMissingValueIndex;
    private final LinkedList<LinkedList<Integer>> copyMissingAttrib;
    private int[] nOfMissAttrib;

    public DataMissingStatistics(DoubleDataTable data) {
        missingAttrib = new LinkedList<LinkedList<Integer>>();
        objectWithMissingValueIndex = new ArrayList<>();
        copyMissingAttrib = new LinkedList<LinkedList<Integer>>();
        copyObjectWithMissingValueIndex = new ArrayList<>();

        Header m_Header = data.attributes();
        boolean containingMissing;
        for (int i = 0; i < data.noOfObjects(); i++) {
            DoubleData singleData = data.getDataObjects().get(i);
            containingMissing = false;
            LinkedList<Integer> missingAttribute = new LinkedList<>();
            for (int attribute = 0; attribute < m_Header.noOfAttr(); attribute++) {
                if (Double.isNaN(singleData.get(attribute))) {
                    containingMissing = true;
                    missingAttribute.add(attribute);
                }
            }
            if(containingMissing){
                objectWithMissingValueIndex.add(i);
                missingAttrib.add(missingAttribute);
                copyObjectWithMissingValueIndex.add(i);
                copyMissingAttrib.add(missingAttribute);
            }
        }
        setNOMissingAttrib(m_Header);
    }

    public LinkedList<LinkedList<Integer>> getMissingAttrib() {
        return missingAttrib;
    }
    public List<Integer> getIndexOfObjectWithMissing() {return objectWithMissingValueIndex;}
    public LinkedList<LinkedList<Integer>> getCopyMissingAttrib() {
        return copyMissingAttrib;
    }
    public List<Integer> getCopyIndexOfObjectWithMissing() {return copyObjectWithMissingValueIndex;}

    public List<DoubleData> getObjectWithMissing(DoubleDataTable data){
        List<DoubleData> objectWithMissing = new ArrayList<>();
        for(int index:objectWithMissingValueIndex){
            objectWithMissing.add(data.getDataObjects().get(index));
        }
            return objectWithMissing;
    }
    public List<DoubleData> getCopyObjectWithMissing(DoubleDataTable data){
        List<DoubleData> objectWithMissing = new ArrayList<>();
        for(int index:copyObjectWithMissingValueIndex){
            objectWithMissing.add(data.getDataObjects().get(index));
        }
        return objectWithMissing;
    }


    public boolean deleteObjectFromList(int index){
        if(index<objectWithMissingValueIndex.size()){
            objectWithMissingValueIndex.remove(index);
            for(int i =0; i<missingAttrib.get(index).size();i++){
                nOfMissAttrib[missingAttrib.get(index).get(i)]--;
            }
            missingAttrib.remove(index);
            return true;
        }else return false;
    }

    private void setNOMissingAttrib(Header head) {
        nOfMissAttrib = new int[head.noOfAttr()];
        Arrays.fill(nOfMissAttrib, 0);
            for (int i = 0; i < missingAttrib.size(); i++) {
                for (int j = 0; j < missingAttrib.get(i).size(); j++) {
                    nOfMissAttrib[missingAttrib.get(i).get(j)]++;
            }
        }
    }
    public int[] nOfMissingAttrib(){
        return nOfMissAttrib;
    }
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i<objectWithMissingValueIndex.size();i++){
            buf.append("Index of object " + objectWithMissingValueIndex.get(i) + " Attributes: " + missingAttrib.get(i) + "\n");
        }
        return buf.toString();
    }
}
