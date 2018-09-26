package org.aksw.fox.nerlearner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;

import org.aksw.fox.data.Token;
import org.aksw.fox.data.Entity;
import org.aksw.fox.data.EntityClassMap;
import org.aksw.fox.utils.FoxTextUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import opennlp.tools.util.HashList;
import weka.core.Instances;

/**
 * 
 * 
 * @author rspeck
 * 
 */
public class PostProcessing implements IPostProcessing {

    public static Logger               LOG          = LogManager.getLogger(PostProcessing.class);

    protected Map<String, Set<Entity>> toolResults  = null;
    protected TokenManager             tokenManager = null;

    /**
     * 
     * PostProcessing.
     * 
     * @param input
     *            sentences
     * @param toolResults
     *            tool name to result set
     */
    public PostProcessing(TokenManager tokenManager, Map<String, Set<Entity>> toolResults) {
        LOG.info("PostProcessing ...");
        this.tokenManager = tokenManager;

        // check entities and try to repair entities
        for (Set<Entity> entites : toolResults.values())
            tokenManager.repairEntities(entites);

        this.toolResults = toolResults;

        if (LOG.isDebugEnabled())
            LOG.debug(toolResults);
    }

    /**
     * 
     * @return
     */
    @Override
    public Set<String> getLabeledInput() {
        return tokenManager.getLabeledInput();
    }

    /**
     * Label oracle.
     * 
     * @param map
     * @return
     */
    @Override
    public Map<String, String> getLabeledMap(Map<String, String> map) {
        Map<String, String> rtn = new HashMap<>();

        // 1. label MWU
        List<Entry<String, String>> tokenEntities = new ArrayList<>();
        for (Entry<String, String> mapEntry : map.entrySet()) {
            if (mapEntry.getKey().contains(" ")) {
                if (LOG.isDebugEnabled())
                    LOG.debug(rtn);
                rtn = labeledEntry(mapEntry, rtn);
                if (LOG.isDebugEnabled())
                    LOG.debug(rtn);
            } else {
                tokenEntities.add(mapEntry);
            }
        }

        // 2. remember used labels of MWU
        Set<String> usedLabels = new HashSet<>();
        for (String label : rtn.keySet()) {
            if (LOG.isDebugEnabled())
                LOG.debug(label);
            Collections.addAll(usedLabels, FoxTextUtil.getSentencesToken(label));
            if (LOG.isDebugEnabled())
                LOG.debug(usedLabels);

        }

        // 3. label token (non MWU)
        for (Entry<String, String> mapEntry : tokenEntities) {
            if (LOG.isDebugEnabled())
                LOG.debug(rtn);
            rtn = labeledEntry(mapEntry, rtn);
            if (LOG.isDebugEnabled())
                LOG.debug(rtn);
        }

        // 4. remove labels used in mwu
        List<String> remove = new ArrayList<>();
        for (String labeledtoken : rtn.keySet()) {
            if (usedLabels.contains(labeledtoken)) {
                remove.add(labeledtoken);
            }
        }
        if (LOG.isDebugEnabled())
            LOG.debug(remove);
        for (String r : remove)
            rtn.remove(r);

        return rtn;
    }

    /**
     * 
     * @return
     */
    @Override
    public Map<String, Set<Entity>> getLabeledToolResults() {
        Map<String, Set<Entity>> rtn = new HashMap<>();

        if (LOG.isDebugEnabled())
            LOG.debug(toolResults);


        // for each tool
        for (Entry<String, Set<Entity>> entry : toolResults.entrySet()) {

            // entities to map
            Map<String, String> resutlsMap = new HashMap<>(); 
            for (Entity entity : entry.getValue()){
                //resutlsMap.put(entity.getFullText(),entity.getCategory());
               for (Token token : entity.getWords()){
                    resutlsMap.put(token.getText(), token.getType());
                }
            } 

            if (LOG.isTraceEnabled())
                LOG.trace(resutlsMap);
            
            // label map
            resutlsMap = getLabeledMap(resutlsMap);
            if (LOG.isTraceEnabled())
                LOG.trace(resutlsMap);

            // label to entity
            Set<Entity> labeledEntities = new HashSet<>();
            for (Entry<String, String> e : resutlsMap.entrySet()) {
                labeledEntities.add(
                        new Entity(e.getKey(), e.getValue(), Entity.DEFAULT_RELEVANCE, entry.getKey())
                        );
            }

            // add to labeled result map
            String toolName = entry.getKey();
            rtn.put(toolName, labeledEntities);

        }
        return rtn;
    }

