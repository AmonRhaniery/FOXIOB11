package org.aksw.fox.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * This class contains constants to map the entity type/class for each NER tool. The types/classes
 * are used in FOX are {@link #L}, {@link #O}, {@link #P}, {@link #M} and {@link #N}.
 *
 * @author rspeck
 *
 */
public class EntityClassMap {
  //IOB encoding
  public static final String B_L = "BLOCATION";
  public static final String I_L = "ILOCATION";
  public static final String B_O = "BORGANIZATION";
  public static final String I_O = "IORGANIZATION";
  public static final String B_P = "BPERSON";
  public static final String I_P = "IPERSON";
  public static final String N = "OUTSIDE";
    //IO encoding
  public static final String O = "ORGANIZATION";
  public static final String L = "LOCATION";
  public static final String P = "PERSON";
  
  

  public static final List<String> entityClasses = Arrays.asList(B_L, I_L, B_O, I_O, B_P, I_P, N);

  protected static final Map<String, String> entityClassesOracel = new HashMap<>();
  static {
    entityClassesOracel.put("ORGANIZATION", O);
    entityClassesOracel.put("LOCATION", L);
    entityClassesOracel.put("PERSON", P);
    entityClassesOracel.put("BORGANIZATION", B_O);
    entityClassesOracel.put("BLOCATION", B_L);
    entityClassesOracel.put("BPERSON", B_P);
    entityClassesOracel.put("IORGANIZATION", I_O);
    entityClassesOracel.put("ILOCATION", I_L);
    entityClassesOracel.put("IPERSON", I_P);
  }

  protected static final Map<String, String> entityClassesNEEL = new HashMap<>();
  static {
    entityClassesNEEL.put("Organization", O);
    entityClassesNEEL.put("Location", L);
    entityClassesNEEL.put("Person", P);
  }

  protected static final Map<String, String> entityClassesILLINOIS = new HashMap<>();
  static {
    entityClassesILLINOIS.put("LOC", L);
    entityClassesILLINOIS.put("ORG", O);
    entityClassesILLINOIS.put("PER", P);
  }

  /**
   * Gets the entity class for a oracel entity type/class.
   */
  public static String oracel(final String tag) {
    String t = entityClassesOracel.get(tag);
    if (t == null) {
      t = getNullCategory();
    }
    return t;
  }

  /**
   * Gets the entity class for a NEEL challenge entity type/class.
   */
  public static String neel(final String tag) {
    String t = entityClassesNEEL.get(tag);
    if (t == null) {
      t = getNullCategory();
    }
    return t;
  }

  /**
   * Gets the entity class for a illinois entity type/class.
   */
  public static String illinois(final String illinoisTag) {
    String t = entityClassesILLINOIS.get(illinoisTag);
    if (t == null) {
      t = getNullCategory();
    }
    return t;
  }

  /**
   * Gets the null type/class.
   */
  public static String getNullCategory() {
    return N;
  }
}
