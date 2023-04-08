package com.alipay.alipaytest.controller;


import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static com.alipay.alipaytest.config.PaymentConstants.*;

@RequestMapping("/alipay")
@Controller
public class AlipayTestController {

    /**
     * 统一收单下单并支付页面接口
     */
    @RequestMapping("/app_pay")
    @ResponseBody
    public String app() throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(URL,
                APPID,
                PRIVATE_KEY,
                FORMAT,
                CHARSET,
                ALIPAY_PUBLIC_KEY,
                SIGN_TYPE);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //异步接收地址，仅支持http/https，公网可访问
        request.setNotifyUrl(PC_CALLBACK);
        //同步跳转地址，仅支持http/https
        request.setReturnUrl(PC_RETURN);
        /******必传参数start******/
        JSONObject bizContent = new JSONObject();
        //商户订单号，商家自定义，保持唯一性
        bizContent.put("out_trade_no", System.currentTimeMillis());
        //支付金额，最小值0.01元
        bizContent.put("total_amount", 0.01);
        //订单标题，不可使用特殊符号
        bizContent.put("subject", "测试商品");
        //电脑网站支付场景固定传值FAST_INSTANT_TRADE_PAY
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        /******必传参数end******/

        //超时时间 1m-15d
        bizContent.put("timeout_express", "5m");

        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = alipayClient.pageExecute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return response.getBody();
    }

    /**
     * 异步回调更新本地订单状态
     */
    @RequestMapping("callback")
    @ResponseBody
    public String callback(HttpServletRequest request) throws AlipayApiException {

        Map<String, String[]> paramsMap = request.getParameterMap();
        HashMap<String, String> hashMap = new HashMap<>();
        for (String s : paramsMap.keySet()) {
            hashMap.put(s, paramsMap.get(s)[0]);
        }

        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(hashMap, ALIPAY_PUBLIC_KEY, CHARSET, SIGN_TYPE);
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            System.out.println("异步调用成功");
            AlipayTradeQueryResponse tradeQueryResponse = query(hashMap.get("out_trade_no"));
            if (!tradeQueryResponse.getTotalAmount().equals(hashMap.get("total_amount"))) {
                return "failure";
            }
            if (tradeQueryResponse.getTradeStatus().equals("TRADE_SUCCESS")) {
                System.out.println("更新数据库订单");
            }
            return "success";
        } else {
            System.out.println("异步调用失败");
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }


    }

    /**
     * 统一收单交易查询
     */
    public AlipayTradeQueryResponse query(String tradeNo) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(URL, APPID, PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", tradeNo);
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
        }
        return response;
    }

    @RequestMapping("pc_return")
    @ResponseBody
    public String pcReturn() {
        System.out.println("同步调用成功");
        return "同步调用成功";
    }

    @RequestMapping("pass_back")
    public void passBack() {
        System.out.println("授权回调地址");
    }

}
