package com.xyc.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xyc.annotation.XycAutowired;
import com.xyc.annotation.XycController;
import com.xyc.annotation.XycRequestMapping;
import com.xyc.annotation.XycRequestParam;
import com.xyc.service.Service;

@XycController
@XycRequestMapping("/xyc")
public class Controller {
	
	@XycAutowired("ServiceImpl")
	private Service service;
	
	@XycRequestMapping("/query")
	public void query(HttpServletRequest request,HttpServletResponse response,
			@XycRequestParam("name") String name,@XycRequestParam("age") String age) {
		try {
			PrintWriter writer = response.getWriter();
			String str = service.query(name, age);
			writer.write(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
