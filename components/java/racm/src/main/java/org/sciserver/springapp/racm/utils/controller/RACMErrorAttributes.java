package org.sciserver.springapp.racm.utils.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RACMErrorAttributes extends DefaultErrorAttributes {
	public RACMErrorAttributes() {
		  // TODO fix
	}

//	@Override
	public Map<String, Object> getErrorAttributes(WebRequest webRequest,
		boolean includeStackTrace) {
		Map<String, Object> defaultAttributes = null;
//		Map<String, Object> defaultAttributes = super.getErrorAttributes(webRequest, includeStackTrace);
// TODO test whether next replaces this properly
		if(includeStackTrace)
			defaultAttributes = super.getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));
		else
			defaultAttributes = super.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());
		Map<String, Object> output = new HashMap<>(2);

		output.put("status", "error");
		output.put("error", defaultAttributes.get("message"));

		return output;
	}
}
