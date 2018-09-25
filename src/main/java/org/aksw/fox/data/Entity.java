package org.aksw.fox.data;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

import org.aksw.fox.data.Token;

/**
 *
 * @author Ren&eacute; Speck <speck@informatik.uni-leipzig.de>
 *
 */
public class Entity implements IData {

  public static final float DEFAULT_RELEVANCE = -1;

  protected String text = "";

  protected ArrayList<Token> words = new ArrayList<Token>(); //tokens of text

  protected String category = ""; //LOCATION, ORGANIZATION, PERSON OR OUTSIDE

  protected String uri = "";

  protected float relevance = DEFAULT_RELEVANCE;

  protected String tool = "";

  /**
   * Start indices.
   */
  protected Set<Integer> indicies = null;

  /**
   *
   * Constructor.
   *
   * @param text
   * @param type
   */
  public Entity(final String text, final String type) {
    this(text, type, DEFAULT_RELEVANCE, "");
  }

  /**
   *
   * Constructor.
   *
   * @param text
   * @param type
   * @param relevance
   */
  public Entity(final String text, final String type, final float relevance) {
    this(text, type, relevance, "");
  }

  /**
   *
   * Constructor.
   *
   * @param text
   * @param type
   * @param relevance
   * @param tool
   */
  public Entity(final String text, final String type, final float relevance, final String tool) {
    this.text = text;
    this.category = type;
    this.relevance = relevance;
    this.tool = tool;
    createWords();
  }

  public void addText(final String text) {
    this.text += " " + text;
    this.words.clear();
    createWords();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Entity other = (Entity) obj;
    if (text == null) {
      if (other.text != null) {
        return false;
      }
    } else if (!text.equals(other.text)) {
      return false;
    }
    if (category == null) {
      if (other.category != null) {
        return false;
      }
    } else if (!category.equals(other.category)) {
      return false;
    }
    return true;
  }

  /**
   * Gets all start indices.
   */
  public Set<Integer> getIndices() {
    return indicies;
  }

  /**
   * Adds start indices
   *
   * @param index
   * @return self
   */
  public Entity addIndicies(final int index) {
    if (indicies == null) {
      indicies = new TreeSet<>();
    }
    indicies.add(index);
    return this;
  }

  /**
   * Adds start indices.
   *
   * @param indices
   * @return self
   */
  public Entity addAllIndicies(final Set<Integer> indices) {
    if (indicies == null) {
      indicies = new HashSet<>();
    }
    indicies.addAll(indices);
    return this;
  }

  @Override
  public String getToolName() {
    return tool;
  }

  public String getFullText() {
    return text;
  }

  public String getCategory() {
    return category;
  }

  public float getRelevance() {
    return relevance;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(final String uri) {
    this.uri = uri;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((text == null) ? 0 : text.hashCode());
    result = (prime * result) + ((category == null) ? 0 : category.hashCode());
    return result;
  }

  public void setText(final String text) {
    this.text = text;
    this.words.clear();
    createWords();
  }

  @Override
  public String toString() {
    String output;
    StringBuilder tokens = new StringBuilder("");
    for (int i=0;i<words.size();i++){
      tokens.append(", token=").append(words.get(i).getText()).append(", type=").append(words.get(i).getType());
    }
    /*output="Entity [text=" + text + ", type=" + category +"{"+tokens+"}"+ ", uri=" + uri + ", tool=" + tool
    + ", relevance=" + relevance + (indicies != null ? ", indicies=" + indicies : "") + "]\n";*/
    return "Entity [text=" + text + ", type=" + category + ", uri=" + uri + ", tool=" + tool
      + ", relevance=" + relevance + (indicies != null ? ", indicies=" + indicies : "") + "]";
      //return output;
  }
  /**
   * Method responsible for spliting the text of an entity into words and its respective IOB tags
   */
  public void createWords(){

      String word[] = this.text.split(" ");
      int i=0;
      for (String t : word){
        t=t.trim();
        if(!t.isEmpty()) {
          if(this.category.equals(EntityClassMap.L)||this.category.equals(EntityClassMap.P)||this.category.equals(EntityClassMap.O)){
            if(i==0)
            {
              words.add(new Token(t,"B"+this.category,i));
              i++;
            } else {
              words.add(new Token(t,"I"+this.category,i));
              i++;
              }
          } else {
            words.add(new Token(t,this.category,i));
            i++;
          }
        } else {
          //words.add(new Token("",this.category,i));
        }
      } 
  }

  /**
   * Returns array of tokens
   * @return ArrayList<Token>
   */
  public ArrayList<Token> getWords(){
    return this.words;
  }
}
