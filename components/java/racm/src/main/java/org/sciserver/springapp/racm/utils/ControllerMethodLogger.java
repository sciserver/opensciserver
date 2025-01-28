package org.sciserver.springapp.racm.utils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class ControllerMethodLogger extends HandlerInterceptorAdapter {
	public static final String METHOD_NAME_ATTRIBUTE = "org.sciserver.racm.method_name";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			request.setAttribute(METHOD_NAME_ATTRIBUTE,
					handlerMethod.getBeanType().getSimpleName() + "." +
							handlerMethod.getMethod().getName());
		}

		return true;
	}

}
