package sample.model;

import rseslib.processing.classification.Classifier;
import rseslib.processing.classification.ClassifierWithDistributedDecision;
import rseslib.processing.rules.*;
import rseslib.structure.attribute.NominalAttribute;
import rseslib.structure.data.DoubleData;
import rseslib.structure.rule.DistributedDecisionRule;
import rseslib.structure.rule.Rule;
import rseslib.structure.table.DoubleDataTable;
import rseslib.structure.vector.Vector;
import rseslib.system.ConfigurationWithStatistics;
import rseslib.system.PropertyConfigurationException;
import rseslib.system.progress.Progress;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Properties;

public class NonDeterClassifier extends ConfigurationWithStatistics implements ClassifierWithDistributedDecision, Classifier {
    public enum GeneratorType {LocalReducts, GlobalReducts, JohnsonReducts, Accurate, Covering}

    public static final String s_sRuleGenerator = "ruleGenerator";

    GeneratorType m_nRuleGeneratorMethod;
    Collection<Rule> m_cDecisionRules = null;
    Collection<Rule> m_cNDDecisionRules = null;
    NominalAttribute m_DecAttr;

    public NonDeterClassifier(Properties prop, DoubleDataTable table, Progress prog) throws PropertyConfigurationException, InterruptedException {
        super(prop);
        setRuleGeneratorMethod(getProperty(s_sRuleGenerator));
        m_DecAttr = table.attributes().nominalDecisionAttribute();
        RuleGenerator rulgen = null;
        TndrRuleGenerator test = new TndrRuleGenerator(getProperties());
        switch (m_nRuleGeneratorMethod) {
            case GlobalReducts:
                System.out.println("Using global reducts");
                rulgen = new GlobalReductsRuleGenerator(getProperties());
                break;
            case LocalReducts:
                System.out.println("Using local reducts");
                rulgen = new LocalReductsRuleGenerator(getProperties());
                break;
            case JohnsonReducts:
                System.out.println("Using Johnons's reducts");
                rulgen = new JohnsonReductsRuleGenerator(getProperties());
                break;
            case Accurate:
                System.out.println("Using AQ15OneRule generator");
                rulgen = new AccurateRuleGenerator(getProperties());
                break;
            case Covering:
                System.out.println("Using CoveringRule generator");
                rulgen = new CoveringRuleGenerator(getProperties());
                break;
        }
        System.out.println("Generate deter rules");
        m_cDecisionRules = rulgen.generate(table, prog);
        System.out.println("Generate Non deter rules");
        m_cNDDecisionRules = test.generate(table, prog);
        System.out.println("Ended gererating both set of rules");
//        for (Rule r : m_cDecisionRules) {
//            System.out.print(r);
//        }
//        System.out.println("\n " + m_cNDDecisionRules.size());
//        System.out.println("+++++++++++++++++++++++++++");
//        for (Rule r : m_cNDDecisionRules) {
//            System.out.print(r + " rank" + ((EqualityDescriptorsRule) r).getDecisionVector().rank());
//        }

    }

    public NonDeterClassifier(Properties prop, DoubleDataTable table, Collection<Rule> rules, Progress prog) throws PropertyConfigurationException, InterruptedException {
        super(prop);
        m_DecAttr = table.attributes().nominalDecisionAttribute();
        TndrRuleGenerator test = new TndrRuleGenerator(getProperties());
        m_cNDDecisionRules = test.generate(table, prog);
        m_cDecisionRules.addAll(rules);
    }

    private void setRuleGeneratorMethod(String method) throws PropertyConfigurationException {
        System.out.println("Found " + method + "Rule Generator");
        try {
            m_nRuleGeneratorMethod = GeneratorType.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw new PropertyConfigurationException("Unknown rule generator type: " + method);
        }
    }

