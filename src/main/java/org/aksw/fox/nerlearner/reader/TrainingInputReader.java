package org.aksw.fox.nerlearner.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.aksw.fox.data.Entity;
import org.aksw.fox.data.Token;
import org.aksw.fox.data.EntityClassMap;
import org.aksw.fox.nerlearner.TokenManager;
import org.aksw.fox.utils.FoxTextUtil;

/**
 * reads input training data
 *
 * @author rspeck
 *
 */
public class TrainingInputReader extends ANERReader {

  /**
   *
   */
  public static void main(final String[] aa) throws Exception {

    final List<String> files = new ArrayList<>();
    final File file = new File("input/4");
    if (!file.exists()) {
      throw new IOException("Can't find file or directory.");
    } else {
      if (file.isDirectory()) {
        // read all files in a directory
        for (final File fileEntry : file.listFiles()) {
          if (fileEntry.isFile() && !fileEntry.isHidden()) {
            files.add(fileEntry.getAbsolutePath());
          }
        }
      } else if (file.isFile()) {
        files.add(file.getAbsolutePath());
      } else {
        throw new IOException("Input isn't a valid file or directory.");
      }
    }

    final String[] a = files.toArray(new String[files.size()]);

    final INERReader trainingInputReader = new TrainingInputReader(a);
    ANERReader.LOG.info("input: ");
    ANERReader.LOG.info(trainingInputReader.getInput());
    ANERReader.LOG.info("oracle: ");
    for (final Entry<String, String> e : trainingInputReader.getEntities().entrySet()) {
      ANERReader.LOG.info(e.getValue() + "-" + e.getKey());
    }
  }

  protected StringBuffer taggedInput = new StringBuffer();
  protected String input = "";
  protected HashMap<String, String> entities = new HashMap<>();

  /**
   * http://www-nlpir.nist.gov/related_projects/muc/proceedings/ne_task.html
   *
   * @param inputPaths
   * @throws IOException
   */

  /**
   * Constructor for loading class.
   */
  public TrainingInputReader() {}

  public TrainingInputReader(final String[] inputPaths) throws IOException {
    super(inputPaths);
  }

  public void initFiles(final String folder) throws IOException {
    final List<String> files = new ArrayList<>();

    final File file = new File(folder);
    if (!file.exists()) {
      throw new IOException("Can't find directory.");
    } else {
      if (file.isDirectory()) {
        // read all files in a directory
        for (final File fileEntry : file.listFiles()) {
          if (fileEntry.isFile() && !fileEntry.isHidden()) {
            files.add(fileEntry.getAbsolutePath());
          }
        }
      } else {
        throw new IOException("Input isn't a valid directory.");
      }
    }

    initFiles(files.toArray(new String[files.size()]));
  }

  @Override
  public void initFiles(final String[] initFiles) throws IOException {
    super.initFiles(initFiles);

    readInputFromFiles();
    parse();
  }

  /**
   *
   * @return
   * @throws IOException
   */
  @Override
  public String getInput() {
    return input;
  }

  @Override
  public HashMap<String, String> getEntities() {
    {
      // remove oracle entities aren't in input
      final Set<Entity> set = new HashSet<>();

      for (final Entry<String, String> oracleEntry : entities.entrySet()) {
        set.add(new Entity(oracleEntry.getKey(), oracleEntry.getValue()));
      }

      // repair entities (use fox token)
      new TokenManager(input).repairEntities(set);

      // use
      entities.clear();
      /*
      for (final Entity e : set) {
        entities.put(e.getFullText(), e.getCategory());
      }
      */
      for (final Entity e : set) {
        for (final Token a : e.getWords()){
          entities.put(a.getText(),a.getType());
        }
      }

    }

    {
      // INFO
      LOG.info("oracle cleaned size: " + entities.size());
      int bl = 0, bo = 0, bp = 0, il = 0, io = 0, ip = 0;
      for (final Entry<String, String> e : entities.entrySet()) {
        if (e.getValue().equals(EntityClassMap.B_L)) {
          bl++;
        }
        if (e.getValue().equals(EntityClassMap.B_O)) {
          bo++;
        }
        if (e.getValue().equals(EntityClassMap.B_P)) {
          bp++;
        }
        if (e.getValue().equals(EntityClassMap.I_L)) {
          il++;
        }
        if (e.getValue().equals(EntityClassMap.I_O)) {
          io++;
        }
        if (e.getValue().equals(EntityClassMap.I_P)) {
          ip++;
        }
      }
      LOG.info("oracle :");      
      LOG.info(bl + " B-LOCs found");
      LOG.info(bo + " B-ORGs found");
      LOG.info(bp + " B-PERs found");
      LOG.info(il + " I-LOCs found");
      LOG.info(io + " I-ORGs found");
      LOG.info(ip + " I-PERs found");
      bl = 0;
      bo = 0;
      bp = 0;
      il = 0;
      io = 0;
      ip = 0;
      for (final Entry<String, String> e : entities.entrySet()) {
        if (e.getValue().equals(EntityClassMap.B_L)) {
          bl++;
        }
        if (e.getValue().equals(EntityClassMap.B_O)) {
          bo++;
        }
        if (e.getValue().equals(EntityClassMap.B_P)) {
          bp++;
        }
        if (e.getValue().equals(EntityClassMap.I_L)) {
          il++;
        }
        if (e.getValue().equals(EntityClassMap.I_O)) {
          io++;
        }
        if (e.getValue().equals(EntityClassMap.I_P)) {
          ip++;
        }
      }
      LOG.info("oracle (token):");
      LOG.info(bl + " B-LOCs found");
      LOG.info(bo + " B-ORGs found");
      LOG.info(bp + " B-PERs found");
      LOG.info(il + " I-LOCs found");
      LOG.info(io + " I-ORGs found");
      LOG.info(ip + " I-PERs found");
      LOG.info(bl + bo + bp + il + io + ip + " total found");
    }

    return entities;
  }

