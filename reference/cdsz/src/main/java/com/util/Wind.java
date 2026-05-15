package com.util;

import java.awt.Graphics2D;
import java.awt.Point;


public class Wind {
  
      public static float    pole        = 70; //风杆长度
      public static double    plume       = 22.0;  //风羽长度
      public static double    interval    = 10.0;  //风羽间隔
      public static float       height      = 8;   //位图高度

    
	  /**
	   * 功能：风向风力填图
	   * 参数：
	   *  g           - 图形设备
	   *  pos         - 站点位置
	   *  dir         - 风向
	   *  power       - 风力
	   * 返回值：
	   *      无e
	   */
	      public void plotWind(Graphics2D g, Point pos, double dir, double power) {//风向风力填图
	          int     xStation    = pos.x;
	          int     yStation    = pos.y;
//	          double  d           = dir;
	          double  p           = power;
	          double  pwr         = power;
	          
	          
	          //绕原点的任一点
	          float x0=xStation;
	          float y0=yStation-pole;
	          
	          float xr0=xStation;
	          float yr0=yStation-8;
	          
	          float  xr=(float)((xr0-xStation)*Math.cos(Math.toRadians(dir))-(yr0-yStation)*Math.sin(Math.toRadians(dir))+xStation);
		      float  yr=(float)((yr0-yStation)*Math.cos(Math.toRadians(dir))-(xr0-xStation)*Math.sin(Math.toRadians(dir))+yStation);
	          // System.out.println(xStation+"++++"+yStation+"+++++"+xr+"++++++++++++++++++"+yr);
	          
	          //风杆顶点坐标
	         float  x1=(float)((x0-xStation)*Math.cos(Math.toRadians(dir))-(y0-yStation)*Math.sin(Math.toRadians(dir))+xStation);
	         float  y1=(float)((y0-yStation)*Math.cos(Math.toRadians(dir))-(x0-xStation)*Math.sin(Math.toRadians(dir))+yStation);
	        
	         
	          double  xPw, yPw, xPw1, yPw1;
	          try {
	              g.drawLine((int)xr,(int)yr,(int)x1,(int)y1);  //风杆
	              int     i   = 0;
	              
		         
	           //   int     tri = 0;
	              pwr     = p + 1.0;
	              while( pwr >= 20.0 ) {//每20m/s画一个三角符号
	            	  //三角形第三遍的长度
	  	            float threeLength=(float)Math.sqrt(Math.pow(plume, 2)+Math.pow(pole-(i*interval), 2));
	  	          //  System.out.println(threeLength);
	  		       
	  	            //三角形在原点的角度
	  	             float angle=(float)Math.toDegrees(Math.atan(plume/(pole-(i*interval))));
	  	         //  System.out.println(i+"==="+angle);
	  	            
	            	   //绕原点的三角形顶点坐标
			           float x00=xStation;
			           float y00=yStation-threeLength;
			          // System.out.println(y00+"====");
			           //三角形顶点坐标
	     	         float  x2=(float)((x00-xStation)*Math.cos(Math.toRadians(dir+angle))-(y00-yStation)*Math.sin(Math.toRadians(dir+angle))+xStation);
	     	         float  y2=(float)((y00-yStation)*Math.cos(Math.toRadians(dir+angle))-(x00-xStation)*Math.sin(Math.toRadians(dir+angle))+yStation);
	            	  //在风杆上的第二个点
			          float x01=xStation;
			          float y01=(float)(yStation-(pole-interval*(i)));
		              float  x3=(float)((x01-xStation)*Math.cos(Math.toRadians(dir))-(y01-yStation)*Math.sin(Math.toRadians(dir))+xStation);
		 	          float  y3=(float)((y01-yStation)*Math.cos(Math.toRadians(dir))-(x01-xStation)*Math.sin(Math.toRadians(dir))+yStation);
			        
	                  xPw     = x3;
	                  yPw     = y3;
	                  xPw1    = x2;
	                  yPw1    =y2;
	                  g.drawLine((int)xPw, (int)yPw, (int)xPw1, (int)yPw1);
	                  
	                  float x011=xStation;
			          float y011=(float)(yStation-(pole-interval*(i+1)));
		              float  x31=(float)((x011-xStation)*Math.cos(Math.toRadians(dir))-(y011-yStation)*Math.sin(Math.toRadians(dir))+xStation);
		 	          float  y31=(float)((y011-yStation)*Math.cos(Math.toRadians(dir))-(x011-xStation)*Math.sin(Math.toRadians(dir))+yStation);
	                  xPw=x31;
	                  yPw=y31;
	                  xPw1    = x2;
	                  yPw1    = y2;
	                  g.drawLine((int)xPw, (int)yPw, (int)xPw1, (int)yPw1);
	                  pwr = pwr - 20.0;
	                  i       ++;
	                  
	              }
	           
	              pwr = pwr - 1.0;
	              while( pwr >= 4.0 || pwr == 3.0 ) {//每4m/s画一根风羽
	            	   float threeLength=(float)Math.sqrt(Math.pow(plume, 2)+Math.pow(pole-(i*interval), 2));
		  	           // System.out.println(threeLength);
		  		       
		  	            //三角形在原点的角度
		  	      
		  	             float angle=(float)Math.toDegrees(Math.atan(plume/(pole-(i*interval))));
		  	         //  System.out.println(i+"==="+angle);
		  	            
		            	   //绕原点的三角形顶点坐标
				           float x00=xStation;
				           float y00=yStation-threeLength;
				         //  System.out.println(y00+"====");
				           //三角形顶点坐标
		     	         float  x2=(float)((x00-xStation)*Math.cos(Math.toRadians(dir+angle))-(y00-yStation)*Math.sin(Math.toRadians(dir+angle))+xStation);
		     	         float  y2=(float)((y00-yStation)*Math.cos(Math.toRadians(dir+angle))-(x00-xStation)*Math.sin(Math.toRadians(dir+angle))+yStation);
		            	  //在风杆上的第二个点
				          float x01=xStation;
				          float y01=(float)(yStation-(pole-interval*(i)));
			              float  x3=(float)((x01-xStation)*Math.cos(Math.toRadians(dir))-(y01-yStation)*Math.sin(Math.toRadians(dir))+xStation);
			 	          float  y3=(float)((y01-yStation)*Math.cos(Math.toRadians(dir))-(x01-xStation)*Math.sin(Math.toRadians(dir))+yStation);
				        
		                  xPw     = x3;
		                  yPw     = y3;
		                  xPw1    = x2;
		                  yPw1    =y2;
		                  g.drawLine((int)xPw, (int)yPw, (int)xPw1, (int)yPw1);
	                  pwr     = pwr - 4.0;
	                  i       ++;
	              }
	              if( pwr >= 0.0 ) {
	            	   float threeLength=(float)Math.sqrt(Math.pow(plume/2, 2)+Math.pow(pole-(i*interval), 2));
		  	        //    System.out.println(threeLength);
		  		           
		  	            //三角形在原点的角度
		  	      
		  	             float angle=(float)Math.toDegrees(Math.atan(plume/2/(pole-(i*interval))));
		  	         //  System.out.println(i+"==="+angle);
		  	            
		            	 
		            	   //绕原点的三角形顶点坐标
				           float x00=xStation;
				           float y00=yStation-threeLength;
				           //System.out.println(y00+"====");
				           //三角形顶点坐标
		     	         float  x2=(float)((x00-xStation)*Math.cos(Math.toRadians(dir+angle))-(y00-yStation)*Math.sin(Math.toRadians(dir+angle))+xStation);
		     	         float  y2=(float)((y00-yStation)*Math.cos(Math.toRadians(dir+angle))-(x00-xStation)*Math.sin(Math.toRadians(dir+angle))+yStation);
		            	  //在风杆上的第二个点
				          float x01=xStation;
				          float y01=(float)(yStation-(pole-interval*(i)));
			              float  x3=(float)((x01-xStation)*Math.cos(Math.toRadians(dir))-(y01-yStation)*Math.sin(Math.toRadians(dir))+xStation);
			 	          float  y3=(float)((y01-yStation)*Math.cos(Math.toRadians(dir))-(x01-xStation)*Math.sin(Math.toRadians(dir))+yStation);
				        
		                  xPw     = x3;
		                  yPw     = y3;
		                  xPw1    = x2;
		                  yPw1    =y2;
		                  g.drawLine((int)xPw, (int)yPw, (int)xPw1, (int)yPw1);
	              }
	        
	          }
	          catch(Exception ex) {
	             // System.out.println(ex.getMessage());
	              ex.printStackTrace();
	          }
	  
	      }
	      
	      Point calcDeltaXY(Double angle, int x1, int y1) {
	          Point point = new Point(0, 0);
	        //  System.out.println(Math.PI);
	          double alpha = angle * Math.PI / 180.0;
	          double theta = Math.PI/2.0 + alpha/2.0 - Math.atan(x1/y1);
	          double length = 2.0 * Math.sqrt(x1*x1 + y1*y1) * Math.sin(alpha/2.0);
	          point.x = (int) (length * Math.sin(theta));
	          point.y = (int) (- length * Math.cos(theta));
	          return point;
	      }
	      

}
