package com.nthexperience.desktop;

public final class GadgetMathCheck {
    private static int checks;
    private static void near(double actual,double expected){checks++;if(Math.abs(actual-expected)>Math.max(1,Math.abs(expected))*1e-8)throw new AssertionError(actual+" != "+expected);}
    private static void rejects(String expression){checks++;try{GadgetMath.evaluate(expression);throw new AssertionError("Accepted "+expression);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){near(GadgetMath.evaluate("2+3*4"),14);near(GadgetMath.evaluate("(2+3)*4"),20);near(GadgetMath.evaluate("-4 + 6 / 2"),-1);near(GadgetMath.evaluate("200 * 10%"),20);near(GadgetMath.evaluate("1e3 / 4"),250);rejects("1/0");rejects("1e309");rejects("1 + (2");rejects("java.lang.Runtime");near(GadgetMath.convert(32,2,1,0),0);near(GadgetMath.convert(100,2,0,1),212);near(GadgetMath.convert(1,0,7,0),1609.344);near(GadgetMath.convert(1,1,2,0),.45359237);near(GadgetMath.convert(1,6,6,4),1024);near(GadgetMath.convert(60,7,1,2),1);near(GadgetMath.convert(1,5,2,0),3.785411784);System.out.println("PASS: "+checks+" calculator, invalid-expression and unit-conversion boundaries");}
}
