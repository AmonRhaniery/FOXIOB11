package org.aksw.fox.data;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * This class is part of an Entity Class. Responsible for holding each word and its respective B-type or I-type.
 * 
 * @author Ren&eacute; Speck <speck@informatik.uni-leipzig.de>
 *
 */
public class Token {


  protected String word = "";

  protected String type = "";
  
  protected int index = 0;

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
  public Token(final String text, final String type, int index) {
    this.word=text;
    this.type=type;
    this.index=index;
  }

  public void setIndex(int x){
    this.index=x;
  }

  public int getIndex(){
    return index;
  }

  public void addWord(final String text) {
    this.word=text;
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
    final Token other = (Token) obj;
    if (word == null) {
      if (other.word != null) {
        return false;
      }
    } else if (!word.equals(other.word)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    } else if (!type.equals(other.type)) {
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
  public Token addIndicies(final int index) {
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
  public Token addAllIndicies(final Set<Integer> indices) {
    if (indicies == null) {
      indicies = new HashSet<>();
    }
    indicies.addAll(indices);
    return this;
  }

  public String getText() {
    return word;
  }

  public String getType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((word == null) ? 0 : word.hashCode());
    result = (prime * result) + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  public void setWord(final String text) {
    this.word = text;
  }
  public void setType(final String type){
      this.type=type;
  }

  @Override
  public String toString() {
    return "Token [text=" + word + ", type=" + type + ", + " + (indicies != null ? ", indicies=" + indicies : "") + "]";
  }
}
