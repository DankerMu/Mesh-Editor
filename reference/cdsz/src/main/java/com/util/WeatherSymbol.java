package com.util;

public class WeatherSymbol {
   String str;
   public String weather(int aa){
	    switch (aa) {
		case 0:break;
		case 1:break;
		case 2:break;
		case 3:break;
		case 4:str="b" ;break;
		case 5:str="c" ;break;
		case 6:str="d" ;break;
		
		case 7:str="e" ;break;
		case 8:str="f" ;break;
		case 9:str="g" ;break;
		case 10:str="h";break;
		case 11:str="i";break;
		case 12:str="j" ;break;
		case 13:str="k" ;break;
		case 14:str="l" ;break;
		case 15:str="m" ;break;
		case 16:str="n" ;break;
		case 17:str="o" ;break;
		case 18:str="p" ;break;
		case 19:str="q" ;break;
		case 20:str="r";break;
		case 21:str="s";break;
		case 22:str="t" ;break;
		case 23:str="u" ;break;
		case 24:str="v" ;break;
		case 25:str="w" ;break;
		case 26:str="x" ;break;
		case 27:str="y" ;break;
		case 28:str="z" ;break;
		case 29:str="{" ;break;
		case 30:str="¡";break;
		case 31:str="¢";break;
		case 32:str="£" ;break;
		case 33:str="¤" ;break;
		case 34:str="¥" ;break;
		case 35:str="¦" ;break;
		case 36:str="§" ;break;
		case 37:str="¨" ;break;
		case 38:str="©" ;break;
		case 39:str="ª" ;break;
		case 40:str="«";break;
		case 41:str="¬";break;
		case 42:str="®" ;break;
		case 43:str="¯" ;break;
		case 44:str="°" ;break;
		case 45:str="±" ;break;
		case 46:str="²" ;break;
		case 47:str="³" ;break;
		case 48:str="´" ;break;
		case 49:str="µ" ;break;
		case 50:str="¶";break;
		case 51:str="·";break;
		case 52:str="¸";break;
		case 53:str="¹";break;
		case 54:str="º";break;
		case 55:str="»";break;
		case 56:str="¼";break;
		case 57:str="½";break;
		case 58:str="¾";break;
		case 59:str="¿";break;
		case 60:str="À";break;
		case 61:str="Á";break;
		case 62:str="Â";break;
		case 63:str="Ã";break;
		case 64:str="Ä";break;
		case 65:str="Å";break;
		case 66:str="Æ";break;
		case 67:str="Ç";break;
		case 68:str="È";break;
		case 69:str="É";break;
		case 70:str="Ê";break;
		case 71:str="Ë";break;
		case 72:str="Ì";break;
		case 73:str="Í";break;
		case 74:str="Î";break;
		case 75:str="Ï";break;
		case 76:str="Ð";break;
		case 77:str="Ñ";break;
		case 78:str="Ò";break;
		case 79:str="Ó";break;
		case 80:str="Ô";break;
		case 81:str="Õ";break;
		case 82:str="Ö";break;
		case 83:str="×";break;
		case 84:str="Ø";break;
		case 85:str="Ù";break;
		case 86:str="Ú";break;
		case 87:str="Û";break;
		case 88:str="Ü";break;
		case 89:str="Ý";break;
		case 90:str="Þ";break;
		case 91:str="ß";break;
		case 92:str="à";break;
		case 93:str="á";break;
		case 94:str="â";break;
		case 95:str="ã";break;
		case 96:str="ä";break;
		case 97:str="å";break;
		case 98:str="æ";break;
		case 99:str="ç";break;
		default:
			break;
		}
	    
	   return str;
   }
   
   public static void main(String[] args) {
	System.out.println(new WeatherSymbol().weather(66));
}
}