    @Override
    public Map<String, Set<Entity>> getToolResults() {
        return toolResults;
    }

     /**
     * Clean the data. We can't have B-types after I-types wthout punctuation or I-types at the beginning
     */ 
    public void cleanData(Map<String,String> labeledEntityToken, Map<String, String> labeledInput){
        // get words of the input
        String input = tokenManager.getInput();

        //set the input map
        Map<String,String> wordsOfInput = new LinkedHashMap<>();
        wordsOfInput.putAll(labeledInput);

        //save previous classification
        Map<String, String> previousLabels = new LinkedHashMap<>();
        previousLabels.putAll(labeledEntityToken);
        labeledEntityToken.clear();
        

        LOG.info("CLEANING \""+input+"\"");

        //Start analysing the each sentence
        ArrayList<String> keysOfWordsOfInput = new ArrayList<>(wordsOfInput.keySet());
        for (int i=0;i<keysOfWordsOfInput.size();i++){
            //current data
            String label = keysOfWordsOfInput.get(i); //to be saved on the Map
            String token = label.split(TokenManager.SEP)[0]; //the word itself
            int index = Integer.parseInt(label.split(TokenManager.SEP)[1]); //the position of the word at the input
            String type = wordsOfInput.get(label); //those at the entityClasses
            String tag=type.substring(0,1); //B I or O
            String category; //LOCATION, PERSON, ORGANIZATION or OUTSIDE
            boolean isProperName = (token.substring(0,1).equals(token.substring(0,1).toUpperCase()) && !StringUtils.isNumeric(token));
            if (type.contains("OUTSIDE")){
                category=type;
            } else {
                category = type.substring(1);
            }

            //next data
            String nextType = "";
            String nextTag="";
            String nextCategory = "";
            String nextLabel = "";
            String nextToken = "";
            int nextIndex = 0;
            boolean isNextProperName = false;

            //next data will be changed if it isn't repeating this iteration after some change
            if(i+1<keysOfWordsOfInput.size()){
                nextLabel = keysOfWordsOfInput.get(i+1);
                nextToken = nextLabel.split(TokenManager.SEP)[0];
                nextIndex = Integer.parseInt(nextLabel.split(TokenManager.SEP)[1]);
                nextType = wordsOfInput.get(nextLabel);
                nextTag=nextType.substring(0,1);
                isNextProperName=(nextToken.substring(0,1).equals(nextToken.substring(0,1).toUpperCase()) && !StringUtils.isNumeric(nextToken));
                if(nextType.contains("OUTSIDE"))
                    nextCategory=nextType;
                else
                    nextCategory = nextType.substring(1);

            } else {
                nextLabel = "";
                nextType="OUTSIDE";
                nextToken = "";
                nextTag = "O";
                nextCategory = "OUTSIDE";
                nextIndex = 0;
                isNextProperName=false;
            }

            //in case we have a string not made by letters
            if(!token.equals("")){
                if(!Character.isLetter(token.charAt(0))){
                    //LOG.info("The token \""+token+"\" is not made by letters");
                    type="OUTSIDE";
                    category="OUTSIDE";
                    tag="O";
                    label="";
                    token="punctuation";
                    isProperName=false;
                }
            }
            if(!nextToken.equals("")){
                if(!Character.isLetter(nextToken.charAt(0))){
                    //LOG.info("The token \""+nextToken+"\" is not made by letters");
                    nextType="OUTSIDE";
                    nextCategory="OUTSIDE";
                    nextTag="O";
                    nextLabel=" ";
                    nextToken="punctuation";
                    isNextProperName=false;
                }
            }


            //in case we have a punctuation
            if(index!=nextIndex-token.length()-1){
                //LOG.info("We have a punctuation between \""+token+"\" and \""+nextToken+"\"");
                type="OUTSIDE";
                category="OUTSIDE";
                tag="O";
                label="";
                token="punctuation";
                isProperName=false;
            }
            //if the whole word is in upper letters, we cannot evaluate the proper name
            if(token.equals(token.toUpperCase())){
                isProperName=false;
                //LOG.info("MAIUSCULAS "+token);
            }
            if(token.equals(nextToken.toUpperCase())){
                isNextProperName=false;
                //LOG.info("MAIUSCULAS "+nextToken);

            }

            //clean the log
            String badwords[] = {"When","During","Later","While","As","An","At","This","In","President","Minister","All","After","One","Professor","The","She","He","It","By","Although","Though","From","Since","These","Those","That","On","I","Am","They","His","Her","Their","You","URL"};
            
            //CLEANING PART
            if(tag.contains("B") && nextTag.contains("O")){
                //change B
                labeledEntityToken.put(label,type);

                //change O
                if (isNextProperName){
                    final String correctType = "I"+category;
                    LOG.info("MUDANÇA ERROR Token \""+nextToken+"\" has no type. Correcting to type \""+correctType+"\" at the sentence: "+input);
                    labeledEntityToken.put(nextLabel,correctType);
                    wordsOfInput.put(nextLabel,correctType);
                    if(i-2>=0)
                        i=i-2;
                    else   
                        if(i-1>=0)
                            i=i-1;   
                    continue;
                } else {
                    //do nothing
                } 
            }
            if(tag.contains("B") && nextTag.contains("I")){
                //change B
                if (nextCategory.contains("ORGANIZATION") && !category.contains("ORGANIZATION")){
                    final String correctType = "B"+nextCategory;
                    LOG.info("MUDANÇA Token \""+token+"\" probably belongs to an Organization Name. Correcting to type \""+correctType+"\" at the sentence: "+input);
                    labeledEntityToken.put(label,correctType);
                    wordsOfInput.put(label,correctType);
                    if(i-2>=0)
                        i=i-2;
                    else   
                        if(i-1>=0)
                            i=i-1; 
                    continue;
                } else {
                    labeledEntityToken.put(label,type);
                }

                //change I
                if(!category.contains(nextCategory)){
                    final String correctType = "I"+category;
                    LOG.info("MUDANÇA Tokens \""+token+"\" and \""+nextToken+"\" are with different types. Correcting \""+nextToken+"\" from \""+nextType+"\" to \""+correctType+"\" at the sentence: "+input);
                    labeledEntityToken.put(nextLabel, correctType);
                    wordsOfInput.put(nextLabel,correctType); 
                    if(i-2>=0)
                        i=i-2;
                    else   
                        if(i-1>=0)
                            i=i-1; 
                    continue;
                } else {
                    labeledEntityToken.put(nextLabel,nextType);
                }    
            }
            if(tag.contains("B") && nextTag.contains("B")){
                //change B
                if(!category.contains(nextCategory)){
                    if (nextCategory.contains("ORGANIZATION") || category.contains("ORGANIZATION")){
                        final String correctType1 = "BORGANIZATION";
                        final String correctType2 = "IORGANIZATION";
                        LOG.info("MUDANÇA Tokens \""+token+"\" and \""+ nextToken+"\" probably belongs to an Organization Name. Correcting both types to \""+correctType1+"\" and \""+correctType2+"\" at the sentence: "+input);
                        labeledEntityToken.put(label,correctType1);
                        wordsOfInput.put(label,correctType1);
                        labeledEntityToken.put(nextLabel,correctType2);
                        wordsOfInput.put(nextLabel,correctType2);
                        if(i-2>=0)
                            i=i-2;
                        else   
                            if(i-1>=0)
                                i=i-1; 
                        continue;
                    } else {
                        LOG.info("MISSING categorias diferentes entre dois Bs na frase "+input);
                        labeledEntityToken.put(label,type);
                    }
                } else {
                    final String correctType1="B"+category;
                    final String correctType2="I"+category;
                    LOG.info("Tokens \""+token+"\" and \""+nextToken+"\" has both B types "+category+". Correcting to B and I.");
                    labeledEntityToken.put(label,correctType1);
                    labeledEntityToken.put(nextLabel,correctType2);
                    wordsOfInput.put(label,correctType1);
                    wordsOfInput.put(nextLabel,correctType2);
                    if(i-2>=0)
                        i=i-2;
                    else   
                        if(i-1>=0)
                            i=i-1; 
                    continue;
                }
                
            }
            if(tag.contains("I") && nextTag.contains("O")){
                //change I
                labeledEntityToken.put(label,type);

                //change O
                if(isNextProperName){
                    boolean badword = false;
                    for(int x=0;x<badwords.length;x++){
                        if(token.contains(badwords[x]))
                            badword=true;
                    }
                    if(!badword)    
                        LOG.info("MISSING token: "+nextToken+" sem categoria após \""+token+"\" na frase "+input);

                }
                
            }
            if(tag.contains("I") && nextTag.contains("I")){
                //change I
                if(!category.contains(nextCategory)){
                    if (nextCategory.contains("ORGANIZATION") || category.contains("ORGANIZATION")){
                        final String correctType = "IORGANIZATION";
                        LOG.info("MUDANÇA Tokens \""+token+"\" and \""+ nextToken+"\" probably belongs to an Organization Name. Correcting both types to \""+correctType+"\". at the sentence: "+input);
                        labeledEntityToken.put(label,correctType);
                        wordsOfInput.put(label,correctType);
                        labeledEntityToken.put(nextLabel,correctType);
                        wordsOfInput.put(nextLabel,correctType);
                        if(i-2>=0)
                            i=i-2;
                        else   
                            if(i-1>=0)
                            i   =i-1; 
                        continue;
                    } else {
                        LOG.info("MISSING categorias diferentes entre dois Is na frase "+input);
                        labeledEntityToken.put(label,type);
                    }
                } else {
                    labeledEntityToken.put(label,type);
                    labeledEntityToken.put(nextLabel,nextType);
                }
                
            }
            if(tag.contains("I") && nextTag.contains("B")){
                //change I
                labeledEntityToken.put(label,type);
                
                //change B
                final String correctType="I"+category;
                LOG.info("MUDANÇA Token \""+nextToken+"\" has wrong type "+nextType+". Correcting to "+correctType+" at the sentence: "+input);
                labeledEntityToken.put(nextLabel,correctType);
                wordsOfInput.put(nextLabel,correctType);
                if(i-2>=0)
                    i=i-2;
                else   
                if(i-1>=0)
                    i=i-1; 
                continue;                
            }
            if(tag.contains("O") && nextTag.contains("O")){
                //change O
                if(isProperName){
                    boolean badword = false;
                    for(int x=0;x<badwords.length;x++){
                        if(token.contains(badwords[x]))
                            badword=true;
                    }
                    if(!badword)    
                    LOG.info("MISSING Token \""+token+"\" has no type at the sentence: "+input);     
                }
                if(isNextProperName){
                    boolean badword = false;
                    for(int x=0;x<badwords.length;x++){
                        if(nextToken.contains(badwords[x]))
                            badword=true;
                    }
                    if(!badword)    
                    LOG.info("MISSING Token \""+nextToken+"\" has no type at the sentence: "+input);     
                }     
            }
            if(tag.contains("O") && nextTag.contains("I")){
                //change O
                if(isProperName){
                    final String correctType="B"+nextCategory;
                    LOG.info("MUDANÇA Token \""+token+"\" has no type. Correcting to "+correctType+" at the sentence: "+input);
                    labeledEntityToken.put(label,correctType);
                    wordsOfInput.put(label,correctType);
                    if(i-2>=0)
                        i=i-2;
                    else   
                        if(i-1>=0)
                            i=i-1; 
                    continue;
                }else{
                    //do nothing
                }

                //change I
                if(isNextProperName){
                    final String correctType="B"+nextCategory;
                    LOG.info("Token \""+nextToken+"\" is starting with an I. Correcting to \""+correctType+"\"");
                    labeledEntityToken.put(nextLabel,correctType);
                    wordsOfInput.put(nextLabel,correctType);
                    if(i-2>=0)
                        i=i-2;
                    else   
                        if(i-1>=0)
                            i=i-1; 
                    continue;
                } else {
                    LOG.info("MUDANÇA Token \""+nextToken+"\" is starting with an I and its not a proper name. Removing it... at the sentence: "+input);
                    wordsOfInput.put(nextLabel,"OUTSIDE");
                }
                
            }
            if(tag.contains("O") && nextTag.contains("B")){
                //change O

                //change B
                labeledEntityToken.put(nextLabel,nextType);
                
            }
        }
        LOG.info("Inicialmente: "+previousLabels);
        LOG.info("Depois: "+labeledEntityToken);
    }

