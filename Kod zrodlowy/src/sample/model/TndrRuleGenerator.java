package sample.model;

import rseslib.processing.rules.RuleGenerator;
import rseslib.structure.attribute.Header;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.*;
import rseslib.structure.rule.*;
import rseslib.structure.table.DoubleDataTable;
import rseslib.system.Configuration;
import rseslib.system.PropertyConfigurationException;
import rseslib.system.progress.Progress;

import java.util.*;
import java.util.BitSet;

/**
 * Created by DD on 2017-06-26.
 */
public class TndrRuleGenerator extends  Configuration implements RuleGenerator{

    public static final String MAX_NO_OF_DEC_VALUES_PROPERLY_NAME = "maxNumberOfDecValues";
    public static final String CONFIDENCE_PROPERLY_VALUE ="confidence";

    private int m_nMaxNumOfDecVal;
    private double m_nConfidence;

    private HashMap<Integer,Integer> dec_mapping;


    public TndrRuleGenerator(Properties prop) throws PropertyConfigurationException {
        super(prop);
        m_nConfidence = getDoubleProperty(CONFIDENCE_PROPERLY_VALUE);
        m_nMaxNumOfDecVal = getIntProperty(MAX_NO_OF_DEC_VALUES_PROPERLY_NAME);
    }

    public Collection<Rule> generate(DoubleDataTable tab, Progress prog) throws InterruptedException{
        HashSet<Rule> rules = new HashSet<>();
        Header m_Header = tab.attributes();
        BitSet reductFull = new BitSet(m_Header.noOfAttr()-1);
        for(int i =0; i<m_Header.noOfAttr()-1;i++){
            reductFull.set(i, true);
        }
        int iteracja = 0;

        NominalAttribute decAttr = tab.attributes().nominalDecisionAttribute();
        dec_mapping = new HashMap<Integer,Integer>();
        for (int i=0;i<decAttr.noOfValues();i++)
        {
            dec_mapping.put((int)decAttr.globalValueCode(i),i);
        }

        for (DoubleData object : tab.getDataObjects()) {
            iteracja++;
            BitSet reduct = new BitSet(m_Header.noOfAttr()-1);
            reduct.or(reductFull);
            EqualityDescriptorsRule ruleActual = generateFullRule(object, reduct);

            while (reduct.cardinality() > 1) {
                List<Rule> rulesFromOneObject = new ArrayList<>();
                List<String> descriCombi = bitSetCombList(reduct);


                for (String nextBSComb : descriCombi) {
                    BitSet bs = stringToBitSet(nextBSComb);
                    EqualityDescriptorsRule rule = new EqualityDescriptorsRule(bs, object);
                    if (rule.hasDescriptorWithMissingValue())
                        throw new NullPointerException("Found Object with missing values - " + "Object nr. " +iteracja+ " " +object);
                    rulesFromOneObject.add(rule);
                }

                //Statistics
                statisticsDecision(tab, rulesFromOneObject);
                //greedy choose teta (decyzja) if all rules Accuracy < alfa then -1
                int ruleWithHighNSupp = chooseGreedyDecValues(rulesFromOneObject);

                if (ruleWithHighNSupp != -1) {
                    if(getNSupp(((EqualityDescriptorsRule)rulesFromOneObject.get(ruleWithHighNSupp)).getDecisionVector())>=getNSupp(ruleActual.getDecisionVector())) {
                        ruleActual = (EqualityDescriptorsRule) rulesFromOneObject.get(ruleWithHighNSupp);
                        reduct = stringToBitSet(descriCombi.get(ruleWithHighNSupp));
                    }else{
                        rules.add(rulesFromOneObject.get(ruleWithHighNSupp));
                        break;
                    }
                } else {
                    rules.add(ruleActual);
                    break;
                }
                if(reduct.cardinality() == 1){
                    rules.add(ruleActual);
                }
            }
        }
        //prog.step();
        return rules;
    }


    private void statisticsDecision(DoubleDataTable table, List<Rule> rules){
        int dec_attr = table.attributes().decision();
        NominalAttribute decAttr = table.attributes().nominalDecisionAttribute();
        for (Rule rule : rules)
        {
            int support = 0;
            int[] decs = new int[decAttr.noOfValues()];
            Arrays.fill(decs,0);
            for (DoubleData object : table.getDataObjects())
            {
                if (rule.matches(object))
                {
                    support++;
                    int dec_idx=dec_mapping.get((int)object.get(dec_attr));
                    decs[dec_idx]++;
                }
            }
            ((AbstractDistrDecRuleWithStatistics)rule).setSupport(support);
            rseslib.structure.vector.Vector dv = new rseslib.structure.vector.Vector(decs.length);
            for (int i=0;i<decs.length;i++) dv.set(i,decs[i]);
            ((AbstractDistrDecRuleWithStatistics)rule).setDecisionVector(dv,decAttr);
            ((AbstractDistrDecRuleWithStatistics)rule).setAccuracy(1);
        }
    }

