package com.gxtourism.config;

import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.factory.Factory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 支付宝沙箱配置：启动时初始化 EasySDK 全局签名选项 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "gxtourism.alipay")
public class AlipayProperties {
    private String appId;
    private String gatewayHost;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String returnUrl;
    /** 手机网站支付的中途退出回跳地址（wap.pay 必填，缺省时回退到 returnUrl） */
    private String quitUrl;
    /**
     * 收银台通道：wap = 手机网站支付 alipay.trade.wap.pay（默认），
     * page = 电脑网站支付 alipay.trade.page.pay。
     * 沙箱网关 page.pay 通道维护期会返回 ACQ.SYSTEM_ERROR，此时 wap.pay 仍可正常打开收银台。
     * 两者共用同一套 RSA2 密钥、alipay.trade.query 查单与异步通知，不影响验签和幂等入账。
     */
    private String channel = "wap";
    /** 服务端异步通知地址（公网可达），下单时需通过 asyncNotify 显式带入 */
    private String notifyUrl;

    @PostConstruct
    public void init() {
        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = this.gatewayHost;
        config.signType = "RSA2";
        config.appId = this.appId;
        config.merchantPrivateKey = this.merchantPrivateKey;
        config.alipayPublicKey = this.alipayPublicKey;
        config.notifyUrl = this.notifyUrl;
        Factory.setOptions(config);
        log.info("支付宝沙箱初始化完成：appId={} gateway={} notifyUrl={}", appId, gatewayHost, notifyUrl);
    }
}