    /**
     * Labeled instances to final entities.
     */
    @Override
    public Set<Entity> instancesToEntities(Instances instances) {
        LOG.info("instancesToEntities ...");

        // get token in input order
        Set<String> labeledToken = getLabeledInput();

        // check size
        if (labeledToken.size() != instances.numInstances())
            LOG.error("\ntoken size != instance data");

        // fill labeledEntityToken and wordsOfInput
        Map<String, String> labeledEntityToken = new LinkedHashMap<>();
        Map<String, String> wordsOfInput = new LinkedHashMap<>();
        int i = 0;
        for (String label : labeledToken) {
            String category = instances.classAttribute().value(Double.valueOf(instances.instance(i).classValue()).intValue());
            // logger.info(category);
            if (EntityClassMap.entityClasses.contains(category) && category != EntityClassMap.getNullCategory()) {
                labeledEntityToken.put(label, category);
                wordsOfInput.put(label, category);
            } else {
                if (EntityClassMap.entityClasses.contains(category) && category == EntityClassMap.getNullCategory()) {
                    wordsOfInput.put(label, category);
                }
            }
            i++;
        }

        //clean data
        cleanData(labeledEntityToken, wordsOfInput);


        // labeledEntityToken to mwu
        List<Entity> results = new ArrayList<>();

        String previousLabel = "";
        for (Entry<String, String> entry : labeledEntityToken.entrySet()) {

            String label = entry.getKey();
            String category = entry.getValue();
            String categoryIO;
            if (category != EntityClassMap.getNullCategory()){
                categoryIO=category.substring(1);
            } else {
                categoryIO=category;
            }

            String token = tokenManager.getToken(tokenManager.getLabelIndex(label));
            int labelIndex = tokenManager.getLabelIndex(label);

            // test previous index
            boolean testIndex = false;
            if (results.size() > 0) {
                int previousIndex = tokenManager.getLabelIndex(previousLabel);
                int previousTokenLen = tokenManager.getToken(previousIndex).length();

                testIndex = labelIndex == previousIndex + previousTokenLen + " ".length();
            }

            // check previous index and entity category
            //TODO: não deveria verificar se vem B entre eles?
            if (testIndex && results.get(results.size() - 1).getCategory().equals(categoryIO)) {
                results.get(results.size() - 1).addText(token);
            } else {
                int index = -1;
                try {
                    index = Integer.valueOf(label.split(TokenManager.SEP)[1]);
                } catch (Exception e) {
                    LOG.error("\n label: " + label, e);
                }
                if (index > -1) {
                    Entity entity = new Entity(token, categoryIO, Entity.DEFAULT_RELEVANCE, "fox");
                    entity.addIndicies(index);
                    results.add(entity);
                }
            }

            // remember last label
            previousLabel = label;
        }
        if (LOG.isDebugEnabled())
            LOG.debug("result: " + results.toString());
        LOG.info("result size: " + results.size());

        // result set
        // Set<Entity> set = new HashSet<>(results);
        Set<Entity> set = new HashSet<>(results);
        for (Entity e : set)
            for (Entity ee : results) {
                // word and class equals?
                // merge indices
                if (e.getFullText().equals(ee.getFullText()) && e.getCategory().equals(ee.getCategory())) {
                    e.addAllIndicies(ee.getIndices());
                } else if (e.getFullText().equals(ee.getFullText()) && !e.getCategory().equals(ee.getCategory())) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("one word 2 classes: " + e.getFullText());
                }

            }
        if (LOG.isDebugEnabled())
            LOG.debug("set: " + set.toString());
        LOG.info("result set size: " + set.size());
        return set;
    }

    // label an entity
    protected Map<String, String> labeledEntry(Entry<String, String> entity, Map<String, String> labeledMap) {

        // token of an entity
        String[] entityToken = FoxTextUtil.getToken(entity.getKey());
        if (LOG.isDebugEnabled()) {
            LOG.debug(entity);
            LOG.debug(Arrays.asList(entityToken));
        }

        // all entity occurrence
        Set<Integer> occurrence = FoxTextUtil.getIndices(entity.getKey(), tokenManager.getTokenInput());
        if (occurrence.size() == 0) {
            LOG.error("entity not found:" + entity.getKey());
        } else {
            for (Integer index : occurrence) {
                // for each occurrence token length
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < entityToken.length; i++) {

                    String label = tokenManager.getLabel(index);
                    if (LOG.isDebugEnabled())
                        LOG.debug(label);
                    if (label != null) {
                        sb.append(label);
                        if (entityToken[entityToken.length - 1] != label)
                            sb.append(" ");
                    }
                    index += entityToken[i].length() + 1;
                }
                // add labeled entity
                labeledMap.put(sb.toString().trim(), entity.getValue());
            }
        }
        if (LOG.isDebugEnabled())
            LOG.debug(labeledMap);
        return labeledMap;
    }
}