  /**
   * Reads PREAMBLE or TEXT tag content to taggedInput.
   *
   **/
  protected void readInputFromFiles() throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("readInputFromFiles ...");
    }

    for (final File file : inputFiles) {
      final BufferedReader br = new BufferedReader(new FileReader(file));
      String line = "";
      boolean includeLine = false;
      while ((line = br.readLine()) != null) {
        // open
        if (line.contains("<PREAMBLE>")) {
          includeLine = true;
          line = line.substring(line.indexOf("<PREAMBLE>") + "<PREAMBLE>".length());
        } else if (line.contains("<TEXT>")) {
          includeLine = true;
          line = line.substring(line.indexOf("<TEXT>") + "<TEXT>".length());
        }
        // close
        if (includeLine) {
          if (line.contains("</PREAMBLE>")) {
            includeLine = false;
            if (line.indexOf("</PREAMBLE>") > 0) {
              taggedInput.append(line.substring(0, line.indexOf("</PREAMBLE>")) + "\n");
            }

          } else if (line.contains("</TEXT>")) {
            includeLine = false;
            if (line.indexOf("</TEXT>") > 0) {
              taggedInput.append(line.substring(0, line.indexOf("</TEXT>")) + "\n");
            }

          } else {
            taggedInput.append(line + "\n");
          }
        }
      }
      br.close();
    }
  }

  /**
   * Reads entities in taggedInput.
   *
   * @return
   */
  protected String parse() {
    input = taggedInput.toString().replaceAll("<p>|</p>", "");

    while (true) {
      final int openTagStartIndex = input.indexOf("<ENAMEX");
      if (openTagStartIndex == -1) {
        break;

      } else {
        final int openTagCloseIndex = input.indexOf(">", openTagStartIndex);
        final int closeTagIndex = input.indexOf("</ENAMEX>");

        try {
          final String taggedWords = input.substring(openTagCloseIndex + 1, closeTagIndex); 
          final String categoriesString = input.substring( 
              openTagStartIndex + "<ENAMEX TYPE=\"".length(), openTagCloseIndex - "\"".length());

          final String[] categories = categoriesString.split("\\|");

          for (final String cat : categories) {
            if (EntityClassMap.oracel(cat) != EntityClassMap.getNullCategory()) {

              final String[] token = FoxTextUtil.getSentenceToken(taggedWords + ".");
              String word = "";
              for (final String t : token) {
               /* boolean first = true;
                if(!t.isEmpty()){
                  if(first==true){
                    put(t,"B"+cat);
                    first=false;
                  } else {
                      put(t,"I"+cat);
                    }
                }*/
                if (!word.isEmpty() && t.isEmpty()) {
                  put(word, cat);
                  word = "";
                } else {
                  word += t + " ";
                }
              } 
              if (!word.isEmpty()) {
                put(word, cat);
              } 
            }
          }

          String escapedCategoriesString = "";
          for (final String cat : categories) {
            escapedCategoriesString += cat + "\\|";
          }

          escapedCategoriesString =
              escapedCategoriesString.substring(0, escapedCategoriesString.length() - 1);

          input = input.replaceFirst("<ENAMEX TYPE=\"" + escapedCategoriesString + "\">", "");
          input = input.replaceFirst("</ENAMEX>", "");

        } catch (final Exception e) {
          LOG.error(e.getLocalizedMessage(), e);
        }
      }
    }

    while (true) {
      final int openTagStartIndex = input.indexOf("<TIMEX");
      if (openTagStartIndex == -1) {
        break;
      } else {
        final int openTagCloseIndex = input.indexOf(">", openTagStartIndex);
        final String category =
            input.substring(openTagStartIndex + "<TIMEX TYPE=\"".length(), openTagCloseIndex - 1);
        input = input.replaceFirst("<TIMEX TYPE=\"" + category + "\">", "");
        input = input.replaceFirst("</TIMEX>", "");
      }
    }

    input = input.trim();
    // input = input.replaceAll("``|''", "");
    // input = input.replaceAll("\\p{Blank}+", " ");
    // input = input.replaceAll("\n ", "\n");
    // input = input.replaceAll("\n+", "\n");
    // input = input.replaceAll("[.]+", ".");
    return input;
  }

  protected void put(String word, final String classs) {
    word = word.trim(); 
    if (!word.isEmpty()) {
      if (entities.get(word) != null) {
        if (!entities.get(word).equals(classs)
            && !entities.get(word).equals(EntityClassMap.getNullCategory())) {
          LOG.debug("Oracle with a token with diff. annos. No disamb. for now. Ignore token.");
          LOG.debug(word + " : " + classs + " | " + entities.get(word));
          entities.put(word, EntityClassMap.getNullCategory());
        }
      } else {
        entities.put(word, classs);
      }
    }
  }
}
