package edu.sjsu.cmpe.cache.client;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.*;
import java.lang.InterruptedException;
import java.io.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.options.Options;


public class CRDTClient implements CRDTCallbackInterface {

    private ConcurrentHashMap<String, CacheServiceInterface> servers;
    private ArrayList<String> success_servers;
    private ConcurrentHashMap<String, ArrayList<String>> dict_Results;

    private static CountDownLatch countDown_Latch;

    public CRDTClient() {

        servers = new ConcurrentHashMap<String, CacheServiceInterface>(3);
        CacheServiceInterface cache = new DistributedCacheService("http://localhost:3000", this);
        CacheServiceInterface cache1 = new DistributedCacheService("http://localhost:3001", this);
        CacheServiceInterface cache2 = new DistributedCacheService("http://localhost:3002", this);
        servers.put("http://localhost:3000", cache);
        servers.put("http://localhost:3001", cache1);
        servers.put("http://localhost:3002", cache2);
    }

    @Override
    public void put_Fail(Exception e) {
    	
        System.out.println("Failed Request");
        
        countDown_Latch.countDown();
    }

    @Override
    public void put_comp(HttpResponse<JsonNode> response, String serverUrl) {
    	
        int code = response.getCode();
        
        System.out.println("Put response code " + code + " on the server " + serverUrl);
        
        success_servers.add(serverUrl);
        
        countDown_Latch.countDown();
    }

    @Override
    public void get_Fail(Exception e) {
        System.out.println("Failed Request") ;
        countDown_Latch.countDown();
    }

    @Override
    public void get_comp(HttpResponse<JsonNode> response, String serverUrl) {

        String val = null;
        
        if (response != null && response.getCode() == 200) {
            val = response.getBody().getObject().getString("value");
                System.out.println("value from server " + serverUrl + "is " + val);
            ArrayList value_servers = dict_Results.get(val);
            if (value_servers == null) {
                value_servers = new ArrayList(3);
            }
            value_servers.add(serverUrl);

            dict_Results.put(val, value_servers);
        }

        countDown_Latch.countDown();
    }


    public void delete(long key, String value) {

        for (final String serverUrl : success_servers) {
        	
            CacheServiceInterface server = servers.get(serverUrl);
            
            server.delete(key);
        }
    }

    public boolean put(long key, String value) throws InterruptedException 
    {
    
    	success_servers = new ArrayList(servers.size());
        
    	countDown_Latch = new CountDownLatch(servers.size());

        for (CacheServiceInterface cacheI : servers.values()) 
        {
        
        	cacheI.put(key, value);
        
        }

        countDown_Latch.await();

        boolean Sucess;
        
        Sucess= Math.round((float)success_servers.size() / servers.size()) == 1;

        if (! Sucess) {
            delete(key, value);
        }
        return Sucess;
    }

    

    public String get(long key) throws InterruptedException {
        dict_Results = new ConcurrentHashMap<String, ArrayList<String>>();
        countDown_Latch = new CountDownLatch(servers.size());

        for (final CacheServiceInterface server : servers.values()) {
            server.get(key);
        }
        
        
        countDown_Latch.await();

       
        String r_value = dict_Results.keys().nextElement();

        if (dict_Results.keySet().size() > 1 || dict_Results.get(r_value).size() != servers.size()) {
            
            ArrayList<String> maxValues = max_keys(dict_Results);
            if (maxValues.size() == 1) {
                r_value = maxValues.get(0);

                ArrayList<String> read_repair_servers = new ArrayList(servers.keySet());
                read_repair_servers.removeAll(dict_Results.get(r_value));
                
                for (String serverUrl : read_repair_servers) 
                {
                	
                    System.out.println("repair " + serverUrl + " value: " + r_value);
                    
                    CacheServiceInterface server = servers.get(serverUrl);
                
                    server.put(key, r_value);
                
                }

            } 
            
            else 
            {
               
            	
            	
            }
        }

        return r_value;

    }

 public ArrayList<String> max_keys(ConcurrentHashMap<String, ArrayList<String>> table) {
        ArrayList<String> keys_max= new ArrayList<String>();
        int max = -1;
        for(Map.Entry<String, ArrayList<String>> entry : table.entrySet()) 
        {
        
        	if(entry.getValue().size() > max) 
        	{
            
        		keys_max.clear(); 
                
        		keys_max.add(entry.getKey());
                
        		max = entry.getValue().size();
            }
        	
            else if(entry.getValue().size() == max)
            {
                keys_max.add(entry.getKey());
            }
        }
        return keys_max;
    }





}