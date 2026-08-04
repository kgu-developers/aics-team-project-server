package kgu.developers.globalutils.logging;

import java.util.regex.Pattern;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class HttpReqResUtils {
	private static final String[] IP_HEADER_CANDIDATES = {
		"X-Forwarded-For",
		"Proxy-Client-IP",
		"WL-Proxy-Client-IP",
		"HTTP_X_FORWARDED_FOR",
		"HTTP_X_FORWARDED",
		"HTTP_X_CLUSTER_CLIENT_IP",
		"HTTP_CLIENT_IP",
		"HTTP_FORWARDED_FOR",
		"HTTP_FORWARDED",
		"HTTP_VIA",
		"REMOTE_ADDR"
	};

	private static final Pattern IPV4_PATTERN = Pattern.compile(
		"^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
	// ponytail: IPv6는 문자 구성만 검사한다. 로그 위조를 막는 목적이므로 정밀 검증이 필요해지면 그때 강화.
	private static final Pattern IPV6_PATTERN = Pattern.compile("^[0-9A-Fa-f:]{2,45}$");

	public static String getClientIpAddressIfServletRequestExist() {
		if (RequestContextHolder.getRequestAttributes() == null) {
			return "0.0.0.0";
		}
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		for (String header : IP_HEADER_CANDIDATES) {
			String ipList = request.getHeader(header);
			if (ipList == null || ipList.isEmpty() || "unknown".equalsIgnoreCase(ipList)) {
				continue;
			}
			String ip = ipList.split(",")[0].trim();
			if (isValidIp(ip)) {
				return ip;
			}
		}
		return request.getRemoteAddr();
	}

	// 프록시 헤더는 클라이언트가 임의로 보낼 수 있으므로 IP 형식이 아니면 로그에 넣지 않는다.
	private static boolean isValidIp(String ip) {
		return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
	}
}
