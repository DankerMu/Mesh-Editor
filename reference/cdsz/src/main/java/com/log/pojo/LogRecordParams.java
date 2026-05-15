package com.log.pojo;

import java.util.Calendar;
import java.util.Date;

import lombok.Data;

import org.springframework.security.core.context.SecurityContextHolder;

import com.util.TimeUtil;

/**
 * @category
 * @date 2025/3/27 14:45
 * @description TODO
 */
@Data
public class LogRecordParams {
    private Long id;
    private String username;
    private String optiontype;
    private String optioncontent;
    private String optiontime;

    public LogRecordParams() {}

    public LogRecordParams(String optiontype, String optioncontent) {
//        ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
//        if(attributes == null)
//        {
//        	this.username = SecurityContextHolder.getContext().getAuthentication().getName();
//        }
//        else
//        {
//        	HttpServletRequest httpServletRequest = attributes.getRequest();
//        	HttpSession session = httpServletRequest.getSession();
//        	this.username = String.valueOf(session.getAttribute("username"));
//        }
//    	this.username = UserNameHolder.userName;
//    	if(UserNameHolder.userName == null)
//    	{
//    		this.username = SecurityContextHolder.getContext().getAuthentication().getName();
//    	}
//    	System.out.println("log get token: " + SecurityContextHolder.getContext().getAuthentication());
    	this.username = SecurityContextHolder.getContext().getAuthentication().getName();
        this.optiontype = optiontype;
        this.optioncontent = optioncontent;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        this.optiontime = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
    }
    public LogRecordParams(String username, String optiontype, String optioncontent) {
        this.username = username;
        this.optiontype = optiontype;
        this.optioncontent = optioncontent;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR_OF_DAY, 8);
        this.optiontime = TimeUtil.date2String(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
    }
}