    private int chooseGreedyDecValues(List<Rule> rules){
        boolean existAnyMeetingConditions = false;
        for(Rule rule:rules){
            //sort teta
            rseslib.structure.vector.Vector decVector = ((EqualityDescriptorsRule)rule).getDecisionVector();
            List<Integer> decSortedByAmount= new ArrayList<>();
            for (int decision = 0; decision < decVector.dimension(); decision++) {
                if(decVector.get(decision)>0){
                    if(!decSortedByAmount.isEmpty()){
                        for(int i=0; i<decSortedByAmount.size();i++){
                            if(decVector.get(decision)>decVector.get(decSortedByAmount.get(i))){
                                decSortedByAmount.add(i,decision);
                                break;
                            }if(i+1== decSortedByAmount.size()) {
                                decSortedByAmount.add(i+1, decision);
                                break;
                            }
                        }
                    }else{
                        decSortedByAmount.add(decision);
                    }
                }
            }
            //choose teta
            List<Integer> choosedTeta= new ArrayList<>();
            int nOMatchedObjDec = 0;
            double confidence = 0.0;
            for(int decision = 0; decision< decSortedByAmount.size(); decision++){
                choosedTeta.add(decSortedByAmount.get(decision));
                nOMatchedObjDec+=decVector.get(decSortedByAmount.get(decision));
                confidence = nOMatchedObjDec/((EqualityDescriptorsRule) rule).getSupport();
                if(confidence>=m_nConfidence){
                    existAnyMeetingConditions = true;
                    ((EqualityDescriptorsRule) rule).setAccuracy(confidence);
                    Collections.sort(choosedTeta);
                    for(int i=0;i<decVector.dimension();i++){
                        boolean noExist= false;
                        for(int index: choosedTeta){
                            if(index==i){
                                noExist=true;
                                break;
                            }
                        }
                        if(!noExist){
                            decVector.set(i,0);
                        }
                    }
                    break;
                }
                if(decision+1==m_nMaxNumOfDecVal) {
                    ((EqualityDescriptorsRule) rule).setAccuracy(0);
                    break;
                }
            }
        }
        int ruleWithHighestNSupp = -1;
        if(existAnyMeetingConditions) {
            ruleWithHighestNSupp = findHighestNSupp(rules);
        }
        return ruleWithHighestNSupp;
    }

    private int findHighestNSupp(List<Rule> rules){
        double highestNSupp = 0;
        int whichRulehaveHighestNSupp = 0;
        for(int i =0; i<rules.size();i++) {
            if (((EqualityDescriptorsRule)rules.get(i)).getAccuracy() >=m_nConfidence) {
                rseslib.structure.vector.Vector decVector = ((EqualityDescriptorsRule) rules.get(i)).getDecisionVector();
                double nSupp = getNSupp(decVector);
                if (nSupp > highestNSupp) {
                    highestNSupp = nSupp;
                    whichRulehaveHighestNSupp = i;
                }

            }
        }
        return whichRulehaveHighestNSupp;
    }

    private List<String> bitSetCombList(BitSet bitSet) {
        List<String> bitSetComb = new ArrayList<>();
        BitSet bit = new BitSet(bitSet.length());
        bit.or(bitSet);
        int size = bitSet.length();
        int posActual = bit.nextSetBit(0);
        while (bit.length() > posActual) {
            if (bit.get(posActual)) {
                bit.flip(posActual);
                char[] tab = new char[size];
                int i = 0;
                while (i < size) {
                    if (bit.get(i)) {
                        tab[i] = '1';
                    } else {
                        tab[i] = '0';
                    }
                    i++;
                }
                //System.out.println(bit);
                String lol = String.valueOf(tab);
                bitSetComb.add(lol);
                bit.flip(posActual);
            }
            posActual++;
        }
        //System.out.println(bitSetComb.toString());
        return bitSetComb;
    }
    private EqualityDescriptorsRule generateFullRule(DoubleData object, BitSet bit){
        EqualityDescriptorsRule rule = new EqualityDescriptorsRule(bit, object);
        int dec_attr = object.attributes().decision();
        NominalAttribute decAttr = object.attributes().nominalDecisionAttribute();
        HashMap<Integer,Integer> dec_mapping = new HashMap<Integer,Integer>();
        for (int i=0;i<decAttr.noOfValues();i++)
        {
            dec_mapping.put((int)decAttr.globalValueCode(i),i);
        }
        int[] decs = new int[decAttr.noOfValues()];
        Arrays.fill(decs,0);
        int dec_idx=dec_mapping.get((int)object.get(dec_attr));
        decs[dec_idx]++;
        rule.setSupport(1);
        rseslib.structure.vector.Vector dv = new rseslib.structure.vector.Vector(decs.length);
        for (int i=0;i<decs.length;i++) dv.set(i,decs[i]);
        rule.setDecisionVector(dv,decAttr);
        rule.setSupport(1);
        rule.setAccuracy(1);
        return rule ;
    }

    private BitSet stringToBitSet(String bitSet){
        int size = bitSet.length();
        BitSet newBitSet = new BitSet(size);
        for(int i = 0;i<size;i++){
            if(bitSet.charAt(i)=='1'){
                newBitSet.set(i);
            }
        }
        return newBitSet;
    }

    public static Double getNSupp(rseslib.structure.vector.Vector decVector){
        double nOMatches = 0.0;
        for(int j=0;j<decVector.dimension();j++){
            nOMatches+=decVector.get(j);
        }
        nOMatches=nOMatches/Math.sqrt(decVector.rank());
        return nOMatches;

    }
}
