package cn.orangeiot.message.sms;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

class SmsSenderUtil {

    protected Random random = new Random();
    
    public String stringMD5(String input) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] inputByteArray = input.getBytes();
        messageDigest.update(inputByteArray);
        byte[] resultByteArray = messageDigest.digest();
        return byteArrayToHex(resultByteArray);
    }
    
    protected String strToHash(String str) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] inputByteArray = str.getBytes();
        messageDigest.update(inputByteArray);
        byte[] resultByteArray = messageDigest.digest();
        return byteArrayToHex(resultByteArray);
    }

    public String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        return new String(resultCharArray);
    }
    
    public int getRandom() {
    	return random.nextInt(999999)%900000+100000;
    }
    
    public HttpURLConnection getPostHttpConn(String url) throws Exception {
        URL object = new URL(url);
        HttpURLConnection conn;
        conn = (HttpURLConnection) object.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestMethod("POST");
        return conn;
	}
    
    public String calculateSig(
    		String appkey,
    		long random,
    		String msg,
    		long curTime,    		
    		ArrayList<String> phoneNumbers) throws NoSuchAlgorithmException {
        String phoneNumbersString = phoneNumbers.get(0);
        for (int i = 1; i < phoneNumbers.size(); i++) {
            phoneNumbersString += "," + phoneNumbers.get(i);
        }
        return strToHash(String.format(
        		"appkey=%s&random=%d&time=%d&mobile=%s",
        		appkey, random, curTime, phoneNumbersString));
    }
    
    public String calculateSigForTempl(
    		String appkey,
    		long random,
    		long curTime,    		
    		ArrayList<String> phoneNumbers) throws NoSuchAlgorithmException {
        String phoneNumbersString = phoneNumbers.get(0);
        for (int i = 1; i < phoneNumbers.size(); i++) {
            phoneNumbersString += "," + phoneNumbers.get(i);
        }
        return strToHash(String.format(
        		"appkey=%s&random=%d&time=%d&mobile=%s",
        		appkey, random, curTime, phoneNumbersString));
    }
    
    public String calculateSigForTempl(
    		String appkey,
    		long random,
    		long curTime,    		
    		String phoneNumber) throws NoSuchAlgorithmException {
    	ArrayList<String> phoneNumbers = new ArrayList<>();
    	phoneNumbers.add(phoneNumber);
    	return calculateSigForTempl(appkey, random, curTime, phoneNumbers);
    }
    
    public JsonArray phoneNumbersToJSONArray(String nationCode, ArrayList<String> phoneNumbers) {
        JsonArray tel = new JsonArray();
        int i = 0;
        do {
            JsonObject telElement = new JsonObject();
            telElement.put("nationcode", nationCode);
            telElement.put("mobile", phoneNumbers.get(i));
            tel.add(telElement);
        } while (++i < phoneNumbers.size());

        return tel;
    }
    
    public JsonArray smsParamsToJSONArray(ArrayList<String> params) {
		JsonArray smsParams = new JsonArray();
        for (int i = 0; i < params.size(); i++) {
        	smsParams.add(params.get(i));
		}
        return smsParams;
    }
    
    public SmsSingleSenderResult jsonToSmsSingleSenderResult(JsonObject json) {
    	SmsSingleSenderResult result = new SmsSingleSenderResult();
    	
    	result.result = json.getInteger("result");
    	result.errMsg = json.getString("errmsg");
    	if (0 == result.result) {
	    	result.ext = json.getString("ext");
	    	result.sid = json.getString("sid");
	    	result.fee = json.getInteger("fee");
    	}
    	return result;
    }
    
    public SmsMultiSenderResult jsonToSmsMultiSenderResult(JsonObject json) {
    	SmsMultiSenderResult result = new SmsMultiSenderResult();
    	
    	result.result = json.getInteger("result");
    	result.errMsg = json.getString("errmsg");    	
    	if (false == json.getBoolean("ext")) {
    		result.ext = json.getString("ext");	
    	}
    	if (0 != result.result) {
    		return result;
    	}
    	
    	result.details = new ArrayList<>();    	
    	JsonArray details = json.getJsonArray("detail");
    	for (int i = 0; i < details.size(); i++) {
    		JsonObject jsonDetail = details.getJsonObject(i);
    		SmsMultiSenderResult.Detail detail = result.new Detail();
    		detail.result = jsonDetail.getInteger("result");
    		detail.errMsg = jsonDetail.getString("errmsg");
    		if (0 == detail.result) {	    		
	    		detail.phoneNumber = jsonDetail.getString("mobile");
	    		detail.nationCode = jsonDetail.getString("nationcode");
	    		if (false == jsonDetail.getBoolean("sid")) {
	    			detail.sid = jsonDetail.getString("sid");	
	    		}		
	    		detail.fee = jsonDetail.getInteger("fee");
    		}
    		result.details.add(detail);
    	}
    	return result;
    }
    

    public SmsStatusPullCallbackResult jsonToSmsStatusPullCallbackrResult(JsonObject json) {
    	SmsStatusPullCallbackResult result = new SmsStatusPullCallbackResult();
    	
    	result.result = json.getInteger("result");
    	result.errmsg = json.getString("errmsg");    	
    	if (true == json.getBoolean("data")) {
    		return result;
    	}
    	result.callbacks = new ArrayList<>();
		JsonArray datas  = json.getJsonArray("data");
    	for(int index = 0 ; index< datas.size(); index++){
			    JsonObject cb = datas.getJsonObject(index);
    			SmsStatusPullCallbackResult.Callback callback = result.new Callback();
    			callback.user_receive_time = cb.getString("user_receive_time");
    			callback.nationcode = cb.getString("nationcode");
    			callback.mobile = cb.getString("mobile");
    			callback.report_status = cb.getString("report_status");
    			callback.errmsg = cb.getString("errmsg");
    			callback.description = cb.getString("description");
    			callback.sid = cb.getString("sid");
    			result.callbacks.add(callback);
    	}
    	return result;
    }
    

    public SmsStatusPullReplyResult jsonToSmsStatusPullReplyResult(JsonObject json){
    	SmsStatusPullReplyResult result = new SmsStatusPullReplyResult();
    	
    	result.result = json.getInteger("result");
    	result.errmsg = json.getString("errmsg");
    	result.count = json.getInteger("count");
    	
    	if (true == json.getBoolean("data")) {
    		return result;
    	}
    	
    	result.replys = new ArrayList<>();  
    	JsonArray datas  = json.getJsonArray("data");
    	for(int index = 0 ; index< datas.size(); index++){
			    JsonObject reply_json = datas.getJsonObject(index);
    			SmsStatusPullReplyResult.Reply reply = result.new Reply();
    			reply.nationcode = reply_json.getString("nationcode");
    		  	reply.mobile = reply_json.getString("mobile");
    		  	reply.sign = reply_json.getString("sign");
    	    	reply.text = reply_json.getString("text"); 
    	    	reply.time = reply_json.getLong("time"); 
    			result.replys.add(reply);
    	}
    	
    	return result;
    }
    public SmsVoiceVerifyCodeSenderResult jsonToSmsVoiceVerifyCodeSenderResult(JsonObject json){
    	SmsVoiceVerifyCodeSenderResult result = new SmsVoiceVerifyCodeSenderResult();
    	result.result = json.getInteger("result");
    	if (false == json.getBoolean("errmsg")) {
    		result.errmsg = json.getString("errmsg");
    	}
    	if (0 == result.result) {
    		result.ext = json.getString("ext");
    		result.callid = json.getString("callid");
    	}
    	return result;
    }

    public SmsVoicePromptSenderResult jsonToSmsVoicePromptSenderResult(JsonObject json){
    	SmsVoicePromptSenderResult result = new SmsVoicePromptSenderResult();
    	result.result = json.getInteger("result");
    	if (false == json.getBoolean("errmsg")) {
    		result.errmsg = json.getString("errmsg");
    	}
    	if (0 == result.result) {
    		result.ext = json.getString("ext");
    		result.callid = json.getString("callid");
    	}
    	return result;
    }
}
