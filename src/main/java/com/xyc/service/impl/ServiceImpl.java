package com.xyc.service.impl;

import com.xyc.service.Service;
import com.xyc.annotation.XycService;

@XycService("ServiceImpl")  //map.put("ServiceImpl",new ServiceImpl())
public class ServiceImpl implements Service{

	@Override
	public String query(String name, String age) {
		return "name="+name+" ; age="+age;
	}

}