    public double classify(DoubleData dObj) throws PropertyConfigurationException {
        DistributedDecisionRule nDRuleWithHighNSupp= null;
        int indxOfHighestSupp = 0;
        int highestSupp = 0;
        double highestNSupp = 0;
        int rankdv = 0;
        boolean foundDR = false,  foundNDR = false;
        int[] decs = new int[m_DecAttr.noOfValues()];
        Arrays.fill(decs, 0);


        HashMap<Integer, Integer> dec_mapping = new HashMap<Integer, Integer>();
        for (int i = 0; i < m_DecAttr.noOfValues(); i++) {
            dec_mapping.put((int) m_DecAttr.globalValueCode(i), i);
        }
        for (Rule rule : m_cDecisionRules) {
            if (rule.matches(dObj)) {
                foundDR = true;
                int dec_idx = dec_mapping.get((int) rule.getDecision());
                decs[dec_idx]++;
            }
        }
        if(foundDR){
            for (int i = 0; i < decs.length; i++) {
                if (decs[i] > highestSupp) {
                    highestSupp = decs[i];
                    indxOfHighestSupp = i;
                }
            }
        }



        for (Rule ruleOne : m_cNDDecisionRules) {
            DistributedDecisionRule rule = (DistributedDecisionRule)ruleOne;
            if (rule.matches(dObj)) {
                foundNDR = true;
                if (nDRuleWithHighNSupp == null) {
                    nDRuleWithHighNSupp = rule;
                    highestNSupp = TndrRuleGenerator.getNSupp(rule.getDecisionVector());
                    rankdv = rule.getDecisionVector().rank();
                } else {
                    if (TndrRuleGenerator.getNSupp(rule.getDecisionVector()) > highestNSupp ||
                            (  TndrRuleGenerator.getNSupp(rule.getDecisionVector()) == highestNSupp && rule.getDecisionVector().rank() < rankdv  )) {
                        nDRuleWithHighNSupp = rule;
                        highestNSupp = TndrRuleGenerator.getNSupp(rule.getDecisionVector());
                        rankdv = rule.getDecisionVector().rank();
                    }
                }
            }
        }


        if(foundDR && foundNDR){
            if(((int)nDRuleWithHighNSupp.getDecisionVector().get(indxOfHighestSupp) != 0) || (highestSupp>highestNSupp)){
                return m_DecAttr.globalValueCode(indxOfHighestSupp);
            }else return nDRuleWithHighNSupp.getDecision();
        }else if(foundDR){
            return m_DecAttr.globalValueCode(indxOfHighestSupp);
        }else if(foundNDR){
            return nDRuleWithHighNSupp.getDecision();
        }
        return Double.NaN;

    }

    public double[] classifyWithDistributedDecision(DoubleData dObj) throws PropertyConfigurationException {
        double[] decDistr = new double[m_DecAttr.noOfValues()];
        Vector highestNSuppDV = new Vector(m_DecAttr.noOfValues());
        double highestNSupp = 0;
        int rankdv = 0;
        for (Rule ruleOne : m_cNDDecisionRules) {
            DistributedDecisionRule rule = (DistributedDecisionRule)ruleOne;
            if (rule.matches(dObj)) {

                if (TndrRuleGenerator.getNSupp(rule.getDecisionVector()) > highestNSupp ||
                        (TndrRuleGenerator.getNSupp(rule.getDecisionVector()) == highestNSupp && rule.getDecisionVector().rank() > rankdv)) {
                    highestNSuppDV = rule.getDecisionVector();
                    highestNSupp = TndrRuleGenerator.getNSupp(rule.getDecisionVector());
                    rankdv = highestNSuppDV.rank();
                }
            }
        }
        for (int i = 0; i < highestNSuppDV.dimension(); i++) {
            decDistr[i] = highestNSuppDV.get(i);
        }
        return decDistr;
    }

    public void calculateStatistics() {
        addToStatistics("number_of__deterministics_rules",Integer.toString(m_cDecisionRules.size()));
        addToStatistics("number_of___non_deterministics_rules",Integer.toString(m_cNDDecisionRules.size()));
    }

    public void resetStatistics() {

    }

    public Collection<Rule> getDecisionRules() {
        return m_cDecisionRules;
    }

    public Collection<Rule> getNDDecisionRules() {
        return m_cNDDecisionRules;
    }
}
