package com.hurray.gateway;

import com.hurray.gateway.ip.Region;
import com.hurray.gateway.ip.TaobaoIPService;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class GatewayServlet extends HttpServlet {

    private static Map<String, String> proxyCityServerMap = new HashMap<>();

    static {
        proxyCityServerMap.put("北京市", "http://www.csdn.net");
        proxyCityServerMap.put("上海市", "http://www.oschina.net");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String remoteIp = getRemoteAddr(req);
        Region region = new TaobaoIPService().getRegion(remoteIp);

        String proxyUrl = "http://www.oschina.net";
        if (proxyCityServerMap.containsKey(region.getCity())) {
            proxyUrl = proxyCityServerMap.get(region.getCity());
        }
        RequestProxy requestProxy = new RequestProxy(proxyUrl, req, resp);
        requestProxy.doProxy();
    }

    /**
     * 获取客户端的Reomote地址，增加了Nginx的代理模式的支持
     *
     * @param req
     * @return 客户端访问的IP地址
     */
    private String getRemoteAddr(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");

        if (!StringUtils.isEmpty(ip)) {
            String[] ips = StringUtils.split(ip, ",");
            if (ips != null) {
                for (String tmpip : ips) {
                    if (StringUtils.isEmpty(tmpip))
                        continue;
                    tmpip = tmpip.trim();
                    if (isIPAddr(tmpip) && !"127.0.0.1".equals(tmpip)) {
                        return tmpip.trim();
                    }
                }
            }
        }

        ip = req.getHeader("X-Real-IP");
        if(isIPAddr(ip)) {
            return ip;
        }
        return req.getRemoteAddr();
    }

    /**
     * 判断字符串是否是合法的IP地址
     *
     * @param addr IP字符串
     * @return true 如果是合法的IP地址，否则返回false
     */
    private static boolean isIPAddr(String addr) {
        if (StringUtils.isEmpty(addr))
            return false;

        String[] ips = StringUtils.split(addr, ".");
        if (ips.length != 4)
            return false;

        try {
            int ipa = Integer.parseInt(ips[0]);
            int ipb = Integer.parseInt(ips[1]);
            int ipc = Integer.parseInt(ips[2]);
            int ipd = Integer.parseInt(ips[3]);
            return ipa >= 0 && ipa <= 255 && ipb >= 0 && ipb <= 255 && ipc >= 0
                    && ipc <= 255 && ipd >= 0 && ipd <= 255;
        } catch (Exception e) {
            return false;
        }
    }
}
