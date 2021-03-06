/* Copyright (c) 2016, 2017                                               */
/*       Institute of Software, Chinese Academy of Sciences               */
/* This file is part of ROLL, a Regular Omega Language Learning library.  */
/* ROLL is free software: you can redistribute it and/or modify           */
/* it under the terms of the GNU General Public License as published by   */
/* the Free Software Foundation, either version 3 of the License, or      */
/* (at your option) any later version.                                    */

/* This program is distributed in the hope that it will be useful,        */
/* but WITHOUT ANY WARRANTY; without even the implied warranty of         */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          */
/* GNU General Public License for more details.                           */

/* You should have received a copy of the GNU General Public License      */
/* along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

package roll.main;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


import roll.automata.NBA;
import roll.learner.LearnerBase;
import roll.query.Query;
import roll.query.QuerySimple;
import roll.table.HashableValue;
import roll.words.Alphabet;
import roll.words.Word;

/**
 * @author Yong Li (liyong@ios.ac.cn)
 * */

public class InteractiveMode {
    
    public static void interact(Options options, PipedOutputStream rollOut, PipedInputStream rollIn, String[] alpha, int alphaNum) {
        // prepare the alphabet
        Alphabet alphabet = prepareAlphabet(options, alpha, alphaNum);
        TeacherNBAInteractive teacher = new TeacherNBAInteractive(rollOut, rollIn);
        LearnerBase<NBA> learner = Executor.getLearner(options, alphabet, teacher);
        
        options.log.println("Initializing learning...");
        learner.startLearning();
        boolean result = false;
        while(! result ) {
            options.log.verbose("Table/Tree is both closed and consistent\n" + learner.toString());
            NBA hypothesis = learner.getHypothesis();
            // along with ce
            System.out.println("Resolving equivalence query for hypothesis (#Q=" + hypothesis.getStateSize() + ")...  ");
            Query<HashableValue> ceQuery = teacher.answerEquivalenceQuery(hypothesis);
            boolean isEq = ceQuery.getQueryAnswer().get();
            if(isEq == true) break;
            ceQuery = getOmegaCeWord(alphabet, rollOut, rollIn);
            ceQuery.answerQuery(null);
            learner.refineHypothesis(ceQuery);
        }
        
        System.out.println("Congratulations! Learning completed...");
    }
    
    private static Alphabet prepareAlphabet(Options options, String[] alpha, int alphaNum) {
        Alphabet alphabet = new Alphabet();
        System.out.println("Please input the number of letters ('a'-'z'): " + alphaNum);
        int numLetters = alphaNum;
        for(int letterNr = 0; letterNr < numLetters; letterNr ++) {
            System.out.println("Please input the " + (letterNr + 1) + "th letter: " + alpha[letterNr]);
            Character letter = alpha[letterNr].toCharArray()[0];
            System.out.println(letter);
            alphabet.addLetter(letter);
        }
        return alphabet;
    }
    
    
    //Okay
    public static boolean getInputAnswer(PipedOutputStream rollOut, PipedInputStream rollIn, Boolean isEqui) {
        boolean answer = false;
        try {
            boolean finished = false;
            while(! finished) {
            	byte[] inputBytes = new byte[1024];
            	int len = rollIn.read(inputBytes);
            	String input = new String(inputBytes, 0, len);
            	input = input.trim();
            	System.out.println(input + "here");
                if(input.equals("1")) {
                    answer = true;
                    finished = true;
                }else if(input.equals("0")) {
                    answer = false;
                    finished = true;
                }
            }
            if(isEqui) {
            	rollOut.write("S-equiSync".getBytes());
            	rollOut.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return answer;
    }
    
//    private static Query<HashableValue> getCeWord(Alphabet alphabet) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        Word word = null;
//        try {
//            do {
//                String input = reader.readLine();
//                word = alphabet.getWordFromString(input);
//                if(word == null)    System.out.println("Illegal input, try again!");
//            }while(word == null);
//            
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return new QuerySimple<HashableValue>(word, alphabet.getEmptyWord());
//    }
    
   
    
    private static Query<HashableValue> getOmegaCeWord(Alphabet alphabet, PipedOutputStream rollOut, PipedInputStream rollIn) {
        Word prefix = null, suffix = null;
        System.out.println("Now you have to input a counterexample for inequivalence.");
        try {
            do {
                System.out.println("please input ce: ");
                byte[] inputBytes = new byte[1024];
                int len = rollIn.read(inputBytes);
                String input = new String(inputBytes, 0, len);
                input = input.trim();
                System.out.println("input counterexample: " + input);
                String[] splittedCounterexample = input.split("\\,");
                String stem = splittedCounterexample[0];
                stem = stem.trim();
                String loop = splittedCounterexample[1];
                loop = loop.trim();
                System.out.println("input stem is " + stem);
                System.out.println("input loop is " + loop);
                boolean valid = true;
                for(int i = 0; i < stem.length(); i ++) {
                    int letter = alphabet.indexOf(stem.charAt(i));
                    if(letter < 0) {
                        valid = false;
                        break;
                    }
                }
                for(int i = 0; i < loop.length(); i ++) {
                    int letter = alphabet.indexOf(loop.charAt(i));
                    if(letter < 0) {
                        valid = false;
                        break;
                    }
                }
                if(valid) {
                    prefix = alphabet.getWordFromString(stem);
                    suffix = alphabet.getWordFromString(loop);
                    System.out.println("OK");
                }else  {
                	String illegal = "I-Illegal input, try again!";
                	rollOut.write(illegal.getBytes());
                	rollOut.flush();
                    System.out.println("I-Illegal input, try again!");
                }
            }while(prefix == null || suffix == null);
            System.out.println("You input a stem: " + prefix.toStringWithAlphabet());
            System.out.println("You input a loop: " + suffix.toStringWithAlphabet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new QuerySimple<HashableValue>(prefix, suffix);
    }
    
    

}
