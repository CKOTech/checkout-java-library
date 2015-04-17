package controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import utilities.HttpMethods;
import apiServices.sharedModels.Response;
import apiServices.sharedModels.ResponseError;

import com.google.gson.Gson;

import exception.CKOException;
import net.sf.json.JSONObject;

public class ApiHttpClient {
	HttpURLConnection connection = null;
	int httpStatus = 0;

	public void createConnection(String uri, String apiKey, String method,String query) throws IOException {

		URL url = new URL(uri);
		
		if(AppSettings.debugMode.equals("true")){		
			System.out.println("**Request**  "+method+":"+uri);
			System.out.println("");
			System.out.println("**Payload**  "+query);
		}
		
		connection = (HttpURLConnection) url.openConnection();

		connection.setConnectTimeout(Integer.parseInt(AppSettings.connectTimeout) * 1000);
		connection.setReadTimeout(Integer.parseInt(AppSettings.readTimeout) * 1000);

		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setRequestMethod(method); 
		connection.setUseCaches(false);
		connection.setInstanceFollowRedirects(true);

		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", apiKey);		
		connection.connect();
	
		if(HttpMethods.Post == method || HttpMethods.Put == method){
			
			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			
			out.write(query);
			out.flush();
			out.close();
		}
	}

	public String getQuery(JSONObject js) {
		String query = js.toString();
		return query;
	}

	
	public <T> Response<T> getResponse(Class<T> type) throws CKOException {
 
		Response<T> jsonResponse = null;
		JSONObject json = null;
		String lines = null;
		T jsonObject = null;
		Gson gson = new Gson();
		BufferedReader reader = null;

		try {
			httpStatus = connection.getResponseCode();
			
			if (this.httpStatus == 200) {
				
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				
				while ((lines = reader.readLine()) != null) {
					json = JSONObject.fromObject(lines);
				}				
				if(AppSettings.debugMode.equals("true")){	
					System.out.println("");
					System.out.println("** HttpResponse**  Status 200 OK :"+json);
				}
				
				jsonObject = gson.fromJson(json.toString(),type);
				
				jsonResponse = new Response<T>(jsonObject);
				jsonResponse.httpStatus= this.httpStatus;				
			} else{
								
				reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				
				while ((lines = reader.readLine()) != null) {
					json = JSONObject.fromObject(lines);
				}
							
				ResponseError error = gson.fromJson(json.toString(),ResponseError.class);

				jsonResponse = new Response<T>(jsonObject);

				jsonResponse.error=error;
				jsonResponse.hasError=true;
				jsonResponse.httpStatus= this.httpStatus;
				
				if(AppSettings.debugMode.equals("true")){	
					System.out.println("** HttpResponse**  StatusError: "+jsonResponse.httpStatus+json);					
				}				
				reader.close();
			}
		} catch (IOException e) {
						
			throw new CKOException(jsonResponse.error.message,jsonResponse.error.errorCode,e);
		}
		finally {
			reader=null;
			connection.disconnect();
		 }		

		return jsonResponse;
	}

}