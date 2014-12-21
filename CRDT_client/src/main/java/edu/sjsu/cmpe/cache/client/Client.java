package edu.sjsu.cmpe.cache.client;

import com.mashape.unirest.http.Unirest;

import java.util.*;
import java.lang.*;
import java.io.*;

public class Client {

    public static void main(String[] args) throws Exception {
    	
        System.out.println("Starting Cache Client...");
        
        CRDTClient CRDTClient = new CRDTClient();

        
        boolean result = CRDTClient.put(1, "a");
        
        System.out.println("result is " + result);
        
        Thread.sleep(30000);
        
        System.out.println("put(1 => a); sleeping for 30s");

//InteliJ
       
        CRDTClient.put(1, "b");
        
        Thread.sleep(30000);
        
        System.out.println("put(1 => b); sleeping for 30s");

        
        String value = CRDTClient.get(1);
        
        System.out.println("get(1) => " + value);

        System.out.println("Exiting Client...");
        
        Unirest.shutdown();
    }

}
