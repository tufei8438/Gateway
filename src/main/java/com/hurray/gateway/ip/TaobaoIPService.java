package com.hurray.gateway.ip;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class TaobaoIPService implements IPService {

    @Override
    public Region getRegion(String ip) {
        String url = String.format("http://ip.taobao.com/service/getIpInfo.php?ip=%s", ip);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        Region region = new Region();
        region.setCity("未知");
        try {
            Response response = client.newCall(request).execute();
            String jsonText = response.body().string();
            JSONObject jsonObject = JSON.parseObject(jsonText);
            if (jsonObject.containsKey("code") && jsonObject.getIntValue("code") == 0) {
                region.setCity(jsonObject.getString("city"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return region;
    }
}
