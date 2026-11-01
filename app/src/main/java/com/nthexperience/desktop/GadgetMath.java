package com.nthexperience.desktop;

import java.math.BigDecimal;
import java.util.Locale;

final class GadgetMath {
    static final String[] CATEGORIES={"Length","Mass","Temperature","Speed","Area","Volume","Data","Time"};
    static final String[][] UNITS={{"m","km","cm","mm","in","ft","yd","mi"},{"kg","g","lb","oz","stone"},{"°C","°F","K"},{"m/s","km/h","mph","knots","ft/s"},{"m²","km²","ft²","acre","hectare"},{"L","mL","US gal","UK gal","US cup","US pint"},{"B","kB","KiB","MB","MiB","GB","GiB"},{"s","min","h","day","week","ms"}};
    private static final double[][] FACTORS={{1,1000,.01,.001,.0254,.3048,.9144,1609.344},{1,.001,.45359237,.028349523125,6.35029318},{1,1,1},{1,1/3.6,.44704,.5144444444,.3048},{1,1000000,.09290304,4046.8564224,10000},{1,.001,3.785411784,4.54609,.2365882365,.473176473},{1,1000,1024,1000000,1048576,1000000000,1073741824},{1,60,3600,86400,604800,.001}};
    static double convert(double value,int category,int from,int to){if(!Double.isFinite(value)||category<0||category>=UNITS.length||from<0||to<0||from>=UNITS[category].length||to>=UNITS[category].length)throw new IllegalArgumentException("Choose valid units");double result;if(category==2){double c=from==1?(value-32)*5/9:from==2?value-273.15:value;result=to==1?c*9/5+32:to==2?c+273.15:c;}else result=value*FACTORS[category][from]/FACTORS[category][to];if(!Double.isFinite(result))throw new IllegalArgumentException("Number is too large");return result;}
    static String number(double n){if(!Double.isFinite(n))throw new IllegalArgumentException("Number is too large");if(n==0)return "0";String plain=BigDecimal.valueOf(n).stripTrailingZeros().toPlainString();return plain.length()>18?String.format(Locale.ROOT,"%.9g",n):plain;}
    static double evaluate(String expression){if(expression==null||expression.length()>120)throw new IllegalArgumentException("Use a shorter expression");Parser parser=new Parser(expression.replace('×','*').replace('÷','/'));double value=parser.sum();parser.space();if(parser.index!=parser.text.length()||!Double.isFinite(value))throw new IllegalArgumentException("Check the expression");return value;}
    private static final class Parser {
        final String text;int index;Parser(String text){this.text=text;}
        void space(){while(index<text.length()&&Character.isWhitespace(text.charAt(index)))index++;}
        boolean take(char c){space();if(index<text.length()&&text.charAt(index)==c){index++;return true;}return false;}
        double sum(){double value=product();while(true){if(take('+'))value+=product();else if(take('-'))value-=product();else return value;}}
        double product(){double value=factor();while(true){if(take('*'))value*=factor();else if(take('/')){double divisor=factor();if(divisor==0)throw new IllegalArgumentException("Cannot divide by zero");value/=divisor;}else return value;}}
        double factor(){if(take('+'))return factor();if(take('-'))return -factor();double value;if(take('(')){value=sum();if(!take(')'))throw new IllegalArgumentException("Close the parentheses");}else{space();int start=index;while(index<text.length()&&(Character.isDigit(text.charAt(index))||text.charAt(index)=='.'))index++;if(index<text.length()&&(text.charAt(index)=='e'||text.charAt(index)=='E')){index++;if(index<text.length()&&(text.charAt(index)=='+'||text.charAt(index)=='-'))index++;while(index<text.length()&&Character.isDigit(text.charAt(index)))index++;}if(start==index)throw new IllegalArgumentException("Enter a number");try{value=Double.parseDouble(text.substring(start,index));}catch(NumberFormatException ex){throw new IllegalArgumentException("Check the number");}}while(take('%'))value/=100;return value;}
    }
}
