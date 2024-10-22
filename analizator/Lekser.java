package analizator;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Map;

import analizator.generated.State;
import analizator.structures.LexUnit;
import analizator.structures.Rule;

public class Lekser {

  private static final int LEX_UNIT_MAX_LENGTH = 256;   // Lexical units longer than 256 chars will cause undefined behavior

  private char activeChar;
  private State activeState;
  private Reader reader;
  private int readLen;            // number of chars read since last accept()
  private int lastValidLen;       // distance from *start to *last

  private int newLines;
  private int lineNumber;
  
  private Rule[] lastAccepted; 

  private Map<State, Rule[]> rules = Rules.getRules();  // TODO: get rules from generated file

  private ArrayList<LexUnit> lexUnits;

  public Lekser(Reader _reader){
    reader = _reader;
    reader.mark(LEX_UNIT_MAX_LENGTH);

    readLen = 0;
    lastValidLen = 0;

    lineNumber = 1;

    activeState = State.S_pocetno;

    lexUnits = new ArrayList<LexUnit>();
  }

  // TODO: change return type or return through getters of private properties
  public void analyse() throws IOException{
    for(;;){
      nextChar();
      
      boolean anyActive = false;
      ArrayList<Rule> accepts = new ArrayList<Rule>();
      
      for(Rule rule : rules.get(activeState)){
        anyActive = anyActive || rule.automat.doTransition(activeChar);
        if(rule.automat.isAccepted()){
          accepts.add(rule);
        }
      }
      if(!accepts.isEmpty()){
        lastAccepted = accepts.toArray(new Rule[0]);
        lastValidLen = readLen;
      }
      if(anyActive){
        // TODO: handle EOF
        continue;
      }
      if(lastAccepted.length == 0){
        //TODO: error recovery
        System.err.println("ERROR: TODO: RECOVER");
      }
      else{
        // TODO: test, potential source of errors
        Rule priorityRule = lastAccepted[0]; // because of ordering, first rule has highest priority

        accept(priorityRule);
      }
      resetAutomatons();
    }

    /*(
      *  for each regex (rule) tied to one of the active states:
      *   check if regex accepts
      *   note what regexes accept in array lastAccepted
      *  if all reject:
      *    if len(lastAccepted) == 0:
      *      error recovery 
      *    if len(lastAccepted) > 1:
      *      only consider the regex tied to the uppermost rule
      *    deal with the transition tied to the last regex that accepted => accept()
      *    reset relevant data structures
    ) */

  }

  private int nextChar() throws IOException{
    activeChar = (char) reader.read();
    if(activeChar == -1){
      return 1;
    }
    // else
    readLen ++;
    return 0;
  }

  private void accept(Rule rule){

    reader.reset();
    int acceptLen;
    if(rule.goBack != -1){
      acceptLen = rule.goBack;
    }
    else{
      acceptLen = lastValidLen;
    }
    char[] lexUnitCharArr = new char[acceptLen];
    reader.read(lexUnitCharArr, 0, lastValidLen);
    String lexUnitString = new String(lexUnitCharArr);

    if(rule.newLine){
      lineNumber++;
    }

    if(rule.lexClass != null){
      lexUnits.add(new LexUnit(rule.lexClass, lexUnitString));
    }

    if(rule.stateTo != null){
      this.activeState = rule.stateTo;
    }
    

    reader.mark(LEX_UNIT_MAX_LENGTH);
    
  }

  private void resetAutomatons(){
    // TODO:
  }

  void handleError(){
    /*
     * TODO:
     * return readCache to reader
     * reset relevant data structures
     */
  }

}
