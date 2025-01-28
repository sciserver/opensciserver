package org.sciserver.springapp.racm.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessDeniedController {
	@GetMapping("/accessdenied")
	public String accessDenied() {
		return "AccessDenied";
	}
}
