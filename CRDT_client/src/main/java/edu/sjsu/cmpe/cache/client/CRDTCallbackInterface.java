package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

public interface CRDTCallbackInterface {

	void get_Fail (Exception e);
	
    void get_comp (HttpResponse<JsonNode> response, String serverUrl);

    void put_comp (HttpResponse<JsonNode> response, String serverUrl);
    
    void put_Fail (Exception e);
    
}