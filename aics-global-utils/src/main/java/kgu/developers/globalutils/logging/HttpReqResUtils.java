package kgu.developers.globalutils.logging;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class HttpReqResUtils {
	public static String getClientIpAddressIfServletRequestExist() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return "0.0.0.0";
		}

		HttpServletRequest request =
			((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();

		return request.getRemoteAddr();
	}
}
