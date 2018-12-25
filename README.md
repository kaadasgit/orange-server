![avatar](http://www.qt86.com/cache/1521181734_923946.png)

  orange服務端code

## http-managent

    後臺管理端rust API接口
    
    java -jar  *.jar

## http-managent

    第三方米米網對接模塊
    
     java -jar  *.jar

## http-server

    對外用戶rust APi接口
    
     java -DHTTP.SERVER.TYPE=kaadas -DHTTP.SERVER.PORT=8090 -DRATELIMITPATH=rateLimit.json  -jar  *.jar
     
     [ note ]*
           -DHTTP.SERVER.TYPE 產品類別
           -DHTTP.SERVER.PORT 啓動端口
           -DRATELIMITPATH 限流配置

## mqtt-apidao

    數據訪問層
    
     java -jar  *.jar /mqtt-apidao/keystore.jceks
     
     [ note ]*
           /mqtt-apidao/keystore.jceks 證書路徑 
     

## mqtt-auth

    mqtt連接認證
    
     java -jar  *.jar

## mqtt-common

    項目公共組件和工具類、包

## mqtt-job

    服務的定時任務model
    
     java -jar  *.jar

## mqtt-message

    消息推送，message收發model
    
     java -DpushDev=false -Xbootclasspath/p:/alpn-boot-8.1.11.v20170118.jar  -jar  *.jar
     
     [ note ]*
             -DpushDev 推送屬性
             -Xbootclasspath/p:  http2協議包


## mqtt-publish

    mqtt業務處理model
    
     java -jar  *.jar
     
     

## mqtt-server

    mqtt服務器
    
     java -jar  *.jar  -c ./config.json  -hc ./zkConf.json
     
     [ note ]*
        -c 配置文件
        -hc 集羣配置
         
    

## reg-addr

    事件總線(EventBus)的注冊地址
    
## rtp-server

    rtp(video、audio)relay服務器
    
     java -jar  *.jar

## sip-server

    sip協議信令服務器
    
     java -jar  *.jar



