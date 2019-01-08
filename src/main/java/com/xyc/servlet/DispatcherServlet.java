package com.xyc.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.xyc.annotation.XycAutowired;
import com.xyc.annotation.XycController;
import com.xyc.annotation.XycRequestMapping;
import com.xyc.annotation.XycRequestParam;
import com.xyc.annotation.XycService;
import com.xyc.controller.Controller;

public class DispatcherServlet extends HttpServlet {
	
	List<String> classNames = new ArrayList<String>();
	Map<String,Object> beans = new HashMap<String,Object>();
	Map<String,Object> handerMap = new HashMap<String,Object>();
	//tomcat启动的时候 实例化 map ioc
	public void init(ServletConfig config) {
		
		basePackageScan("com.xyc");
		
		//对classNames
		doInstance();
		
		doAutowired();
		
		doUrlMapping(); //  xyc/query-------->method
	}
	
	public void doUrlMapping() {
		for(Map.Entry<String, Object> entry : beans.entrySet()) {
			Object instance = entry.getValue();
			Class<?> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(XycController.class)) {
				XycRequestMapping mapping1 = clazz.getAnnotation(XycRequestMapping.class);
				String classPath = mapping1.value();  //    /xyc
				
				Method[] methods = clazz.getMethods();
				for(Method method : methods) {
					if(method.isAnnotationPresent(XycRequestMapping.class)) {
						XycRequestMapping mapping2 = method.getAnnotation(XycRequestMapping.class);
						String methodPath = mapping2.value();  //  /query
						
						String requestPath = classPath + methodPath;
						handerMap.put(requestPath, method);
					}else {
						continue;
					}
				}
			}else {
				continue;
			}
		}
	}
	
	public void doAutowired() {
		for(Map.Entry<String, Object> entry : beans.entrySet()) {
			Object instance = entry.getValue();
			Class<?> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(XycController.class)) {
				Field[] fields = clazz.getDeclaredFields();
				for(Field field : fields) {
					if(field.isAnnotationPresent(XycAutowired.class)) {
						XycAutowired auto = field.getAnnotation(XycAutowired.class);
						String key = auto.value();  //key=ServiceImpl
						Object bean = beans.get(key);
						field.setAccessible(true);  //权限打开（private）
						try {
							field.set(instance, bean);
						} catch (Exception e) {
						}
					}else {
						continue;
					}
				}
			}else {
				continue;
			}
		}
	}
	
	public void doInstance() {
		for(String className:classNames) {
			//com.xyc....Service.class
			String cn = className.replace(".class", "");
			//com.xyc....Service
			try {
				Class<?> clazz = Class.forName(cn);
				if(clazz.isAnnotationPresent(XycController.class)) {
					//控制类
					Object instance = clazz.newInstance();
					XycRequestMapping mapping = clazz.getAnnotation(XycRequestMapping.class);
					String key = mapping.value();
					//创建map
					beans.put(key, instance);
				}else if(clazz.isAnnotationPresent(XycService.class)) {
					//服务类
					Object instance = clazz.newInstance();
					XycService service = clazz.getAnnotation(XycService.class);
					String key = service.value();
					//创建map
					beans.put(key, instance);
				}else {
					continue;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void basePackageScan(String basePackage) {  //basePackage = "com.xyc"
		//扫描编译好的类路径  ...class e:/edsd\\ 
		//url = E:/work/com/xyc
		URL url = this.getClass().getClassLoader().getResource("/"+basePackage.replaceAll("\\.", "/"));
		String fileStr = url.getFile();  //E:/work/com/xyc
		File file = new File(fileStr); 
		
		String[] list = file.list();  //文件名
		for(String path : list) {
			File filePath = new File(fileStr+path);
			if(filePath.isDirectory()) {
				basePackageScan(basePackage+"."+path);
			}else {
				//com.xyc......Service.class
				classNames.add(basePackage+"."+filePath.getName());
			}
		}
	}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String uri = req.getRequestURI();  //   /xyc-mvc              /xyc/query---->method
		String contextPath = req.getContextPath();   //   /xyc-mvc 
		String path = uri.replace(contextPath, "");   //   /xyc/query   ---->key----->method
		
		Method method = (Method) handerMap.get(path);
		Controller instance = (Controller) beans.get("/"+path.split("/")[1]);
		
		Object[] args = hand(req,resp,method);
		try {
			method.invoke(instance, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	private static Object[] hand(HttpServletRequest request,HttpServletResponse response,Method method) {
		//拿到当前执行的方法有哪些参数
		Class<?>[] paramClazzs = method.getParameterTypes();
		//根据参数个数，new 一个参数的数组，将方法里的所有参数赋值到args来
		Object[] args = new Object[paramClazzs.length];
		
		int args_i = 0;
		int index = 0;
		for(Class<?> paramClazz : paramClazzs) {
			if(ServletRequest.class.isAssignableFrom(paramClazz)) {
				args[args_i++] = request;
			}
			if(ServletResponse.class.isAssignableFrom(paramClazz)) {
				args[args_i++] = response;
			}
			//从0-3判断有没有RequestParam注解，很明显paramClazz为0和1时，不是
			//当为2和3时为@RequestParam，需要解析
			//[@com.xyc.annotation.XycRequestParam(value=name)]
			Annotation[] paramANs = method.getParameterAnnotations()[index];
			if(paramANs.length > 0) {
				for(Annotation paramAn : paramANs) {
					if(XycRequestParam.class.isAssignableFrom(paramAn.getClass())) {
						XycRequestParam rp = (XycRequestParam) paramAn;
						//找到注解里的name和age
						args[args_i++] = request.getParameter(rp.value());
					}
				}
			}
			index++;
		}
		return args;
	}
}
