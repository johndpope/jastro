package com.marklipson.astrologyclock;

/**
 * Simple tokenizer.  Returns:
 *   - String - for words and punctuation
 *   - Integer - for numeric values
 *   - null at end of input
 */
public class BasicTokenizer
{
  private String input;
  private int pos;
  private Object nextToken;
  private int lastTokenOffs;
  private int lastTokenLen;
  
  /**
   * Parse a string.
   */
  public BasicTokenizer( String input )
  {
    this.input = input;
  }
  /**
   * Get current offset within input.
   */
  public int getLastTokenOffs()
  {
    return lastTokenOffs;
  }
  public int getLastTokenLen()
  {
    return lastTokenLen;
  }
  /**
   * Get next token.
   */
  private Object nextToken()
  {
    // skip whitespace
    while (pos < input.length()  &&  Character.isWhitespace( input.charAt( pos ) ))
      pos ++;
    lastTokenOffs = pos;
    lastTokenLen = 0;
    // blank string indicates EOF
    if (pos >= input.length())
      return null;
    char ch = input.charAt( pos );
    if (Character.isDigit( ch ))
    {
      // a number
      int p0 = pos;
      while (pos < input.length()  &&  Character.isDigit( input.charAt( pos ) ))
        pos ++;
      lastTokenLen = pos - lastTokenOffs;
      return Integer.valueOf( input.substring( p0, pos ) );
    }
    else if (Character.isLetter( ch ))
    {
      // a word
      int p0 = pos;
      while (pos < input.length()  &&  Character.isLetter( input.charAt( pos ) ))
        pos ++;
      lastTokenLen = pos - lastTokenOffs;
      return input.substring( p0, pos );
    }
    else
    {
      // return everything else as single letter tokens
      pos ++;
      lastTokenLen = pos - lastTokenOffs;
      return String.valueOf( ch );
    }
  }
  /**
   * Test for end of input.
   */
  boolean isEOF()
  {
    return (pos >= input.length()  &&  nextToken == null);
  }
  /**
   * Get the next token in the stream and skip it.
   */
  Object next()
  {
    if (nextToken != null)
    {
      Object token = nextToken;
      nextToken = null;
      return token;
    }
    else
    {
      Object token = nextToken();
      return token;
    }
  }
  /**
   * Look ahead at the next token.
   */
  Object lookahead()
  {
    if (nextToken != null)
      return nextToken;
    nextToken = nextToken();
    return nextToken;
  }
}
